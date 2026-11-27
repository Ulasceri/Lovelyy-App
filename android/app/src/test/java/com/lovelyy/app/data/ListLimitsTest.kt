package com.lovelyy.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListLimitsTest {
    @Test
    fun nextLimitAddsOnePage() {
        assertEquals(400, ListLimits.nextLimit(200))
        assertEquals(30, ListLimits.nextLimit(20, 10))
    }

    @Test
    fun fullWindowMeansMore() {
        assertTrue(ListLimits.hasMore(200, 200))
        assertFalse(ListLimits.hasMore(199, 200))
        assertFalse(ListLimits.hasMore(0, 200))
    }

    @Test
    fun countOnlyNeededWhenWindowIsFull() {
        assertTrue(ListLimits.needsCount(200, 200))
        assertFalse(ListLimits.needsCount(12, 200))
    }

    @Test
    fun realTotalWins() {
        assertEquals(CountLabel(320, false), ListLimits.countLabel(200, 200, 320L))
    }

    @Test
    fun totalNeverBelowLoaded() {
        // A stale count must not show fewer than what is on screen.
        assertEquals(CountLabel(200, false), ListLimits.countLabel(200, 200, 150L))
    }

    @Test
    fun unknownTotalOnFullWindowIsAtLeast() {
        assertEquals(CountLabel(200, true), ListLimits.countLabel(200, 200, null))
    }

    @Test
    fun unknownTotalOnPartialWindowIsExact() {
        assertEquals(CountLabel(7, false), ListLimits.countLabel(7, 200, null))
    }
}
