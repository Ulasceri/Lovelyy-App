package com.lovelyy.app.data

import com.lovelyy.app.data.SignalLimits.DAY_MS
import com.lovelyy.app.data.SignalLimits.HOUR_MS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalLimitsTest {
    private val now = 1_800_000_000_000L
    private val cfg = SignalLimits.Config(perHour = 15, perDay = 60)

    @Test
    fun firstSignalStartsBothWindows() {
        assertEquals(SignalLimits.Plan(true, 1, true, 1), SignalLimits.plan(null, now))
    }

    @Test
    fun countsUpInsideTheWindows() {
        val s = SignalLimits.State(now - 10 * 60_000, 4, now - 5 * HOUR_MS, 20)
        assertEquals(SignalLimits.Plan(false, 5, false, 21), SignalLimits.plan(s, now))
    }

    @Test
    fun restartsAWindowOnceItIsOver() {
        val s = SignalLimits.State(now - HOUR_MS, 15, now - 2 * HOUR_MS, 30)
        assertEquals(SignalLimits.Plan(true, 1, false, 31), SignalLimits.plan(s, now))
        val d = SignalLimits.State(now - 2 * HOUR_MS, 3, now - DAY_MS - 1, 60)
        assertEquals(SignalLimits.Plan(true, 1, true, 1), SignalLimits.plan(d, now))
    }

    @Test
    fun overTheLimitWhenAWindowIsFull() {
        val hourFull = SignalLimits.plan(SignalLimits.State(now - 60_000, 15, now - 60_000, 15), now)
        assertTrue(SignalLimits.isOverLimit(hourFull, cfg))
        val dayFull = SignalLimits.plan(SignalLimits.State(now - 2 * HOUR_MS, 15, now - 3 * HOUR_MS, 60), now)
        assertTrue(SignalLimits.isOverLimit(dayFull, cfg))
        val ok = SignalLimits.plan(SignalLimits.State(now - 60_000, 14, now - 60_000, 59), now)
        assertFalse(SignalLimits.isOverLimit(ok, cfg))
    }

    @Test
    fun flipSwapsOnlyExistingWindows() {
        val s = SignalLimits.State(now - 2 * HOUR_MS, 5, null, 0)
        // Clock says the hour is over; flipped (server thinks it isn't) counts up instead. No day window yet: always new.
        assertEquals(SignalLimits.Plan(false, 6, true, 1), SignalLimits.plan(s, now, flip = true))
    }
}
