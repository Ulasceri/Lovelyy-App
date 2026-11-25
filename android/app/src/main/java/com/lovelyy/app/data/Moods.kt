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

class MoodRepository(private val db: FirebaseFirestore) {

    private fun moods(coupleId: String) =
        db.collection(Paths.COUPLES).document(coupleId).collection(Paths.MOODS)

    suspend fun set(coupleId: String, uid: String, kind: MoodKind, note: String) {
        moods(coupleId).document(uid).set(
            mapOf(
                "kind" to kind.key,
                "note" to note.trim().take(MAX_NOTE),
                "at" to System.currentTimeMillis(),
            ),
        ).await()
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
