package com.lovelyy.app.data

import android.content.Context
import androidx.core.content.edit
import com.lovelyy.app.BuildConfig

/**
 * What this phone remembers about Plus screens: the automatic paywall's once-a-day and dismiss counters, the
 * day the Home cards were closed and which "Plus açıldı" was already shown. Local on purpose: these are about
 * what this person saw, not about the couple. Cleared with the couple id in the key, so a new couple starts fresh.
 */
object PlusPrefs {
    private const val PREFS = "lovelyy_plus"

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun paywallMemory(context: Context, coupleId: String): PaywallMemory {
        val p = prefs(context)
        return PaywallMemory(
            lastShownDay = p.getString("paywall.shown.$coupleId", null),
            dismissals = p.getInt("paywall.dismissals.$coupleId", 0),
            lastDismissedAt = p.getLong("paywall.dismissedAt.$coupleId", 0L).takeIf { it > 0 },
        )
    }

    fun savePaywallMemory(context: Context, coupleId: String, m: PaywallMemory) = prefs(context).edit {
        putString("paywall.shown.$coupleId", m.lastShownDay)
        putInt("paywall.dismissals.$coupleId", m.dismissals)
        putLong("paywall.dismissedAt.$coupleId", m.lastDismissedAt ?: 0L)
    }

    /** The day-12/13 card (or the takeover card, [kind]) was closed on [day]. */
    fun cardDismissedOn(context: Context, coupleId: String, kind: String): String? = prefs(context).getString("card.$kind.$coupleId", null)

    fun dismissCard(context: Context, coupleId: String, kind: String, day: String) = prefs(context).edit { putString("card.$kind.$coupleId", day) }

    fun celebrationSeen(context: Context, coupleId: String): String? = prefs(context).getString("celebrated.$coupleId", null)

    fun markCelebrated(context: Context, coupleId: String, key: String) = prefs(context).edit { putString("celebrated.$coupleId", key) }

    /** Takeover sheet (P10) was already shown by itself for this purchase key. */
    fun takeoverShown(context: Context, coupleId: String): String? = prefs(context).getString("takeover.$coupleId", null)

    fun markTakeoverShown(context: Context, coupleId: String, key: String) = prefs(context).edit { putString("takeover.$coupleId", key) }

    /** Debug builds only: shifts Plus time forward to test the trial end on an emulator (see PlusClock). */
    fun debugOffset(context: Context): Long = if (BuildConfig.DEBUG) prefs(context).getLong("debug.offset", 0L) else 0L

    fun setDebugOffset(context: Context, millis: Long) {
        if (BuildConfig.DEBUG) prefs(context).edit { putLong("debug.offset", millis) }
    }
}

/**
 * The time Plus rules are evaluated at. Real time in release builds; debug builds can move it forward from the
 * Plus management screen to test trial days, the paywall and the offer countdown without waiting two weeks.
 */
object PlusClock {
    fun now(context: Context): Long = System.currentTimeMillis() + PlusPrefs.debugOffset(context)
}
