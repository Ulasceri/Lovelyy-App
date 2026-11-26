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

/** A signal was refused on the phone because the previous one went out less than the minimum gap ago. */
class SignalThrottledException : Exception("Signals are sent at most once every few seconds")

/** The hourly or daily signal limit is reached (see [SignalLimits]); the screens show a friendly message. */
class SignalLimitException : Exception("Signal limit reached")

/**
 * Signal rate limit, mirrored by firestore.rules (validLimits). The numbers live in the Firestore doc
 * config/limits (signalsPerHour, signalsPerDay, minGapSeconds), so they can be changed in the Firebase
 * console without an app update; [Config] holds the defaults used when the doc or a field is missing.
 * couples/{id}/limits/{uid} keeps one hourly and one daily window, both started with the server time.
 * Pure (unit-tested in SignalLimitsTest).
 */
object SignalLimits {
    const val HOUR_MS = 3_600_000L
    const val DAY_MS = 24 * HOUR_MS

    data class Config(val perHour: Int = 15, val perDay: Int = 60, val minGapMs: Long = 2_000L)

    /** My limits doc: window starts (server time, millis) and counts; null starts = no window yet. */
    data class State(val hourStart: Long?, val hourCount: Int, val dayStart: Long?, val dayCount: Int)

    /** What the next write does: start a new window (count 1) or count up in the current one. */
    data class Plan(val resetHour: Boolean, val hourCount: Int, val resetDay: Boolean, val dayCount: Int)

    /**
     * The next write, judged with the phone's clock [now]. A window is restarted once it is over.
     * [flip] swaps the choice for windows that exist: the fallback when the server disagreed because
     * this phone's clock is off (counting up in an expired window is always accepted by the rules).
     */
    fun plan(state: State?, now: Long, flip: Boolean = false): Plan {
        fun reset(start: Long?, length: Long): Boolean {
            if (start == null) return true
            val over = now >= start + length
            return if (flip) !over else over
        }
        val rh = reset(state?.hourStart, HOUR_MS)
        val rd = reset(state?.dayStart, DAY_MS)
        return Plan(
            resetHour = rh, hourCount = if (rh) 1 else (state?.hourCount ?: 0) + 1,
            resetDay = rd, dayCount = if (rd) 1 else (state?.dayCount ?: 0) + 1,
        )
    }

    fun isOverLimit(plan: Plan, config: Config): Boolean = plan.hourCount > config.perHour || plan.dayCount > config.perDay
}

class SignalRepository(private val db: FirebaseFirestore) {

    private fun signals(coupleId: String) =
        db.collection(Paths.COUPLES).document(coupleId).collection(Paths.SIGNALS)

    /** Uptime-clock time of the last send from this phone (any screen or widget). */
    @Volatile private var lastSentAt: Long? = null

    @Volatile private var config: SignalLimits.Config? = null
    @Volatile private var configReadAt: Long = 0L

    /** The limits from config/limits, re-read at most every 10 minutes; defaults if it can't be read. */
    private suspend fun limitsConfig(): SignalLimits.Config {
        val now = android.os.SystemClock.elapsedRealtime()
        config?.let { if (now - configReadAt < CONFIG_TTL_MS) return it }
        val defaults = SignalLimits.Config()
        val read = try {
            val snap = db.collection(Paths.CONFIG).document(Paths.LIMITS_CONFIG_DOC).get().await()
            SignalLimits.Config(
                perHour = snap.getLong("signalsPerHour")?.toInt() ?: defaults.perHour,
                perDay = snap.getLong("signalsPerDay")?.toInt() ?: defaults.perDay,
                minGapMs = snap.getLong("minGapSeconds")?.let { it * 1000 } ?: defaults.minGapMs,
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            timber.log.Timber.w(e, "Reading the signal limits failed, using defaults")
            config ?: defaults
        }
        config = read
        configReadAt = now
        return read
    }

    /**
     * Sends a signal to the partner. Rate limited on two levels: this phone refuses a send within the
     * minimum gap ([SignalThrottledException]) or over the hourly/daily limit ([SignalLimitException]),
     * and firestore.rules refuse a signal unless the same batch updates couples/{id}/limits/{me} within
     * the same limits, in server time. That keeps a buggy loop, a modified client or a partner who
     * won't stop from flooding the other phone (and the project's daily write quota).
     */
    suspend fun send(
        coupleId: String,
        me: UserProfile,
        partnerUid: String,
        kind: SignalKind,
        emotion: Emotion? = null,
        mood: MoodKind? = null,
        text: String? = null,
    ) {
        val cfg = limitsConfig()
        val now = android.os.SystemClock.elapsedRealtime()
        synchronized(this) {
            val last = lastSentAt
            if (last != null && now - last < cfg.minGapMs + GAP_MARGIN_MS) throw SignalThrottledException()
            lastSentAt = now
        }
        val couple = db.collection(Paths.COUPLES).document(coupleId)
        val limitRef = couple.collection(Paths.LIMITS).document(me.uid)
        val current = limitRef.get().await()
        val hourStart = current.getTimestamp("hourStart")
        val dayStart = current.getTimestamp("dayStart")
        val state = if (!current.exists()) null else SignalLimits.State(
            hourStart = hourStart?.toDate()?.time,
            hourCount = current.getLong("hourCount")?.toInt() ?: 0,
            dayStart = dayStart?.toDate()?.time,
            dayCount = current.getLong("dayCount")?.toInt() ?: 0,
        )
        val signal = mapOf(
            "kind" to kind.key,
            "emotion" to emotion?.key,
            "mood" to mood?.key,
            "from" to me.uid,
            "fromName" to me.displayName,
            "to" to partnerUid,
            "text" to text?.take(MAX_TEXT),
            "at" to System.currentTimeMillis(),
            "createdAt" to FieldValue.serverTimestamp(),
        )

        suspend fun commit(plan: SignalLimits.Plan) {
            // Kept windows are written back as the exact Timestamp read (server time has microseconds).
            val limits = mapOf(
                "signalAt" to FieldValue.serverTimestamp(),
                "hourStart" to if (plan.resetHour) FieldValue.serverTimestamp() else hourStart,
                "hourCount" to plan.hourCount,
                "dayStart" to if (plan.resetDay) FieldValue.serverTimestamp() else dayStart,
                "dayCount" to plan.dayCount,
            )
            val batch = db.batch()
            batch.set(limitRef, limits)
            batch.set(signals(coupleId).document(), signal)
            batch.commit().await()
        }

        val plan = SignalLimits.plan(state, System.currentTimeMillis())
        if (SignalLimits.isOverLimit(plan, cfg)) throw SignalLimitException()
        try {
            commit(plan)
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            if (e.code != com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
            // The phone's clock may disagree with the server about a window being over: try the other way once.
            val flipped = SignalLimits.plan(state, System.currentTimeMillis(), flip = true)
            if (flipped == plan || SignalLimits.isOverLimit(flipped, cfg)) throw SignalLimitException()
            try {
                commit(flipped)
            } catch (e2: com.google.firebase.firestore.FirebaseFirestoreException) {
                if (e2.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) throw SignalLimitException()
                throw e2
            }
        }
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

    companion object {
        /** Added to the configured minimum gap, so phone-side timing never hits the server limit. */
        const val GAP_MARGIN_MS = 100L
        const val CONFIG_TTL_MS = 10 * 60_000L
        /** Longest [Signal.text] the rules accept. */
        const val MAX_TEXT = 120
    }
}
