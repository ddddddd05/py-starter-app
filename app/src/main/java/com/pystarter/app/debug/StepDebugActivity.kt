package com.pystarter.app.debug

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.pystarter.app.game.PixelSpriteView
import com.pystarter.app.game.Sprites
import kotlin.math.roundToInt

/**
 * 单步慢放（可视化调试器），用 Kotlin + Jetpack Compose 写：
 * 代码一行一行地执行——当前行高亮，“变量背包”实时显示每个变量的值（刚变化的会闪黄），
 * 下面是到这一步为止的输出；每当有新的输出，像素小骑士就砍一刀，在循环里还会显示“第几轮”。
 *
 * 逐行执行由 Python 的 sys.settrace 完成（pystarter/tracer.py），这里只负责“回放”。
 */
class StepDebugActivity : ComponentActivity() {

    private val vm: StepDebugViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val code = intent.getStringExtra(EXTRA_CODE).orEmpty()
        val inputs = intent.getStringExtra(EXTRA_INPUTS) ?: "[]"
        vm.load(code, inputs)
        setContent { DebugScreen(vm, onBack = { finish() }) }
    }

    companion object {
        const val EXTRA_CODE = "code"
        const val EXTRA_INPUTS = "inputs"

        /** Java 页面也能直接调用：StepDebugActivity.Companion.start(this, code, inputsJson)。 */
        @JvmStatic
        fun start(context: Context, code: String, inputsJson: String) {
            context.startActivity(Intent(context, StepDebugActivity::class.java)
                .putExtra(EXTRA_CODE, code)
                .putExtra(EXTRA_INPUTS, inputsJson))
        }
    }
}

// ---------- 像素风配色（和 res/values/colors.xml 一致） ----------
private val PxBg = Color(0xFF14182E)
private val PxPanel = Color(0xFF1D2B53)
private val PxBlack = Color(0xFF000000)
private val PxWhite = Color(0xFFFFF1E8)
private val PxYellow = Color(0xFFFFEC27)
private val PxGreen = Color(0xFF00E436)
private val PxBlue = Color(0xFF29ADFF)
private val PxRed = Color(0xFFFF004D)
private val PxStone = Color(0xFF5F574F)
private val PxSilver = Color(0xFFC2C3C7)
private val Mono = FontFamily.Monospace

@Composable
private fun DebugScreen(vm: StepDebugViewModel, onBack: () -> Unit) {
    val s by vm.state.collectAsState()
    Column(
        Modifier
            .fillMaxSize()
            .background(PxBg)
            .systemBarsPadding()
    ) {
        // 顶部栏
        Row(
            Modifier
                .fillMaxWidth()
                .background(PxBlack)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PxLabel("<", PxYellow, 20.sp, Modifier.clickable { onBack() }.padding(end = 12.dp))
            PxLabel("单步慢放", PxWhite, 16.sp, Modifier.weight(1f))
            if (!s.loading) PxLabel("第 ${s.index + 1}/${s.steps.size} 步", PxBlue, 14.sp)
        }

        if (s.loading) {
            PxLabel("小法师正在一行一行地执行……", PxSilver, 15.sp, Modifier.padding(20.dp))
            return@Column
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            HeroStrip(s)
            Spacer(Modifier.height(12.dp))
            CodePanel(s)
            Spacer(Modifier.height(12.dp))
            VarsPanel(s)
            Spacer(Modifier.height(12.dp))
            OutputPanel(s)
        }

        Controls(s, vm)
    }
}

/** 小骑士：有新输出时冲上去砍一刀；在循环里显示第几轮。 */
@Composable
private fun HeroStrip(s: DebugUiState) {
    val cur = s.current ?: return
    val lunge = remember { Animatable(0f) }
    LaunchedEffect(s.index) {
        if (cur.newOutput.isNotEmpty() && s.index > 0) {
            lunge.animateTo(48f, tween(140))
            lunge.animateTo(0f, tween(180))
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .pxPanel(PxPanel)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AndroidView(
            factory = { ctx -> PixelSpriteView(ctx).apply { setSprite(Sprites.hero()) } },
            modifier = Modifier
                .size(56.dp)
                .offset { IntOffset(lunge.value.roundToInt(), 0) }
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            val where = when {
                cur.line == null -> "程序运行结束"
                cur.func != null -> "在函数 ${cur.func}() 里，准备执行第 ${cur.line} 行"
                else -> "准备执行第 ${cur.line} 行"
            }
            PxLabel(where, PxWhite, 14.sp)
            if (cur.loop != null) PxLabel("循环 · 第 ${cur.loop} 轮", PxYellow, 13.sp)
            if (cur.newOutput.isNotEmpty() && s.index > 0) PxLabel("砍！输出了：${cur.newOutput.trimEnd()}", PxGreen, 13.sp)
        }
    }
}

/** 代码：当前行高亮并带箭头。 */
@Composable
private fun CodePanel(s: DebugUiState) {
    val line = s.current?.line
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF0B0F1F))
            .border(2.dp, PxStone)
            .padding(vertical = 8.dp)
    ) {
        s.codeLines.forEachIndexed { i, text ->
            val active = line == i + 1
            val bg by animateColorAsState(if (active) Color(0x55FFEC27) else Color.Transparent, label = "line")
            // 整行高亮；代码太长时自动换行，不用左右滑
            Row(Modifier.fillMaxWidth().background(bg).padding(horizontal = 8.dp, vertical = 2.dp)) {
                PxLabel(if (active) "▶" else " ", PxYellow, 14.sp, Modifier.width(18.dp))
                PxLabel("${i + 1}".padStart(2), if (active) PxYellow else PxStone, 14.sp, Modifier.width(26.dp))
                Text(text.ifEmpty { " " }, color = if (active) PxWhite else PxSilver,
                    fontFamily = Mono, fontSize = 15.sp, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** 变量背包：每个变量一个格子；这一步刚变化的格子变黄。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VarsPanel(s: DebugUiState) {
    val cur = s.current?.vars.orEmpty()
    val prev = s.previous?.vars.orEmpty()
    PxLabel("变量背包", PxYellow, 15.sp)
    Spacer(Modifier.height(6.dp))
    if (cur.isEmpty()) {
        PxLabel("（还没有变量）", PxStone, 14.sp)
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cur.forEach { (name, value) ->
            val changed = prev[name] != value
            val color by animateColorAsState(if (changed) PxYellow else PxWhite, label = "var")
            Column(
                Modifier
                    .pxPanel(if (changed) Color(0xFF3A3410) else PxPanel)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                PxLabel(name, PxBlue, 13.sp)
                PxLabel(value, color, 15.sp)
            }
        }
    }
}

@Composable
private fun OutputPanel(s: DebugUiState) {
    PxLabel("屏幕输出", PxYellow, 15.sp)
    Spacer(Modifier.height(6.dp))
    val text = s.current?.output.orEmpty()
    Text(
        text.ifEmpty { "（还没有输出）" },
        color = PxGreen, fontFamily = Mono, fontSize = 14.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(PxBlack)
            .border(2.dp, PxGreen)
            .padding(10.dp)
    )
    if (s.atEnd && s.error != null) {
        Spacer(Modifier.height(8.dp))
        PxLabel("⚠ ${s.error}", PxRed, 14.sp)
    }
    if (s.atEnd && s.truncated) {
        Spacer(Modifier.height(8.dp))
        PxLabel("步数太多，只回放了前面一段。检查一下循环会不会停下来。", PxRed, 14.sp)
    }
}

@Composable
private fun Controls(s: DebugUiState, vm: StepDebugViewModel) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(PxBlack)
            .padding(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PxButton("< 上一步", PxStone, PxWhite, Modifier.weight(1f)) { vm.prev() }
            PxButton(if (s.playing) "暂停" else if (s.atEnd) "重新播放" else "自动播放", PxGreen, PxBlack,
                Modifier.weight(1.2f)) { vm.togglePlay() }
            PxButton("下一步 >", PxBlue, PxBlack, Modifier.weight(1f)) { vm.next() }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val speed = when (s.speedMs) { 450L -> "快"; 1500L -> "慢"; else -> "中" }
            PxButton("速度：$speed", PxYellow, PxBlack, Modifier.weight(1f)) { vm.cycleSpeed() }
            PxButton("从头开始", PxStone, PxWhite, Modifier.weight(1f)) { vm.restart() }
        }
    }
}

// ---------- 像素风小组件 ----------

@Composable
private fun PxLabel(text: String, color: Color, size: androidx.compose.ui.unit.TextUnit, modifier: Modifier = Modifier) {
    Text(text, color = color, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = size, modifier = modifier)
}

/** 方角 + 黑描边 + 右下硬阴影，和 XML 里的像素面板一致。 */
private fun Modifier.pxPanel(fill: Color): Modifier = this
    .background(PxBlack)
    .padding(end = 3.dp, bottom = 3.dp)
    .background(fill)
    .border(2.dp, PxWhite)

@Composable
private fun PxButton(text: String, fill: Color, textColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(50.dp)
            .background(PxBlack)
            .padding(end = 4.dp, bottom = 4.dp)
            .background(fill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        PxLabel(text, textColor, 15.sp)
    }
}
