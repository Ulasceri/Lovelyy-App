package com.lovelyy.app.telemetry

import android.util.Log
import timber.log.Timber

/**
 * Timber to Crashlytics. WARN and ERROR lines become breadcrumbs; an exception logged at ERROR
 * (Timber.e(throwable, ...)) is also recorded as a non-fatal. DEBUG/INFO are dropped.
 */
class CrashlyticsTree(private val reporter: () -> CrashReporter) : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int) = priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority < Log.WARN) return
        val level = if (priority >= Log.ERROR) "E" else "W"
        val crash = reporter()
        crash.breadcrumb("$level/${tag ?: "-"}: $message")
        if (priority >= Log.ERROR && t != null) crash.logNonFatal(t)
    }
}
