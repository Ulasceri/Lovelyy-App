package com.lovelyy.app.data

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.edit
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import kotlin.math.abs

/**
 * Pure math of [TrustedClock] (unit-tested in ServerTimeTest).
 *
 * The server stamps a write somewhere between sending it and getting the answer, so the best guess for "the
 * server time at the moment the answer came back" is the stamp plus half the round trip. The clock is then kept
 * as a base on the phone's monotonic clock (time since boot): it does not move when the user changes the clock or
 * the time zone, and stays right until the phone restarts.
 */
object ServerTime {
    /** Base so that `base + elapsed` is the server time. [sentAt]/[receivedAt] are elapsed millis around the write. */
    fun base(serverMillis: Long, sentAt: Long, receivedAt: Long): Long {
        val roundTrip = (receivedAt - sentAt).coerceAtLeast(0L)
        return serverMillis + roundTrip / 2 - receivedAt
    }

    /** How far the phone's own clock is ahead of the server (negative = behind). */
    fun skew(base: Long, wallMillis: Long, elapsed: Long): Long = wallMillis - (base + elapsed)

    /** Above this the phone's clock counts as wrong (only logged; the app uses server time anyway). */
    const val WRONG_CLOCK_MS = 2 * 60_000L
}

/**
 * The time the app counts with: server time, learned by [ClockSync], so moving the phone's clock (forward to cheat
 * the cat, back by mistake, or a wrong clock after travel) changes nothing. Until the first sync it is the phone's
 * own clock. Saved with the boot count so a cold start without a connection still uses the last sync.
 *
 * Shared "today"s ([CoupleClock.today]), the cat's needs and sleep ([CatRules]) and Plus time ([PlusClock]) use it.
 * firestore.rules reject cat times that are not near the server clock, so a phone must use this, not
 * System.currentTimeMillis(), for anything it writes into the cat.
 */
object TrustedClock {
    private const val PREFS = "trusted_clock"
    private const val K_BASE = "base"
    private const val K_BOOT = "boot"

    /** server time = base + elapsedRealtime. Null until synced (then the phone's own clock is used). */
    @Volatile private var base: Long? = null
    @Volatile private var prefs: SharedPreferences? = null
    @Volatile private var bootCount: Int = -1

    /** Application.onCreate: reuse the last sync if the phone has not restarted since. */
    fun init(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        bootCount = runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT) }.getOrDefault(-1)
        if (bootCount >= 0 && p.getInt(K_BOOT, -2) == bootCount && p.contains(K_BASE)) base = p.getLong(K_BASE, 0L)
    }

    /** Server time in epoch millis (the phone's clock until the first sync). */
    fun now(): Long {
        val b = base ?: return System.currentTimeMillis()
        return b + SystemClock.elapsedRealtime()
    }

    val isSynced: Boolean get() = base != null

    internal fun elapsed(): Long = SystemClock.elapsedRealtime()

    internal fun set(newBase: Long) {
        base = newBase
        val skew = ServerTime.skew(newBase, System.currentTimeMillis(), elapsed())
        if (abs(skew) > ServerTime.WRONG_CLOCK_MS) Timber.w("Phone clock is off by %d min; using server time", skew / 60_000)
        prefs?.edit { putLong(K_BASE, newBase); putInt(K_BOOT, bootCount) }
    }
}

/**
 * Learns the server time: writes couples/{id}/clock/{uid} = {at: serverTimestamp} and reads the stamp back from the
 * server (firestore.rules accept only the server time there). One write and one read per app start; care actions
 * call [ensure] first because their times must be near the server clock or the rules deny them.
 */
class ClockSync(private val db: FirebaseFirestore) {
    private val mutex = Mutex()
    @Volatile private var syncedThisProcess = false

    /** Syncs once per process (also when an earlier boot's sync was reused: the phone may have been set since). */
    suspend fun ensure(coupleId: String, uid: String) {
        if (syncedThisProcess) return
        mutex.withLock {
            if (syncedThisProcess) return
            try {
                withTimeout(SYNC_TIMEOUT_MS) { sync(coupleId, uid) }
                syncedThisProcess = true
            } catch (e: Exception) {
                // The caller was cancelled (or its own timeout ran out): pass that on.
                currentCoroutineContext().ensureActive()
                // Our own timeout or a failed write/read: keep the last known time and go on.
                if (e is CancellationException && e !is TimeoutCancellationException) throw e
                Timber.w(e, "Clock sync failed; using %s", if (TrustedClock.isSynced) "the last sync" else "the phone clock")
            }
        }
    }

    private suspend fun sync(coupleId: String, uid: String) {
        val ref = db.collection(Paths.COUPLES).document(coupleId).collection(Paths.CLOCK).document(uid)
        val sentAt = TrustedClock.elapsed()
        ref.set(mapOf("at" to FieldValue.serverTimestamp())).await()
        val receivedAt = TrustedClock.elapsed()
        val stamp = ref.get(Source.SERVER).await().getTimestamp("at") ?: return
        TrustedClock.set(ServerTime.base(stamp.toDate().time, sentAt, receivedAt))
    }

    companion object {
        const val SYNC_TIMEOUT_MS = 10_000L
    }
}
