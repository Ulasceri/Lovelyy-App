package com.lovelyy.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CalendarMathTest {
    private fun ev(date: LocalDate, yearly: Boolean) = CalEvent("x", "t", date, "🎂", yearly, "u", "U")

    private val today = LocalDate.of(2026, 11, 25)

    @Test
    fun oneTimePlan() {
        val plan = ev(LocalDate.of(2026, 12, 3), yearly = false)
        assertEquals(8L, CalendarMath.daysUntil(plan, today))
        assertTrue(CalendarMath.occursOn(plan, LocalDate.of(2026, 12, 3)))
        assertFalse(CalendarMath.occursOn(plan, LocalDate.of(2027, 12, 3)))
        assertNull(CalendarMath.nextOccurrence(ev(LocalDate.of(2026, 11, 24), false), today))
        assertEquals(0L, CalendarMath.daysUntil(ev(today, false), today))
    }

    @Test
    fun yearlyRollsToNextYear() {
        val birthday = ev(LocalDate.of(1998, 3, 14), yearly = true)
        assertEquals(LocalDate.of(2027, 3, 14), CalendarMath.nextOccurrence(birthday, today))
        assertEquals(29, CalendarMath.yearsAt(birthday, LocalDate.of(2027, 3, 14)))
        val laterThisYear = ev(LocalDate.of(2020, 12, 1), yearly = true)
        assertEquals(6L, CalendarMath.daysUntil(laterThisYear, today))
        assertEquals(0L, CalendarMath.daysUntil(ev(LocalDate.of(2024, 11, 25), true), today))
    }

    @Test
    fun yearlyOccursOnlyFromFirstYear() {
        val anniv = ev(LocalDate.of(2025, 10, 14), yearly = true)
        assertTrue(CalendarMath.occursOn(anniv, LocalDate.of(2026, 10, 14)))
        assertFalse(CalendarMath.occursOn(anniv, LocalDate.of(2024, 10, 14)))
        assertFalse(CalendarMath.occursOn(anniv, LocalDate.of(2026, 10, 15)))
    }

    @Test
    fun leapDayFallsBackTo28Feb() {
        val leap = ev(LocalDate.of(2024, 2, 29), yearly = true)
        assertEquals(LocalDate.of(2027, 2, 28), CalendarMath.nextOccurrence(leap, today))
        assertTrue(CalendarMath.occursOn(leap, LocalDate.of(2027, 2, 28)))
        assertTrue(CalendarMath.occursOn(leap, LocalDate.of(2028, 2, 29)))
        assertFalse(CalendarMath.occursOn(leap, LocalDate.of(2028, 2, 28)))
    }

    @Test
    fun questionIsStablePerDay() {
        assertEquals(QuestionBank.forDay(today), QuestionBank.forDay(LocalDate.of(2026, 11, 25)))
        val n = QuestionBank.QUESTIONS.size.toLong()
        assertEquals(QuestionBank.forDay(today), QuestionBank.forDay(today.plusDays(n)))
    }
}
