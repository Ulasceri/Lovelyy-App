package com.lovelyy.app.security

import com.google.firebase.appcheck.FirebaseAppCheck
import timber.log.Timber

/**
 * Firebase App Check: every Firestore request carries a token proving it comes from this app on a real
 * device (Play Integrity in release builds, a debug token in debug builds, see [AppCheckProviders]).
 * Once enforcement is turned on in the Firebase console, scripts and modified clients that only have the
 * public API key are refused. Setup and the safe order to enable enforcement: docs/security.md.
 */
object AppCheckSetup {
    fun install() {
        runCatching {
            FirebaseAppCheck.getInstance().apply {
                installAppCheckProviderFactory(AppCheckProviders.factory())
                setTokenAutoRefreshEnabled(true)
            }
        }.onFailure { Timber.e(it, "App Check setup failed") }
    }
}
