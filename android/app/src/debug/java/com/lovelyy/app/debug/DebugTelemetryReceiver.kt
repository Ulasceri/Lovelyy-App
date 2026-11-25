package com.lovelyy.app.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lovelyy.app.analytics.AnalyticsEvent
import com.lovelyy.app.analytics.DebugPing
import com.lovelyy.app.telemetry.Telemetry
import timber.log.Timber

/**
 * DEBUG BUILDS ONLY (src/debug). Lets you test telemetry from adb, with no button on any screen.
 * Only the shell (adb) can send these: the receiver requires android.permission.DUMP.
 *
 *   adb shell am broadcast -n com.lovelyy.app/.debug.DebugTelemetryReceiver -a com.lovelyy.app.debug.TELEMETRY --ez enabled true
 *   adb shell am broadcast -n com.lovelyy.app/.debug.DebugTelemetryReceiver -a com.lovelyy.app.debug.PING
 *   adb shell am broadcast -n com.lovelyy.app/.debug.DebugTelemetryReceiver -a com.lovelyy.app.debug.NON_FATAL
 *   adb shell am broadcast -n com.lovelyy.app/.debug.DebugTelemetryReceiver -a com.lovelyy.app.debug.CRASH
 *
 * See docs/analytics.md.
 */
class DebugTelemetryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TELEMETRY -> {
                if (intent.hasExtra(EXTRA_ENABLED)) {
                    Telemetry.consent.setEnabled(intent.getBooleanExtra(EXTRA_ENABLED, false))
                } else {
                    Telemetry.consent.reset()
                }
                Timber.i("debug: telemetry_enabled=%s", Telemetry.consent.enabled.value)
            }
            ACTION_PING -> Telemetry.track(DebugPing)
            ACTION_EVENT -> {
                // Sends one of the real events, e.g. --es name cat_fed (for checking reports/DebugView).
                val event = when (intent.getStringExtra(EXTRA_NAME)) {
                    "pairing_completed" -> AnalyticsEvent.PairingCompleted
                    "cat_fed" -> AnalyticsEvent.CatFed
                    "cat_watered" -> AnalyticsEvent.CatWatered
                    "cat_played" -> AnalyticsEvent.CatPlayed
                    "cat_petted" -> AnalyticsEvent.CatPetted(strokes = 3)
                    "cat_slept" -> AnalyticsEvent.CatSlept
                    else -> DebugPing
                }
                Telemetry.track(event)
            }
            ACTION_NON_FATAL -> Timber.e(IllegalStateException("Test non-fatal from adb"), "debug: test non-fatal")
            ACTION_CRASH -> {
                Timber.w("debug: about to throw a test crash")
                throw RuntimeException("Test crash from adb (debug build only)")
            }
        }
    }

    companion object {
        const val ACTION_TELEMETRY = "com.lovelyy.app.debug.TELEMETRY"
        const val ACTION_PING = "com.lovelyy.app.debug.PING"
        const val ACTION_EVENT = "com.lovelyy.app.debug.EVENT"
        const val ACTION_NON_FATAL = "com.lovelyy.app.debug.NON_FATAL"
        const val ACTION_CRASH = "com.lovelyy.app.debug.CRASH"
        const val EXTRA_ENABLED = "enabled"
        const val EXTRA_NAME = "name"
    }
}
