package com.lovelyy.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawingMathTest {
    @Test
    fun dropsPointsThatBarelyMoveButKeepsEnds() {
        val pts = listOf(0f, 0f, 0.001f, 0f, 0.002f, 0f, 0.1f, 0.1f, 0.1005f, 0.1f)
        val out = DrawingMath.simplify(pts, minStep = 0.004f)
        assertEquals(listOf(0f, 0f, 0.1f, 0.1f, 0.1005f, 0.1f), out)
    }

    @Test
    fun capsVeryLongStrokes() {
        val pts = (0 until 2000).flatMap { listOf(it / 2000f, 0.5f) }
        val out = DrawingMath.simplify(pts, minStep = 0f, max = 400)
        assertTrue(out.size / 2 <= 400)
        assertEquals(0f, out[0])
    }

    @Test
    fun shortStrokesAreKept() {
        val dot = listOf(0.3f, 0.3f, 0.3f, 0.3f)
        assertEquals(dot, DrawingMath.simplify(dot))
    }

    private fun st(id: String, at: Long) = Stroke(id, "u", 0, 0.01f, listOf(0f, 0f), at)

    @Test
    fun newestWindowIsReversedIntoDrawingOrder() {
        val desc = listOf(st("c", 3), st("b", 2), st("a", 1))
        assertEquals(listOf("a", "b", "c"), DrawingMath.inDrawingOrder(desc).map { it.id })
    }

    @Test
    fun savedIdIsStableForTheSameCanvas() {
        val one = DrawingMath.inDrawingOrder(listOf(st("b", 2), st("a", 1)))
        val two = DrawingMath.inDrawingOrder(listOf(st("a", 1), st("b", 2), st("c", 3)))
        assertEquals(DrawingMath.savedId(one), DrawingMath.savedId(two))
    }

    @Test
    fun stampChangesOnAddUndoAndClear() {
        val canvas = listOf(st("a", 1), st("b", 2))
        val stamp = DrawingMath.stampOf(canvas)
        assertEquals(DrawingMath.stamp(2, "b"), stamp)
        assertTrue(stamp != DrawingMath.stampOf(canvas + st("c", 3)))
        assertTrue(stamp != DrawingMath.stampOf(canvas.dropLast(1)))
        assertEquals(DrawingMath.stamp(0, null), DrawingMath.stampOf(emptyList()))
    }

    @Test
    fun cachedCanvasMatchesServerStamp() {
        val canvas = listOf(st("a", 1), st("b", 2))
        assertTrue(DrawingMath.stampMatches(DrawingMath.stamp(2, "b"), canvas, max = 600))
        assertTrue(!DrawingMath.stampMatches(DrawingMath.stamp(3, "c"), canvas, max = 600))
        assertTrue(!DrawingMath.stampMatches(DrawingMath.stamp(1, "b"), canvas, max = 600))
        assertTrue(DrawingMath.stampMatches(DrawingMath.stamp(0, null), emptyList(), max = 600))
        // Over the cap only the newest strokes are loaded: the newest id decides.
        assertTrue(DrawingMath.stampMatches(DrawingMath.stamp(5, "b"), canvas, max = 2))
        assertTrue(!DrawingMath.stampMatches(DrawingMath.stamp(5, "z"), canvas, max = 2))
    }
}
