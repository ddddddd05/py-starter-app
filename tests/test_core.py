"""Python 核心逻辑的离线测试（在电脑上跑，不需要安卓环境）。

运行：python -m pytest -q
"""
import json
import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "python"))

from pystarter import api, checker, lessons, runner, tutor  # noqa: E402

ALL_QUESTIONS = [(l["id"], i, q) for l in lessons.LESSONS for i, q in enumerate(l["questions"])]


# ---------- 题目内容自检：参考答案、选项、填空都要和真实运行结果一致 ----------
@pytest.mark.parametrize("lid,idx,q", [x for x in ALL_QUESTIONS if x[2]["type"] == "code"],
                         ids=lambda v: str(v) if not isinstance(v, dict) else "")
def test_reference_answer_passes_its_own_checker(lid, idx, q):
    result = checker.check(lid, idx, q["reference"])
    assert result["correct"], (lid, idx, result)


@pytest.mark.parametrize("lid,idx,q", [x for x in ALL_QUESTIONS if x[2]["type"] == "choice"],
                         ids=lambda v: str(v) if not isinstance(v, dict) else "")
def test_choice_answer_matches_real_output(lid, idx, q):
    """选择题：真的运行题目代码，正确选项必须描述了真实输出（防止题目本身出错）。"""
    inputs = ["3"] if "input()" in q["code"] else None
    out = runner.run(q["code"], inputs)
    assert out["ok"], out
    real = out["program_output"]                 # 不含回显的输入
    right = q["options"][q["answer"]]
    if right.endswith("行"):                      # “4 行”这类选项比对行数
        assert len(real.strip().splitlines()) == int(right.split()[0])
    elif right == "一直不停":
        assert not out["ok"]
    else:
        last = real.strip().splitlines()[-1] if "最后" in q["prompt"] else real.strip()
        assert last == right, (lid, idx, real, right)


@pytest.mark.parametrize("lid,idx,q", [x for x in ALL_QUESTIONS if x[2]["type"] == "fill"],
                         ids=lambda v: str(v) if not isinstance(v, dict) else "")
def test_fill_answers_make_runnable_code(lid, idx, q):
    for a in q["answers"]:
        code = q["template"].replace("___", a)
        res = runner.run(code, ["5"])
        assert res["ok"], (lid, idx, a, res)
        assert checker.check(lid, idx, a)["correct"]


def test_every_question_has_three_hints_and_lessons_are_numbered():
    assert [l["id"] for l in lessons.LESSONS] == list(range(1, 13))
    for lid, idx, q in ALL_QUESTIONS:
        assert len(q["hints"]) == 3, (lid, idx)


def test_public_question_hides_answers():
    for lid, idx, q in ALL_QUESTIONS:
        pub = lessons.public_question(lid, idx)
        assert not {"answer", "answers", "reference", "expected_output", "hints"} & set(pub)


# ---------- 运行学生代码 ----------
def test_run_prints_and_input():
    r = runner.run('n = int(input())\nprint(n * 2)', ["21"])
    assert r["ok"] and r["output"] == "21\n42\n"          # 显示：回显输入 + 输出
    assert r["program_output"] == "42\n"                  # 判题：只有程序输出


def test_input_echo_does_not_affect_judging():
    assert checker.check(5, 2, "n = int(input())\nprint(n * 2)")["correct"]


def test_infinite_loop_is_stopped():
    r = runner.run("while True:\n    x = 1")
    assert not r["ok"] and r["error"]["kind"] == "timeout" and "死循环" in r["error"]["message"]


def test_huge_output_is_stopped():
    r = runner.run("while True:\n    print('a' * 100)")
    assert not r["ok"] and "太多" in r["error"]["message"]


@pytest.mark.parametrize("code", ["import os", "open('a.txt')", "eval('1+1')", "().__class__.__bases__"])
def test_dangerous_code_is_blocked(code):
    r = runner.run(code)
    assert not r["ok"] and r["error"]["kind"] == "forbidden"


@pytest.mark.parametrize("code,keyword", [
    ('if 1 > 0\n    print("a")', "冒号"),
    ('print("你好)', "引号"),
    ("print（“你好”）", "中文符号"),
    ("print(hello)", "加引号"),
    ('print("年龄" + 18)', "str"),
    ("print(1 / 0)", "除以 0"),
    ("a = [1]\nprint(a[3])", "从 0 开始"),
    ("if True:\nprint(1)", "缩进"),
    ("x = int('abc')", "像数字"),
])
def test_errors_are_explained_in_plain_language(code, keyword):
    r = runner.run(code)
    assert not r["ok"]
    assert keyword in r["error"]["message"], r["error"]


def test_error_reports_line_number():
    r = runner.run("a = 1\nb = 2\nprint(c)")
    assert r["error"]["line"] == 3 and r["error"]["message"].startswith("第 3 行")


def test_missing_input_is_explained():
    r = runner.run("a = input()\nb = input()", ["1"])
    assert not r["ok"] and r["error"]["kind"] == "input"


# ---------- 判题 ----------
def test_right_output_but_missing_required_feature():
    # 第 7 关要求用 for 循环；用 print 直接写出 1~5 结果对，但要提醒练习 for
    r = checker.check(7, 2, "print(1)\nprint(2)\nprint(3)\nprint(4)\nprint(5)")
    assert not r["correct"] and "for 循环" in r["feedback"]


def test_wrong_output_shows_expected():
    r = checker.check(1, 2, 'print("我开始学Python了")')       # 少了空格
    assert not r["correct"] and r["expected"] == "我开始学 Python 了\n"


def test_trailing_spaces_are_ignored():
    assert checker.check(1, 2, 'print("我开始学 Python 了   ")')["correct"]


def test_choice_and_fill():
    assert checker.check(1, 0, "1")["correct"] and not checker.check(1, 0, "0")["correct"]
    assert not checker.check(1, 0, "abc")["correct"]
    assert checker.check(1, 1, " print ")["correct"]


# ---------- AI 老师 ----------
def fake_llm(reply):
    return lambda *a, **k: reply


def test_no_key_uses_builtin_hint():
    h = tutor.hint(7, 2, "", None, 2)
    assert h["source"] == "builtin" and h["text"] == lessons.get_lesson(7)["questions"][2]["hints"][1]


def test_ai_hint_passes_when_no_leak():
    h = tutor.hint(7, 2, "for i in range(5):\n    print(i)", None, 1, api_key="k",
                   llm=fake_llm("思路对了！想想 range 从几开始、到几结束。"))
    assert h["source"] == "ai" and not h["blocked"]


def test_leaked_code_answer_is_replaced():
    leak = "直接这样写：\nfor i in range(1, 6):\n    print(i)"
    h = tutor.hint(7, 2, "", None, 1, api_key="k", llm=fake_llm(leak))
    assert h["blocked"] and h["source"] == "builtin"


def test_leaked_fill_word_is_blocked_before_level_3():
    q = lessons.get_lesson(1)["questions"][1]
    assert tutor.leaks_answer("答案就是 print", q, 1)
    assert not tutor.leaks_answer("答案就是 print", q, 3)


def test_leaked_choice_is_blocked():
    q = lessons.get_lesson(6)["questions"][0]
    assert tutor.leaks_answer("应该选 B", q, 2)
    assert not tutor.leaks_answer("先看看 x 和 10 谁大", q, 2)


def test_network_error_falls_back():
    def broken(*a, **k):
        raise OSError("no network")
    h = tutor.hint(1, 2, "", None, 1, api_key="k", llm=broken)
    assert h["source"] == "builtin" and "连不上" in h["note"]


def test_prompt_contains_error_and_level():
    q = lessons.get_lesson(6)["questions"][2]
    res = checker.check(6, 2, 'if temperature > 28\n    print("开空调")')
    msgs = tutor.build_messages(q, "c", "x", res, 2)
    assert "冒号" in msgs[1]["content"] and "2 级" in msgs[1]["content"]
    assert "不能给出完整答案" in msgs[0]["content"]


# ---------- 给 Java 的接口 ----------
def test_api_returns_json_strings():
    assert len(json.loads(api.list_lessons())) == 12
    lesson = json.loads(api.get_lesson(3))
    assert "reference" not in json.dumps(lesson, ensure_ascii=False)
    assert json.loads(api.check(3, 2, "price = 15\ncount = 3\nprint(price * count)"))["correct"]
    assert json.loads(api.run_code("print(1+1)"))["output"] == "2\n"
    assert json.loads(api.hint(1, 2, "", 1))["source"] == "builtin"
    assert "error" in json.loads(api.get_lesson(99))


def test_huge_range_blocked_before_oom():
    r = runner.run("nums = list(range(10**9))\nprint(len(nums))")
    assert not r["ok"]
    assert r["error"]["kind"] == "timeout"
    assert "range" in r["error"]["message"]


def test_tutor_endpoint_normalizes_url():
    assert tutor._endpoint("https://api.deepseek.com/") == "https://api.deepseek.com/chat/completions"
    assert tutor._endpoint("https://api.deepseek.com/v1/chat/completions/") == "https://api.deepseek.com/v1/chat/completions"
