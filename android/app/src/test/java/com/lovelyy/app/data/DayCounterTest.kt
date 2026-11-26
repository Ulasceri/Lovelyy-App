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
    fun yearMilestonesAreCalendarBased() {
        val start = LocalDate.of(2024, 2, 14)
        val all = DayCounter.milestones(1500, start)
        // 14 Feb 2024 -> 14 Feb 2027 is 1096 days (2024 is a leap year), not 3 * 365.
        val third = all.first { it.years == 3 }
        assertEquals(1096L, third.day)
        assertEquals(LocalDate.of(2027, 2, 14), start.plusDays(third.day))
        val (prev, next) = DayCounter.next(1017, start)
        assertEquals(1000L, prev?.day) // the round day 1000 is the latest milestone before day 1017
        assertEquals(3, next.years)
        assertEquals(79L, next.day - 1017)
    }

    @Test
    fun leapDayStartLandsOn28FebInPlainYears() {
        val start = LocalDate.of(2024, 2, 29)
        val first = DayCounter.milestones(400, start).first { it.years == 1 }
        assertEquals(LocalDate.of(2025, 2, 28), start.plusDays(first.day))
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
