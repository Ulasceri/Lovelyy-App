package com.lovelyy.app.ui.cat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatCardGeometryTest {
    private fun oldStage(width: Float) = width / CatCardGeometry.STAGE_RATIO

    @Test fun cardKeepsItsOldHeight() {
        for (w in listOf(296f, 320f, 350f, 372f, 412f)) {
            // The need bars below lost NEEDS_TRIM_DP of top padding, which the video took over.
            val total = CatCardGeometry.STRIP_DP + CatCardGeometry.videoHeightDp(w) + CatCardGeometry.SPEECH_DP -
                CatCardGeometry.NEEDS_TRIM_DP
            assertEquals(oldStage(w), total, 0.01f)
        }
    }

    @Test fun boardSizesAt390pxScreen() {
        // 390 px board, 20 px side padding: the card is 350 wide and the board's video is 236 high.
        assertEquals(236f, CatCardGeometry.videoHeightDp(350f), 10f)
    }

    @Test fun catStaysFullyVisibleOnCommonWidths() {
        for (w in listOf(320f, 350f, 372f, 412f)) {
            val (top, bottom) = CatCardGeometry.visibleWindow(w, CatCardGeometry.videoHeightDp(w))
            assertTrue("ears cut at $w: top=$top", top <= CatCardGeometry.CAT_TOP)
            assertTrue("paws cut at $w: bottom=$bottom", bottom >= CatCardGeometry.CAT_BOTTOM)
        }
    }

    @Test fun windowIsWholeVideoWhenAreaIsSquareOrTaller() {
        assertEquals(0f to 1f, CatCardGeometry.visibleWindow(300f, 300f))
        assertEquals(0f to 1f, CatCardGeometry.visibleWindow(300f, 320f))
    }

    @Test fun cropBiasMovesTheWindow() {
        val (top, bottom) = CatCardGeometry.visibleWindow(100f, 60f, cropBias = 0f)
        assertEquals(0f, top, 0.0001f)
        assertEquals(0.6f, bottom, 0.0001f)
    }

    @Test fun neverNegative() {
        assertEquals(0f, CatCardGeometry.videoHeightDp(10f), 0f)
    }
}
