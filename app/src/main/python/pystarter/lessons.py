"""关卡内容：给完全没学过编程的人。

每一关 = 一段大白话讲解（concept）+ 一个比喻（analogy）+ 一段示例代码（example）+ 3 道题：
- choice 选择题：看代码选输出，answer 是正确选项的下标
- fill   填空题：template 里的 ___ 填一个词，answers 列出所有可接受的写法
- code   自己写：程序真的运行学生代码，输出和 expected_output 一致才算对；
         inputs 是模拟用户在键盘上输入的内容；must_use 要求必须用到某种写法（如 for 循环）；
         reference 是参考答案，只用于判题和防止 AI 老师泄露答案，不会显示给学生

题目里的 hints 是“程序自带的三级提示”，AI 老师不可用（没网、没填 Key）时就用它们。
"""
from __future__ import annotations

LESSONS: list[dict] = [
    {
        "id": 1, "title": "让电脑说话", "keyword": "print",
        "concept": "print 的意思是“打印、显示”。把想让电脑说的话放进 print( ) 的括号里，电脑就会把它显示出来。"
                   "文字要用英文引号包起来，告诉电脑“这是一句话，不是命令”。",
        "analogy": "print 就像一个喇叭：你把话放进去，它就大声说出来。",
        "example": 'print("你好，世界")',
        "questions": [
            {"type": "choice", "prompt": "运行下面的代码，屏幕上会显示什么？",
             "code": 'print("我爱学习")', "options": ["print(\"我爱学习\")", "我爱学习", "\"我爱学习\""],
             "answer": 1,
             "hints": ["print 会把括号里的内容显示出来。", "引号只是告诉电脑“这是文字”，本身不会显示。",
                       "显示的是引号里面的文字。"]},
            {"type": "fill", "prompt": "补全代码，让电脑显示“早上好”。",
             "template": '___("早上好")', "answers": ["print"],
             "hints": ["哪个词能让电脑把话显示出来？", "这个词的意思是“打印、显示”。", "它是 5 个英文字母，p 开头。"]},
            {"type": "code", "prompt": "写一行代码，让电脑显示：我开始学 Python 了",
             "starter": "", "expected_output": "我开始学 Python 了\n", "must_use": ["print"],
             "reference": 'print("我开始学 Python 了")',
             "hints": ["用 print 把一句话显示出来。", "要显示的文字需要放在英文引号里。",
                       "格式是 print(\"……\")，把省略号换成要显示的话。"]},
        ],
    },
    {
        "id": 2, "title": "变量：贴了名字的盒子", "keyword": "变量",
        "concept": "变量就是给一个值起名字，方便以后再用。写法是 名字 = 值。这里的 = 不是“等于”，而是“装进去”。",
        "analogy": "变量像一个贴了名字的盒子：name = \"小明\" 就是把“小明”装进名叫 name 的盒子里。",
        "example": 'name = "小明"\nprint(name)',
        "questions": [
            {"type": "choice", "prompt": "运行后会显示什么？", "code": 'fruit = "苹果"\nprint(fruit)',
             "options": ["fruit", "苹果", "\"苹果\""], "answer": 1,
             "hints": ["fruit 是盒子的名字，里面装的是什么？", "print(fruit) 显示的是盒子里装的东西。", "盒子里装的是“苹果”。"]},
            {"type": "fill", "prompt": "把数字 18 装进名叫 age 的盒子里。", "template": "age ___ 18",
             "answers": ["="],
             "hints": ["“装进去”用哪个符号？", "这个符号在数学里读作“等于”。", "就是一个等号。"]},
            {"type": "code", "prompt": "把你的城市“福州”装进变量 city，然后用 print 显示这个变量。",
             "starter": "", "expected_output": "福州\n", "must_use": ["assign", "print"],
             "reference": 'city = "福州"\nprint(city)',
             "hints": ["先用 = 把“福州”装进 city。", "第二行用 print 显示 city。",
                       "注意第二行 print 括号里写的是变量名 city，不要加引号。"]},
        ],
    },
    {
        "id": 3, "title": "让电脑算数", "keyword": "数字计算",
        "concept": "电脑最会算数。加减乘除分别是 + - * /。数字不用加引号；加了引号就变成文字，电脑就不会算了。",
        "analogy": "Python 就像一个超级计算器，乘号是 * ，除号是 / 。",
        "example": "print(3 + 5)\nprint(10 * 2)",
        "questions": [
            {"type": "choice", "prompt": "运行后会显示什么？", "code": "print(6 * 7)",
             "options": ["6 * 7", "42", "67"], "answer": 1,
             "hints": ["* 在 Python 里是什么运算？", "* 是乘号。", "算一下 6 乘以 7。"]},
            {"type": "fill", "prompt": "补全代码，算出 100 除以 4。", "template": "print(100 ___ 4)",
             "answers": ["/", "//"],
             "hints": ["除号在 Python 里怎么写？", "它是键盘上问号旁边的斜线。", "就是 / 。"]},
            {"type": "code", "prompt": "一杯奶茶 15 元，买 3 杯。用 price 和 count 两个变量，算出并显示总价。",
             "starter": "price = 15\ncount = 3\n", "expected_output": "45\n", "must_use": ["print"],
             "reference": "price = 15\ncount = 3\nprint(price * count)",
             "hints": ["总价 = 单价 × 数量。", "乘号是 * 。", "用 print 显示 price * count 的结果。"]},
        ],
    },
    {
        "id": 4, "title": "文字拼接", "keyword": "字符串",
        "concept": "用引号包起来的文字叫“字符串”。两个字符串用 + 连起来，就拼成一句更长的话。"
                   "数字要先用 str( ) 变成文字，才能和文字拼在一起。",
        "analogy": "字符串像一串珠子，+ 就是把两串珠子接起来。",
        "example": 'name = "小红"\nprint("你好，" + name)',
        "questions": [
            {"type": "choice", "prompt": "运行后会显示什么？", "code": 'print("Py" + "thon")',
             "options": ["Py thon", "Python", "Py+thon"], "answer": 1,
             "hints": ["+ 对文字来说是“拼起来”。", "拼接时中间不会自动加空格。", "两段文字直接连在一起。"]},
            {"type": "fill", "prompt": "把数字 age 变成文字，才能拼接。", "template": 'age = 20\nprint("我今年" + ___(age) + "岁")',
             "answers": ["str"],
             "hints": ["哪个函数能把数字变成文字？", "它是“字符串”英文 string 的缩写。", "3 个字母，s 开头。"]},
            {"type": "code", "prompt": "已有变量 name = \"小明\"，请显示：欢迎你，小明",
             "starter": 'name = "小明"\n', "expected_output": "欢迎你，小明\n", "must_use": ["print"],
             "reference": 'name = "小明"\nprint("欢迎你，" + name)',
             "hints": ["用 + 把“欢迎你，”和 name 拼起来。", "“欢迎你，”要加引号，name 是变量不加引号。",
                       "注意逗号是中文逗号，要放在引号里面。"]},
        ],
    },
    {
        "id": 5, "title": "听你说话：input", "keyword": "input",
        "concept": "input( ) 会停下来等你在键盘上输入，然后把你输入的内容交给程序。括号里可以写一句提示语。"
                   "注意：input 拿到的永远是文字，要做计算得先用 int( ) 变成整数。",
        "analogy": "input 像一个话筒：程序把话筒递给你，你说完它再继续。",
        "example": 'name = input("你叫什么名字？")\nprint("你好，" + name)',
        "questions": [
            {"type": "choice", "prompt": "用户输入 3 之后，下面的代码会显示什么？",
             "code": 'n = input()\nprint(n + n)', "options": ["6", "33", "报错"], "answer": 1,
             "hints": ["input 拿到的是文字还是数字？", "input 拿到的是文字“3”。", "两个文字“3”用 + 拼起来是什么？"]},
            {"type": "fill", "prompt": "把输入的内容变成整数，才能做加法。", "template": "n = ___(input())\nprint(n + 1)",
             "answers": ["int"],
             "hints": ["哪个函数能把文字变成整数？", "“整数”的英文是 integer。", "3 个字母，i 开头。"]},
            {"type": "code", "prompt": "让用户输入一个数字，显示这个数字的 2 倍。（测试时会输入 21）",
             "starter": "", "inputs": ["21"], "expected_output": "42\n", "must_use": ["input", "print"],
             "reference": "n = int(input())\nprint(n * 2)",
             "hints": ["先用 input() 接收输入。", "input 拿到的是文字，要用 int( ) 变成整数。",
                       "把 int(input()) 装进变量，再 print 它乘以 2 的结果。"]},
        ],
    },
    {
        "id": 6, "title": "做判断：if", "keyword": "if",
        "concept": "if 让程序做选择：如果条件成立，就执行下面缩进的代码；else 表示“否则”。"
                   "条件后面要加英文冒号，下面的代码要往右缩进 4 个空格。",
        "analogy": "if 就像过马路：如果是绿灯，就走；否则，就等。",
        "example": 'score = 85\nif score >= 60:\n    print("及格")\nelse:\n    print("不及格")',
        "questions": [
            {"type": "choice", "prompt": "运行后会显示什么？", "code": 'x = 5\nif x > 10:\n    print("大")\nelse:\n    print("小")',
             "options": ["大", "小", "大 小"], "answer": 1,
             "hints": ["先看条件 x > 10 成不成立。", "5 大于 10 吗？", "条件不成立时执行 else 下面的代码。"]},
            {"type": "fill", "prompt": "补全判断语句。", "template": 'age = 20\n___ age >= 18:\n    print("成年")',
             "answers": ["if"],
             "hints": ["哪个词表示“如果”？", "它是 2 个字母。", "i 开头。"]},
            {"type": "code", "prompt": "已有 temperature = 30。如果温度大于 28，显示“开空调”，否则显示“不用开”。",
             "starter": "temperature = 30\n", "expected_output": "开空调\n", "must_use": ["if"],
             "reference": 'temperature = 30\nif temperature > 28:\n    print("开空调")\nelse:\n    print("不用开")',
             "hints": ["用 if 写条件 temperature > 28。", "条件后面加冒号，下一行缩进 4 个空格再写 print。",
                       "别忘了 else: 和它下面缩进的 print。"]},
        ],
    },
    {
        "id": 7, "title": "重复做事：for 循环", "keyword": "for",
        "concept": "for 循环让一段代码重复执行。range(5) 会依次给出 0、1、2、3、4，一共 5 个数。"
                   "同样要加冒号，循环里的代码要缩进。",
        "analogy": "for 循环像跑圈：range(5) 就是“跑 5 圈”，每跑一圈，圈数 i 就加 1。",
        "example": "for i in range(3):\n    print(i)",
        "questions": [
            {"type": "choice", "prompt": "下面的代码一共会显示几行？", "code": 'for i in range(4):\n    print("加油")',
             "options": ["3 行", "4 行", "5 行"], "answer": 1,
             "hints": ["range(4) 会给出几个数？", "range(4) 给出 0、1、2、3。", "每个数执行一次 print。"]},
            {"type": "fill", "prompt": "补全代码，让它重复 10 次。", "template": 'for i in ___(10):\n    print("学习")',
             "answers": ["range"],
             "hints": ["哪个函数能生成一串数字？", "它的意思是“范围”。", "5 个字母，r 开头。"]},
            {"type": "code", "prompt": "用 for 循环显示 1 到 5，每行一个数字。",
             "starter": "", "expected_output": "1\n2\n3\n4\n5\n", "must_use": ["for"],
             "reference": "for i in range(1, 6):\n    print(i)",
             "hints": ["range 可以写开始和结束：range(开始, 结束)。", "结束的那个数取不到。",
                       "想要 1 到 5，结束值应该写 6。"]},
        ],
    },
    {
        "id": 8, "title": "满足条件就一直做：while", "keyword": "while",
        "concept": "while 循环会在条件成立时一直重复，条件不成立就停下。一定要记得在循环里改变条件，"
                   "否则条件永远成立，就会变成停不下来的“死循环”。",
        "analogy": "while 像倒水：只要杯子没满，就一直倒。",
        "example": "n = 1\nwhile n <= 3:\n    print(n)\n    n = n + 1",
        "questions": [
            {"type": "choice", "prompt": "运行后最后显示的数字是？", "code": "n = 1\nwhile n < 4:\n    print(n)\n    n = n + 1",
             "options": ["3", "4", "一直不停"], "answer": 0,
             "hints": ["n 从 1 开始，每次加 1。", "当 n 变成 4 时，n < 4 还成立吗？", "最后一次显示时 n 是几？"]},
            {"type": "fill", "prompt": "补全代码，避免死循环。", "template": "n = 0\nwhile n < 3:\n    print(n)\n    n = n ___ 1",
             "answers": ["+"],
             "hints": ["要让 n 慢慢变大，最后让条件不成立。", "n 每次应该增加。", "用加号。"]},
            {"type": "code", "prompt": "用 while 循环，从 10 开始，每次减 3，显示所有大于 0 的数（10、7、4、1）。",
             "starter": "n = 10\n", "expected_output": "10\n7\n4\n1\n", "must_use": ["while"],
             "reference": "n = 10\nwhile n > 0:\n    print(n)\n    n = n - 3",
             "hints": ["条件是 n > 0。", "循环里先 print(n)，再让 n 变小。", "每次减 3：n = n - 3。"]},
        ],
    },
    {
        "id": 9, "title": "列表：一排小格子", "keyword": "list",
        "concept": "列表可以装很多个值，用方括号 [ ] 包起来，值之间用逗号隔开。"
                   "要拿某一个值，用它的位置（下标），注意位置从 0 开始数。",
        "analogy": "列表像一排储物柜，柜子编号从 0 开始。",
        "example": 'fruits = ["苹果", "香蕉", "橙子"]\nprint(fruits[0])',
        "questions": [
            {"type": "choice", "prompt": "运行后会显示什么？", "code": 'colors = ["红", "绿", "蓝"]\nprint(colors[1])',
             "options": ["红", "绿", "蓝"], "answer": 1,
             "hints": ["列表的位置从几开始数？", "位置 0 是“红”。", "位置 1 是第二个。"]},
            {"type": "fill", "prompt": "在列表末尾加一个“葡萄”。", "template": 'fruits = ["苹果"]\nfruits.___("葡萄")',
             "answers": ["append"],
             "hints": ["哪个方法能在列表末尾添加东西？", "它的意思是“追加”。", "6 个字母，a 开头。"]},
            {"type": "code", "prompt": "已有成绩列表 scores = [80, 95, 70]，用 for 循环把每个成绩显示出来，每行一个。",
             "starter": "scores = [80, 95, 70]\n", "expected_output": "80\n95\n70\n", "must_use": ["for"],
             "reference": "scores = [80, 95, 70]\nfor s in scores:\n    print(s)",
             "hints": ["for 循环可以直接遍历列表：for s in scores:。", "循环里 print(s)。", "记得冒号和缩进。"]},
        ],
    },
    {
        "id": 10, "title": "字典：查名字找内容", "keyword": "dict",
        "concept": "字典用大括号 { } 存“键: 值”的配对，用键就能查到对应的值，就像查字典一样。",
        "analogy": "字典像通讯录：用名字（键）查到电话（值）。",
        "example": 'phone = {"小明": "123", "小红": "456"}\nprint(phone["小红"])',
        "questions": [
            {"type": "choice", "prompt": "运行后会显示什么？", "code": 'price = {"可乐": 3, "薯片": 6}\nprint(price["薯片"])',
             "options": ["3", "6", "薯片"], "answer": 1,
             "hints": ["用键“薯片”去查。", "“薯片”对应的值是多少？", "看冒号右边。"]},
            {"type": "fill", "prompt": "补全代码，查出小明的分数。", "template": 'score = {"小明": 90}\nprint(score[___])',
             "answers": ['"小明"', "'小明'"],
             "hints": ["方括号里放的是“键”。", "键是一个名字。", "名字是文字，要加引号。"]},
            {"type": "code", "prompt": "已有 stock = {\"苹果\": 5, \"香蕉\": 8}，显示香蕉的库存数量。",
             "starter": 'stock = {"苹果": 5, "香蕉": 8}\n', "expected_output": "8\n", "must_use": ["print"],
             "reference": 'stock = {"苹果": 5, "香蕉": 8}\nprint(stock["香蕉"])',
             "hints": ["用键去查值：字典名[键]。", "键是“香蕉”，要加引号。", "把查到的值放进 print。"]},
        ],
    },
    {
        "id": 11, "title": "函数：打包好的小工具", "keyword": "def",
        "concept": "函数是把几行代码打包起来、起个名字，以后一句话就能调用。用 def 定义，return 把结果交回来。",
        "analogy": "函数像榨汁机：放进水果（参数），按下按钮，出来果汁（返回值）。",
        "example": "def double(x):\n    return x * 2\n\nprint(double(5))",
        "questions": [
            {"type": "choice", "prompt": "运行后会显示什么？", "code": "def add(a, b):\n    return a + b\n\nprint(add(2, 3))",
             "options": ["add(2, 3)", "5", "23"], "answer": 1,
             "hints": ["add 函数把 a 和 b 怎么处理？", "a 是 2，b 是 3。", "return 返回 a + b 的结果。"]},
            {"type": "fill", "prompt": "补全代码，定义一个函数。", "template": '___ hello():\n    print("你好")',
             "answers": ["def"],
             "hints": ["定义函数用哪个词？", "它是“定义” define 的缩写。", "3 个字母，d 开头。"]},
            {"type": "code", "prompt": "定义函数 square(n)，返回 n 的平方；然后显示 square(9) 的结果。",
             "starter": "", "expected_output": "81\n", "must_use": ["def", "return"],
             "reference": "def square(n):\n    return n * n\n\nprint(square(9))",
             "hints": ["用 def square(n): 开头。", "函数里用 return 返回 n * n。", "最后 print(square(9))。"]},
        ],
    },
    {
        "id": 12, "title": "综合练习：成绩等级", "keyword": "综合",
        "concept": "把学过的变量、列表、循环、判断、函数组合起来，就能解决真实的小问题。",
        "analogy": "前面学的是积木，这一关是用积木搭一个小房子。",
        "example": "def level(score):\n    if score >= 90:\n        return \"优秀\"\n    return \"继续加油\"\n\nprint(level(95))",
        "questions": [
            {"type": "choice", "prompt": "运行后会显示什么？",
             "code": "count = 0\nfor s in [50, 80, 90]:\n    if s >= 60:\n        count = count + 1\nprint(count)",
             "options": ["1", "2", "3"], "answer": 1,
             "hints": ["count 统计的是什么？", "哪些成绩 >= 60？", "80 和 90 符合条件。"]},
            {"type": "fill", "prompt": "补全代码，让函数返回结果。", "template": 'def grade(s):\n    if s >= 60:\n        ___ "及格"\n    return "不及格"',
             "answers": ["return"],
             "hints": ["函数把结果交回来用哪个词？", "意思是“返回”。", "6 个字母，r 开头。"]},
            {"type": "code", "prompt": "写函数 level(score)：90 分及以上返回“优秀”，60 到 89 返回“及格”，60 以下返回“不及格”。"
                                       "然后对列表 [95, 72, 40] 里的每个成绩，显示它的等级。",
             "starter": "", "expected_output": "优秀\n及格\n不及格\n", "must_use": ["def", "for", "if"],
             "reference": 'def level(score):\n    if score >= 90:\n        return "优秀"\n    elif score >= 60:\n'
                          '        return "及格"\n    return "不及格"\n\nfor s in [95, 72, 40]:\n    print(level(s))',
             "hints": ["先用 def 写函数，里面用 if / elif / else 分三种情况。", "elif 的意思是“否则如果”。",
                       "最后用 for 遍历列表，对每个成绩 print(level(s))。"]},
        ],
    },
]


def lesson_list() -> list[dict]:
    """给“学习路线”页面用：只要标题和关键词，不带题目。"""
    return [{"id": l["id"], "title": l["title"], "keyword": l["keyword"],
             "questions": len(l["questions"])} for l in LESSONS]


def get_lesson(lesson_id: int) -> dict:
    for l in LESSONS:
        if l["id"] == lesson_id:
            return l
    raise KeyError(f"没有第 {lesson_id} 关")


def public_question(lesson_id: int, index: int) -> dict:
    """给做题页面用：去掉参考答案和正确选项，不让答案出现在界面层。"""
    q = dict(get_lesson(lesson_id)["questions"][index])
    for secret in ("answer", "answers", "reference", "expected_output", "hints"):
        q.pop(secret, None)
    return q
