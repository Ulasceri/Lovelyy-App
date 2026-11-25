package com.lovelyy.app.telemetry

import android.content.Context
import com.lovelyy.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether analytics, crash reports and performance data may be collected.
 *
 * Stored as the boolean "telemetry_enabled". Until someone chooses, the default comes from
 * BuildConfig.TELEMETRY_DEFAULT_ENABLED (release: on, debug: off). A future privacy setting
 * only needs to call [setEnabled]; [Telemetry] applies the change to Firebase right away.
 */
class TelemetryConsent(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, BuildConfig.TELEMETRY_DEFAULT_ENABLED))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    fun setEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        _enabled.value = value
    }

    /** Forget the choice and go back to the build's default. */
    fun reset() {
        prefs.edit().remove(KEY_ENABLED).apply()
        _enabled.value = BuildConfig.TELEMETRY_DEFAULT_ENABLED
    }

    companion object {
        private const val PREFS = "telemetry"
        const val KEY_ENABLED = "telemetry_enabled"
    }
}
