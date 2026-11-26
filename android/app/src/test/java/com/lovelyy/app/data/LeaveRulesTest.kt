package com.lovelyy.app.data

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaveRulesTest {

    @Test
    fun signInFreshOnlyWithinFourMinutes() {
        val now = 10_000_000L
        assertTrue(LeaveRules.signInFresh(now - 60_000, now))
        assertTrue(LeaveRules.signInFresh(now - LeaveRules.FRESH_MS, now))
        assertFalse(LeaveRules.signInFresh(now - LeaveRules.FRESH_MS - 1, now))
        assertFalse(LeaveRules.signInFresh(null, now))
        // A sign-in "in the future" means a wrong clock: don't trust it.
        assertFalse(LeaveRules.signInFresh(now + 60_000, now))
    }

    @Test
    fun partnerEndedOnlyWhenSomeoneElseEndedIt() {
        val open = Couple("C", listOf("a", "b"))
        val ended = open.copy(endedAt = Timestamp(1_700_000_000L, 0), endedBy = "a", endedByName = "Ada")
        assertFalse(LeaveRules.partnerEnded(open, "b"))
        assertFalse(LeaveRules.partnerEnded(null, "b"))
        assertFalse(LeaveRules.partnerEnded(ended, "a"))
        assertTrue(LeaveRules.partnerEnded(ended, "b"))
    }

    @Test
    fun everySharedCollectionIsDeleted() {
        // Every collection name in Paths except the top-level ones and the cat doc id: a new shared feature
        // must be added to LeaveRules.SHARED, or unpairing would leave its data behind.
        val notShared = setOf(Paths.USERS, Paths.COUPLES, Paths.PAIR_CODES, Paths.CAT_DOC)
        val all = Paths::class.java.declaredFields
            .filter { java.lang.reflect.Modifier.isStatic(it.modifiers) && it.type == String::class.java }
            .map { it.isAccessible = true; it.get(null) as String }
            .toSet()
        assertEquals(all - notShared, LeaveRules.SHARED.toSet())
    }
}
