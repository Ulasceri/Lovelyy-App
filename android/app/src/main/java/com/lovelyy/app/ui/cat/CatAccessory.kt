package com.lovelyy.app.ui.cat

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.Composable
import com.lovelyy.app.data.CareRewards.Accessory

private val Rose = Color(0xFFE86A8A)
private val RoseDeep = Color(0xFFC94E6E)
private val Orange = Color(0xFFF07B45)
private val OrangeDeep = Color(0xFFC95E2E)
private val Ink = Color(0xFF4A3B35)
private val PeachBand = Color(0xFFFFB38A)

/**
 * Draws [accessory] on the sitting cat. Coordinates are in the 720×720 cat picture (cat_base and the first
 * frame of the idle video): eyes at (305, 258) and (413, 258), neck at y ≈ 368, top of the head at y ≈ 150.
 * Fill the same square box the picture fills.
 */
@Composable
fun CatAccessoryLayer(accessory: Accessory, modifier: Modifier) {
    Canvas(modifier) {
        val k = size.width / 720f
        when (accessory) {
            Accessory.Kurdele -> bow(Offset(440f * k, 170f * k), 34f * k, Rose, RoseDeep, tilt = -18f)
            Accessory.Papyon -> bow(Offset(360f * k, 370f * k), 40f * k, Orange, OrangeDeep, tilt = 0f)
            Accessory.Sapka -> hat(k)
            Accessory.Gozluk -> glasses(k)
        }
    }
}

/** A bow: two rounded wings and a knot. [s] = half the height of a wing. */
private fun DrawScope.bow(c: Offset, s: Float, color: Color, deep: Color, tilt: Float) {
    rotateAround(c, tilt) {
        val left = Path().apply {
            moveTo(c.x, c.y)
            cubicTo(c.x - s * 0.8f, c.y - s * 1.3f, c.x - s * 2.1f, c.y - s * 1.1f, c.x - s * 2f, c.y)
            cubicTo(c.x - s * 2.1f, c.y + s * 1.1f, c.x - s * 0.8f, c.y + s * 1.3f, c.x, c.y)
            close()
        }
        val right = Path().apply {
            moveTo(c.x, c.y)
            cubicTo(c.x + s * 0.8f, c.y - s * 1.3f, c.x + s * 2.1f, c.y - s * 1.1f, c.x + s * 2f, c.y)
            cubicTo(c.x + s * 2.1f, c.y + s * 1.1f, c.x + s * 0.8f, c.y + s * 1.3f, c.x, c.y)
            close()
        }
        drawPath(left, color)
        drawPath(right, color)
        drawPath(left, deep, style = Stroke(s * 0.12f))
        drawPath(right, deep, style = Stroke(s * 0.12f))
        drawRoundRect(deep, Offset(c.x - s * 0.45f, c.y - s * 0.55f), Size(s * 0.9f, s * 1.1f), CornerRadius(s * 0.35f))
    }
}

/** A small brown hat with a peach band, sitting between the ears. */
private fun DrawScope.hat(k: Float) {
    val cx = 360f * k
    drawOval(Ink, Offset(cx - 92f * k, 150f * k), Size(184f * k, 34f * k))
    drawRoundRect(Ink, Offset(cx - 56f * k, 78f * k), Size(112f * k, 86f * k), CornerRadius(18f * k))
    drawRect(PeachBand, Offset(cx - 56f * k, 136f * k), Size(112f * k, 18f * k))
}

/** Round glasses over the eyes. */
private fun DrawScope.glasses(k: Float) {
    val r = 40f * k
    val w = 7f * k
    val l = Offset(305f * k, 260f * k)
    val rt = Offset(413f * k, 260f * k)
    drawCircle(Color.White.copy(alpha = 0.18f), r, l)
    drawCircle(Color.White.copy(alpha = 0.18f), r, rt)
    drawCircle(Ink, r, l, style = Stroke(w))
    drawCircle(Ink, r, rt, style = Stroke(w))
    drawLine(Ink, Offset(l.x + r, l.y - 4f * k), Offset(rt.x - r, rt.y - 4f * k), w, StrokeCap.Round)
}

private inline fun DrawScope.rotateAround(c: Offset, degrees: Float, block: DrawScope.() -> Unit) {
    drawContext.transform.rotate(degrees, c)
    block()
    drawContext.transform.rotate(-degrees, c)
}
