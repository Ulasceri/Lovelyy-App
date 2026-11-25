package com.lovelyy.app.ui.heartbeat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.lovelyy.app.data.Emotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Colors and motion of each feeling. */
data class EmotionStyle(val color: Color, val soft: Color, val deep: Color)

fun Emotion.style(): EmotionStyle = when (this) {
    Emotion.Ozlem -> EmotionStyle(Color(0xFF8EC5E8), Color(0xFFE3F1FA), Color(0xFF3F7FA8))
    Emotion.Sevgi -> EmotionStyle(Color(0xFFFFB38A), Color(0xFFFFE6D6), Color(0xFFE07A45))
    Emotion.Ask -> EmotionStyle(Color(0xFFFF6F91), Color(0xFFFFE0E8), Color(0xFFD94470))
    Emotion.Sarilma -> EmotionStyle(Color(0xFFF5B95C), Color(0xFFFDEFD6), Color(0xFFC98A28))
    Emotion.Opucuk -> EmotionStyle(Color(0xFFF48FB1), Color(0xFFFCE4EE), Color(0xFFD0587F))
    Emotion.Huzur -> EmotionStyle(Color(0xFF7CCBB4), Color(0xFFDDF2EC), Color(0xFF3F8C75))
}

private const val LOOP_MS = 12_000f

/**
 * The animated picture of a feeling: a beating heart plus particles that differ per emotion
 * (rising hearts for sevgi, a burst for aşk, twinkling stars for özlem, hugging arms for
 * sarılma, orbiting kisses for öpücük, slow ripples for huzur).
 * [energy] 0..1 makes it stronger (used while the heart is being held).
 */
@Composable
fun EmotionVisual(emotion: Emotion, modifier: Modifier = Modifier, energy: Float = 0f, heartScale: Float = 0.42f) {
    val t by rememberInfiniteTransition(label = "emotion").animateFloat(
        0f, 1f, infiniteRepeatable(tween(LOOP_MS.toInt(), easing = LinearEasing)), label = "t",
    )
    val style = remember(emotion) { emotion.style() }
    Canvas(modifier) {
        val ms = t * LOOP_MS
        val c = center
        val r = size.minDimension / 2f

        // Soft glow behind everything.
        drawCircle(
            Brush.radialGradient(listOf(style.color.copy(alpha = 0.30f + 0.2f * energy), Color.Transparent), c, r),
            radius = r, center = c,
        )

        when (emotion) {
            Emotion.Sevgi -> rising(ms, style, r, energy)
            Emotion.Ask -> burst(ms, style, r, energy)
            Emotion.Ozlem -> twinkle(ms, style, r, energy)
            Emotion.Sarilma -> Unit // arms are drawn around the heart below
            Emotion.Opucuk -> orbit(ms, style, r, energy)
            Emotion.Huzur -> ripples(ms, style, r, energy)
        }

        val boost = 1f + 0.6f * energy
        val beat = when (emotion) {
            Emotion.Sevgi -> doubleBeat(phase(ms, 1000f), 0.10f * boost)
            Emotion.Ask -> doubleBeat(phase(ms, 600f), 0.14f * boost)
            Emotion.Ozlem -> 1f + 0.06f * boost * sin(phase(ms, 2400f) * 2 * PI).toFloat()
            Emotion.Sarilma -> 1f
            Emotion.Opucuk -> doubleBeat(phase(ms, 1200f), 0.08f * boost)
            Emotion.Huzur -> 1f + 0.05f * boost * sin(phase(ms, 3000f) * 2 * PI).toFloat()
        }
        val hs = r * 2 * heartScale
        if (emotion == Emotion.Sarilma) {
            hug(ms, style, hs, energy)
        } else {
            val tilt = if (emotion == Emotion.Opucuk) 8f * sin(phase(ms, 1200f) * 2 * PI).toFloat() else 0f
            val droop = if (emotion == Emotion.Ozlem) 4f * sin(phase(ms, 2400f) * 2 * PI).toFloat() else 0f
            rotate(tilt + droop, c) { heart(c, hs * beat, style) }
        }
    }
}

private fun phase(ms: Float, period: Float) = (ms % period) / period

/** ba-dum: a big bump, then a smaller one, then rest. */
private fun doubleBeat(p: Float, amp: Float): Float = when {
    p < 0.14f -> 1f + amp * sin(p / 0.14f * PI).toFloat()
    p in 0.22f..0.36f -> 1f + amp * 0.65f * sin((p - 0.22f) / 0.14f * PI).toFloat()
    else -> 1f
}

fun heartPath(w: Float, h: Float, path: Path = Path()): Path = path.apply {
    reset()
    moveTo(w * 0.5f, h * 0.92f)
    cubicTo(w * 0.18f, h * 0.70f, -w * 0.02f, h * 0.45f, w * 0.12f, h * 0.22f)
    cubicTo(w * 0.24f, h * 0.02f, w * 0.46f, h * 0.06f, w * 0.5f, h * 0.27f)
    cubicTo(w * 0.54f, h * 0.06f, w * 0.76f, h * 0.02f, w * 0.88f, h * 0.22f)
    cubicTo(w * 1.02f, h * 0.45f, w * 0.82f, h * 0.70f, w * 0.5f, h * 0.92f)
    close()
}

private fun DrawScope.heart(center: Offset, s: Float, style: EmotionStyle, alpha: Float = 1f, shine: Boolean = true) {
    translate(center.x - s / 2, center.y - s / 2) {
        val p = heartPath(s, s)
        drawPath(p, Brush.verticalGradient(listOf(style.color, style.deep), 0f, s), alpha = alpha)
        if (shine) {
            drawCircle(Color.White.copy(alpha = 0.45f * alpha), radius = s * 0.07f, center = Offset(s * 0.28f, s * 0.30f))
        }
    }
}

private fun DrawScope.miniHeart(at: Offset, s: Float, color: Color, alpha: Float) {
    translate(at.x - s / 2, at.y - s / 2) { drawPath(heartPath(s, s), color, alpha = alpha.coerceIn(0f, 1f)) }
}

private fun DrawScope.rising(ms: Float, st: EmotionStyle, r: Float, energy: Float) {
    val n = 9 + (6 * energy).toInt()
    for (i in 0 until n) {
        val p = (phase(ms, 3200f) + i.toFloat() / n) % 1f
        val x = center.x + sin((i * 1.7f) + p * 3f) * r * 0.55f
        val y = center.y + r * 0.7f - p * r * 1.5f
        val a = if (p < 0.2f) p / 0.2f else 1f - (p - 0.2f) / 0.8f
        miniHeart(Offset(x, y), r * (0.10f + 0.05f * ((i % 3) / 2f)), if (i % 2 == 0) st.color else st.deep, a * 0.9f)
    }
}

private fun DrawScope.burst(ms: Float, st: EmotionStyle, r: Float, energy: Float) {
    val n = 12
    for (wave in 0..1) {
        val p = (phase(ms, 1200f) + wave * 0.5f) % 1f
        for (i in 0 until n) {
            val ang = (i * 2 * PI / n + wave * PI / n).toFloat()
            val d = r * (0.3f + 0.65f * p)
            val at = Offset(center.x + cos(ang) * d, center.y + sin(ang) * d)
            miniHeart(at, r * (0.09f + 0.04f * energy) * (1.2f - p * 0.5f), if (i % 2 == 0) st.color else st.deep, 1f - p)
        }
    }
}

private fun DrawScope.twinkle(ms: Float, st: EmotionStyle, r: Float, energy: Float) {
    val spots = listOf(-0.7f to -0.5f, 0.65f to -0.6f, -0.8f to 0.25f, 0.75f to 0.3f, -0.3f to -0.85f, 0.35f to -0.82f, -0.55f to 0.7f, 0.5f to 0.72f)
    spots.forEachIndexed { i, (dx, dy) ->
        val p = (phase(ms, 2400f) + i * 0.13f) % 1f
        val a = sin(p * PI).toFloat()
        val s = r * (0.05f + 0.03f * a) * (1f + energy)
        val at = Offset(center.x + dx * r * 0.85f, center.y + dy * r * 0.85f)
        star(at, s, st.deep.copy(alpha = 0.35f + 0.55f * a))
    }
    // Slow falling dots, like quiet rain while missing someone.
    for (i in 0 until 6) {
        val p = (phase(ms, 4000f) + i / 6f) % 1f
        val x = center.x + (i - 2.5f) * r * 0.28f
        val y = center.y - r * 0.9f + p * r * 1.8f
        drawCircle(st.color.copy(alpha = 0.5f * sin(p * PI).toFloat()), radius = r * 0.025f, center = Offset(x, y))
    }
}

private fun DrawScope.star(at: Offset, s: Float, color: Color) {
    val p = Path().apply {
        moveTo(at.x, at.y - s * 2)
        quadraticTo(at.x, at.y, at.x + s * 2, at.y)
        quadraticTo(at.x, at.y, at.x, at.y + s * 2)
        quadraticTo(at.x, at.y, at.x - s * 2, at.y)
        quadraticTo(at.x, at.y, at.x, at.y - s * 2)
        close()
    }
    drawPath(p, color)
}

private fun DrawScope.orbit(ms: Float, st: EmotionStyle, r: Float, energy: Float) {
    val n = 6
    val base = phase(ms, 4000f) * 2 * PI
    for (i in 0 until n) {
        val ang = (base + i * 2 * PI / n).toFloat()
        val d = r * (0.62f + 0.06f * sin(ang * 3))
        val at = Offset(center.x + cos(ang) * d, center.y + sin(ang) * d * 0.8f)
        val s = r * (0.11f + 0.04f * energy)
        rotate(ang * 57.3f + 90f, at) { miniHeart(at, s, if (i % 2 == 0) st.color else st.deep, 0.85f) }
    }
}

private fun DrawScope.ripples(ms: Float, st: EmotionStyle, r: Float, energy: Float) {
    for (i in 0 until 3) {
        val p = (phase(ms, 3000f) + i / 3f) % 1f
        drawCircle(
            st.deep.copy(alpha = 0.35f * (1f - p) * (1f + energy)),
            radius = r * (0.35f + 0.6f * p),
            center = center,
            style = Stroke(width = r * 0.025f),
        )
    }
}

/** Sarılma: the heart squeezes while two soft arms close around it. */
private fun DrawScope.hug(ms: Float, st: EmotionStyle, hs: Float, energy: Float) {
    val p = phase(ms, 1500f)
    val squeeze = (sin(p * 2 * PI).toFloat() + 1f) / 2f // 0..1
    val sx = 1f - 0.08f * squeeze * (1f + energy)
    val sy = 1f + 0.05f * squeeze * (1f + energy)
    scale(sx, sy, center) { heart(center, hs, st) }
    val gap = hs * (0.62f - 0.10f * squeeze)
    val armW = hs * 0.16f
    for (side in listOf(-1f, 1f)) {
        val cx = center.x + side * gap
        drawArc(
            color = st.deep.copy(alpha = 0.85f),
            startAngle = if (side < 0) 120f else -60f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(cx - hs * 0.45f, center.y - hs * 0.45f),
            size = Size(hs * 0.9f, hs * 0.9f),
            style = Stroke(width = armW, cap = StrokeCap.Round),
        )
    }
}
