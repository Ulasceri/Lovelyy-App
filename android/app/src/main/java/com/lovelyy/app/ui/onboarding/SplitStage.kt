package com.lovelyy.app.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Quicksand
import com.lovelyy.app.ui.theme.StageMint
import com.lovelyy.app.ui.theme.StagePeach
import kotlin.math.atan2

// The onboarding idea (approved design, boards A1-A6): the screen is split by a slanted seam.
// Above the seam is the partner's screen (mint), below it is yours (peach). What you do on your
// side shows up on theirs.

/** How far the seam drops from the right edge to the left edge, as a share of the stage height. */
private const val SEAM_DROP = 0.12f

/** The seam's tilt in degrees for a stage of this size (negative = rises to the right). */
fun seamAngle(widthPx: Float, heightPx: Float): Float =
    -Math.toDegrees(atan2((SEAM_DROP * heightPx).toDouble(), widthPx.toDouble())).toFloat()

/** The two-sided stage. [content] is placed over it; the seam always crosses the centre. */
@Composable
fun SplitStage(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .background(StageMint)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val left = size.height * (0.5f + SEAM_DROP / 2)
            val right = size.height * (0.5f - SEAM_DROP / 2)
            val mine = Path().apply {
                moveTo(0f, left)
                lineTo(size.width, right)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(mine, StagePeach)
            drawLine(Cream, Offset(0f, left), Offset(size.width, right), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        }
        content()
    }
}

/** White pill with a round badge: "Senin ekranın", "Sevgilinin ekranı". */
@Composable
fun SideChip(text: String, color: Color, modifier: Modifier = Modifier, badge: @Composable () -> Unit) {
    Row(
        modifier
            .softShadow(CircleShape, 4.dp)
            .clip(CircleShape)
            .background(CreamCard)
            .padding(start = 5.dp, top = 5.dp, bottom = 5.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) { badge() }
        Text(text, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown, maxLines = 1)
    }
}

/** A notification-like white card: round badge, bold line, soft line. */
@Composable
fun StageBubble(title: String, subtitle: String, modifier: Modifier = Modifier, badge: @Composable () -> Unit) {
    Row(
        modifier
            .softShadow(RoundedCornerShape(18.dp), 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CreamCard)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        badge()
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 19.sp, color = Brown, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, color = BrownSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Round badge used inside chips and bubbles. */
@Composable
fun Badge(color: Color, size: Dp = 36.dp, content: @Composable () -> Unit) {
    Box(Modifier.size(size).clip(CircleShape).background(color), contentAlignment = Alignment.Center) { content() }
}

/** Shows [content] with a small pop once [visible], after [delayMs] (the design's `lv-pop`). */
@Composable
fun PopIn(visible: Boolean, modifier: Modifier = Modifier, delayMs: Int = 0, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = scaleIn(tween(350, delayMillis = delayMs), initialScale = 0.7f) + fadeIn(tween(200, delayMillis = delayMs)),
        exit = fadeOut(tween(120)),
    ) { content() }
}

/** A ring that keeps growing and fading around a "try me" control. */
@Composable
fun PulseRing(color: Color, modifier: Modifier = Modifier) {
    val t by rememberInfiniteTransition(label = "pulse").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Restart), label = "t",
    )
    Box(
        modifier
            .graphicsLayer {
                scaleX = 1f + 0.35f * t
                scaleY = 1f + 0.35f * t
                alpha = (1f - t) * 0.5f
            }
            .border(3.dp, color, CircleShape)
    )
}

/** Thin story-style progress bars: [filled] of [total]. */
@Composable
fun StageProgress(filled: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(if (i < filled) Brown else Brown.copy(alpha = 0.2f))
            )
        }
    }
}
