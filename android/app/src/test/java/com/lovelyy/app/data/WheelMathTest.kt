package com.lovelyy.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WheelMathTest {
    @Test
    fun noRotationPutsFirstItemUnderPointer() {
        assertEquals(0, WheelMath.indexAt(0f, 6))
        // Rotating clockwise by one wedge brings the last item under the pointer.
        assertEquals(5, WheelMath.indexAt(360f / 6, 6))
    }

    @Test
    fun spinLandsOnTargetForEveryItemAndCount() {
        for (count in WheelMath.MIN_ITEMS..WheelMath.MAX_ITEMS) {
            var rotation = 0f
            for (target in 0 until count) {
                for (jitter in listOf(-1f, 0f, 1f)) {
                    val next = WheelMath.spinTo(rotation, target, count, turns = 4, jitter = jitter)
                    assertTrue("spins forward at least 4 turns", next >= rotation + 360f * 4)
                    assertEquals("count=$count target=$target jitter=$jitter", target, WheelMath.indexAt(next, count))
                    rotation = next
                }
            }
        }
    }

    @Test
    fun aSecondSpinWithinTheWindowIsRejected() {
        val now = 1_000_000L
        assertTrue("no previous spin", WheelMath.acceptsNewSpin(null, now))
        assertFalse("partner spun a moment ago", WheelMath.acceptsNewSpin(now - 300, now))
        assertFalse("partner clock slightly ahead", WheelMath.acceptsNewSpin(now + 1_000, now))
        assertFalse("still animating", WheelMath.acceptsNewSpin(now - WheelMath.SPIN_MS, now))
        assertTrue("previous spin finished", WheelMath.acceptsNewSpin(now - WheelMath.SPIN_WINDOW_MS, now))
        assertTrue("long ago", WheelMath.acceptsNewSpin(now - 60_000, now))
        assertTrue("window covers the whole animation", WheelMath.SPIN_WINDOW_MS > WheelMath.SPIN_MS)
    }

    @Test
    fun recentNeedsAStamp() {
        assertFalse(WheelMath.isRecent(0L, 100L))
        assertTrue(WheelMath.isRecent(100L, 100L))
    }

    @Test
    fun lateViewerAnimatesTheRestButNotTooShort() {
        val at = 50_000L
        assertEquals(WheelMath.SPIN_MS, WheelMath.remainingMs(at, at))
        assertEquals(WheelMath.SPIN_MS - 1_000, WheelMath.remainingMs(at, at + 1_000))
        assertEquals(1_200, WheelMath.remainingMs(at, at + 3_000))
        assertEquals("clock skew never lengthens it", WheelMath.SPIN_MS, WheelMath.remainingMs(at, at - 2_000))
    }

    @Test
    fun negativeRotationStillMapsIntoRange() {
        val i = WheelMath.indexAt(-725f, 5)
        assertTrue(i in 0 until 5)
    }
}
