package com.lovelyy.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lovelyy.app.data.AuthRepository
import com.lovelyy.app.data.CatRepository
import com.lovelyy.app.data.EventRepository
import com.lovelyy.app.data.LocationRepository
import com.lovelyy.app.data.DreamRepository
import com.lovelyy.app.data.WheelRepository
import com.lovelyy.app.data.CapsuleRepository
import com.lovelyy.app.data.DrawingRepository
import com.lovelyy.app.data.LeaveRepository
import com.lovelyy.app.data.MoodRepository
import com.lovelyy.app.data.NoteRepository
import com.lovelyy.app.data.QuestionRepository
import com.lovelyy.app.data.SignalRepository
import com.lovelyy.app.signals.SignalCenter
import com.lovelyy.app.data.PairingRepository
import com.lovelyy.app.data.UserRepository
import com.lovelyy.app.telemetry.Telemetry

class LovelyyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Dates and name endings follow the language of the strings Android shows (docs/LOCALIZATION.md).
        com.lovelyy.app.text.AppLocale.init(this)
        // Daily questions come from resources (values/questions.xml, translatable); always loaded first.
        loadQuestions()
        // Logging, analytics, crash reports, performance. See docs/analytics.md.
        Telemetry.init(this)
        // App Check before any Firestore call, so every request carries a token (docs/security.md).
        if (isFirebaseConfigured) com.lovelyy.app.security.AppCheckSetup.install()
        // The couple's shared time zone saved last time (shared "today"s, also for widgets).
        com.lovelyy.app.data.CoupleClock.init(this)
        // Server time learned last time (unless the phone restarted since), see TrustedClock.
        com.lovelyy.app.data.TrustedClock.init(this)
        // Partner signals: the listener starts when the app comes on screen (SignalCenter.onAppVisible), not here,
        // so a process started by WorkManager or a widget doesn't re-read the latest signals (see SignalWorker).
    }

    /** A language change swaps the resources; re-read the questions (same order and size in every language). */
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        loadQuestions()
    }

    private fun loadQuestions() {
        com.lovelyy.app.data.QuestionBank.init(resources.getStringArray(R.array.daily_questions).toList())
    }

    /** False until app/google-services.json is added (see README). */
    val isFirebaseConfigured: Boolean by lazy { FirebaseApp.getApps(this).isNotEmpty() }

    // Tiny hand-made "dependency container". Only touch these when Firebase is configured.
    val authRepository by lazy { AuthRepository(FirebaseAuth.getInstance()) }
    val userRepository by lazy { UserRepository(FirebaseFirestore.getInstance()) }
    val pairingRepository by lazy { PairingRepository(FirebaseFirestore.getInstance()) }
    val clockSync by lazy { com.lovelyy.app.data.ClockSync(FirebaseFirestore.getInstance()) }
    val catRepository by lazy { CatRepository(FirebaseFirestore.getInstance(), clockSync) }
    val signalRepository by lazy { SignalRepository(FirebaseFirestore.getInstance()) }
    val moodRepository by lazy { MoodRepository(FirebaseFirestore.getInstance()) }
    val questionRepository by lazy { QuestionRepository(FirebaseFirestore.getInstance()) }
    val noteRepository by lazy { NoteRepository(FirebaseFirestore.getInstance()) }
    val eventRepository by lazy { EventRepository(FirebaseFirestore.getInstance()) }
    val locationRepository by lazy { LocationRepository(FirebaseFirestore.getInstance()) }
    val dreamRepository by lazy { DreamRepository(FirebaseFirestore.getInstance()) }
    val wheelRepository by lazy { WheelRepository(FirebaseFirestore.getInstance()) }
    val capsuleRepository by lazy { CapsuleRepository(FirebaseFirestore.getInstance()) }
    val coupleSettingsRepository by lazy { com.lovelyy.app.data.CoupleSettingsRepository(FirebaseFirestore.getInstance()) }
    val drawingRepository by lazy { DrawingRepository(FirebaseFirestore.getInstance()) }
    val leaveRepository by lazy { LeaveRepository(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance()) }
    val signalCenter by lazy { SignalCenter(this) }
    /** Lovelyy Plus: the couple's plus map (Firestore) and Google Play purchases. */
    val plusRepository by lazy { com.lovelyy.app.data.PlusRepository(FirebaseFirestore.getInstance()) }
    val billingRepository by lazy { com.lovelyy.app.billing.BillingRepository(this) }

    /** A screen asked for by a widget or notification tap; Home opens it and clears it. */
    val pendingRoute = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
}
