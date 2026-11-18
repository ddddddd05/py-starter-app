"""判题：对错由程序决定，不交给 AI 猜。

- 选择题：比对选项下标
- 填空题：去掉空格后和可接受答案比对
- 写代码：先真的运行、比对输出，再检查是否用了这一关要练的写法（如必须用 for 循环）
"""
from __future__ import annotations

import ast

from .lessons import get_lesson
from .runner import run

# must_use 里的名字 → 在语法树里怎么认
NODE_RULES = {
    "for": (ast.For,), "while": (ast.While,), "if": (ast.If,), "def": (ast.FunctionDef,),
    "return": (ast.Return,), "assign": (ast.Assign, ast.AugAssign),
}
NAME_LABELS = {"for": "for 循环", "while": "while 循环", "if": "if 判断", "def": "def 定义函数",
               "return": "return 返回结果", "assign": "变量赋值（=）", "print": "print", "input": "input"}


def uses(code: str, feature: str) -> bool:
    try:
        tree = ast.parse(code)
    except SyntaxError:
        return False
    if feature in NODE_RULES:
        return any(isinstance(n, NODE_RULES[feature]) for n in ast.walk(tree))
    # 其余当作函数名：print、input……
    return any(isinstance(n, ast.Call) and isinstance(n.func, ast.Name) and n.func.id == feature
               for n in ast.walk(tree))


def normalize_output(text: str) -> str:
    """忽略每行末尾多余的空格和结尾多余的空行，初学者不该因为这个被判错。"""
    lines = [line.rstrip() for line in text.replace("\r\n", "\n").split("\n")]
    while lines and lines[-1] == "":
        lines.pop()
    return "\n".join(lines)


def check(lesson_id: int, index: int, answer) -> dict:
    """返回 {"correct", "feedback", "output", "error"}。"""
    q = get_lesson(lesson_id)["questions"][index]
    if q["type"] == "choice":
        try:
            correct = int(answer) == q["answer"]
        except (TypeError, ValueError):
            correct = False
        return {"correct": correct, "output": "", "error": None,
                "feedback": "答对了！" if correct else "不对哦，再想想，或者点“问老师”。"}

    if q["type"] == "fill":
        given = "".join(str(answer).split())
        correct = any(given == "".join(a.split()) for a in q["answers"])
        return {"correct": correct, "output": "", "error": None,
                "feedback": "答对了！" if correct else "还不对，看看提示再试一次。"}

    code = str(answer)
    if not code.strip():
        return {"correct": False, "output": "", "error": None, "feedback": "还没有写代码哦。"}
    result = run(code, q.get("inputs"))
    if not result["ok"]:
        return {"correct": False, "output": result["output"], "error": result["error"],
                "feedback": result["error"]["message"]}
    # 判题只看程序自己 print 的内容；回显的输入只用于显示
    if normalize_output(result["program_output"]) != normalize_output(q["expected_output"]):
        return {"correct": False, "output": result["output"], "error": None,
                "feedback": "程序能运行，但显示的内容和要求不一样。对比一下“你的输出”和题目要求。",
                "expected": q["expected_output"]}
    missing = [f for f in q.get("must_use", []) if not uses(code, f)]
    if missing:
        need = "、".join(NAME_LABELS.get(f, f) for f in missing)
        return {"correct": False, "output": result["output"], "error": None,
                "feedback": f"结果对了！不过这一关要练习 {need}，试着用它来写。"}
    return {"correct": True, "output": result["output"], "error": None, "feedback": "完全正确！"}
