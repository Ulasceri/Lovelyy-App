package com.lovelyy.app.telemetry

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import com.lovelyy.app.BuildConfig
import com.lovelyy.app.analytics.AnalyticsEvent
import com.lovelyy.app.analytics.AnalyticsTracker
import com.lovelyy.app.analytics.FirebaseAnalyticsTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

/**
 * One entry point for logging, analytics, crash reports and performance traces.
 * Call [init] once from Application.onCreate. Before that, and without Firebase, everything is a no-op.
 */
object Telemetry {

    @Volatile var analytics: AnalyticsTracker = AnalyticsTracker.NoOp
        private set
    @Volatile var crash: CrashReporter = CrashReporter.NoOp
        private set
    lateinit var consent: TelemetryConsent
        private set

    @Volatile private var firebaseReady = false

    fun init(app: Application) {
        // Logging first, so everything below can log.
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        // Warnings/errors also go to Crashlytics. It's planted in debug too, but nothing is sent
        // unless collection is on, which is off by default in debug (see TelemetryConsent).
        Timber.plant(CrashlyticsTree { crash })

        consent = TelemetryConsent(app)
        firebaseReady = FirebaseApp.getApps(app).isNotEmpty()
        if (!firebaseReady) {
            Timber.w("Firebase not configured: telemetry disabled")
            return
        }
        analytics = FirebaseAnalyticsTracker(FirebaseAnalytics.getInstance(app))
        crash = FirebaseCrashReporter(FirebaseCrashlytics.getInstance()).apply {
            setKey("build_type", BuildConfig.BUILD_TYPE)
        }
        // Apply the stored choice now and whenever it changes (e.g. a future privacy toggle).
        consent.enabled
            .onEach { applyCollection(app, it) }
            .launchIn(CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))
    }

    private fun applyCollection(app: Application, enabled: Boolean) {
        Timber.i("telemetry collection %s", if (enabled) "enabled" else "disabled")
        FirebaseAnalytics.getInstance(app).apply {
            setAnalyticsCollectionEnabled(enabled)
            // Opting out also forgets the app-instance id.
            if (!enabled) resetAnalyticsData()
        }
        FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(enabled)
            // Opting out also throws away crash reports still waiting to be sent.
            if (!enabled) deleteUnsentReports()
        }
        FirebasePerformance.getInstance().isPerformanceCollectionEnabled = enabled
    }

    /** Account deletion: forget the analytics app-instance id and drop crash reports still waiting to be sent. */
    fun wipe(app: android.content.Context) {
        if (!firebaseReady) return
        runCatching {
            FirebaseAnalytics.getInstance(app).resetAnalyticsData()
            FirebaseCrashlytics.getInstance().deleteUnsentReports()
        }.onFailure { Timber.w(it, "Wiping telemetry data failed") }
    }

    fun track(event: AnalyticsEvent) = analytics.track(event)

    /** Logs a screen_view and remembers the screen for crash reports (route name only). */
    fun onScreen(screenName: String) {
        analytics.screen(screenName)
        crash.setKey("screen", screenName)
    }

    @PublishedApi
    internal fun startTrace(name: String): Trace? {
        if (!firebaseReady) return null
        return runCatching { FirebasePerformance.getInstance().newTrace(name).apply { start() } }.getOrNull()
    }
}
