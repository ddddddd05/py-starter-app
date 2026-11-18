package com.pystarter.app

import androidx.lifecycle.ViewModel
import org.json.JSONArray
import org.json.JSONObject

/**
 * 一场挑战的全部状态。
 *
 * 以前这些都是 BattleActivity 的字段：旋转手机时页面会销毁重建，字段全部丢失，
 * 战斗就从第 1 战重新开始、心也回满（甚至可以被用来“回血”）。
 * 放进 ViewModel 后，页面重建时状态还在，只需要按状态把界面重新画出来。
 */
class BattleViewModel : ViewModel() {
    // 这一关的数据（只从 Python 取一次）
    @JvmField var info: JSONObject? = null          // level_info：章节、敌人、学习卡片
    @JvmField var questions: JSONArray? = null      // 这一关的 3 道题（不含答案）
    @JvmField var theme: String = "village"

    // 进度
    @JvmField var stage = 0
    @JvmField var hearts = MAX_HEARTS
    @JvmField var mistakes = 0
    @JvmField var hintsUsed = 0
    @JvmField var battlesWon = 0
    @JvmField var usedNotes = false
    @JvmField var busy = false
    @JvmField var stageWon = false

    // 当前这场战斗（复仇怪的题来自更早的关卡）
    @JvmField var qLesson = 0
    @JvmField var qIndex = 0
    @JvmField var question: JSONObject? = null
    @JvmField var type: String = ""
    @JvmField var hintLevel = 0
    @JvmField var lastChoice = -1
    @JvmField var revenge: JSONObject? = null
    @JvmField var revengeName: String? = null

    // 重建界面需要的显示信息
    @JvmField var enemyName: String = ""
    @JvmField var stageTitle: String = ""

    val loaded: Boolean get() = info != null && questions != null

    companion object {
        const val MAX_HEARTS = 3
    }
}
