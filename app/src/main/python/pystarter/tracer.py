"""单步慢放：把学生代码一行一行地执行，记录每一步的“现场”，给手机上的可视化调试页面用。

每一步记录：
- line：马上要执行的是第几行（高亮这一行）
- vars：此刻学生自己的变量（变量背包），值用 repr 显示，太长就截断
- output：到这一步为止屏幕上的全部输出
- new_output：这一步刚刚新增的输出（有新输出时，像素小骑士砍一刀）
- loop：如果这一行在循环里，是第几轮（给“for 一次打 5 只史莱姆”这类动画用）

安全限制和 runner.run 一样：先做语法树检查，只给白名单内置函数，限制步数和时间。
"""
from __future__ import annotations

import ast
import sys
import time

from .runner import SAFE_BUILTINS, NeedInput, StopRun, check_code, explain

MAX_TRACE_STEPS = 400        # 慢放最多记录多少步：再多就没法一步步看了
MAX_SECONDS = 2.0
MAX_VALUE_LEN = 40
FILENAME = "<学生代码>"


def _short(value) -> str:
    text = repr(value)
    return text if len(text) <= MAX_VALUE_LEN else text[: MAX_VALUE_LEN - 1] + "…"


def _user_vars(frame) -> dict:
    """只显示学生自己定义的变量：去掉内置函数、自己定义的函数本身和以下划线开头的名字。"""
    scope = frame.f_locals if frame.f_code.co_name != "<module>" else frame.f_globals
    result = {}
    for name, value in scope.items():
        if name.startswith("_") or callable(value):
            continue
        result[name] = _short(value)
    return result


def _loop_lines(code: str) -> dict[int, int]:
    """每一行属于哪个循环（循环头所在的行号），用来数“第几轮”。"""
    try:
        tree = ast.parse(code)
    except SyntaxError:
        return {}
    owner: dict[int, int] = {}
    for node in ast.walk(tree):
        if isinstance(node, (ast.For, ast.While)):
            for child in node.body:
                for sub in ast.walk(child):
                    if hasattr(sub, "lineno"):
                        owner.setdefault(sub.lineno, node.lineno)
    return owner


def trace(code: str, inputs: list[str] | None = None) -> dict:
    """返回 {"ok", "steps", "error", "truncated"}。steps 是逐步的现场记录。"""
    problem = check_code(code)
    if problem:
        return {"ok": False, "steps": [], "truncated": False,
                "error": {"kind": "forbidden", "line": None, "message": problem}}

    out: list[str] = []
    pending = list(inputs or [])
    steps: list[dict] = []
    loop_owner = _loop_lines(code)
    loop_round: dict[int, int] = {}
    last_len = [0]
    truncated = [False]
    started = time.monotonic()

    def fake_print(*args, sep=" ", end="\n"):
        out.append(sep.join(str(a) for a in args) + end)

    def fake_input(prompt=""):
        if prompt:
            out.append(str(prompt))
        if not pending:
            raise NeedInput()
        value = pending.pop(0)
        out.append(value + "\n")
        return value

    def record(frame, line):
        text = "".join(out)
        owner = loop_owner.get(line)
        if line in {o for o in loop_owner.values()}:
            # 回到循环头：开始新的一轮
            loop_round[line] = loop_round.get(line, 0) + 1
        steps.append({
            "line": line,
            "vars": _user_vars(frame),
            "output": text,
            "new_output": text[last_len[0]:],
            "loop": loop_round.get(owner) if owner else None,
            "func": None if frame.f_code.co_name == "<module>" else frame.f_code.co_name,
        })
        last_len[0] = len(text)

    def tracer(frame, event, arg):
        if frame.f_code.co_filename != FILENAME:
            return tracer
        if event == "line":
            if len(steps) >= MAX_TRACE_STEPS:
                truncated[0] = True
                raise StopRun("steps")
            if time.monotonic() - started > MAX_SECONDS:
                raise StopRun("timeout")
            record(frame, frame.f_lineno)
        return tracer

    env = {"__builtins__": {**SAFE_BUILTINS, "print": fake_print, "input": fake_input}, "__name__": "__main__"}
    error = None
    old = sys.gettrace()
    try:
        compiled = compile(code, FILENAME, "exec")
        sys.settrace(tracer)
        try:
            exec(compiled, env)
        finally:
            sys.settrace(old)
    except StopRun as e:
        if str(e) != "steps":
            error = explain(e, code)
    except BaseException as e:  # noqa: BLE001
        error = explain(e, code)

    # 最后一步：程序结束后的现场（最终的变量和输出）
    final_text = "".join(out)
    final_vars = {k: _short(v) for k, v in env.items()
                  if not k.startswith("_") and not callable(v)}
    steps.append({"line": None, "vars": final_vars, "output": final_text,
                  "new_output": final_text[last_len[0]:], "loop": None, "func": None})
    return {"ok": error is None, "steps": steps, "error": error, "truncated": truncated[0]}
