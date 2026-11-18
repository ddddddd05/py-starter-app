"""闯关玩法这一层的离线测试：章节、学习卡片、小练习、错因标签、判题结果里的游戏字段。

运行：python -m pytest -q
"""
import json
import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "python"))

from pystarter import api, game, lessons, runner  # noqa: E402

LESSON_IDS = [l["id"] for l in lessons.LESSONS]


def test_chapters_cover_every_lesson_exactly_once():
    ids = [lid for c in game.CHAPTERS for lid in c["lessons"]]
    assert sorted(ids) == sorted(LESSON_IDS)


def test_every_lesson_has_one_practice_question():
    assert sorted(game.PRACTICE) == sorted(LESSON_IDS)


@pytest.mark.parametrize("lid", LESSON_IDS)
def test_practice_answer_matches_real_output(lid):
    """小练习也要真的运行：正确选项必须和程序真实输出一致。"""
    p = game.PRACTICE[lid]
    code = p["code"] or p["run"]
    out = runner.run(code, p.get("inputs"))
    assert out["ok"], out
    real = out["program_output"].strip()
    right = p["options"][p["answer"]]
    if lid == 1:                                   # 选的是“哪行代码”，比对它运行后的输出
        assert real == "你好"
    elif right.endswith("行"):
        assert len(real.splitlines()) == int(right.split()[0])
    elif "最后" in p["prompt"]:
        assert real.splitlines()[-1] == right
    elif "分两行" in right:
        assert real.splitlines() == right.split("（")[0].split()
    else:
        assert real == right
    for i, wrong in enumerate(p["options"]):       # 错误选项不能碰巧也对
        if i != p["answer"] and not wrong.endswith("行") and "分两行" not in wrong:
            assert wrong != real


@pytest.mark.parametrize("lid", LESSON_IDS)
def test_cards_split_concept_without_losing_text(lid):
    lesson = lessons.get_lesson(lid)
    cs = game.cards(lesson)
    assert 1 <= len(cs) <= 5
    assert "".join(cs).replace("。", "") == lesson["concept"].replace("。", "")


@pytest.mark.parametrize("lid", LESSON_IDS)
def test_level_info_hides_practice_answer_and_has_three_battles(lid):
    info = json.loads(api.level_info(lid))
    assert "error" not in info
    assert "answer" not in info["practice"] and "run" not in info["practice"]
    assert [b["stage"] for b in info["battles"]] == ["小怪", "精英", "Boss"]
    assert info["is_chapter_boss"] == (lid in (4, 8, 12))


def test_check_practice():
    assert json.loads(api.check_practice(2, 1))["correct"]
    r = json.loads(api.check_practice(2, 0))
    assert not r["correct"] and r["explain"]


# ---------- 判题结果里的游戏字段 ----------
def code_q(lid):
    return [i for i, q in enumerate(lessons.get_lesson(lid)["questions"]) if q["type"] == "code"][0]


def test_correct_answer_has_attack_event():
    lid = 1
    idx = code_q(lid)
    r = json.loads(api.check(lid, idx, lessons.get_lesson(lid)["questions"][idx]["reference"]))
    assert r["correct"]
    assert r["events"][0]["action"] == "attack"
    assert r["tag"] is None


@pytest.mark.parametrize("lid,code,tag", [
    (6, 'score = 85\nif score >= 60\n    print("及格")', "colon"),
    (1, 'print("我开始学 Python 了)', "quote"),
    (1, "print(我开始学)", "name"),
    (8, "n = 1\nwhile n > 0:\n    n = n + 1", "loop"),
    (1, 'print("别的话")', "output"),
    (7, 'print(1)\nprint(2)\nprint(3)', None),     # 第 7 关要求用 for：结果对也会是 must_use 或 output
])
def test_error_tags(lid, code, tag):
    idx = code_q(lid)
    r = json.loads(api.check(lid, idx, code))
    assert not r["correct"]
    assert r["tag"] in game.ERROR_TAGS
    if tag:
        assert r["tag"] == tag
    assert r["npc"] and r["tag_name"]
    assert r["feedback"]                            # 原来的大白话说明一定保留
    assert any(e["action"] == "hurt" for e in r["events"])


def test_wrong_choice_is_tagged():
    r = json.loads(api.check(1, 0, 0))
    assert not r["correct"] and r["tag"] == "output"


def test_map_info():
    info = json.loads(api.map_info())
    assert len(info["chapters"]) == 3 and len(info["lessons"]) == 12


# ---------- 融入式玩法：按真实执行过程生成的事件 ----------
def events_of(lid, code):
    r = json.loads(api.check(lid, 2, code))
    assert r["correct"], r
    return r


def by_action(r, action):
    return [e for e in r["events"] if e["action"] == action]


def test_for_loop_gives_combo_equal_to_rounds():
    r = events_of(7, "for i in range(1, 6):\n    print(i)")
    combo = by_action(r, "combo")
    assert combo and combo[0]["times"] == 5 and combo[0]["rounds"] == 5
    assert "5 连击" in r["npc"]


def test_while_loop_combo():
    r = events_of(8, "n = 10\nwhile n > 0:\n    print(n)\n    n = n - 3")
    assert by_action(r, "combo")[0]["rounds"] == 4


def test_combo_is_capped():
    code = "scores = [80, 95, 70]\nfor s in scores:\n    print(s)"
    assert by_action(events_of(9, code), "combo")[0]["times"] == 3
    assert game.MAX_COMBO >= 5


def test_if_branch_taken_is_reported_with_condition():
    r = events_of(6, 'temperature = 30\nif temperature > 28:\n    print("开空调")\nelse:\n    print("不用开")')
    b = by_action(r, "branch")[0]
    assert b["path"] == "if" and b["condition"] == "temperature > 28"
    assert "走了 if" in r["npc"]


def test_else_branch_detected():
    # 用一个“反过来写”的正确答案：条件不成立，走 else 也能输出“开空调”
    r = events_of(6, 'temperature = 30\nif temperature <= 28:\n    print("不用开")\nelse:\n    print("开空调")')
    assert by_action(r, "branch")[0]["path"] == "else"


def test_def_becomes_skill():
    r = events_of(11, "def square(n):\n    return n * n\n\nprint(square(9))")
    assert [e["name"] for e in by_action(r, "skill")] == ["square"]
    assert "【square】" in r["npc"]


def test_mp_bonus_for_concise_code_and_tip_for_long_code():
    short = events_of(7, "for i in range(1, 6):\n    print(i)")
    assert short["mp"] == {"used": 2, "par": 2, "bonus": True, "tip": None}
    # 能过 must_use 但写得很啰嗦：循环只是摆设，其余靠复制粘贴
    long = "for i in range(1, 2):\n    print(1)\nprint(2)\nprint(3)\nprint(4)\nprint(5)"
    r = events_of(7, long)
    assert not r["mp"]["bonus"] and r["mp"]["tip"]


def test_choice_and_fill_have_no_integrated_events():
    r = json.loads(api.check(1, 0, 1))
    assert r["correct"] and r["mp"] is None
    assert [e["action"] for e in r["events"]] == ["attack"]


@pytest.mark.parametrize("lid", LESSON_IDS)
def test_every_reference_answer_gets_mp_bonus(lid):
    q = lessons.get_lesson(lid)["questions"][2]
    r = events_of(lid, q["reference"])
    assert r["mp"]["bonus"], (lid, r["mp"])
