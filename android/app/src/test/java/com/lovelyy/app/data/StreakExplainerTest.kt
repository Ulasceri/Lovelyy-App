package com.lovelyy.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakExplainerTest {
    @Test fun showsByItselfOnTheFirstStreakDay() {
        assertTrue(StreakExplainer.shouldAutoShow(currentStreak = 1, seen = false))
    }

    @Test fun showsOnceForCouplesThatAlreadyHadAStreak() {
        assertTrue(StreakExplainer.shouldAutoShow(currentStreak = 12, seen = false))
    }

    @Test fun neverAgainOnceSeen() {
        assertFalse(StreakExplainer.shouldAutoShow(currentStreak = 1, seen = true))
        assertFalse(StreakExplainer.shouldAutoShow(currentStreak = 30, seen = true))
    }

    @Test fun notWithoutAStreak() {
        assertFalse(StreakExplainer.shouldAutoShow(currentStreak = 0, seen = false))
    }

    @Test fun headingFollowsTheStreak() {
        assertEquals(StreakExplainer.Heading.Intro, StreakExplainer.heading(0))
        assertEquals(StreakExplainer.Heading.Started, StreakExplainer.heading(1))
        assertEquals(StreakExplainer.Heading.Running, StreakExplainer.heading(2))
        assertEquals(StreakExplainer.Heading.Running, StreakExplainer.heading(19))
    }

    @Test fun analyticsKeysAreFixed() {
        assertEquals(listOf("auto", "chip", "card"), StreakExplainerSource.entries.map { it.key })
    }
}
