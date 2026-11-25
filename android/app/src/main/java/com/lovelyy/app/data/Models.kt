package com.lovelyy.app.data

import com.google.firebase.Timestamp

/** Firestore: users/{uid} */
data class UserProfile(
    val uid: String,
    val displayName: String = "",
    val photoUrl: String? = null,
    /** Set once the user is paired. Same id as couples/{coupleId}. */
    val coupleId: String? = null,
    /** The pairing code this user is currently sharing (if any). */
    val pairCode: String? = null,
)

/** Firestore: couples/{coupleId}. Only the two members can read or write it. */
data class Couple(
    val id: String,
    val members: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    /** The day the relationship started (set later by the couple, used by the day counter). */
    val startDate: Timestamp? = null,
) {
    fun partnerOf(uid: String): String? = members.firstOrNull { it != uid }
}

/** Errors the UI knows how to explain (mapped to Turkish strings in the UI layer). */
enum class AppError {
    Network,
    InvalidCredentials,
    EmailInUse,
    WeakPassword,
    InvalidEmail,
    GoogleNotConfigured,
    GoogleCancelled,
    CodeNotFound,
    CodeExpired,
    OwnCode,
    AlreadyPaired,
    PermissionDenied,
    Unknown,
}

class AppException(val error: AppError, cause: Throwable? = null) : Exception(error.name, cause)
