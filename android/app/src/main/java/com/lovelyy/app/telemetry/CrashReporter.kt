package com.lovelyy.app.telemetry

import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Crash reporting. Never put personal data in breadcrumbs or keys, and never set a user id.
 */
interface CrashReporter {
    /** Records a caught exception (shows in Crashlytics as a "non-fatal"). */
    fun logNonFatal(throwable: Throwable)

    /** A short line of context sent along with the next crash or non-fatal. */
    fun breadcrumb(message: String)

    /** Context like screen=home. Keys and values must not identify anyone. */
    fun setKey(key: String, value: String)

    object NoOp : CrashReporter {
        override fun logNonFatal(throwable: Throwable) = Unit
        override fun breadcrumb(message: String) = Unit
        override fun setKey(key: String, value: String) = Unit
    }
}

class FirebaseCrashReporter(private val crashlytics: FirebaseCrashlytics) : CrashReporter {
    override fun logNonFatal(throwable: Throwable) = crashlytics.recordException(throwable)
    override fun breadcrumb(message: String) = crashlytics.log(message)
    override fun setKey(key: String, value: String) = crashlytics.setCustomKey(key, value)
}
