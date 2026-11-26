package com.lovelyy.app.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Feelings you can send with a heartbeat. Each one has its own look (ui/heartbeat/Emotions.kt). */
enum class Emotion(val key: String) {
    Ozlem("ozlem"),
    Sevgi("sevgi"),
    Ask("ask"),
    Sarilma("sarilma"),
    Opucuk("opucuk"),
    Huzur("huzur");

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key }
    }
}

enum class SignalKind(val key: String) {
    /** Press-and-hold heartbeat with an [Emotion]. */
    Heartbeat("heartbeat"),
    /** One-tap "aklımdasın" nudge. */
    Nudge("nudge"),
    /** "My mood changed" notice. */
    Mood("mood"),
    /** "I answered today's question." */
    Question("question"),
    /** "I left a note." */
    Note("note"),
    /** "I added something to our calendar" ([Signal.text] = title). */
    Event("event"),
    /** "I spun the decision wheel" ([Signal.text] = "wheel: result"). */
    Wheel("wheel");

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key }
    }
}

/** couples/{id}/signals/{auto}: something one partner sends the other. */
data class Signal(
    val id: String,
    val kind: SignalKind,
    val emotion: Emotion? = null,
    val mood: MoodKind? = null,
    val from: String,
    val fromName: String,
    val to: String,
    /** Extra text (e.g. the calendar entry's title). */
    val text: String? = null,
    /** Sender's clock, millis. Only for display ("5 dakika önce"); never trusted for "is this new?". */
    val at: Long,
    /**
     * Server time of the write, millis (Firestore `createdAt`). For the sender's own pending write
     * this is the local estimate. Null only for odd legacy docs without the field.
     */
    val createdAt: Long? = null,
) {
    /** Best clock-independent time of the signal: the server's, else the sender's. */
    val time: Long get() = createdAt ?: at
}

/**
 * Decides which signals are new for me without trusting the sender's clock.
 *
 * A signal is fresh when it is addressed to me, its id is not in the remembered "seen" ids and its
 * server time is after [State.floor]. The floor is set once, on the first check after install or
 * sign-in (first-run grace: only the last [FIRST_RUN_GRACE_MS] notify, never the whole history),
 * and is compared with server timestamps only. If my own clock is ahead, the floor is pulled back
 * to the newest signal already there, so later signals still count as new.
 */
object SignalFreshness {
    const val FIRST_RUN_GRACE_MS = 10 * 60_000L
    /** Larger than any query window (30), so a signal still in view is never forgotten. */
    const val MAX_SEEN_IDS = 100

    data class State(val floor: Long, val seen: List<String>)

    data class Decision(
        /** New signals for me, oldest first. */
        val fresh: List<Signal>,
        /** State to save for the next check. */
        val state: State,
    )

    /**
     * @param state the saved state, or null on the first check.
     * @param now my clock, only used to place the first-run floor.
     * @param graceMs how far back the first check may still notify.
     */
    fun decide(signals: List<Signal>, myUid: String, state: State?, now: Long, graceMs: Long = FIRST_RUN_GRACE_MS): Decision {
        val floor = state?.floor ?: run {
            val byClock = now - graceMs
            val newestServer = signals.mapNotNull { it.createdAt }.maxOrNull()
            if (newestServer != null) minOf(byClock, newestServer) else byClock
        }
        val seen = state?.seen.orEmpty()
        val seenSet = seen.toHashSet()
        val mine = signals.filter { it.to == myUid }
        val fresh = mine.filter { it.id !in seenSet && it.time > floor }.sortedBy { it.time }
        // Remember every signal for me we have looked at (fresh or too old), newest additions last.
        val added = mine.sortedBy { it.time }.map { it.id }.filter { it !in seenSet }
        return Decision(fresh, State(floor, (seen + added).takeLast(MAX_SEEN_IDS)))
    }

    /**
     * Splits fresh signals (oldest first) into the newest [cap] to notify one by one and the older
     * rest to fold into one summary notification.
     */
    fun split(fresh: List<Signal>, cap: Int): Pair<List<Signal>, List<Signal>> =
        fresh.takeLast(cap) to fresh.dropLast(cap)
}

class SignalRepository(private val db: FirebaseFirestore) {

    private fun signals(coupleId: String) =
        db.collection(Paths.COUPLES).document(coupleId).collection(Paths.SIGNALS)

    suspend fun send(
        coupleId: String,
        me: UserProfile,
        partnerUid: String,
        kind: SignalKind,
        emotion: Emotion? = null,
        mood: MoodKind? = null,
        text: String? = null,
    ) {
        signals(coupleId).add(
            mapOf(
                "kind" to kind.key,
                "emotion" to emotion?.key,
                "mood" to mood?.key,
                "from" to me.uid,
                "fromName" to me.displayName,
                "to" to partnerUid,
                "text" to text,
                "at" to System.currentTimeMillis(),
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    /** The latest signals of the couple, newest first (both directions). */
    fun observeRecent(coupleId: String, limit: Long = 30): Flow<List<Signal>> = callbackFlow {
        val reg = signals(coupleId)
            // Server time, not the sender's clock: a phone with a wrong clock can't push its signals out of the window.
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snap, err ->
                if (err != null) close(err) else if (snap != null) trySend(snap.documents.mapNotNull { it.toSignal() })
            }
        awaitClose { reg.remove() }
    }

    /** One-time read for the background check. */
    suspend fun recent(coupleId: String, limit: Long = 20): List<Signal> =
        signals(coupleId).orderBy("createdAt", Query.Direction.DESCENDING).limit(limit).get().await()
            .documents.mapNotNull { it.toSignal() }

    private fun DocumentSnapshot.toSignal(): Signal? {
        val kind = SignalKind.of(getString("kind")) ?: return null
        return Signal(
            id = id,
            kind = kind,
            emotion = Emotion.of(getString("emotion")),
            mood = MoodKind.of(getString("mood")),
            from = getString("from") ?: return null,
            fromName = getString("fromName").orEmpty(),
            to = getString("to") ?: return null,
            text = getString("text"),
            at = getLong("at") ?: return null,
            // Pending local writes have no server time yet: use the SDK's estimate instead of null.
            createdAt = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time,
        )
    }
}
