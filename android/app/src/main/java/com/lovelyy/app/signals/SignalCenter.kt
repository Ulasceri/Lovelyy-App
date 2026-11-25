package com.lovelyy.app.signals

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.lovelyy.app.LovelyyApplication
import com.lovelyy.app.MainActivity
import com.lovelyy.app.R
import com.lovelyy.app.data.Emotion
import com.lovelyy.app.data.MoodKind
import com.lovelyy.app.data.Signal
import com.lovelyy.app.data.SignalKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Delivers heartbeats, nudges and mood notices from the partner.
 *
 * Free Spark plan = no server, so no real push (FCM needs a trusted sender such as a Cloud
 * Function, which needs the Blaze plan). What we do instead:
 *  - While the app process is alive (open, or recently in the background), a Firestore
 *    listener sees a new signal within a second: in the app it plays a full-screen animation,
 *    in the background it posts a local notification with a heartbeat vibration.
 *  - When the app is fully closed, [SignalWorker] checks every ~15 minutes (Android's minimum)
 *    and posts the notifications it missed.
 */
class SignalCenter(private val app: LovelyyApplication) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var listeningTo: String? = null

    private val _incoming = MutableSharedFlow<Signal>(extraBufferCapacity = 4)
    /** New signals for me while the app is on screen (the UI shows them big). */
    val incoming: SharedFlow<Signal> = _incoming

    private val _recent = MutableStateFlow<List<Signal>>(emptyList())
    /** The couple's latest signals, newest first (both directions). */
    val recent: StateFlow<List<Signal>> = _recent

    /** Set by MainActivity: true while the app is on screen. */
    @Volatile var appVisible = false

    fun start(coupleId: String, myUid: String) {
        val key = "$coupleId/$myUid"
        if (listeningTo == key && job?.isActive == true) return
        stop()
        listeningTo = key
        save(app) { putString(K_COUPLE, coupleId); putString(K_UID, myUid) }
        if (lastSeen(app) == 0L) save(app) { putLong(K_LAST, System.currentTimeMillis() - FIRST_RUN_GRACE_MS) }
        SignalWorker.schedule(app)
        job = scope.launch {
            app.signalRepository.observeRecent(coupleId)
                .catch { /* offline or signed out: the worker will catch up */ }
                .collect { list ->
                    _recent.value = list
                    val last = lastSeen(app)
                    val fresh = list.filter { it.to == myUid && it.at > last }.sortedBy { it.at }
                    if (fresh.isEmpty()) return@collect
                    save(app) { putLong(K_LAST, fresh.last().at) }
                    if (appVisible) {
                        fresh.takeLast(1).forEach { _incoming.tryEmit(it) }
                        vibrate(app, fresh.last().kind)
                    } else {
                        fresh.takeLast(MAX_NOTIFY).forEach { notify(app, it) }
                    }
                }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        listeningTo = null
        _recent.value = emptyList()
    }

    /** Sign out: forget everything so a new account starts clean. */
    fun reset() {
        stop()
        SignalWorker.cancel(app)
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { clear() }
    }

    companion object {
        private const val PREFS = "signals"
        private const val K_LAST = "lastSeenAt"
        private const val K_COUPLE = "coupleId"
        private const val K_UID = "uid"
        private const val CHANNEL = "partner_signals"
        private const val FIRST_RUN_GRACE_MS = 10 * 60_000L
        private const val MAX_NOTIFY = 3
        /** Heartbeat-like vibration: ba-dum ... ba-dum. */
        val HEARTBEAT_PATTERN = longArrayOf(0, 90, 110, 140, 520, 90, 110, 140)
        val NUDGE_PATTERN = longArrayOf(0, 60, 90, 60)

        private inline fun save(context: Context, block: android.content.SharedPreferences.Editor.() -> Unit) =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit(action = block)

        fun lastSeen(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(K_LAST, 0L)
        fun markSeen(context: Context, at: Long) = save(context) { putLong(K_LAST, at) }
        fun savedCouple(context: Context): Pair<String, String>? {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val c = p.getString(K_COUPLE, null) ?: return null
            val u = p.getString(K_UID, null) ?: return null
            return c to u
        }

        /** "Ulaş seni çok özledi…" */
        fun message(context: Context, s: Signal): String {
            val who = s.fromName.ifBlank { context.getString(R.string.home_partner_fallback) }
            return when (s.kind) {
                SignalKind.Heartbeat -> context.getString((s.emotion ?: Emotion.Sevgi).messageRes(), who)
                SignalKind.Nudge -> context.getString(R.string.signal_nudge_msg, who)
                SignalKind.Mood -> {
                    val m = s.mood ?: MoodKind.Mutlu
                    context.getString(R.string.signal_mood_msg, who, context.getString(m.labelRes()).lowercase(), m.emoji)
                }
            }
        }

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val ch = NotificationChannel(CHANNEL, context.getString(R.string.signal_channel), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.signal_channel_desc)
                enableVibration(true)
                vibrationPattern = HEARTBEAT_PATTERN
                setSound(
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build(),
                )
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }

        fun canNotify(context: Context): Boolean =
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        fun notify(context: Context, s: Signal) {
            ensureChannel(context)
            if (!canNotify(context)) return
            val open = PendingIntent.getActivity(
                context, 1,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val n = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_heart)
                .setColor(0xFFFFB38A.toInt())
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(message(context, s))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVibrate(if (s.kind == SignalKind.Nudge) NUDGE_PATTERN else HEARTBEAT_PATTERN)
                .setAutoCancel(true)
                .setContentIntent(open)
                .build()
            try {
                NotificationManagerCompat.from(context).notify(s.id.hashCode(), n)
            } catch (_: SecurityException) {
            }
        }

        fun vibrate(context: Context, kind: SignalKind) {
            val vibrator = if (Build.VERSION.SDK_INT >= 31) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return
            val pattern = if (kind == SignalKind.Nudge) NUDGE_PATTERN else HEARTBEAT_PATTERN
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }
}

/** Catches up on signals while the app is closed (no push on the Spark plan). */
class SignalWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as LovelyyApplication
        if (!app.isFirebaseConfigured || FirebaseAuth.getInstance().currentUser == null) return Result.success()
        val (coupleId, myUid) = SignalCenter.savedCouple(app) ?: return Result.success()
        if (app.signalCenter.appVisible) return Result.success()
        return try {
            val last = SignalCenter.lastSeen(app)
            val fresh = app.signalRepository.recent(coupleId).filter { it.to == myUid && it.at > last }.sortedBy { it.at }
            if (fresh.isNotEmpty()) {
                SignalCenter.markSeen(app, fresh.last().at)
                fresh.takeLast(3).forEach { SignalCenter.notify(app, it) }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK = "partner-signals-check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SignalWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(WORK)
    }
}
