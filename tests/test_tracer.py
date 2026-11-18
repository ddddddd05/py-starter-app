"""单步慢放（tracer）的离线测试。运行：python -m pytest -q"""
import json
import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "python"))

from pystarter import api, lessons, tracer  # noqa: E402


def lines(r):
    return [s["line"] for s in r["steps"]]


def test_straight_line_program_steps_each_line_and_tracks_vars():
    r = tracer.trace('a = 2\nb = a * 3\nprint(b)')
    assert r["ok"]
    assert lines(r) == [1, 2, 3, None]
    assert r["steps"][0]["vars"] == {}                 # 第 1 行执行前还没有变量
    assert r["steps"][1]["vars"] == {"a": "2"}
    assert r["steps"][2]["vars"] == {"a": "2", "b": "6"}
    final = r["steps"][-1]
    assert final["output"] == "6\n" and final["new_output"] == "6\n"


def test_for_loop_counts_rounds_and_new_output_per_round():
    r = tracer.trace('for i in range(3):\n    print("砍")')
    assert r["ok"]
    body = [s for s in r["steps"] if s["line"] == 2]
    assert [s["loop"] for s in body] == [1, 2, 3]
    assert [s["vars"]["i"] for s in body] == ["0", "1", "2"]
    # 每轮 print 的输出出现在下一步的 new_output 里：一共 3 次“砍”
    assert sum(s["new_output"].count("砍") for s in r["steps"]) == 3


def test_function_frame_shows_local_vars_and_name():
    r = tracer.trace("def double(x):\n    return x * 2\n\nprint(double(5))")
    assert r["ok"]
    inside = [s for s in r["steps"] if s["func"] == "double"]
    assert inside and inside[0]["vars"] == {"x": "5"}
    assert r["steps"][-1]["output"] == "10\n"
    assert "double" not in r["steps"][-1]["vars"]       # 函数本身不放进变量背包


def test_input_is_fed_and_echoed():
    r = tracer.trace('name = input("名字？")\nprint("你好" + name)', ["Tom"])
    assert r["ok"]
    assert r["steps"][-1]["output"] == "名字？Tom\n你好Tom\n"


def test_infinite_loop_is_truncated_not_hanging():
    r = tracer.trace("n = 0\nwhile True:\n    n = n + 1")
    assert r["truncated"]
    assert len(r["steps"]) == tracer.MAX_TRACE_STEPS + 1
    assert r["error"] is None                          # 截断不算报错，只是不再往下记


def test_runtime_error_keeps_steps_before_it():
    r = tracer.trace('x = 1\nprint(y)')
    assert not r["ok"]
    assert r["error"]["kind"] == "NameError" and r["error"]["line"] == 2
    assert lines(r)[:2] == [1, 2]


def test_forbidden_code_is_rejected():
    r = tracer.trace("import os")
    assert not r["ok"] and r["steps"] == []


def test_long_values_are_shortened():
    r = tracer.trace("s = 'a' * 100\nprint(len(s))")
    assert len(r["steps"][-1]["vars"]["s"]) <= tracer.MAX_VALUE_LEN


@pytest.mark.parametrize("lesson", lessons.LESSONS, ids=lambda l: str(l["id"]))
def test_every_lesson_example_can_be_traced(lesson):
    inputs = ["小明"] if "input(" in lesson["example"] else None
    r = json.loads(api.trace_code(lesson["example"], json.dumps(inputs or [])))
    assert r["ok"], r
    assert len(r["steps"]) >= 2
