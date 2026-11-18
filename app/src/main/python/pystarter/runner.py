"""在手机上运行学生写的代码。

三道防线（教学用途，不等于真正的沙箱）：
1. 运行前做语法树检查：不允许 import、不允许访问双下划线属性、不允许 open / eval / exec 这类危险函数；
2. 运行时只提供一小份“白名单”内置函数，print 和 input 换成我们自己的版本（输出存起来，输入用题目给的）；
3. 用 sys.settrace 数执行了多少行，超过上限或超时就停下来——初学者最常写出的死循环不会把 App 卡死。

报错会翻译成大白话，并指出是第几行。
"""
from __future__ import annotations

import ast
import builtins
import sys
import time

MAX_STEPS = 200_000          # 最多执行多少行（正常练习题只需要几十到几千行）
MAX_SECONDS = 2.0
MAX_OUTPUT = 10_000          # 输出最多保留多少个字
MAX_RANGE_LEN = 100_000      # 防止误写 list(range(10**9)) 导致手机端内存溢出

FORBIDDEN_NAMES = {"open", "eval", "exec", "compile", "__import__", "globals", "locals", "vars",
                   "getattr", "setattr", "delattr", "exit", "quit", "breakpoint", "help", "memoryview"}


def _safe_range(*args):
    r = builtins.range(*args)
    if len(r) > MAX_RANGE_LEN:
        raise StopRun("range")
    return r


SAFE_BUILTINS = {name: getattr(builtins, name)
                 for name in ("len", "int", "float", "str", "bool", "list", "dict", "tuple", "set",
                              "sum", "min", "max", "abs", "round", "sorted", "enumerate", "zip", "isinstance",
                              "type", "reversed", "map", "filter", "any", "all", "ord", "chr",
                              "Exception", "ValueError", "TypeError", "IndexError", "KeyError",
                              "ZeroDivisionError", "True", "False", "None")}
SAFE_BUILTINS["range"] = _safe_range


class StopRun(Exception):
    """运行太久（多半是死循环）或输出太多。"""


class NeedInput(Exception):
    """程序要求输入，但题目给的输入已经用完。"""


def check_code(code: str) -> str | None:
    """运行前检查。有问题返回给学生看的说明，没问题返回 None。语法错误交给运行阶段统一翻译。"""
    try:
        tree = ast.parse(code)
    except SyntaxError:
        return None
    for node in ast.walk(tree):
        if isinstance(node, (ast.Import, ast.ImportFrom)):
            return f"第 {node.lineno} 行：练习里不需要 import，先用学过的内容完成吧。"
        if isinstance(node, ast.Attribute) and node.attr.startswith("__"):
            return f"第 {node.lineno} 行：不能使用双下划线开头的属性。"
        if isinstance(node, ast.Name) and node.id in FORBIDDEN_NAMES:
            return f"第 {node.lineno} 行：练习里不能使用 {node.id}。"
    return None


def run(code: str, inputs: list[str] | None = None) -> dict:
    """运行代码，返回 {"ok", "output", "error"}；error 是 {"kind", "line", "message"} 或 None。"""
    problem = check_code(code)
    if problem:
        return {"ok": False, "output": "", "program_output": "",
                "error": {"kind": "forbidden", "line": None, "message": problem}}

    out: list[str] = []        # 给学生看的：程序输出 + 回显的输入（像真正的终端）
    printed: list[str] = []    # 判题用的：只有程序自己 print 的内容
    size = [0]
    pending = list(inputs or [])

    def fake_print(*args, sep=" ", end="\n"):
        text = sep.join(str(a) for a in args) + end
        size[0] += len(text)
        if size[0] > MAX_OUTPUT:
            raise StopRun("output")
        out.append(text)
        printed.append(text)

    def fake_input(prompt=""):
        if prompt:
            fake_print(prompt, end="")
        if not pending:
            raise NeedInput()
        value = pending.pop(0)
        out.append(value + "\n")          # 回显输入只用于显示，不算程序的输出
        return value

    env = {"__builtins__": {**SAFE_BUILTINS, "print": fake_print, "input": fake_input},
           "__name__": "__main__"}
    steps = [0]
    started = time.monotonic()

    def tracer(frame, event, arg):
        if event == "line" and frame.f_code.co_filename == "<学生代码>":
            steps[0] += 1
            if steps[0] > MAX_STEPS or (steps[0] % 1000 == 0 and time.monotonic() - started > MAX_SECONDS):
                raise StopRun("timeout")
        return tracer

    error = None
    old = sys.gettrace()
    try:
        compiled = compile(code, "<学生代码>", "exec")
        sys.settrace(tracer)
        try:
            exec(compiled, env)
        finally:
            sys.settrace(old)
    except BaseException as e:  # noqa: BLE001  学生代码里任何异常都要变成友好提示，不能让 App 崩溃
        error = explain(e, code)
    return {"ok": error is None, "output": "".join(out), "program_output": "".join(printed), "error": error}


def _line_of(e: BaseException) -> int | None:
    if isinstance(e, SyntaxError):
        return e.lineno
    tb = e.__traceback__
    line = None
    while tb is not None:
        if tb.tb_frame.f_code.co_filename == "<学生代码>":
            line = tb.tb_lineno
        tb = tb.tb_next
    return line


def explain(e: BaseException, code: str) -> dict:
    """把 Python 报错翻译成初学者看得懂的话。"""
    line = _line_of(e)
    where = f"第 {line} 行：" if line else ""
    text = code.splitlines()[line - 1] if line and 0 < line <= len(code.splitlines()) else ""
    kind = type(e).__name__

    if isinstance(e, StopRun):
        if str(e) == "range":
            msg = f"range( ) 范围太大了（超过 {MAX_RANGE_LEN:,}），练习里不需要这么大的数字，检查一下是不是多打了个 0。"
        elif str(e) == "output":
            msg = "输出的内容太多了，是不是循环停不下来？"
        else:
            msg = "程序运行太久被停下了，很可能是死循环：检查循环条件会不会变成“不成立”。"
        return {"kind": "timeout", "line": line if str(e) == "range" else None, "message": msg}
    if isinstance(e, NeedInput):
        return {"kind": "input", "line": line, "message": f"{where}程序在等你输入，但这道题只准备了这么多输入，检查一下是不是多写了 input()。"}
    if isinstance(e, IndentationError):
        msg = "缩进不对。冒号下面的代码要往右缩进 4 个空格，同一层的代码要对齐。"
    elif isinstance(e, SyntaxError):
        stripped = text.strip()
        if stripped.startswith(("if ", "for ", "while ", "def ", "else", "elif ")) and not stripped.endswith(":"):
            msg = "这一行末尾好像少了英文冒号 : 。"
        elif text.count('"') % 2 == 1 or text.count("'") % 2 == 1:
            msg = "引号没有配对，文字要用一对英文引号包起来。"
        elif text.count("(") != text.count(")"):
            msg = "括号没有配对，检查一下左括号和右括号的数量。"
        elif any(ch in text for ch in "“”‘’（）：，"):
            msg = "这一行用了中文符号。代码里的引号、括号、冒号、逗号要用英文的（文字内容里可以用中文）。"
        else:
            msg = "写法不符合 Python 的规则，对照例子检查一下拼写和符号。"
    elif isinstance(e, NameError):
        name = getattr(e, "name", None) or ""
        msg = (f"电脑不认识 {name}。如果它是一段文字，要加引号；如果是变量，检查一下名字是不是写错了、"
               f"有没有先赋值。")
    elif isinstance(e, TypeError) and "concatenate" in str(e):
        msg = "文字和数字不能直接用 + 拼在一起，先用 str( ) 把数字变成文字。"
    elif isinstance(e, TypeError) and "unsupported operand" in str(e):
        msg = "这两样东西不能这样运算，检查一下是不是把文字当成数字用了（可以用 int( ) 转换）。"
    elif isinstance(e, ZeroDivisionError):
        msg = "不能除以 0。"
    elif isinstance(e, IndexError):
        msg = "列表里没有这个位置。别忘了位置从 0 开始数，最后一个位置是“长度 - 1”。"
    elif isinstance(e, KeyError):
        msg = f"字典里没有 {e} 这个键，检查一下名字和引号。"
    elif isinstance(e, ValueError) and "int()" in str(e):
        msg = "int( ) 只能把像数字的文字变成整数，例如 \"12\"，不能转换 \"abc\"。"
    else:
        msg = f"程序出错了（{kind}）：{e}"
    return {"kind": kind, "line": line, "message": where + msg}
