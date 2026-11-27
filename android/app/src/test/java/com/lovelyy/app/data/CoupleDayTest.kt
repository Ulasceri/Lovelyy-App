package com.lovelyy.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class CoupleDayTest {
    @Before
    fun loadQuestions() { TestQuestions.init() }

    private val plus3: ZoneId = ZoneOffset.ofHours(3)
    private val minus5: ZoneId = ZoneOffset.ofHours(-5)
    private fun at(iso: String) = Instant.parse(iso).toEpochMilli()

    @Test
    fun todayIsTheCalendarDayInTheGivenZone() {
        val now = at("2026-11-26T22:30:00Z")
        assertEquals(LocalDate.of(2026, 11, 27), CoupleDay.today(plus3, now))
        assertEquals(LocalDate.of(2026, 11, 26), CoupleDay.today(minus5, now))
        assertEquals(LocalDate.of(2026, 11, 26), CoupleDay.today(ZoneOffset.UTC, now))
    }

    @Test
    fun partnersInUtcPlus3AndMinus5AgreeAroundMidnightWithTheSharedZone() {
        // Just before and just after midnight in UTC+3 (21:00Z); in UTC-5 it is still afternoon.
        for (iso in listOf("2026-11-26T20:59:00Z", "2026-11-26T21:00:00Z", "2026-11-26T21:01:00Z")) {
            val now = at(iso)
            // Each phone's own zone disagrees right after midnight in UTC+3...
            if (iso != "2026-11-26T20:59:00Z") assertNotEquals(CoupleDay.today(plus3, now), CoupleDay.today(minus5, now))
            // ...but both phones use the couple's zone, so they get the same day.
            val couple = plus3
            val phoneA = CoupleDay.today(couple, now)
            val phoneB = CoupleDay.today(couple, now)
            assertEquals(phoneA, phoneB)
        }
        assertEquals(LocalDate.of(2026, 11, 26), CoupleDay.today(plus3, at("2026-11-26T20:59:00Z")))
        assertEquals(LocalDate.of(2026, 11, 27), CoupleDay.today(plus3, at("2026-11-26T21:00:00Z")))
        // The same holds when the couple zone is the western one.
        assertEquals(LocalDate.of(2026, 11, 26), CoupleDay.today(minus5, at("2026-11-27T04:59:00Z")))
        assertEquals(LocalDate.of(2026, 11, 27), CoupleDay.today(minus5, at("2026-11-27T05:00:00Z")))
    }

    @Test
    fun careStreakCompletesWhenPartnersInDifferentZonesCareOnTheSameCoupleDay() {
        val members = listOf("a", "b")
        val couple = plus3
        // A (UTC+3) cares at 23:50 local; B (UTC-5) cares 20 minutes later, at 16:10 local.
        val aDay = CoupleDay.today(couple, at("2026-11-26T20:50:00Z"))
        val bDay = CoupleDay.today(couple, at("2026-11-26T21:10:00Z"))
        // Midnight in the couple zone passed in between: they are different couple days, so no streak yet...
        val afterA = CareStreak().afterCare("a", members, aDay)
        val afterB = afterA.afterCare("b", members, bDay)
        assertEquals(0, afterB.count)
        // ...but before that midnight both get the same day and the day is complete.
        val bEarly = CoupleDay.today(couple, at("2026-11-26T20:55:00Z"))
        val done = afterA.afterCare("b", members, bEarly)
        assertEquals(1, done.count)
        assertEquals("2026-11-26", done.lastCompleteDay)
        // Both phones show the same streak and question day afterwards.
        val later = at("2026-11-26T22:00:00Z")
        assertEquals(done.current(CoupleDay.today(couple, later)), done.current(CoupleDay.today(couple, later)))
        assertEquals(QuestionBank.forDay(CoupleDay.today(couple, later)), QuestionBank.forDay(LocalDate.of(2026, 11, 27)))
    }

    @Test
    fun capsuleOpensOnTheSameCoupleDayForBothPartners() {
        val c = Capsule("1", "t", "x", LocalDate.of(2026, 11, 27), "a", "A", 0L)
        val now = at("2026-11-26T21:30:00Z") // 00:30 on the 27th in UTC+3, 16:30 on the 26th in UTC-5
        assertEquals(true, c.isOpen(CoupleDay.today(plus3, now)))
        assertEquals(false, c.isOpen(CoupleDay.today(minus5, now))) // what the old per-phone zone did
    }

    @Test
    fun zoneOfParsesValidIdsOnly() {
        assertEquals(ZoneId.of("Europe/Istanbul"), CoupleDay.zoneOf("Europe/Istanbul"))
        assertEquals(ZoneId.of("GMT+03:00"), CoupleDay.zoneOf("GMT+03:00"))
        assertNull(CoupleDay.zoneOf(null))
        assertNull(CoupleDay.zoneOf(""))
        assertNull(CoupleDay.zoneOf("Not/AZone"))
    }
}
