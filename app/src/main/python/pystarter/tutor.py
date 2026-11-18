"""AI 老师：用大白话给提示，不直接给答案。

提示分三级，学生每多问一次，提示更具体一级：
  1 级：指出问题出在哪个方向（概念层面）
  2 级：指出具体是哪一行、哪个写法有问题
  3 级：给出关键的那一步怎么写，但不给完整答案

防泄露（不能只靠提示词“劝”模型）：
- 程序检查回复里有没有出现参考答案的代码行、填空答案、选择题正确选项；
- 一旦命中，丢弃 AI 的回复，改用这一级的内置提示，并在结果里标记 blocked = True。
没填 API Key 或网络失败时，同样退回内置提示，App 照样能用。
"""
from __future__ import annotations

import json
import re
import urllib.error
import urllib.request

from .lessons import get_lesson

SYSTEM_PROMPT = """你是一位非常耐心的 Python 入门老师，学生是完全没学过编程的人。
规则：
1. 用大白话，不用术语；必须用术语时，紧跟着用括号解释。
2. 绝对不能给出完整答案，不能写出完整的正确代码，最多写一个关键的小片段（不超过一行）。
3. 按提示等级回答：1 级只说问题在哪个方向；2 级指出具体哪一行、哪个写法；3 级说出关键那一步怎么写。
4. 先肯定学生做对的部分，再指出问题。语气温和、鼓励。
5. 回答不超过 80 个字。"""


def _norm_line(line: str) -> str:
    return re.sub(r"\s+", "", line)


def leaks_answer(reply: str, question: dict, level: int) -> bool:
    """判断 AI 的回复是不是把答案说出来了。"""
    text = _norm_line(reply)
    qtype = question["type"]
    if qtype == "code":
        ref_lines = [_norm_line(l) for l in question["reference"].splitlines()
                     if len(_norm_line(l)) > 6]          # 太短的行（如 else:）不算
        hit = sum(1 for l in ref_lines if l in text)
        # 一行以上的参考代码出现在回复里，或者参考答案的主体（超过一半的行）出现 → 算泄露
        return hit >= 2 or (ref_lines and hit / len(ref_lines) > 0.5)
    if qtype == "fill":
        # 1、2 级提示不能直接说出填空的词；3 级允许（内置的 3 级提示本来就很接近答案）
        return level < 3 and any(_norm_line(a) and _norm_line(a) in text for a in question["answers"])
    if qtype == "choice":
        right = question["options"][question["answer"]]
        letter = "ABCDEFG"[question["answer"]]
        return bool(re.search(rf"(答案|选|应该是|正确的?是)[：:\s]*({re.escape(letter)}|{re.escape(_norm_line(right))})",
                              text))
    return False


def build_messages(question: dict, concept: str, student_answer: str, check_result: dict | None,
                   level: int) -> list[dict]:
    parts = [f"【这一关讲的概念】{concept}", f"【题目】{question['prompt']}"]
    if question.get("code"):
        parts.append(f"【题目里的代码】\n{question['code']}")
    if question.get("template"):
        parts.append(f"【填空模板】\n{question['template']}")
    if question["type"] == "choice":
        opts = "；".join(f"{'ABCDEFG'[i]}. {o}" for i, o in enumerate(question["options"]))
        parts.append(f"【选项】{opts}")
    parts.append(f"【学生的回答】\n{student_answer or '（还没作答）'}")
    if check_result:
        if check_result.get("error"):
            parts.append(f"【运行报错】{check_result['error']['message']}")
        elif check_result.get("output"):
            parts.append(f"【学生代码的输出】\n{check_result['output']}")
        parts.append(f"【判题结果】{check_result.get('feedback', '')}")
    parts.append(f"【提示等级】{level} 级")
    return [{"role": "system", "content": SYSTEM_PROMPT}, {"role": "user", "content": "\n".join(parts)}]


def _endpoint(base_url: str) -> str:
    url = base_url.strip().rstrip("/")
    if url.endswith("/chat/completions"):
        return url
    return url + "/chat/completions"


def call_llm(messages: list[dict], api_key: str, base_url: str, model: str, timeout: float = 20) -> str:
    """OpenAI 兼容接口（默认 DeepSeek）。只用标准库，不额外打包依赖进 App。"""
    req = urllib.request.Request(
        _endpoint(base_url),
        data=json.dumps({"model": model, "messages": messages, "temperature": 0.3, "max_tokens": 300}).encode(),
        headers={"Content-Type": "application/json", "Authorization": f"Bearer {api_key}"})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    return data["choices"][0]["message"]["content"].strip()


def hint(lesson_id: int, index: int, student_answer: str, check_result: dict | None, level: int,
         api_key: str = "", base_url: str = "https://api.deepseek.com", model: str = "deepseek-chat",
         llm=call_llm) -> dict:
    """返回 {"text", "source": "ai"|"builtin", "blocked", "level", "note"}。llm 参数用于测试替换。"""
    level = max(1, min(3, int(level)))
    lesson = get_lesson(lesson_id)
    q = lesson["questions"][index]
    builtin = q["hints"][level - 1]
    if not api_key:
        if check_result and check_result.get("error"):
            err_msg = check_result["error"]["message"]
            text = f"小精灵诊断：{err_msg}\n💡 提示：{builtin}"
        elif check_result and not check_result.get("correct", True) and check_result.get("feedback"):
            text = f"{builtin}"
        else:
            text = builtin
        return {"text": text, "source": "builtin", "blocked": False, "level": level,
                "note": "离线小精灵诊断（填 Key 可解锁自由对话）"}
    try:
        reply = llm(build_messages(q, lesson["concept"], student_answer, check_result, level),
                    api_key, base_url, model)
    except (urllib.error.URLError, TimeoutError, KeyError, ValueError, OSError) as e:
        return {"text": builtin, "source": "builtin", "blocked": False, "level": level,
                "note": f"AI 老师暂时连不上（{type(e).__name__}），先看内置提示。"}
    if leaks_answer(reply, q, level):
        return {"text": builtin, "source": "builtin", "blocked": True, "level": level,
                "note": "AI 的回答里包含了答案，已替换成提示。"}
    return {"text": reply, "source": "ai", "blocked": False, "level": level, "note": ""}
