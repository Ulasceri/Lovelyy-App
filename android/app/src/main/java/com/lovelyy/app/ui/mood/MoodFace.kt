package com.lovelyy.app.ui.mood

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.lovelyy.app.data.MoodKind
import com.lovelyy.app.ui.components.drawSvg
import com.lovelyy.app.ui.components.svgPath
import com.lovelyy.app.ui.theme.Brown

/** One face of the approved mood design: colored circle, eyes and mouth drawn in a 48×48 box. */
data class FaceStyle(val color: Color, val eyes: String, val eyesFilled: Boolean, val mouth: String)

private const val DOTS = "M17 20v2M31 20v2"

/** The 8 moods shown in the picker, in the design's order. */
val PickerMoods = listOf(
    MoodKind.Mutlu, MoodKind.Asik, MoodKind.Huzurlu, MoodKind.Ozlemli,
    MoodKind.Yorgun, MoodKind.Uzgun, MoodKind.Sinirli, MoodKind.Hasta,
)

/** Faces from the design. Older moods that are no longer in the picker borrow the closest face. */
fun MoodKind.face(): FaceStyle = when (this) {
    MoodKind.Mutlu, MoodKind.Heyecanli, MoodKind.Enerjik ->
        FaceStyle(Color(0xFFFFD9A0), DOTS, false, "M16 28c2 4 5 6 8 6s6-2 8-6")
    MoodKind.Asik -> FaceStyle(
        Color(0xFFFFB3B3),
        "M17 24s-4-2.5-4-5.2a2.2 2.2 0 0 1 4-1.3 2.2 2.2 0 0 1 4 1.3c0 2.7-4 5.2-4 5.2z M31 24s-4-2.5-4-5.2a2.2 2.2 0 0 1 4-1.3 2.2 2.2 0 0 1 4 1.3c0 2.7-4 5.2-4 5.2z",
        true, "M17 29c2 3 4.5 4.5 7 4.5s5-1.5 7-4.5",
    )
    MoodKind.Huzurlu -> FaceStyle(Color(0xFFBFE6DA), "M14 21c1.5 1.5 4.5 1.5 6 0M28 21c1.5 1.5 4.5 1.5 6 0", false, "M18 30c2 2 10 2 12 0")
    MoodKind.Ozlemli -> FaceStyle(Color(0xFFCFD8F5), "M17 20v2M31 20v2M33.5 26.5v2.5", false, "M19 32c2-1.5 8-1.5 10 0")
    MoodKind.Yorgun -> FaceStyle(Color(0xFFE8DCD3), "M14 21h6M28 21h6", false, "M22 31a2 2 0 1 0 4 0a2 2 0 1 0-4 0")
    MoodKind.Uzgun, MoodKind.Sikkin -> FaceStyle(Color(0xFFC8DDF0), DOTS, false, "M17 33c2-3 4.5-4.5 7-4.5s5 1.5 7 4.5")
    MoodKind.Sinirli, MoodKind.Stresli -> FaceStyle(Color(0xFFF7B9A6), "M14 17.5l6 3M34 17.5l-6 3M17 23.5v1M31 23.5v1", false, "M18 32c3-2 9-2 12 0")
    MoodKind.Hasta -> FaceStyle(Color(0xFFD5E8B8), "M15 19l4 3M19 19l-4 3M29 19l4 3M33 19l-4 3", false, "M17 31c2-2 3 2 5 0s3 2 5 0 2.5-1 4 0")
}

/** "Not shared yet": a quiet face with a flat mouth. */
private val EmptyFace = FaceStyle(Color(0xFFF3ECE6), DOTS, false, "M19 31h10")

@Composable
fun MoodFace(kind: MoodKind?, modifier: Modifier) {
    val f = kind?.face() ?: EmptyFace
    val eyes = remember(f.eyes) { svgPath(f.eyes) }
    val mouth = remember(f.mouth) { svgPath(f.mouth) }
    Canvas(modifier) {
        val r = size.minDimension / 48f * 22f
        drawCircle(f.color, r)
        if (f.eyesFilled) drawSvg(eyes, 48f, Brown)
        drawSvg(eyes, 48f, Brown, strokeWidth = 2.6f)
        drawSvg(mouth, 48f, Brown, strokeWidth = 2.6f)
    }
}
