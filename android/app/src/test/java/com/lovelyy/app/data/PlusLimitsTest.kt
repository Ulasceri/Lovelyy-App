package com.lovelyy.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlusLimitsTest {

    @Test fun freeLimitsMatchTheApprovedPlan() {
        assertEquals(50, PlusLimits.Limit.Notes.free)
        assertEquals(10, PlusLimits.Limit.Events.free)
        assertEquals(20, PlusLimits.Limit.Dreams.free)
        assertEquals(1, PlusLimits.Limit.Capsules.free)
        assertEquals(2, PlusLimits.Limit.Wheels.free)
        assertEquals(10, PlusLimits.Limit.Drawings.free)
        assertEquals(6, PlusLimits.FREE_DRAWING_COLORS)
    }

    @Test fun onlyAddingIsBlockedAtTheLimit() {
        assertTrue(PlusLimits.canAdd(PlusLimits.Limit.Notes, 49, isPlus = false))
        assertFalse(PlusLimits.canAdd(PlusLimits.Limit.Notes, 50, isPlus = false))
        assertFalse(PlusLimits.canAdd(PlusLimits.Limit.Notes, 63, isPlus = false))
        assertTrue(PlusLimits.atLimit(PlusLimits.Limit.Notes, 63, isPlus = false))
        assertFalse(PlusLimits.atLimit(PlusLimits.Limit.Notes, 12, isPlus = false))
        // The note on top only once the list is over the limit.
        assertFalse(PlusLimits.overLimit(PlusLimits.Limit.Notes, 50, isPlus = false))
        assertTrue(PlusLimits.overLimit(PlusLimits.Limit.Notes, 51, isPlus = false))
        assertFalse(PlusLimits.overLimit(PlusLimits.Limit.Notes, 63, isPlus = true))
    }

    @Test fun plusHasNoLimit() {
        for (limit in PlusLimits.Limit.entries) {
            assertTrue(PlusLimits.canAdd(limit, 10_000, isPlus = true))
            assertFalse(PlusLimits.atLimit(limit, 10_000, isPlus = true))
        }
        assertTrue(PlusLimits.colorAllowed(9, isPlus = true))
    }

    @Test fun freeColours() {
        assertTrue(PlusLimits.colorAllowed(5, isPlus = false))
        assertFalse(PlusLimits.colorAllowed(6, isPlus = false))
    }

    @Test fun keysAreUniqueAndAnalyticsSafe() {
        val keys = PlusLimits.Limit.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        assertTrue(keys.all { it.matches(Regex("^[a-z_]+$")) })
    }
}
