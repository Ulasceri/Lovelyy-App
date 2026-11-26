package com.lovelyy.app.data

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaveRulesTest {

    @Test
    fun signInFreshOnlyWithinFourMinutesOfServerTime() {
        val auth = 1_700_000_000L // seconds, Firebase's clock
        assertTrue(LeaveRules.signInFreshByToken(auth, auth + 60))
        assertTrue(LeaveRules.signInFreshByToken(auth, auth + LeaveRules.FRESH_MS / 1000))
        assertFalse(LeaveRules.signInFreshByToken(auth, auth + LeaveRules.FRESH_MS / 1000 + 1))
        // Token issued in the same second as the sign-in (or rounded a second earlier) is fresh.
        assertTrue(LeaveRules.signInFreshByToken(auth, auth))
        assertTrue(LeaveRules.signInFreshByToken(auth, auth - 1))
        assertFalse(LeaveRules.signInFreshByToken(auth, auth - 60))
        assertFalse(LeaveRules.signInFreshByToken(null, auth))
        assertFalse(LeaveRules.signInFreshByToken(auth, null))
        assertFalse(LeaveRules.signInFreshByToken(0, auth))
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
        // Every collection name in Paths except the top-level ones (users, couples, pairCodes, config) and doc ids: a new shared feature
        // must be added to LeaveRules.SHARED, or unpairing would leave its data behind.
        val notShared = setOf(Paths.USERS, Paths.COUPLES, Paths.PAIR_CODES, Paths.CAT_DOC, Paths.SETTINGS_DOC, Paths.CONFIG)
        val all = Paths::class.java.declaredFields
            .filter { java.lang.reflect.Modifier.isStatic(it.modifiers) && it.type == String::class.java && !it.name.endsWith("_DOC") }
            .map { it.isAccessible = true; it.get(null) as String }
            .toSet()
        assertEquals(all - notShared, LeaveRules.SHARED.toSet())
    }

    @Test
    fun everySharedPathIsPurgedOnUnpair() {
        // Account deletion relies on unpair's purge: a collection missing here would stay in Firestore.
        val all = Paths::class.java.declaredFields
            .filter { java.lang.reflect.Modifier.isStatic(it.modifiers) && it.type == String::class.java && !it.name.endsWith("_DOC") }
            .map { it.get(null) as String }
            .toSet() - LeaveRules.NOT_SHARED
        assertEquals(all, LeaveRules.SHARED.toSet())
        assertEquals(LeaveRules.SHARED.size, LeaveRules.SHARED.toSet().size)
    }

    @Test
    fun accountDeletionClearsEveryLocalPrefsFileButTelemetryChoice() {
        assertFalse("telemetry" in LeaveRules.LOCAL_PREFS)
        assertTrue(LeaveRules.LOCAL_PREFS.containsAll(listOf("inbox", "signals", "capsule_draft", "drawing_widget", "widget_couple", "cat_widget")))
    }
}
