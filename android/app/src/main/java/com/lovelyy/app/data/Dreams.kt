package com.lovelyy.app.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Categories of a dream (labels in strings.xml, dr_cat_*). */
enum class DreamCategory(val key: String) {
    Seyahat("seyahat"), Deneyim("deneyim"), Hayat("hayat"), Diger("diger");

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key } ?: Diger
    }
}

/** couples/{id}/dreams/{auto}: one item of the shared bucket list (Hayaller listesi). */
data class Dream(
    val id: String,
    val title: String,
    val category: DreamCategory,
    /** Free text goal like "Nisan 2027 hedefi" or "büyük hayal". */
    val goal: String,
    val by: String,
    val byName: String,
    val at: Long,
    /** When it came true; null while it's still a dream. */
    val doneAt: Long?,
    /** Last title/category/goal edit (millis) and who made it; null on dreams written before these fields existed. */
    val updatedAt: Long? = null,
    val updatedBy: String? = null,
) {
    /** The part an editor changes (not doneAt: ticking a dream off is never a conflict). */
    val content: DreamContent get() = DreamContent(title, category, goal)
}

data class DreamContent(val title: String, val category: DreamCategory, val goal: String)

/** The partner changed the dream after this edit was opened. [current] is the version on the server now. */
class DreamConflictException(val current: Dream) : EditConflictException("Dream changed meanwhile")

class DreamRepository(private val db: FirebaseFirestore) {

    private fun dreams(coupleId: String) =
        db.collection(Paths.COUPLES).document(coupleId).collection(Paths.DREAMS)

    /**
     * Adds ([opened] null) or edits a dream ([opened] = it as it was when the editor opened). Editing throws
     * [ItemGoneException] if the partner deleted it meanwhile and [DreamConflictException] if the partner
     * changed it meanwhile, unless [force]. Offline, the edit is written without the conflict check.
     */
    suspend fun save(coupleId: String, me: UserProfile, opened: Dream?, title: String, category: DreamCategory, goal: String, force: Boolean = false) {
        val data = mapOf(
            "title" to title.trim().take(MAX_TITLE),
            "category" to category.key,
            "goal" to goal.trim().take(MAX_GOAL),
        )
        if (opened == null) {
            dreams(coupleId).add(
                data + mapOf("by" to me.uid, "byName" to me.displayName, "at" to System.currentTimeMillis(), "doneAt" to null),
            ).await()
        } else {
            dreams(coupleId).document(opened.id).updateUnlessChanged(
                db,
                data + mapOf("updatedAt" to System.currentTimeMillis(), "updatedBy" to me.uid),
                force,
                parse = { it.toDream() },
                isConflict = { EditRules.isConflict(opened.content, opened.updatedAt, it.content, it.updatedAt, it.updatedBy, me.uid) },
                conflict = { DreamConflictException(it) },
            )
        }
    }

    /** Throws [ItemGoneException] if the partner deleted the dream meanwhile. */
    suspend fun setDone(coupleId: String, id: String, done: Boolean) {
        dreams(coupleId).document(id).updateExisting(mapOf("doneAt" to if (done) System.currentTimeMillis() else null))
    }

    suspend fun delete(coupleId: String, id: String) {
        dreams(coupleId).document(id).delete().await()
    }

    fun observe(coupleId: String): Flow<List<Dream>> = callbackFlow {
        val reg = dreams(coupleId).orderBy("at", Query.Direction.DESCENDING).limit(300).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
            } else if (snap != null) {
                trySend(snap.documents.mapNotNull { it.toDream() })
            }
        }
        awaitClose { reg.remove() }
    }

    private fun DocumentSnapshot.toDream(): Dream? = Dream(
        id = id,
        title = getString("title") ?: return null,
        category = DreamCategory.of(getString("category")),
        goal = getString("goal").orEmpty(),
        by = getString("by").orEmpty(),
        byName = getString("byName").orEmpty(),
        at = getLong("at") ?: 0L,
        doneAt = getLong("doneAt"),
        updatedAt = getLong("updatedAt"),
        updatedBy = getString("updatedBy"),
    )

    companion object {
        const val MAX_TITLE = 80
        const val MAX_GOAL = 40
    }
}
