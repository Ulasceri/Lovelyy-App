package com.lovelyy.app.signals

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.workDataOf
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.lovelyy.app.LovelyyApplication
import com.lovelyy.app.DeepLink
import com.lovelyy.app.R
import com.lovelyy.app.data.Emotion
import com.lovelyy.app.data.MoodKind
import com.lovelyy.app.data.Signal
import com.lovelyy.app.data.SignalFreshness
import com.lovelyy.app.data.SignalKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import timber.log.Timber

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
 *
 * "Is this new?" never trusts the sender's clock: see [SignalFreshness] (seen ids + server time).
 *
 * Why a signal can still arrive late while the app is in the background (platform limits we can't
 * fix without real push):
 *  - Android freezes cached background processes (the "cached apps freezer", Android 11+, and
 *    Doze / App Standby on idle phones). A frozen process keeps its Firestore listener registered
 *    but receives nothing until something wakes it, so the listener alone can lag by minutes.
 *  - Periodic WorkManager work can't run more often than every 15 minutes and Doze may defer it
 *    further (maintenance windows); one-time work with a delay is also only "at the earliest".
 * What we do about it: the listener is started as soon as the app comes on screen and keeps running
 * while the process lives, re-subscribing after transient errors instead of stopping for good; and
 * when the app goes to the background we queue a short series of one-time checks (~1, 3, 7 and 15
 * minutes later, see [SignalWorker.scheduleFollowUps]). Each check asks only for signals newer than
 * the last one handled (one read when there is nothing new). Only FCM (Blaze plan + a Cloud
 * Function) makes delivery instant.
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

    /** True while the app is on screen. Set by MainActivity through [onAppVisible]. */
    @Volatile var appVisible = false
        private set

    /** MainActivity onStart/onStop. Going to the background queues a few quick catch-up checks. */
    fun onAppVisible(visible: Boolean) {
        appVisible = visible
        if (!app.isFirebaseConfigured || savedCouple(app) == null) return
        if (visible) runCatching { resumeIfSignedIn() }.onFailure { Timber.w(it, "Resuming signal updates failed") }
        if (visible) SignalWorker.cancelFollowUps(app) else SignalWorker.scheduleFollowUps(app)
    }

    /**
     * The app came on screen: listen again for the couple saved last time (before Home is even shown), and
     * keep listening as long as the process lives, also in the background. Not called when WorkManager or a
     * widget starts the process: [SignalWorker] does its own cheap check there, and a listener opened in such
     * a short-lived process re-read the latest signals each time (up to 30 reads per 15-minute check).
     */
    fun resumeIfSignedIn() {
        if (!app.isFirebaseConfigured) return
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val (coupleId, myUid) = savedCouple(app) ?: return
        if (user.uid == myUid) start(coupleId, myUid)
    }

    fun start(coupleId: String, myUid: String) {
        val key = "$coupleId/$myUid"
        if (listeningTo == key && job?.isActive == true) return
        stop()
        listeningTo = key
        save(app) { putString(K_COUPLE, coupleId); putString(K_UID, myUid) }
        SignalWorker.schedule(app)
        job = scope.launch {
            app.signalRepository.observeRecent(coupleId)
                // Keep listening while the process lives: re-subscribe after transient errors
                // (network, unavailable). Permission denied means unpaired or signed out: stop then.
                .retryWhen { cause, attempt ->
                    val denied = (cause as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                    if (denied) return@retryWhen false
                    Timber.w(cause, "Signal updates failed, re-subscribing")
                    delay((5_000L shl attempt.coerceAtMost(6L).toInt()).coerceAtMost(5 * 60_000L))
                    true
                }
                .catch { Timber.w(it, "Signal updates stopped (unpaired or signed out?)") /* the worker will catch up */ }
                .collect { list ->
                    _recent.value = list
                    // No in-app overlay yet (its design isn't approved): everything is a notification.
                    deliver(app, myUid, list)
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
        /** Pre-fix "newest sender time seen". Only read to migrate without re-notifying. */
        private const val K_LEGACY_LAST = "lastSeenAt"
        private const val K_FLOOR = "floor"
        private const val K_SEEN = "seenIds"
        private const val K_CURSOR = "cursor"
        private const val K_COUPLE = "coupleId"
        private const val K_UID = "uid"
        private const val CHANNEL = "partner_signals"
        /** Notified one by one; older fresh signals are folded into one summary notification. */
        private const val MAX_NOTIFY = 3
        /** Listener and worker run in the same process: decide + save as one step. */
        private val LOCK = Any()
        /** Heartbeat-like vibration: ba-dum ... ba-dum. */
        val HEARTBEAT_PATTERN = longArrayOf(0, 90, 110, 140, 520, 90, 110, 140)
        val NUDGE_PATTERN = longArrayOf(0, 60, 90, 60)

        private inline fun save(context: Context, block: android.content.SharedPreferences.Editor.() -> Unit) =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit(action = block)

        /** Picks the new signals for me in [list], remembers them and posts their notifications. */
        fun deliver(context: Context, myUid: String, list: List<Signal>) {
            val fresh = synchronized(LOCK) {
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val state = if (prefs.contains(K_FLOOR)) {
                    SignalFreshness.State(
                        floor = prefs.getLong(K_FLOOR, 0L),
                        seen = prefs.getString(K_SEEN, null).orEmpty().split(',').filter { it.isNotEmpty() },
                        cursor = prefs.getLong(K_CURSOR, 0L).takeIf { it > 0L },
                    )
                } else {
                    null
                }
                // Updating from the old sender-clock version: what is already there was handled, no grace.
                val grace = if (prefs.contains(K_LEGACY_LAST)) 0L else SignalFreshness.FIRST_RUN_GRACE_MS
                val decision = SignalFreshness.decide(list, myUid, state, System.currentTimeMillis(), grace)
                save(context) {
                    putLong(K_FLOOR, decision.state.floor)
                    putString(K_SEEN, decision.state.seen.joinToString(","))
                    decision.state.cursor?.let { putLong(K_CURSOR, it) }
                    remove(K_LEGACY_LAST)
                }
                decision.fresh
            }
            if (fresh.isEmpty()) return
            val (single, rest) = SignalFreshness.split(fresh, MAX_NOTIFY)
            if (rest.isNotEmpty()) notifySummary(context, rest)
            single.forEach { notify(context, it) }
        }

        /** Newest server time of a signal for me already handled; the background check asks only for newer ones. */
        fun cursor(context: Context): Long? = synchronized(LOCK) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(K_CURSOR, 0L).takeIf { it > 0L }
        }

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
                SignalKind.Question -> context.getString(R.string.signal_question_msg, who)
                SignalKind.Note -> context.getString(R.string.signal_note_msg, who)
                SignalKind.Event -> context.getString(R.string.signal_event_msg, who, s.text.orEmpty())
                SignalKind.Wheel -> context.getString(R.string.signal_wheel_msg, who, s.text.orEmpty())
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

        private fun route(kind: SignalKind) = when (kind) {
            SignalKind.Heartbeat, SignalKind.Nudge -> DeepLink.HEARTBEAT
            SignalKind.Mood -> DeepLink.MOOD
            SignalKind.Question -> DeepLink.QUESTION
            SignalKind.Note -> DeepLink.NOTES
            SignalKind.Event -> DeepLink.PLANS
            SignalKind.Wheel -> DeepLink.WHEEL
        }

        fun notify(context: Context, s: Signal) {
            // A tap opens the screen the signal is about.
            val open = DeepLink.intent(context, route(s.kind), requestCode = 1000 + s.kind.ordinal)
            post(context, s.id.hashCode(), message(context, s), open, if (s.kind == SignalKind.Nudge) NUDGE_PATTERN else HEARTBEAT_PATTERN)
        }

        /** "Bora sana 5 kalp daha gönderdi": the older fresh signals beyond [MAX_NOTIFY], in one notification. */
        fun notifySummary(context: Context, rest: List<Signal>) {
            val who = rest.last().fromName.ifBlank { context.getString(R.string.home_partner_fallback) }
            val hearts = rest.all { it.kind == SignalKind.Heartbeat }
            val text = context.resources.getQuantityString(if (hearts) R.plurals.signal_summary_hearts else R.plurals.signal_summary_mixed, rest.size, who, rest.size)
            val kinds = rest.map { it.kind }.distinct()
            val route = if (kinds.size == 1) route(kinds.single()) else null
            val open = DeepLink.intent(context, route, requestCode = 1100)
            post(context, "summary:${rest.last().id}".hashCode(), text, open, HEARTBEAT_PATTERN)
        }

        /** The colored app logo for the notification's large icon, drawn once at a small size and kept. */
        @Volatile private var logo: Bitmap? = null

        private fun largeIcon(context: Context): Bitmap? = logo ?: runCatching {
            val px = context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
            ContextCompat.getDrawable(context, R.mipmap.ic_launcher)?.toBitmap(px, px)
        }.onFailure { Timber.w(it, "Notification logo") }.getOrNull()?.also { logo = it }

        private fun post(context: Context, id: Int, text: String, open: PendingIntent, vibrate: LongArray) {
            ensureChannel(context)
            if (!canNotify(context)) return
            val n = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_logo)
                .setColor(ContextCompat.getColor(context, R.color.peach))
                .setLargeIcon(largeIcon(context))
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVibrate(vibrate)
                .setAutoCancel(true)
                .setContentIntent(open)
                .build()
            try {
                NotificationManagerCompat.from(context).notify(id, n)
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

/**
 * Catches up on signals while the app is closed (no push on the Spark plan). Runs every ~15
 * minutes, plus a few one-time follow-ups right after the app goes to the background.
 */
class SignalWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as LovelyyApplication
        if (!app.isFirebaseConfigured || FirebaseAuth.getInstance().currentUser == null) return Result.success()
        val (coupleId, myUid) = SignalCenter.savedCouple(app) ?: return Result.success()
        if (app.signalCenter.appVisible) return Result.success()
        return try {
            // Only signals newer than the last one handled: usually none, billed as one read (not 20).
            SignalCenter.deliver(app, myUid, app.signalRepository.recent(coupleId, newerThan = SignalCenter.cursor(app)))
            val step = inputData.getInt(K_STEP, -1)
            if (step >= 0 && !app.signalCenter.appVisible) enqueueFollowUp(app, step + 1)
            Result.success()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Timber.w(e, "Signal check failed, will retry")
            Result.retry()
        }
    }

    companion object {
        private const val WORK = "partner-signals-check"
        private const val WORK_SOON = "partner-signals-soon"
        private const val K_STEP = "step"
        /** Delays between the follow-up checks after going to the background (~1, 3, 7, 15 min). */
        private val FOLLOW_UP_MINUTES = longArrayOf(1, 2, 4, 8)

        private fun network() = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SignalWorker>(15, TimeUnit.MINUTES)
                .setConstraints(network())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** App went to the background: check again soon, a few times, until the periodic check takes over. */
        fun scheduleFollowUps(context: Context) = enqueueFollowUp(context, 0)

        fun cancelFollowUps(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_SOON)
        }

        private fun enqueueFollowUp(context: Context, step: Int) {
            if (step >= FOLLOW_UP_MINUTES.size) return
            val request = OneTimeWorkRequestBuilder<SignalWorker>()
                .setInitialDelay(FOLLOW_UP_MINUTES[step], TimeUnit.MINUTES)
                .setConstraints(network())
                .setInputData(workDataOf(K_STEP to step))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_SOON, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK)
            cancelFollowUps(context)
        }
    }
}
