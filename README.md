# Python 起步：给零基础的人入门 Python 的安卓 App

一个安卓原生 App：12 个关卡带完全没学过编程的人入门 Python。每关先用大白话 + 比喻讲概念，再做 3 道题（选择、填空、自己写代码）。写的代码**在手机上真实运行**，做错了可以问 **AI 老师**：只给提示、不给答案。

> 开发方式：用 Claude Code 辅助开发（vibe coding），本人负责需求、交互设计、题目设计、验收和测试。

## 功能

| 功能 | 说明 | 技术 |
|---|---|---|
| 学习路线 | 12 关，从 `print` 到函数，显示每关进度和连续学习天数 | RecyclerView、Material 3 |
| 讲解 + 示例 | 大白话、比喻、可运行的示例代码 | — |
| 三种题型 | 选择、填空、自己写代码 | — |
| 手机上运行 Python | 学生写的代码在 App 内嵌的 Python 解释器里执行，死循环自动停止，危险操作被拦截，报错翻译成大白话并标出第几行 | Chaquopy |
| 判题 | 真的运行代码、比对输出，并检查是否用了这一关要练的写法（如 for 循环），不让 AI 猜对错 | Python `ast` |
| AI 老师 | 三级提示，一次比一次具体；程序检查回复里有没有泄露答案，泄露就换成内置提示；没网、没填 Key 时也能用内置提示 | DeepSeek API（OpenAI 兼容） |
| 错题本、打卡 | 做题记录保存在手机本地，错题可以重做，连续学习天数 | Room |
| 每日提醒 | 晚上 8 点，今天还没学才提醒 | WorkManager、通知 |

## 结构

```
app/src/main/java/com/pystarter/app/     安卓原生部分（Java）
  PyStarterApp.java      启动内嵌 Python、通知渠道、安排提醒
  PyBridge.java          Java 调 Python：后台线程执行，结果回到主线程
  MainActivity.java      学习路线（关卡列表）
  LessonActivity.java    关卡讲解 + 题目入口
  QuestionActivity.java  做题、运行、提交、问 AI 老师
  ProgressActivity.java  进度和错题本
  SettingsActivity.java  API Key、提醒开关
  ReminderWorker.java    每日提醒
  data/                  Room 数据库（做题记录、打卡）
app/src/main/python/pystarter/           核心逻辑（Python）
  lessons.py             12 关内容和题目
  runner.py              安全运行学生代码、翻译报错
  checker.py             判题
  tutor.py               AI 老师（三级提示 + 防泄露答案）
  api.py                 给 Java 调用的接口（统一返回 JSON）
tests/test_core.py       Python 核心逻辑的离线测试
```

**为什么用 Java + Python**：界面和手机能力（通知、数据库、后台任务）用安卓原生的 Java；因为要在手机上真实运行学生写的 Python，所以用 Chaquopy 把 Python 解释器打包进 App，判题和 AI 老师的逻辑也用 Python 写。

## 测试

```bash
python -m pytest -q
```

Python 部分 69 个离线测试，其中包括**题目内容自检**：每道“写代码”题的参考答案必须能通过自己的判题；每道选择题都真的运行代码，正确选项必须和真实输出一致；每个填空答案放回模板后代码必须能运行。

## 运行

用 Android Studio 打开本目录，等 Gradle 同步完成，选择模拟器或真机运行。AI 老师需要在 App 的“设置”里填 API Key（只保存在手机上）；不填也能用内置提示。

## 局限

- 只有安卓端（iOS 原生需要 macOS 和 Xcode）；
- 代码运行的安全限制是教学级别的，不等于真正的沙箱；
- 题库是固定的 12 关 36 题。
