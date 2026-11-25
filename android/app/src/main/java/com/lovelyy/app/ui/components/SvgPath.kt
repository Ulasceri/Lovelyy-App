package com.lovelyy.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Parses an SVG path string ("M12 20s-7-4.35...") once. Used to draw the design's exact icons. */
fun svgPath(d: String): Path = PathParser().parsePathString(d).toPath()

/** Draws [path] (drawn in a [viewBox]-sized square) scaled to this canvas, filled or stroked. */
fun DrawScope.drawSvg(path: Path, viewBox: Float, color: Color, strokeWidth: Float? = null) {
    val s = size.minDimension / viewBox
    withTransform({ scale(s, s, pivot = androidx.compose.ui.geometry.Offset.Zero) }) {
        if (strokeWidth == null) drawPath(path, color)
        else drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** A filled icon from the design (24×24 viewBox). */
@Composable
fun SvgIcon(d: String, color: Color, modifier: Modifier) {
    val path = remember(d) { svgPath(d) }
    Canvas(modifier) { drawSvg(path, 24f, color) }
}

/** A stroked icon from the design (24×24 viewBox, round caps). */
@Composable
fun SvgStrokeIcon(d: String, color: Color, modifier: Modifier, strokeWidth: Float = 2f) {
    val path = remember(d) { svgPath(d) }
    Canvas(modifier) { drawSvg(path, 24f, color, strokeWidth) }
}

/** The design's heart (24×24). */
const val HEART_D = "M12 20s-7-4.35-7-10a4 4 0 0 1 7-2.65A4 4 0 0 1 19 10c0 5.65-7 10-7 10z"

/** 44dp white round button with a stroked design icon (edit, history...). */
@Composable
fun RoundSvgButton(d: String, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier
            .size(44.dp)
            .softShadow(androidx.compose.foundation.shape.CircleShape, 4.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(com.lovelyy.app.ui.theme.CreamCard)
            .semantics { contentDescription = label; role = Role.Button }
            .bounceClick(onClick = onClick),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        SvgStrokeIcon(d, com.lovelyy.app.ui.theme.Brown, Modifier.size(22.dp))
    }
}

/** Clock icon used by the "Geçmiş" buttons. */
const val CLOCK_D = "M12 3.5a8.5 8.5 0 1 1 0 17a8.5 8.5 0 1 1 0-17zM12 7.5V12l3 2"
