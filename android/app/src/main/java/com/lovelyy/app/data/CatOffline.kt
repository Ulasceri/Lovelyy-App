package com.lovelyy.app.data

import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.TimeoutCancellationException

/**
 * Care actions run in a Firestore transaction, which needs a connection (it can't queue offline: the shared
 * need math would break). This tells "no connection" apart from other failures so the UI can say so.
 */
object CatOffline {
    /** How long a care action may wait for the server before we call it offline. */
    const val ACTION_TIMEOUT_MS = 8_000L

    fun isOffline(e: Throwable): Boolean = when (e) {
        is TimeoutCancellationException -> true
        is FirebaseFirestoreException -> isOfflineCode(e.code.name)
        else -> false
    }

    /** True for the Firestore error code (by name) that means "no connection". Pure, so it is unit-testable. */
    fun isOfflineCode(codeName: String?): Boolean = codeName == "UNAVAILABLE"
}
