package com.lovelyy.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DayCounterTest {
    private val zone = ZoneId.of("Europe/Istanbul")

    @Test
    fun splitsElapsedTime() {
        val start = DayCounter.startMillis(LocalDate.of(2025, 10, 14), zone)
        val now = start + 412L * 86_400_000L + 6 * 3_600_000L + 23 * 60_000L + 8_000L + 345
        val t = DayCounter.together(start, now)
        assertEquals(412L, t.days)
        assertEquals(6, t.hours)
        assertEquals(23, t.minutes)
        assertEquals(8, t.seconds)
        assertEquals(345, t.millis)
        assertEquals(413L, DayCounter.dayNumber(start, now))
    }

    @Test
    fun futureStartCountsAsZero() {
        val t = DayCounter.together(1_000_000L, 0L)
        assertEquals(0L, t.days)
        assertEquals(0, t.millis)
    }

    @Test
    fun nextMilestone() {
        val (prev, next) = DayCounter.next(412)
        assertEquals(365L, prev?.day)
        assertEquals(1, prev?.years)
        assertEquals(500L, next.day)
        val (p0, n0) = DayCounter.next(0)
        assertEquals(null, p0)
        assertEquals(100L, n0.day)
    }

    @Test
    fun milestonesAreSortedAndUnique() {
        val all = DayCounter.milestones(4000)
        assertEquals(all.sortedBy { it.day }, all)
        assertEquals(all.size, all.map { it.day }.distinct().size)
        assertTrue(all.any { it.day == 730L && it.years == 2 })
        assertTrue(all.any { it.day == 1000L })
    }
}
