package com.lovelyy.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalFreshnessTest {
    private val me = "me"
    private val partner = "partner"
    private val now = 1_000_000_000L
    private val min = 60_000L

    private fun sig(id: String, createdAt: Long?, at: Long = createdAt ?: 0L, to: String = me, kind: SignalKind = SignalKind.Heartbeat) =
        Signal(id = id, kind = kind, from = if (to == me) partner else me, fromName = "Bora", to = to, at = at, createdAt = createdAt)

    private fun ids(list: List<Signal>) = list.map { it.id }

    @Test
    fun firstRunOnlyNotifiesWithinGrace() {
        val list = listOf(sig("old", now - 60 * min), sig("recent", now - 2 * min))
        val d = SignalFreshness.decide(list, me, state = null, now = now)
        assertEquals(listOf("recent"), ids(d.fresh))
        // Both are remembered, so neither notifies again.
        assertEquals(emptyList<String>(), ids(SignalFreshness.decide(list, me, d.state, now + min).fresh))
    }

    @Test
    fun firstRunWithLegacyStateNotifiesNothingAlreadyThere() {
        val list = listOf(sig("a", now - 2 * min))
        assertTrue(SignalFreshness.decide(list, me, null, now, graceMs = 0).fresh.isEmpty())
    }

    @Test
    fun senderClockBehindStillNotifies() {
        val d0 = SignalFreshness.decide(listOf(sig("a", now - 30 * min)), me, null, now)
        // Sender's clock is a day behind: `at` is old, server time is new.
        val behind = sig("b", createdAt = now + min, at = now - 24 * 60 * min)
        val d1 = SignalFreshness.decide(listOf(behind, sig("a", now - 30 * min)), me, d0.state, now + min)
        assertEquals(listOf("b"), ids(d1.fresh))
    }

    @Test
    fun senderClockAheadDoesNotBlockLaterSignals() {
        val d0 = SignalFreshness.decide(emptyList(), me, null, now)
        val ahead = sig("a", createdAt = now + min, at = now + 24 * 60 * min)
        val d1 = SignalFreshness.decide(listOf(ahead), me, d0.state, now + min)
        assertEquals(listOf("a"), ids(d1.fresh))
        val next = sig("b", createdAt = now + 2 * min, at = now + 2 * min)
        val d2 = SignalFreshness.decide(listOf(next, ahead), me, d1.state, now + 2 * min)
        assertEquals(listOf("b"), ids(d2.fresh))
    }

    @Test
    fun myClockAheadOnFirstRunFallsBackToNewestServerTime() {
        // My clock is two hours ahead: now - grace would be after everything on the server.
        val serverNow = now - 120 * min
        val d0 = SignalFreshness.decide(listOf(sig("a", serverNow - min)), me, null, now)
        assertTrue(d0.fresh.isEmpty())
        val d1 = SignalFreshness.decide(listOf(sig("b", serverNow + min), sig("a", serverNow - min)), me, d0.state, now + 2 * min)
        assertEquals(listOf("b"), ids(d1.fresh))
    }

    @Test
    fun ignoresMyOwnSignalsAndPendingTimestampsUseEstimateOrAt() {
        val d0 = SignalFreshness.decide(emptyList(), me, null, now)
        val mine = sig("mine", createdAt = null, at = now + min, to = partner)
        val legacy = sig("legacy", createdAt = null, at = now + min)
        val d1 = SignalFreshness.decide(listOf(mine, legacy), me, d0.state, now + min)
        assertEquals(listOf("legacy"), ids(d1.fresh))
    }

    @Test
    fun freshIsOldestFirstAndSeenIsCapped() {
        val d0 = SignalFreshness.decide(emptyList(), me, null, now)
        val list = (1..150).map { sig("s$it", now + it * 1000L) }.reversed()
        val d1 = SignalFreshness.decide(list, me, d0.state, now)
        assertEquals("s1", d1.fresh.first().id)
        assertEquals("s150", d1.fresh.last().id)
        assertEquals(SignalFreshness.MAX_SEEN_IDS, d1.state.seen.size)
        assertEquals("s150", d1.state.seen.last())
    }

    @Test
    fun splitKeepsNewestAndSummarizesRest() {
        val fresh = (1..5).map { sig("s$it", now + it) }
        val (single, rest) = SignalFreshness.split(fresh, 3)
        assertEquals(listOf("s3", "s4", "s5"), ids(single))
        assertEquals(listOf("s1", "s2"), ids(rest))
        val (few, none) = SignalFreshness.split(fresh.take(2), 3)
        assertEquals(2, few.size)
        assertTrue(none.isEmpty())
    }

    @Test
    fun cursorFollowsNewestServerTimeOfSignalsForMe() {
        val d0 = SignalFreshness.decide(emptyList(), me, null, now)
        assertEquals(null, d0.state.cursor)
        // My own signal (possibly a local estimate) never moves the cursor.
        val mineSent = sig("s", createdAt = now + 10 * min, to = partner)
        val d1 = SignalFreshness.decide(listOf(mineSent, sig("a", now + min)), me, d0.state, now + min)
        assertEquals(now + min, d1.state.cursor)
        // An empty "newer than cursor" result keeps it.
        assertEquals(now + min, SignalFreshness.decide(emptyList(), me, d1.state, now + 2 * min).state.cursor)
        val d2 = SignalFreshness.decide(listOf(sig("b", now + 3 * min)), me, d1.state, now + 3 * min)
        assertEquals(listOf("b"), ids(d2.fresh))
        assertEquals(now + 3 * min, d2.state.cursor)
    }
}
