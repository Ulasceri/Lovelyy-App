package com.lovelyy.app.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
    Mood("mood");

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
    /** Sender's clock, millis. Used for ordering and "is this new?". */
    val at: Long,
)

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
    ) {
        signals(coupleId).add(
            mapOf(
                "kind" to kind.key,
                "emotion" to emotion?.key,
                "mood" to mood?.key,
                "from" to me.uid,
                "fromName" to me.displayName,
                "to" to partnerUid,
                "at" to System.currentTimeMillis(),
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    /** The latest signals of the couple, newest first (both directions). */
    fun observeRecent(coupleId: String, limit: Long = 30): Flow<List<Signal>> = callbackFlow {
        val reg = signals(coupleId)
            .orderBy("at", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snap, err ->
                if (err != null) close(err) else if (snap != null) trySend(snap.documents.mapNotNull { it.toSignal() })
            }
        awaitClose { reg.remove() }
    }

    /** One-time read for the background check. */
    suspend fun recent(coupleId: String, limit: Long = 20): List<Signal> =
        signals(coupleId).orderBy("at", Query.Direction.DESCENDING).limit(limit).get().await()
            .documents.mapNotNull { it.toSignal() }

    private fun com.google.firebase.firestore.DocumentSnapshot.toSignal(): Signal? {
        val kind = SignalKind.of(getString("kind")) ?: return null
        return Signal(
            id = id,
            kind = kind,
            emotion = Emotion.of(getString("emotion")),
            mood = MoodKind.of(getString("mood")),
            from = getString("from") ?: return null,
            fromName = getString("fromName").orEmpty(),
            to = getString("to") ?: return null,
            at = getLong("at") ?: return null,
        )
    }
}
