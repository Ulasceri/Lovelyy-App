package com.lovelyy.app.data

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

/** Collection names. Keep in sync with firebase/firestore.rules. */
object Paths {
    const val USERS = "users"
    const val COUPLES = "couples"
    const val PAIR_CODES = "pairCodes"
    const val CAT = "cat"
    const val CAT_DOC = "main"
    const val SIGNALS = "signals"
    const val MOODS = "moods"
    const val QUESTIONS = "questions"
    const val NOTES = "notes"
    const val EVENTS = "events"
    const val LOCATIONS = "locations"
    const val DREAMS = "dreams"
    const val WHEELS = "wheels"
    const val WHEEL_SPINS = "wheelSpins"
    const val WHEEL_LOG = "wheelLog"
    const val MOOD_LOG = "moodLog"
    const val CAPSULES = "capsules"
    const val STROKES = "strokes"
    const val DRAWING = "drawing"
    const val DRAWINGS = "drawings"
}

internal fun DocumentSnapshot.toUserProfile(): UserProfile? =
    if (!exists()) null else UserProfile(
        uid = id,
        displayName = getString("displayName").orEmpty(),
        photoUrl = getString("photoUrl"),
        coupleId = getString("coupleId"),
        pairCode = getString("pairCode"),
    )

@Suppress("UNCHECKED_CAST")
internal fun DocumentSnapshot.toCouple(): Couple? =
    if (!exists()) null else Couple(
        id = id,
        members = (get("members") as? List<String>).orEmpty(),
        createdAt = getTimestamp("createdAt"),
        startDate = getTimestamp("startDate"),
        endedAt = getTimestamp("endedAt"),
        endedBy = getString("endedBy"),
        endedByName = getString("endedByName"),
    )

/** The shared item being edited was deleted (usually by the partner) before the edit reached the server. */
class ItemGoneException(cause: Throwable? = null) : Exception("Item was deleted", cause)

/**
 * Edits an existing document without ever re-creating it: update() fails with NOT_FOUND when the document
 * is gone, which is turned into [ItemGoneException] so screens can tell the user instead of failing silently.
 */
internal suspend fun DocumentReference.updateExisting(data: Map<String, Any?>) {
    try {
        update(data).await()
    } catch (e: FirebaseFirestoreException) {
        if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) throw ItemGoneException(e)
        throw e
    }
}
