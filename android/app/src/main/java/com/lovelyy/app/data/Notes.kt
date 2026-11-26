package com.lovelyy.app.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
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
    /** Last text/color edit (millis) and who made it; null on notes written before these fields existed. */
    val updatedAt: Long? = null,
    val updatedBy: String? = null,
)

/** The partner changed the note after this edit was opened. [current] is the version on the server now. */
class NoteConflictException(val current: Note) : Exception("Note changed meanwhile")

/** Pure rules for notes (unit-tested in NoteRulesTest). */
object NoteRules {
    /**
     * True if saving an edit would silently overwrite someone else's change: the server version differs
     * from the one the editor was opened with ([openedText], [openedUpdatedAt]) and the last edit was not
     * mine. Old notes have no updatedAt on either side, so then only the text is compared.
     */
    fun isConflict(
        openedText: String,
        openedUpdatedAt: Long?,
        serverText: String,
        serverUpdatedAt: Long?,
        serverUpdatedBy: String?,
        me: String,
    ): Boolean {
        val changed = openedUpdatedAt != serverUpdatedAt || openedText != serverText
        return changed && serverUpdatedBy != me
    }
}

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

    /**
     * Saves an edit of [opened] (the note as it was when the editor opened). Throws [ItemGoneException] if
     * the partner deleted it meanwhile, and [NoteConflictException] if the partner changed it meanwhile,
     * unless [force] (the user chose to overwrite). Offline, a transaction can't run; then the edit is
     * written as before, without the conflict check.
     */
    suspend fun update(coupleId: String, me: UserProfile, opened: Note, text: String, color: Int, force: Boolean = false) {
        val ref = notes(coupleId).document(opened.id)
        val data = mapOf(
            "text" to text.trim().take(MAX_NOTE),
            "color" to color.coerceIn(0, NOTE_COLORS - 1),
            "updatedAt" to System.currentTimeMillis(),
            "updatedBy" to me.uid,
        )
        try {
            db.runTransaction { tx ->
                val snap = tx.get(ref)
                if (!snap.exists()) throw ItemGoneException()
                val current = snap.toNote() ?: throw ItemGoneException()
                if (!force && NoteRules.isConflict(opened.text, opened.updatedAt, current.text, current.updatedAt, current.updatedBy, me.uid)) {
                    throw NoteConflictException(current)
                }
                tx.update(ref, data)
                null
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.causeOf<ItemGoneException>()?.let { throw it }
            e.causeOf<NoteConflictException>()?.let { throw it }
            if (!e.isOffline()) throw e
            ref.updateExisting(data)
        }
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
                val list = snap.documents.mapNotNull { it.toNote() }
                trySend(list.sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.at }))
            }
        }
        awaitClose { reg.remove() }
    }

    private fun DocumentSnapshot.toNote(): Note? {
        val text = getString("text") ?: return null
        return Note(
        id = id,
        text = text,
        color = (getLong("color") ?: 0L).toInt(),
        pinned = getBoolean("pinned") == true,
        by = getString("by").orEmpty(),
        byName = getString("byName").orEmpty(),
        at = getLong("at") ?: 0L,
        updatedAt = getLong("updatedAt"),
        updatedBy = getString("updatedBy"),
        )
    }

    companion object {
        const val MAX_NOTE = 1000
    }
}
