package com.lovelyy.app.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** How someone feels today. Labels live in strings.xml (mood_kind_*). */
enum class MoodKind(val key: String, val emoji: String) {
    Mutlu("mutlu", "😊"),
    Asik("asik", "🥰"),
    Huzurlu("huzurlu", "😌"),
    Heyecanli("heyecanli", "🤩"),
    Enerjik("enerjik", "💪"),
    Ozlemli("ozlemli", "🥺"),
    Yorgun("yorgun", "😴"),
    Uzgun("uzgun", "😢"),
    Stresli("stresli", "😣"),
    Sinirli("sinirli", "😤"),
    Sikkin("sikkin", "😕"),
    Hasta("hasta", "🤒");

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key }
    }
}

/** couples/{id}/moods/{uid}: each partner's current mood. */
data class MoodEntry(val uid: String, val kind: MoodKind, val note: String, val at: Long)

/** One line of the mood history: someone's last mood on [day]. */
data class MoodDay(val day: java.time.LocalDate, val entry: MoodEntry)

class MoodRepository(private val db: FirebaseFirestore) {

    private fun moods(coupleId: String) =
        db.collection(Paths.COUPLES).document(coupleId).collection(Paths.MOODS)

    private fun log(coupleId: String) =
        db.collection(Paths.COUPLES).document(coupleId).collection(Paths.MOOD_LOG)

    suspend fun set(coupleId: String, uid: String, kind: MoodKind, note: String) {
        val now = System.currentTimeMillis()
        val data = mapOf("kind" to kind.key, "note" to note.trim().take(MAX_NOTE), "at" to now)
        moods(coupleId).document(uid).set(data).await()
        // History (Ruh hali geçmişi): one entry per person per day, the last mood of the day wins.
        val day = java.time.LocalDate.now().toString()
        log(coupleId).document("${uid}_$day").set(data + mapOf("uid" to uid, "day" to day))
    }

    /** The mood log of the last [days] days (both partners), newest first. */
    fun observeHistory(coupleId: String, days: Long = 31): Flow<List<MoodDay>> = callbackFlow {
        val from = java.time.LocalDate.now().minusDays(days - 1).toString()
        val reg = log(coupleId).whereGreaterThanOrEqualTo("day", from).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
            } else if (snap != null) {
                trySend(
                    snap.documents.mapNotNull { d ->
                        val kind = MoodKind.of(d.getString("kind")) ?: return@mapNotNull null
                        val day = d.getString("day")?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() } ?: return@mapNotNull null
                        MoodDay(day, MoodEntry(d.getString("uid").orEmpty(), kind, d.getString("note").orEmpty(), d.getLong("at") ?: 0L))
                    }.sortedByDescending { it.entry.at },
                )
            }
        }
        awaitClose { reg.remove() }
    }

    /** Both partners' moods, keyed by uid. */
    fun observe(coupleId: String): Flow<Map<String, MoodEntry>> = callbackFlow {
        val reg = moods(coupleId).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
            } else if (snap != null) {
                trySend(
                    snap.documents.mapNotNull { d ->
                        val kind = MoodKind.of(d.getString("kind")) ?: return@mapNotNull null
                        MoodEntry(d.id, kind, d.getString("note").orEmpty(), d.getLong("at") ?: 0L)
                    }.associateBy { it.uid },
                )
            }
        }
        awaitClose { reg.remove() }
    }

    companion object {
        const val MAX_NOTE = 140
    }
}
