package com.lovelyy.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** The shared care streak when one phone's clock is a day off (two-emulator finding). */
class CareStreakClockTest {
    private val members = listOf("deniz", "ece")
    private val d26 = LocalDate.of(2026, 11, 26)
    private val d27 = d26.plusDays(1)
    private val d28 = d26.plusDays(2)

    /** Deniz already cared on the 27th; the 26th was complete. */
    private val start = CareStreak(count = 3, best = 3, lastCompleteDay = "2026-11-26", careDay = "2026-11-27", careBy = listOf("deniz"))

    @Test fun reproPhoneAheadThenBothCareOnTheRealDayGrowsTheStreak() {
        // Deniz's clock says the 28th.
        val ahead = start.afterCare("deniz", members, d28)
        assertEquals(3, ahead.count)
        // Clock corrected; Ece cares on the real day (27th): she joins the stored day, the day completes.
        val s = ahead.afterCare("ece", members, d27)
        assertEquals(4, s.count)
        assertEquals(4, s.best)
        assertEquals(4, s.current(d27))
        assertEquals(4, s.current(d28)) // not broken the next day
        assertTrue(s.caredToday("ece", d27))
        assertTrue(s.caredToday("deniz", d27))
        // The 28th was counted in advance; the 29th continues normally.
        val d29 = d28.plusDays(1)
        val next = s.afterCare("deniz", members, d28).afterCare("ece", members, d28)
            .afterCare("ece", members, d29).afterCare("deniz", members, d29)
        assertEquals(5, next.count)
        assertEquals("2026-11-29", next.lastCompleteDay)
    }

    @Test fun phoneAheadCompletingTheDayIsKeptWhenThePartnerCaresAgain() {
        // Ece (correct) cared on the 27th, then Deniz's phone (ahead) cares: the 28th starts.
        val ece = CareStreak(count = 2, best = 2, lastCompleteDay = "2026-11-26").afterCare("ece", members, d27)
        val s = ece.afterCare("deniz", members, d28)
        assertEquals("2026-11-28", s.careDay)
        assertEquals(listOf("deniz"), s.careBy)
        assertEquals(2, s.current(d27)) // still alive
        // Ece cares again on the 27th: merged, completed, the streak grows.
        val done = s.afterCare("ece", members, d27)
        assertEquals(3, done.count)
        assertEquals("2026-11-28", done.lastCompleteDay)
    }

    @Test fun phoneBehindMergesIntoTheStoredDayInsteadOfReplacingIt() {
        // Ece cared on the 27th; Deniz's phone says the 26th.
        val ece = CareStreak(count = 3, best = 3, lastCompleteDay = "2026-11-26", careDay = "2026-11-27", careBy = listOf("ece"))
        val s = ece.afterCare("deniz", members, d26)
        assertEquals("2026-11-27", s.careDay)
        assertEquals(listOf("ece", "deniz"), s.careBy)
        assertEquals(4, s.count)
        assertEquals("2026-11-27", s.lastCompleteDay)
        // On the behind phone the stored day counts as today.
        assertTrue(s.caredToday("deniz", d26))
        assertEquals(4, s.current(d26))
    }

    @Test fun phoneBehindNeverResetsOrMovesTheStreakBack() {
        val done = CareStreak(count = 5, best = 5, lastCompleteDay = "2026-11-27", careDay = "2026-11-27", careBy = members)
        val s = done.afterCare("deniz", members, d26).afterCare("ece", members, d26)
        assertEquals(5, s.count)
        assertEquals(5, s.best)
        assertEquals("2026-11-27", s.lastCompleteDay)
        assertEquals("2026-11-27", s.careDay)
    }

    @Test fun currentToleratesALastCompleteDayInTheFuture() {
        val s = CareStreak(count = 7, best = 7, lastCompleteDay = "2026-11-28")
        assertEquals(7, s.current(d27))
        assertEquals(7, s.current(d26))
        assertEquals(7, s.current(d28))
        assertEquals(7, s.current(d28.plusDays(1)))
        assertEquals(0, s.current(d28.plusDays(2)))
    }

    @Test fun normalDayToDayGrowth() {
        var s = CareStreak()
        var day = d26
        for (expected in 1..4) {
            s = s.afterCare("deniz", members, day).afterCare("ece", members, day)
            assertEquals(expected, s.current(day))
            day = day.plusDays(1)
        }
        assertEquals(4, s.best)
        assertFalse(s.caredToday("ece", day))
    }

    @Test fun aRealMissedDayStillBreaksTheStreak() {
        val s = CareStreak().afterCare("deniz", members, d26).afterCare("ece", members, d26)
        assertEquals(1, s.count)
        assertEquals(0, s.current(d28)) // the 27th was missed
        // Only one partner caring on the 27th doesn't save it either.
        val half = s.afterCare("deniz", members, d27)
        assertEquals(0, half.current(d28))
        val again = half.afterCare("deniz", members, d28).afterCare("ece", members, d28)
        assertEquals(1, again.count)
        assertEquals(1, again.best)
    }
}
