"""迷宫关卡引擎：用 Python 代码直接指挥小骑士走格子迷宫、躲 Bug 怪、捡宝石。

玩家可用的指令：
- move()         往前走一格
- turn_left()    向左转 90 度
- turn_right()   向右转 90 度
- collect()      捡起脚下的宝石
- ahead_clear()  前方一格是否可以走（True / False）
- on_gem()       脚下是否有宝石（True / False）
- at_goal()      是否已经到达终点旗帜（True / False）
"""
from __future__ import annotations

import sys
import time

from .runner import SAFE_BUILTINS, StopRun, check_code, explain

# 方向：0=右(东), 1=下(南), 2=左(西), 3=上(北)
DIRS = [(1, 0), (0, 1), (-1, 0), (0, -1)]
MAX_ACTIONS = 80
MAX_TRACE_STEPS = 50_000
MAX_SECONDS = 2.0

MAZES = [
    {
        "id": 1,
        "title": "第 1 关：初入迷宫",
        "desc": "用 move() 往前走、turn_right() 右转，帮小骑士走到终点旗帜！",
        "starter": "move()\nmove()\n",
        "hint": "从起点向右走 2 步，再右转往下走 2 步就能到终点。",
        "grid": [
            "S..#.",
            "##.#.",
            "#.E..",
            "#.##.",
            ".....",
        ],
        "start_dir": 0,
        "require_gems": False,
    },
    {
        "id": 2,
        "title": "第 2 关：循环冲锋",
        "desc": "前面有一排宝石，用 for 循环少写重复代码！走到终点前别忘了捡宝石。",
        "starter": "for i in range(4):\n    move()\n",
        "hint": "从左到右一共 4 步，每走一步检查一次 if on_gem(): collect()。",
        "grid": [
            ".....",
            "SGGGE",
            "#####",
            ".....",
            ".....",
        ],
        "start_dir": 0,
        "require_gems": True,
    },
    {
        "id": 3,
        "title": "第 3 关：绕开 Bug 怪",
        "desc": "前方有 Bug 怪（B）拦路！绕过去拿到宝石再到达终点。",
        "starter": "# 向下绕过 Bug 怪再右转\nturn_right()\nmove()\n",
        "hint": "先右转往下走 2 步，左转直走 4 步并 collect() 捡宝石，再左转往上走 2 步到终点。",
        "grid": [
            "S.B.E",
            ".##..",
            "....G",
            "#####",
            ".....",
        ],
        "start_dir": 0,
        "require_gems": True,
    },
    {
        "id": 4,
        "title": "第 4 关：自动巡航骑士",
        "desc": "用 while not at_goal(): 配合 if ahead_clear():，写一个能自己拐弯的智能骑士！",
        "starter": "while not at_goal():\n    if ahead_clear():\n        move()\n    else:\n        turn_right()\n",
        "hint": "前方能走就 move()，撞墙前用 else: turn_right() 自动右转！",
        "grid": [
            "S.G..",
            "###..",
            "..E..",
            "##.##",
            ".....",
        ],
        "start_dir": 0,
        "require_gems": True,
    },
]


def _parse_grid(grid: list[str]):
    h = len(grid)
    w = len(grid[0])
    sx = sy = gx = gy = 0
    gems = set()
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch == "S":
                sx, sy = x, y
            elif ch == "E":
                gx, gy = x, y
            elif ch == "G":
                gems.add((x, y))
    return w, h, sx, sy, gx, gy, gems


def list_mazes() -> list[dict]:
    return [
        {
            "id": m["id"],
            "title": m["title"],
            "desc": m["desc"],
            "starter": m["starter"],
            "hint": m["hint"],
            "grid": m["grid"],
            "start_dir": m["start_dir"],
            "require_gems": m["require_gems"],
        }
        for m in MAZES
    ]


def get_maze(maze_id: int) -> dict:
    for m in MAZES:
        if m["id"] == maze_id:
            return m
    raise ValueError(f"没有第 {maze_id} 关迷宫")


def run_maze(maze_id: int, code: str) -> dict:
    """运行学生的迷宫代码，返回 {"ok", "won", "feedback", "frames", "error"}。"""
    m = get_maze(int(maze_id))
    problem = check_code(code)
    if problem:
        return {"ok": False, "won": False, "feedback": problem, "frames": [],
                "error": {"kind": "forbidden", "line": None, "message": problem}}

    grid = m["grid"]
    w, h, x, y, gx, gy, gems = _parse_grid(grid)
    d = m["start_dir"]

    def snapshot(action: str, msg: str = ""):
        return {
            "x": x,
            "y": y,
            "dir": d,
            "action": action,
            "gems": [[gx_, gy_] for gx_, gy_ in sorted(gems)],
            "msg": msg,
        }

    frames = [snapshot("start", "小骑士准备出发！")]

    class MazeStop(Exception):
        pass

    def is_clear(nx: int, ny: int) -> bool:
        if nx < 0 or ny < 0 or nx >= w or ny >= h:
            return False
        return grid[ny][nx] not in ("#", "B")

    def guard():
        if len(frames) > MAX_ACTIONS:
            raise MazeStop("走的步数太多啦（超过 80 步），检查一下循环是不是停不下来。")

    def move():
        nonlocal x, y
        guard()
        dx, dy = DIRS[d]
        nx, ny = x + dx, y + dy
        if nx < 0 or ny < 0 or nx >= w or ny >= h:
            frames.append(snapshot("hit", "哎呀，撞到迷宫边界了！"))
            raise MazeStop("小骑士撞到迷宫边界了，试试先转个弯。")
        cell = grid[ny][nx]
        if cell == "#":
            frames.append(snapshot("hit", "哎呀，撞到石墙了！"))
            raise MazeStop("前面是石墙，过不去哦，试试 turn_left() 或 turn_right()。")
        if cell == "B":
            x, y = nx, ny
            frames.append(snapshot("hit", "踩到 Bug 怪了！"))
            raise MazeStop("小骑士踩到 Bug 怪了！绕开它吧。")
        x, y = nx, ny
        frames.append(snapshot("move", ""))

    def turn_left():
        nonlocal d
        guard()
        d = (d - 1) % 4
        frames.append(snapshot("turn", ""))

    def turn_right():
        nonlocal d
        guard()
        d = (d + 1) % 4
        frames.append(snapshot("turn", ""))

    def collect():
        guard()
        if (x, y) in gems:
            gems.remove((x, y))
            frames.append(snapshot("collect", "捡到一颗宝石！"))

    def ahead_clear() -> bool:
        dx, dy = DIRS[d]
        return is_clear(x + dx, y + dy)

    def on_gem() -> bool:
        return (x, y) in gems

    def at_goal() -> bool:
        return (x, y) == (gx, gy)

    env = {
        "__builtins__": {
            **SAFE_BUILTINS,
            "move": move,
            "turn_left": turn_left,
            "turn_right": turn_right,
            "collect": collect,
            "ahead_clear": ahead_clear,
            "on_gem": on_gem,
            "at_goal": at_goal,
        },
        "__name__": "__main__",
    }

    steps = [0]
    started = time.monotonic()

    def tracer(frame, event, arg):
        if event == "line" and frame.f_code.co_filename == "<迷宫代码>":
            steps[0] += 1
            if steps[0] > MAX_TRACE_STEPS or (steps[0] % 500 == 0 and time.monotonic() - started > MAX_SECONDS):
                raise StopRun("timeout")
        return tracer

    error = None
    old = sys.gettrace()
    try:
        compiled = compile(code, "<迷宫代码>", "exec")
        sys.settrace(tracer)
        try:
            exec(compiled, env)
        finally:
            sys.settrace(old)
    except MazeStop as e:
        return {"ok": False, "won": False, "feedback": str(e), "frames": frames,
                "error": {"kind": "maze", "line": None, "message": str(e)}}
    except BaseException as e:  # noqa: BLE001
        err = explain(e, code)
        return {"ok": False, "won": False, "feedback": err["message"], "frames": frames, "error": err}

    if (x, y) != (gx, gy):
        return {"ok": True, "won": False, "frames": frames, "error": None,
                "feedback": "代码跑完了，但小骑士还没走到终点旗帜哦！"}
    if m["require_gems"] and gems:
        return {"ok": True, "won": False, "frames": frames, "error": None,
                "feedback": f"到终点啦！不过还有 {len(gems)} 颗宝石没捡（记得用 collect()）。"}

    frames.append(snapshot("win", "通关成功！"))
    return {"ok": True, "won": True, "frames": frames, "error": None,
            "feedback": f"太棒了！小骑士只用了 {len(frames) - 1} 个动作就通关啦！"}
