package com.lovelyy.app.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Sticky-note colors (index into the palette in ui/notes). */
const val NOTE_COLORS = 5

/** couples/{id}/notes/{auto}: a shared sticky note. */
data class Note(
    val id: String,
    val text: String,
    val color: Int,
    val pinned: Boolean,
    val by: String,
    val byName: String,
    val at: Long,
)

class NoteRepository(private val db: FirebaseFirestore) {

    private fun notes(coupleId: String) =
        db.collection(Paths.COUPLES).document(coupleId).collection(Paths.NOTES)

    suspend fun add(coupleId: String, me: UserProfile, text: String, color: Int) {
        notes(coupleId).add(
            mapOf(
                "text" to text.trim().take(MAX_NOTE),
                "color" to color.coerceIn(0, NOTE_COLORS - 1),
                "pinned" to false,
                "by" to me.uid,
                "byName" to me.displayName,
                "at" to System.currentTimeMillis(),
            ),
        ).await()
    }

    /** Throws [ItemGoneException] if the partner deleted the note meanwhile. */
    suspend fun update(coupleId: String, id: String, text: String, color: Int) {
        notes(coupleId).document(id).updateExisting(
            mapOf("text" to text.trim().take(MAX_NOTE), "color" to color.coerceIn(0, NOTE_COLORS - 1)),
        )
    }

    /** Throws [ItemGoneException] if the partner deleted the note meanwhile. */
    suspend fun setPinned(coupleId: String, id: String, pinned: Boolean) {
        notes(coupleId).document(id).updateExisting(mapOf("pinned" to pinned))
    }

    suspend fun delete(coupleId: String, id: String) {
        notes(coupleId).document(id).delete().await()
    }

    /** All notes, pinned first, then newest first. */
    fun observe(coupleId: String): Flow<List<Note>> = callbackFlow {
        val reg = notes(coupleId).orderBy("at", Query.Direction.DESCENDING).limit(200).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
            } else if (snap != null) {
                val list = snap.documents.mapNotNull { d ->
                    Note(
                        id = d.id,
                        text = d.getString("text") ?: return@mapNotNull null,
                        color = (d.getLong("color") ?: 0L).toInt(),
                        pinned = d.getBoolean("pinned") == true,
                        by = d.getString("by").orEmpty(),
                        byName = d.getString("byName").orEmpty(),
                        at = d.getLong("at") ?: 0L,
                    )
                }
                trySend(list.sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.at }))
            }
        }
        awaitClose { reg.remove() }
    }

    companion object {
        const val MAX_NOTE = 1000
    }
}
