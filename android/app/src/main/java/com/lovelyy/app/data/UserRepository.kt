package com.lovelyy.app.data

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository(private val db: FirebaseFirestore) {

    private fun userRef(uid: String) = db.collection(Paths.USERS).document(uid)
    private fun coupleRef(id: String) = db.collection(Paths.COUPLES).document(id)

    /** Creates users/{uid} the first time someone signs in. Safe to call on every launch. */
    suspend fun ensureUserDoc(user: FirebaseUser) {
        createOrNameUser(user.uid, user.displayName.orEmpty(), photoUrl = user.photoUrl?.toString(), overwriteBlankName = false)
    }

    /**
     * Right after an email sign-up: store the typed name. Runs as a transaction because
     * [ensureUserDoc] may be creating the same doc at the same moment.
     */
    suspend fun setNameIfBlank(uid: String, name: String) {
        createOrNameUser(uid, name, photoUrl = null, overwriteBlankName = true)
    }

    private suspend fun createOrNameUser(uid: String, name: String, photoUrl: String?, overwriteBlankName: Boolean) {
        val ref = userRef(uid)
        val cleanName = name.trim().take(MAX_NAME)
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            if (!snap.exists()) {
                tx.set(
                    ref,
                    mapOf(
                        "displayName" to cleanName,
                        "photoUrl" to photoUrl,
                        "coupleId" to null,
                        "pairCode" to null,
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                )
            } else if (overwriteBlankName && cleanName.isNotEmpty() && snap.getString("displayName").isNullOrBlank()) {
                tx.update(ref, "displayName", cleanName)
            }
            null
        }.await()
    }

    suspend fun updateName(uid: String, name: String) {
        userRef(uid).update("displayName", name.trim().take(MAX_NAME)).await()
    }

    /** Live users/{uid}. Emits null while the doc doesn't exist yet. */
    fun observeUser(uid: String): Flow<UserProfile?> = callbackFlow {
        val reg = userRef(uid).addSnapshotListener { snap, err ->
            if (err != null) close(err) else trySend(snap?.toUserProfile())
        }
        awaitClose { reg.remove() }
    }

    /** Live couples/{id}. Only works for the two members (Firestore rules). */
    fun observeCouple(id: String): Flow<Couple?> = callbackFlow {
        val reg = coupleRef(id).addSnapshotListener { snap, err ->
            if (err != null) close(err) else trySend(snap?.toCouple())
        }
        awaitClose { reg.remove() }
    }

    companion object {
        const val MAX_NAME = 40
    }
}
