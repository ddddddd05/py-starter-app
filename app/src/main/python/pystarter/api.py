"""给 Java 调用的入口：所有函数都接收和返回 JSON 字符串。

Java 端（Chaquopy）调用方式：
    PyObject api = Python.getInstance().getModule("pystarter.api");
    String json = api.callAttr("check", 1, 0, "1").toString();

统一用 JSON 字符串传递，Java 那边只需要一个 JSON 解析，不用关心 Python 的数据类型。
出错时返回 {"error": "..."}，不让异常穿透到 Java 导致 App 崩溃。
"""
from __future__ import annotations

import json
from functools import wraps

from . import checker, game, lessons, runner, tracer, tutor


def _json_api(fn):
    @wraps(fn)
    def wrapper(*args, **kwargs):
        try:
            return json.dumps(fn(*args, **kwargs), ensure_ascii=False)
        except Exception as e:  # noqa: BLE001
            return json.dumps({"error": f"{type(e).__name__}: {e}"}, ensure_ascii=False)
    return wrapper


@_json_api
def list_lessons():
    return lessons.lesson_list()


@_json_api
def get_lesson(lesson_id):
    l = lessons.get_lesson(int(lesson_id))
    return {"id": l["id"], "title": l["title"], "concept": l["concept"], "analogy": l["analogy"],
            "example": l["example"],
            "questions": [lessons.public_question(l["id"], i) for i in range(len(l["questions"]))]}


@_json_api
def run_code(code, inputs_json="[]"):
    return runner.run(str(code), json.loads(inputs_json or "[]"))


@_json_api
def trace_code(code, inputs_json="[]"):
    """单步慢放：逐行执行，返回每一步的行号、变量和输出。"""
    return tracer.trace(str(code), json.loads(inputs_json or "[]"))


@_json_api
def check(lesson_id, index, answer):
    """判题结果额外带上 events / tag / npc，给闯关战斗用；原来的字段不变。"""
    lid, idx = int(lesson_id), int(index)
    return game.decorate(lid, idx, checker.check(lid, idx, answer), answer)


@_json_api
def map_info():
    return game.map_info()


@_json_api
def level_info(lesson_id):
    return game.level_info(int(lesson_id))


@_json_api
def check_practice(lesson_id, answer):
    return game.check_practice(int(lesson_id), answer)


@_json_api
def hint(lesson_id, index, answer, level, api_key="", base_url="https://api.deepseek.com",
         model="deepseek-chat"):
    result = checker.check(int(lesson_id), int(index), answer) if str(answer).strip() else None
    return tutor.hint(int(lesson_id), int(index), str(answer), result, int(level),
                      api_key=str(api_key or ""), base_url=str(base_url), model=str(model))
