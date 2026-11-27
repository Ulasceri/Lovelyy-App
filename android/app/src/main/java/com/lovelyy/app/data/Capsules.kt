package com.lovelyy.app.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

/**
 * couples/{id}/capsules/{auto}: a letter to the future (zaman kapsülü). The text stays hidden in the
 * app until [openOn]. Like Günün sorusu this is an app rule: Firestore rules can't hide one field.
 */
data class Capsule(
    val id: String,
    val title: String,
    val text: String,
    val openOn: LocalDate,
    val by: String,
    val byName: String,
    val at: Long,
) {
    fun isOpen(today: LocalDate) = !openOn.isAfter(today)
}

class CapsuleRepository(private val db: FirebaseFirestore) {

    private fun capsules(coupleId: String) =
        db.collection(Paths.COUPLES).document(coupleId).collection(Paths.CAPSULES)

    suspend fun add(coupleId: String, me: UserProfile, title: String, text: String, openOn: LocalDate) {
        capsules(coupleId).add(
            mapOf(
                "title" to title.trim().take(MAX_TITLE),
                "text" to text.trim().take(MAX_TEXT),
                "openOn" to openOn.toString(),
                "by" to me.uid,
                "byName" to me.displayName,
                "at" to System.currentTimeMillis(),
            ),
        ).await()
    }

    suspend fun delete(coupleId: String, id: String) {
        capsules(coupleId).document(id).delete().await()
    }

    /** Newest [limit] letters (200 by default; counts derived from that list are "at most 200", a couple writes a few per year). */
    fun observe(coupleId: String, limit: Long = 200): Flow<List<Capsule>> =
        observeQuery(capsules(coupleId).orderBy("at", Query.Direction.DESCENDING).limit(limit))

    /** Letters still sealed after [today] (openOn is an ISO date, so a string range works on a single-field index). */
    fun observeSealed(coupleId: String, today: LocalDate): Flow<List<Capsule>> =
        observeQuery(capsules(coupleId).whereGreaterThan("openOn", today.toString()).limit(200))

    /** Letters whose day has come (openOn <= [today]), the latest opening first, [limit] at a time. */
    fun observeOpened(coupleId: String, today: LocalDate, limit: Long): Flow<List<Capsule>> =
        observeQuery(capsules(coupleId).whereLessThanOrEqualTo("openOn", today.toString()).orderBy("openOn", Query.Direction.DESCENDING).limit(limit))

    /** How many letters are open by [today], counted on the server; null offline. */
    suspend fun openedCount(coupleId: String, today: LocalDate): Long? =
        capsules(coupleId).whereLessThanOrEqualTo("openOn", today.toString()).countOrNull()

    private fun observeQuery(query: Query): Flow<List<Capsule>> = callbackFlow {
        val reg = query.addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
            } else if (snap != null) {
                trySend(
                    snap.documents.mapNotNull { d ->
                        val open = d.getString("openOn")?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return@mapNotNull null
                        Capsule(
                            id = d.id,
                            title = d.getString("title").orEmpty(),
                            text = d.getString("text").orEmpty(),
                            openOn = open,
                            by = d.getString("by").orEmpty(),
                            byName = d.getString("byName").orEmpty(),
                            at = d.getLong("at") ?: 0L,
                        )
                    },
                )
            }
        }
        awaitClose { reg.remove() }
    }

    companion object {
        const val MAX_TITLE = 60
        const val MAX_TEXT = 4000
    }
}
