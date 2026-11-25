package com.lovelyy.app.data

import org.junit.Assert.assertEquals
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
    fun negativeRotationStillMapsIntoRange() {
        val i = WheelMath.indexAt(-725f, 5)
        assertTrue(i in 0 until 5)
    }
}
