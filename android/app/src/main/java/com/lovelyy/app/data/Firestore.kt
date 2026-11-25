package com.lovelyy.app.data

import com.google.firebase.firestore.DocumentSnapshot

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
    )
