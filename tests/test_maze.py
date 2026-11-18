from pystarter import api, maze
import json


def test_all_four_mazes_solvable():
    solutions = {
        1: "move()\nmove()\nturn_right()\nmove()\nmove()\n",
        2: "for i in range(4):\n    move()\n    if on_gem():\n        collect()\n",
        3: "turn_right()\nmove()\nmove()\nturn_left()\nmove()\nmove()\nmove()\nmove()\ncollect()\nturn_left()\nmove()\nmove()\n",
        4: "while not at_goal():\n    if on_gem():\n        collect()\n    if ahead_clear():\n        move()\n    else:\n        turn_right()\n",
    }
    for mid, code in solutions.items():
        res = maze.run_maze(mid, code)
        assert res["won"], f"Maze {mid} failed: {res}"
        assert res["frames"][-1]["action"] == "win"


def test_maze_wall_and_bug_collision():
    res1 = maze.run_maze(1, "turn_right()\nmove()\n")
    assert not res1["won"]
    assert "石墙" in res1["feedback"]

    res3 = maze.run_maze(3, "move()\nmove()\n")
    assert not res3["won"]
    assert "Bug" in res3["feedback"]


def test_maze_api_json():
    mazes = json.loads(api.list_mazes())
    assert len(mazes) == 4
    r = json.loads(api.run_maze(1, "move()\nmove()\nturn_right()\nmove()\nmove()"))
    assert r["won"]
