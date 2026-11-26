package com.lovelyy.app.data

import android.content.Context
import androidx.core.content.edit

/**
 * What this phone remembers about onboarding. Local on purpose: it is about this device's permission
 * windows and first-run moments, not about the couple.
 */
object OnboardingPrefs {
    private const val PREFS = "lovelyy_onboarding"
    private const val KEY_PAIRING_PREFIX = "pairing_started_"
    private const val KEY_NOTIFICATIONS_ASKED = "notifications_asked"
    private const val KEY_STREAK_EXPLAINER_PREFIX = "streak_explainer_seen_"

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** This user saw the pairing screen on this phone, so the "you are paired" flow is still owed to them. */
    fun markPairingStarted(context: Context, uid: String) {
        if (!pairedFlowPending(context, uid)) prefs(context).edit { putBoolean(KEY_PAIRING_PREFIX + uid, true) }
    }

    /** True from the pairing screen until the post-pairing flow is finished. Never true for people paired before. */
    fun pairedFlowPending(context: Context, uid: String): Boolean = prefs(context).getBoolean(KEY_PAIRING_PREFIX + uid, false)

    fun finishPairedFlow(context: Context, uid: String) = prefs(context).edit { remove(KEY_PAIRING_PREFIX + uid) }

    /** The notification question was already put to the user (primer or system window), so Home must not ask again. */
    fun notificationsAsked(context: Context): Boolean = prefs(context).getBoolean(KEY_NOTIFICATIONS_ASKED, false)

    fun markNotificationsAsked(context: Context) = prefs(context).edit { putBoolean(KEY_NOTIFICATIONS_ASKED, true) }

    /**
     * "Seri nedir?" (board S1) was already shown for this couple on this phone, by itself or from the streak chip
     * or card, so it must not open by itself again. Per couple: a new pairing starts a new streak.
     */
    fun streakExplainerSeen(context: Context, coupleId: String): Boolean =
        prefs(context).getBoolean(KEY_STREAK_EXPLAINER_PREFIX + coupleId, false)

    fun markStreakExplainerSeen(context: Context, coupleId: String) =
        prefs(context).edit { putBoolean(KEY_STREAK_EXPLAINER_PREFIX + coupleId, true) }
}
