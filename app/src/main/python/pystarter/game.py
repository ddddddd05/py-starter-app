"""闯关玩法用到的内容：章节、学习卡片、学习阶段的小练习、怪物、错因标签、NPC 台词。

和 lessons.py 分开：lessons.py 只放“教什么、考什么”，这里放“怎么包装成游戏”。
题目、判题、AI 老师都不变，游戏层只是换一种方式呈现。
"""
from __future__ import annotations

import re

from .lessons import LESSONS, get_lesson

# ---------- 章节：每 4 关一章，每章最后一关是章节 Boss ----------
CHAPTERS: list[dict] = [
    {"id": 1, "title": "新手村", "lessons": [1, 2, 3, 4], "theme": "village"},
    {"id": 2, "title": "迷雾森林", "lessons": [5, 6, 7, 8], "theme": "forest"},
    {"id": 3, "title": "代码城堡", "lessons": [9, 10, 11, 12], "theme": "castle"},
]

# 每章的三种敌人：小怪（选择题）、精英（填空题）、Boss（写代码题）
ENEMIES = {
    "village": ["史莱姆", "淘气蘑菇", "稻草人队长"],
    "forest": ["迷雾蝙蝠", "树精", "森林巨狼"],
    "castle": ["骷髅兵", "幽灵骑士", "黑龙"],
}
STAGE_NAMES = ["小怪", "精英", "Boss"]
BOSS_TITLES = {4: "章节 Boss：稻草人之王", 8: "章节 Boss：迷雾巨狼王", 12: "最终 Boss：代码黑龙"}

# ---------- 学习阶段的小练习：每关 1 道，很简单，答错不扣血，只是确认学会了 ----------
# 题目代码会在测试里真的运行，保证正确选项和真实输出一致。
PRACTICE: dict[int, dict] = {
    1: {"prompt": "下面哪一行能让电脑显示“你好”？", "code": "",
        "options": ['print("你好")', 'say("你好")', "print(你好)"], "answer": 0, "run": 'print("你好")',
        "explain": "要用 print，并且文字要放在英文引号里。"},
    2: {"prompt": "运行后会显示什么？", "code": 'pet = "小猫"\nprint(pet)',
        "options": ["pet", "小猫"], "answer": 1,
        "explain": "print(pet) 显示的是盒子 pet 里装的东西，也就是“小猫”。"},
    3: {"prompt": "运行后会显示什么？", "code": "print(2 * 3)",
        "options": ["6", "2 * 3", "23"], "answer": 0,
        "explain": "* 是乘号，2 * 3 = 6；数字不加引号，电脑才会算。"},
    4: {"prompt": "运行后会显示什么？", "code": 'print("火" + "球")',
        "options": ["火 球", "火球", "火+球"], "answer": 1,
        "explain": "两段文字用 + 会直接拼在一起，中间不会多出空格。"},
    5: {"prompt": "如果输入“小明”，运行后会显示什么？", "code": 'name = input()\nprint("你好" + name)',
        "inputs": ["小明"], "options": ["你好name", "你好小明"], "answer": 1,
        "explain": "input() 把你输入的“小明”装进 name，再和“你好”拼起来。"},
    6: {"prompt": "运行后会显示什么？", "code": 'hp = 30\nif hp > 50:\n    print("很健康")\nelse:\n    print("快喝药")',
        "options": ["很健康", "快喝药"], "answer": 1,
        "explain": "30 > 50 不成立，所以走 else 那条路。"},
    7: {"prompt": "运行后一共显示几行？", "code": 'for i in range(4):\n    print("攻击")',
        "options": ["3 行", "4 行", "5 行"], "answer": 1,
        "explain": "range(4) 会让循环做 4 次。"},
    8: {"prompt": "运行后最后显示的数字是几？", "code": "n = 1\nwhile n < 4:\n    print(n)\n    n = n + 1",
        "options": ["3", "4"], "answer": 0,
        "explain": "n 变成 4 时，n < 4 不成立，循环停下，所以 4 不会显示。"},
    9: {"prompt": "运行后会显示什么？", "code": 'bag = ["剑", "盾", "药水"]\nprint(bag[1])',
        "options": ["剑", "盾"], "answer": 1,
        "explain": "列表的位置从 0 开始数，bag[1] 是第二个：盾。"},
    10: {"prompt": "运行后会显示什么？", "code": 'weak = {"史莱姆": "火", "幽灵": "光"}\nprint(weak["幽灵"])',
         "options": ["火", "光"], "answer": 1,
         "explain": "字典用名字查内容：weak[\"幽灵\"] 查到的是“光”。"},
    11: {"prompt": "运行后会显示什么？", "code": "def add_one(x):\n    return x + 1\n\nprint(add_one(9))",
         "options": ["9", "10"], "answer": 1,
         "explain": "add_one 把收到的数加 1 再交回来，9 + 1 = 10。"},
    12: {"prompt": "运行后会显示什么？", "code": 'for s in [80, 50]:\n    if s >= 60:\n        print("过")\n    else:\n        print("挂")',
         "options": ["过 过", "过 挂（分两行）"], "answer": 1,
         "explain": "80 >= 60 显示“过”，50 不满足显示“挂”，每次 print 各占一行。"},
}

# ---------- 错因标签：错题变成“复仇怪”时用的名字 ----------
ERROR_TAGS = {
    "colon": ("冒号怪", "咒语末尾少了蓄力符 : ，法术没发出来！"),
    "indent": ("缩进怪", "咒语排得歪歪扭扭，魔法阵对不齐！"),
    "quote": ("引号怪", "引号只有一半，咒语漏气了！"),
    "bracket": ("括号怪", "括号没配对，法力从缺口跑掉了！"),
    "chinese": ("中文符号怪", "咒语里混进了中文符号，魔法书看不懂！"),
    "name": ("拼写怪", "你念了一个魔法书里没有的词！"),
    "type": ("类型怪", "文字和数字搅在一起，法术失控了！"),
    "loop": ("死循环怪", "法术停不下来，差点把自己转晕！"),
    "index": ("越界怪", "伸手去拿背包里不存在的格子！"),
    "key": ("查无此怪", "图鉴里查不到这个名字！"),
    "output": ("偏差怪", "法术放出来了，但打偏了！"),
    "must_use": ("偷懒怪", "结果对了，可这一关的招式没用上！"),
    "syntax": ("乱码怪", "咒语的写法不对，魔法书没反应！"),
    "other": ("迷糊怪", "法术出了点意外！"),
}

def chapter_of(lesson_id: int) -> dict:
    for c in CHAPTERS:
        if lesson_id in c["lessons"]:
            return c
    raise KeyError(lesson_id)


def cards(lesson: dict) -> list[str]:
    """把一大段讲解拆成几张小卡片：按句号断句，每张卡片 1～2 句。"""
    sentences = [s + "。" for s in re.split(r"。", lesson["concept"]) if s.strip()]
    result: list[str] = []
    for s in sentences:
        if result and len(result[-1]) + len(s) <= 40:
            result[-1] += s
        else:
            result.append(s)
    return result


def error_tag(lesson_id: int, index: int, result: dict) -> str:
    """根据判题结果判断错因，返回 ERROR_TAGS 的键。选择题和填空题按知识点归类为 output / syntax 之外的类型。"""
    q = get_lesson(lesson_id)["questions"][index]
    if q["type"] != "code":
        return "output"
    error = result.get("error")
    if error:
        kind = error.get("kind")
        msg = error.get("message", "")
        if kind == "timeout":
            return "loop"
        if kind == "IndentationError":
            return "indent"
        if kind == "NameError":
            return "name"
        if kind == "TypeError":
            return "type"
        if kind == "IndexError":
            return "index"
        if kind == "KeyError":
            return "key"
        if "冒号" in msg:
            return "colon"
        if "引号" in msg:
            return "quote"
        if "括号" in msg:
            return "bracket"
        if "中文符号" in msg:
            return "chinese"
        if kind == "SyntaxError":
            return "syntax"
        return "other"
    if "要练习" in result.get("feedback", ""):
        return "must_use"
    return "output"


def decorate(lesson_id: int, index: int, result: dict) -> dict:
    """给判题结果加上游戏层需要的字段：events（驱动动画）、tag（错因）、npc（台词）。
    原来的 feedback（大白话说明 + 行号）保持不变，NPC 台词只是额外加在上面。"""
    out = dict(result)
    if result.get("correct"):
        out["events"] = [{"action": "attack", "target": "enemy", "times": 1}]
        out["tag"] = None
        out["npc"] = "漂亮！这一击正中要害！"
    else:
        tag = error_tag(lesson_id, index, result)
        out["events"] = [{"action": "miss"}, {"action": "hurt", "target": "hero", "amount": 1}]
        out["tag"] = tag
        out["tag_name"] = ERROR_TAGS[tag][0]
        out["npc"] = ERROR_TAGS[tag][1]
    return out


def level_info(lesson_id: int) -> dict:
    """一关的完整游戏信息：学习卡片、小练习、三场战斗的敌人。"""
    lesson = get_lesson(lesson_id)
    chapter = chapter_of(lesson_id)
    enemies = ENEMIES[chapter["theme"]]
    practice = {k: v for k, v in PRACTICE[lesson_id].items() if k not in ("answer", "run")}
    battles = []
    for i, q in enumerate(lesson["questions"]):
        name = enemies[i]
        if i == 2 and lesson_id in BOSS_TITLES:
            name = BOSS_TITLES[lesson_id]
        battles.append({"stage": STAGE_NAMES[i], "enemy": name, "type": q["type"]})
    return {"chapter": {k: chapter[k] for k in ("id", "title", "theme")},
            "is_chapter_boss": lesson_id in BOSS_TITLES,
            "cards": cards(lesson), "analogy": lesson["analogy"], "example": lesson["example"],
            "practice": practice, "battles": battles}


def check_practice(lesson_id: int, answer) -> dict:
    p = PRACTICE[lesson_id]
    try:
        correct = int(answer) == p["answer"]
    except (TypeError, ValueError):
        correct = False
    return {"correct": correct, "explain": p["explain"], "answer": p["answer"]}


def map_info() -> dict:
    return {"chapters": CHAPTERS,
            "lessons": [{"id": l["id"], "title": l["title"], "keyword": l["keyword"]} for l in LESSONS]}
