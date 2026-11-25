package com.lovelyy.app.data

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
)

class DreamRepository(private val db: FirebaseFirestore) {

    private fun dreams(coupleId: String) =
        db.collection(Paths.COUPLES).document(coupleId).collection(Paths.DREAMS)

    suspend fun save(coupleId: String, me: UserProfile, id: String?, title: String, category: DreamCategory, goal: String) {
        val data = mapOf(
            "title" to title.trim().take(MAX_TITLE),
            "category" to category.key,
            "goal" to goal.trim().take(MAX_GOAL),
        )
        if (id == null) {
            dreams(coupleId).add(
                data + mapOf("by" to me.uid, "byName" to me.displayName, "at" to System.currentTimeMillis(), "doneAt" to null),
            ).await()
        } else {
            dreams(coupleId).document(id).update(data).await()
        }
    }

    suspend fun setDone(coupleId: String, id: String, done: Boolean) {
        dreams(coupleId).document(id).update("doneAt", if (done) System.currentTimeMillis() else null).await()
    }

    suspend fun delete(coupleId: String, id: String) {
        dreams(coupleId).document(id).delete().await()
    }

    fun observe(coupleId: String): Flow<List<Dream>> = callbackFlow {
        val reg = dreams(coupleId).orderBy("at", Query.Direction.DESCENDING).limit(300).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
            } else if (snap != null) {
                trySend(
                    snap.documents.mapNotNull { d ->
                        Dream(
                            id = d.id,
                            title = d.getString("title") ?: return@mapNotNull null,
                            category = DreamCategory.of(d.getString("category")),
                            goal = d.getString("goal").orEmpty(),
                            by = d.getString("by").orEmpty(),
                            byName = d.getString("byName").orEmpty(),
                            at = d.getLong("at") ?: 0L,
                            doneAt = d.getLong("doneAt"),
                        )
                    },
                )
            }
        }
        awaitClose { reg.remove() }
    }

    companion object {
        const val MAX_TITLE = 80
        const val MAX_GOAL = 40
    }
}
