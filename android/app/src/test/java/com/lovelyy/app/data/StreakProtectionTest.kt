package com.lovelyy.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class StreakProtectionTest {

    private val members = listOf("a", "b")
    private val nov27: LocalDate = LocalDate.parse("2026-11-27")

    /** Both partners cared on every day from [from] to [to] (inclusive). */
    private fun cared(from: LocalDate, to: LocalDate, start: CareStreak = CareStreak()): CareStreak {
        var s = start
        var d = from
        while (!d.isAfter(to)) {
            s = s.afterCare("a", members, d).afterCare("b", members, d)
            d = d.plusDays(1)
        }
        return s
    }

    /** A 12-day streak whose last complete day is the day before [today]. */
    private fun twelveUntilYesterday(today: LocalDate = nov27) = cared(today.minusDays(12), today.minusDays(1))

    // ---------- P5a ----------

    @Test fun dangerCardOnlyAfterEightWhenTodayIsMissing() {
        val s = twelveUntilYesterday().afterCare("a", members, nov27)
        assertEquals(12, s.current(nov27))
        assertFalse(StreakProtection.showDangerCard(s, nov27, hour = 19))
        assertTrue(StreakProtection.showDangerCard(s, nov27, hour = 20))
        assertTrue(StreakProtection.showDangerCard(s, nov27, hour = 23))
        // The day-12 trial card is the day's one Plus surface: no second card.
        assertFalse(StreakProtection.showDangerCard(s, nov27, hour = 21, trialCardShown = true))
    }

    @Test fun noDangerCardWhenDoneOffOrNoStreak() {
        val done = cared(nov27.minusDays(12), nov27)
        assertFalse(StreakProtection.showDangerCard(done, nov27, hour = 22))
        assertFalse(StreakProtection.showDangerCard(CareStreak(), nov27, hour = 22))
        val off = StreakProtection.freeze(twelveUntilYesterday(), nov27, isPlus = true)!!
        assertFalse(StreakProtection.showDangerCard(off, nov27, hour = 22))
    }

    @Test fun millisToMidnightInTheCoupleZone() {
        val zone = ZoneId.of("Europe/Istanbul")
        val now = ZonedDateTime.of(2026, 11, 27, 21, 20, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals((2 * 60 + 40) * 60_000L, StreakProtection.millisToMidnight(now, zone))
    }

    // ---------- P5b ----------

    @Test fun dayOffKeepsTheStreakAndItContinuesTomorrow() {
        val s = StreakProtection.freeze(twelveUntilYesterday(), nov27, isPlus = true)!!
        assertEquals(12, s.current(nov27))
        val tomorrow = nov27.plusDays(1)
        assertEquals(12, s.current(tomorrow))
        val next = s.afterCare("a", members, tomorrow).afterCare("b", members, tomorrow)
        assertEquals(13, next.current(tomorrow))
        assertEquals(13, next.best)
    }

    @Test fun withoutDayOffAMissedDayBreaksTheStreak() {
        val s = twelveUntilYesterday()
        assertEquals(0, s.current(nov27.plusDays(1)))
        val next = s.afterCare("a", members, nov27.plusDays(1)).afterCare("b", members, nov27.plusDays(1))
        assertEquals(1, next.count)
    }

    @Test fun twoDaysOffPerMonthSharedAndRenewedOnTheFirst() {
        var s = cared(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-07"))
        s = StreakProtection.freeze(s, LocalDate.parse("2026-11-08"), isPlus = true)!!
        assertEquals(1, StreakProtection.freezesLeft(s, LocalDate.parse("2026-11-09")))
        assertEquals(listOf(LocalDate.parse("2026-11-08")), StreakProtection.freezeDaysThisMonth(s, LocalDate.parse("2026-11-20")))
        s = cared(LocalDate.parse("2026-11-09"), LocalDate.parse("2026-11-26"), s)
        s = StreakProtection.freeze(s, nov27, isPlus = true)!!
        assertEquals(0, StreakProtection.freezesLeft(s, nov27.plusDays(1)))
        assertEquals(2, s.freezeUsed)
        val nov28 = nov27.plusDays(1)
        assertEquals(StreakProtection.FreezeRefusal.NoneLeft, StreakProtection.freezeRefusal(s, nov28, isPlus = true))
        assertNull(StreakProtection.freeze(s, nov28, isPlus = true))
        // The 1st: two new days off.
        val dec1 = LocalDate.parse("2026-12-01")
        assertEquals(LocalDate.parse("2026-12-01"), StreakProtection.freezesRenewOn(nov27))
        s = cared(nov28, LocalDate.parse("2026-11-30"), s)
        assertEquals(2, StreakProtection.freezesLeft(s, dec1))
        val dec = StreakProtection.freeze(s, dec1, isPlus = true)!!
        assertEquals("2026-12", dec.freezeMonth)
        assertEquals(1, dec.freezeUsed)
    }

    @Test fun dayOffRefusals() {
        val s = twelveUntilYesterday()
        assertEquals(StreakProtection.FreezeRefusal.NotPlus, StreakProtection.freezeRefusal(s, nov27, isPlus = false))
        assertNull(StreakProtection.freeze(s, nov27, isPlus = false))
        assertEquals(StreakProtection.FreezeRefusal.NoStreak, StreakProtection.freezeRefusal(CareStreak(), nov27, isPlus = true))
        assertEquals(StreakProtection.FreezeRefusal.AlreadyComplete, StreakProtection.freezeRefusal(cared(nov27, nov27, s), nov27, isPlus = true))
        val off = StreakProtection.freeze(s, nov27, isPlus = true)!!
        assertEquals(StreakProtection.FreezeRefusal.AlreadyOff, StreakProtection.freezeRefusal(off, nov27, isPlus = true))
    }

    @Test fun keptDaysAreCapped() {
        var s = cared(nov27.minusDays(1), nov27.minusDays(1))
        var d = nov27
        repeat(10) {
            s = s.copy(freezeMonth = null, freezeUsed = 0, lastCompleteDay = d.minusDays(1).toString())
            s = StreakProtection.freeze(s, d, isPlus = true)!!
            d = d.plusDays(1)
        }
        assertEquals(StreakProtection.KEPT_DAYS, s.freezeDays.size)
        assertEquals(d.minusDays(1).toString(), s.freezeDays.last())
    }

    // ---------- P5c ----------

    @Test fun repairTheDayAfterTheBreakBeforeCaring() {
        val s = cared(nov27.minusDays(13), nov27.minusDays(2)) // 12 days, yesterday missed
        assertEquals(0, s.current(nov27))
        val b = StreakProtection.repairable(s, nov27)
        assertNotNull(b)
        assertEquals(12, b!!.count)
        assertEquals(nov27.minusDays(1), b.missedDay)
        assertNull(StreakProtection.repair(s, nov27, isPlus = false))
        val fixed = StreakProtection.repair(s, nov27, isPlus = true)!!
        assertEquals(12, fixed.current(nov27))
        val next = fixed.afterCare("a", members, nov27).afterCare("b", members, nov27)
        assertEquals(13, next.current(nov27))
        assertNull(StreakProtection.repairable(fixed, nov27))
    }

    @Test fun repairAfterBothAlreadyCaredToday() {
        val s = cared(nov27.minusDays(13), nov27.minusDays(2)).let { cared(nov27, nov27, it) }
        assertEquals(1, s.current(nov27))
        assertEquals(12, s.brokenCount)
        val b = StreakProtection.repairable(s, nov27)!!
        assertEquals(13, b.count)
        assertTrue(b.restartedToday)
        val fixed = StreakProtection.repair(s, nov27, isPlus = true)!!
        assertEquals(13, fixed.current(nov27))
        assertEquals(13, fixed.best)
        assertEquals(0, fixed.brokenCount)
        assertEquals(14, cared(nov27.plusDays(1), nov27.plusDays(1), fixed).current(nov27.plusDays(1)))
    }

    @Test fun repairOnlyWithin24HoursAndForOneMissedDay() {
        val s = cared(nov27.minusDays(13), nov27.minusDays(2))
        // The next day the window is gone (two missed days).
        assertNull(StreakProtection.repairable(s, nov27.plusDays(1)))
        // Two missed days can never be made up.
        assertNull(StreakProtection.repairable(cared(nov27.minusDays(13), nov27.minusDays(3)), nov27))
        // Nothing to make up: alive streak, or no streak at all.
        assertNull(StreakProtection.repairable(twelveUntilYesterday(), nov27))
        assertNull(StreakProtection.repairable(CareStreak(), nov27))
    }

    @Test fun aDayOffBeforeTheMissedDayStillAllowsRepair() {
        var s = cared(nov27.minusDays(13), nov27.minusDays(3))
        s = StreakProtection.freeze(s, nov27.minusDays(2), isPlus = true)!!
        val b = StreakProtection.repairable(s, nov27)!!
        assertEquals(nov27.minusDays(1), b.missedDay)
        assertEquals(11, StreakProtection.repair(s, nov27, isPlus = true)!!.current(nov27))
    }

    @Test fun breakKeyIsTheMissedDay() {
        val b = StreakProtection.repairable(cared(nov27.minusDays(13), nov27.minusDays(2)), nov27)!!
        assertEquals("2026-11-26", StreakProtection.breakKey(b))
    }
}
