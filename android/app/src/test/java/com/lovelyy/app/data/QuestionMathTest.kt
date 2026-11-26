package com.lovelyy.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class QuestionMathTest {
    private val today = LocalDate.of(2026, 11, 25)
    private fun day(offset: Long, both: Boolean): DayQuestion {
        val a = Answer("a", "A", "x", 0)
        val b = Answer("b", "B", "y", 0)
        return DayQuestion(today.minusDays(offset).toString(), "q", if (both) mapOf("a" to a, "b" to b) else mapOf("a" to a))
    }

    @Test
    fun countsBackFromToday() {
        assertEquals(3, QuestionMath.streak(listOf(day(0, true), day(1, true), day(2, true), day(4, true)), today))
    }

    @Test
    fun todayStillOpenCountsFromYesterday() {
        assertEquals(2, QuestionMath.streak(listOf(day(0, false), day(1, true), day(2, true)), today))
    }

    @Test
    fun brokenStreakIsZero() {
        assertEquals(0, QuestionMath.streak(listOf(day(2, true)), today))
        assertEquals(0, QuestionMath.streak(emptyList(), today))
    }

    @Test
    fun answerEditableUntilPartnerAnswers() {
        assertFalse(QuestionMath.isLocked(emptySet(), "a"))
        assertFalse(QuestionMath.isLocked(setOf("a"), "a"))
        // The partner answered first: my first answer is still allowed.
        assertFalse(QuestionMath.isLocked(setOf("b"), "a"))
        assertTrue(QuestionMath.isLocked(setOf("a", "b"), "a"))
    }
}
