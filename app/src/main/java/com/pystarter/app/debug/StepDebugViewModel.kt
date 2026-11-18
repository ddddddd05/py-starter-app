package com.pystarter.app.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pystarter.app.PyBridge
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

/** 一步的现场：将要执行的行、变量背包、到此为止的输出。 */
data class TraceStep(
    val line: Int?,                       // null 表示程序已经结束
    val vars: Map<String, String>,
    val output: String,
    val newOutput: String,
    val loop: Int?,                       // 在循环里的话是第几轮
    val func: String?,                    // 在哪个函数里（null 表示主程序）
)

data class DebugUiState(
    val loading: Boolean = true,
    val codeLines: List<String> = emptyList(),
    val steps: List<TraceStep> = emptyList(),
    val index: Int = 0,
    val playing: Boolean = false,
    val speedMs: Long = 900,
    val error: String? = null,
    val truncated: Boolean = false,
) {
    val current: TraceStep? get() = steps.getOrNull(index)
    val previous: TraceStep? get() = steps.getOrNull(index - 1)
    val atEnd: Boolean get() = index >= steps.lastIndex
}

/**
 * 单步慢放的状态都放在 ViewModel 里：旋转屏幕时页面会重建，但 ViewModel 不会，
 * 所以播放到第几步、是否在自动播放都不会丢。
 */
class StepDebugViewModel : ViewModel() {

    private val _state = MutableStateFlow(DebugUiState())
    val state: StateFlow<DebugUiState> = _state.asStateFlow()
    private var playJob: Job? = null
    private var loaded = false

    /** 只加载一次（页面重建时不重复跑 Python）。 */
    fun load(code: String, inputsJson: String) {
        if (loaded) return
        loaded = true
        _state.update { it.copy(codeLines = code.split("\n")) }
        PyBridge.call({ result -> onTrace(result) }, "trace_code", code, inputsJson)
    }

    private fun onTrace(r: JSONObject) {
        val steps = mutableListOf<TraceStep>()
        val arr = r.optJSONArray("steps")
        for (i in 0 until (arr?.length() ?: 0)) {
            val s = arr!!.getJSONObject(i)
            val varsJson = s.optJSONObject("vars") ?: JSONObject()
            val vars = linkedMapOf<String, String>()
            varsJson.keys().forEach { k -> vars[k] = varsJson.optString(k) }
            steps += TraceStep(
                line = if (s.isNull("line")) null else s.optInt("line"),
                vars = vars,
                output = s.optString("output"),
                newOutput = s.optString("new_output"),
                loop = if (s.isNull("loop")) null else s.optInt("loop"),
                func = if (s.isNull("func")) null else s.optString("func"),
            )
        }
        // error 可能是学生代码的报错 {"message": ...}，也可能是 Python 层自身出错时的一段文字
        val err = r.optJSONObject("error")?.optString("message")
            ?: (r.opt("error") as? String)
        _state.update {
            it.copy(loading = false, steps = steps, index = 0, error = err,
                truncated = r.optBoolean("truncated"))
        }
    }

    fun next() = _state.update { if (it.atEnd) it else it.copy(index = it.index + 1) }

    fun prev() {
        pause()
        _state.update { if (it.index == 0) it else it.copy(index = it.index - 1) }
    }

    fun restart() {
        pause()
        _state.update { it.copy(index = 0) }
    }

    fun togglePlay() = if (_state.value.playing) pause() else play()

    fun cycleSpeed() = _state.update {
        it.copy(speedMs = when (it.speedMs) { 900L -> 450L; 450L -> 1500L; else -> 900L })
    }

    private fun play() {
        if (_state.value.atEnd) _state.update { it.copy(index = 0) }
        _state.update { it.copy(playing = true) }
        playJob?.cancel()
        playJob = viewModelScope.launch {
            while (isActive && !_state.value.atEnd) {
                delay(_state.value.speedMs)
                next()
            }
            _state.update { it.copy(playing = false) }
        }
    }

    private fun pause() {
        playJob?.cancel()
        _state.update { it.copy(playing = false) }
    }
}
