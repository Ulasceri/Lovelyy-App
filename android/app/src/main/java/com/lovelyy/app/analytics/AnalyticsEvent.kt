package com.lovelyy.app.analytics

/**
 * Every analytics event the app sends. One place, typed, so names stay consistent.
 *
 * Rules (see docs/analytics.md):
 * - name: snake_case, object_action in past tense ("cat_fed"), at most 40 characters.
 * - params: snake_case keys, values String / Long / Int / Double / Boolean.
 * - NEVER personal data: no names, emails, uids, couple ids, pairing codes or free text.
 */
sealed interface AnalyticsEvent {
    val name: String
    val params: Map<String, Any> get() = emptyMap()

    /** Two people are now a couple (logged on both phones, once each). */
    data object PairingCompleted : AnalyticsEvent {
        override val name = "pairing_completed"
    }

    data object CatFed : AnalyticsEvent {
        override val name = "cat_fed"
    }

    data object CatWatered : AnalyticsEvent {
        override val name = "cat_watered"
    }

    data object CatPlayed : AnalyticsEvent {
        override val name = "cat_played"
    }

    /** [strokes]: how many strokes the petting gesture had (a count, nothing personal). */
    data class CatPetted(val strokes: Int) : AnalyticsEvent {
        override val name = "cat_petted"
        override val params: Map<String, Any> get() = mapOf("strokes" to strokes.toLong())
    }

    data object CatSlept : AnalyticsEvent {
        override val name = "cat_slept"
    }

    /** The couple's first day was set or changed on the day counter. */
    data object StartDateSet : AnalyticsEvent {
        override val name = "start_date_set"
    }

    /** "Bu anı paylaş" on the day counter opened the share sheet. */
    data object CounterShared : AnalyticsEvent {
        override val name = "counter_shared"
    }

    /** A heartbeat was sent. [emotion] is a fixed key like "ozlem", never free text. */
    data class HeartbeatSent(val emotion: String) : AnalyticsEvent {
        override val name = "heartbeat_sent"
        override val params: Map<String, Any> get() = mapOf("emotion" to emotion)
    }

    data object NudgeSent : AnalyticsEvent {
        override val name = "nudge_sent"
    }

    /** A mood was shared. [mood] is a fixed key like "mutlu"; [hasNote] only says whether a note was added. */
    data class MoodShared(val mood: String, val hasNote: Boolean) : AnalyticsEvent {
        override val name = "mood_shared"
        override val params: Map<String, Any> get() = mapOf("mood" to mood, "has_note" to hasNote)
    }

    data object QuestionAnswered : AnalyticsEvent {
        override val name = "question_answered"
    }

    data object NoteAdded : AnalyticsEvent {
        override val name = "note_added"
    }

    /** A calendar entry was added. [yearly] = a special day that repeats every year. */
    data class PlanAdded(val yearly: Boolean) : AnalyticsEvent {
        override val name = "plan_added"
        override val params: Map<String, Any> get() = mapOf("yearly" to yearly)
    }

    /** Location sharing on the Mesafe screen was turned on or off. No coordinates or city, ever. */
    data class LocationSharingChanged(val sharing: Boolean) : AnalyticsEvent {
        override val name = "location_sharing_changed"
        override val params: Map<String, Any> get() = mapOf("sharing" to sharing)
    }

    /** The user's rough location was refreshed. [cityOnly] = rounded to ~10 km. */
    data class LocationUpdated(val cityOnly: Boolean) : AnalyticsEvent {
        override val name = "location_updated"
        override val params: Map<String, Any> get() = mapOf("city_only" to cityOnly)
    }

    /** The user changed their own display name on the profile. */
    data object ProfileNameChanged : AnalyticsEvent {
        override val name = "profile_name_changed"
    }

    /** A dream was added to the bucket list. [category] is a fixed key (seyahat, deneyim, hayat, diger). */
    data class DreamAdded(val category: String) : AnalyticsEvent {
        override val name = "dream_added"
        override val params: Map<String, Any> get() = mapOf("category" to category)
    }

    /** A dream was marked as came true. */
    data object DreamCompleted : AnalyticsEvent {
        override val name = "dream_completed"
    }

    /** The decision wheel was spun. [builtIn] = one of the three default wheels (custom labels are never sent). */
    data class WheelSpun(val builtIn: Boolean, val items: Int) : AnalyticsEvent {
        override val name = "wheel_spun"
        override val params: Map<String, Any> get() = mapOf("built_in" to builtIn, "items" to items)
    }

    /** A custom wheel was created. */
    data object WheelCreated : AnalyticsEvent {
        override val name = "wheel_created"
    }

    /** A time-capsule letter was sealed, to open in [days] days. */
    data class CapsuleSealed(val days: Int) : AnalyticsEvent {
        override val name = "capsule_sealed"
        override val params: Map<String, Any> get() = mapOf("days" to days)
    }

    /** The shared drawing was saved to Anılar. */
    data class DrawingSaved(val strokes: Int) : AnalyticsEvent {
        override val name = "drawing_saved"
        override val params: Map<String, Any> get() = mapOf("strokes" to strokes)
    }

    /** A saved drawing was deleted from Çizimlerimiz (for both partners). */
    data object DrawingDeleted : AnalyticsEvent {
        override val name = "drawing_deleted"
    }

    /** Bildirimler: "mark all read" was tapped. */
    data object InboxReadAll : AnalyticsEvent {
        override val name = "inbox_read_all"
    }

    /** This user removed the pairing; the shared data was deleted for both. */
    data object CoupleUnpaired : AnalyticsEvent {
        override val name = "couple_unpaired"
    }

    /** This user deleted their account. */
    data object AccountDeleted : AnalyticsEvent {
        override val name = "account_deleted"
    }

    /** A password reset mail was sent from Hesap. */
    data object PasswordResetSent : AnalyticsEvent {
        override val name = "password_reset_sent"
    }

    /** The try-it intro after Karşılama ended. [skipped] = left through "Atla"; [demosTried] = 0..3 demos tapped. */
    data class IntroFinished(val skipped: Boolean, val demosTried: Int) : AnalyticsEvent {
        override val name = "intro_finished"
        override val params: Map<String, Any> get() = mapOf("skipped" to skipped, "demos_tried" to demosTried)
    }

    /**
     * A permission primer after pairing was answered. [permission] is "notifications" or "location";
     * [allowed] is false for "Şimdi değil" and for a refusal in the system window.
     */
    data class PermissionPrimerAnswered(val permission: String, val allowed: Boolean) : AnalyticsEvent {
        override val name = "permission_primer_answered"
        override val params: Map<String, Any> get() = mapOf("permission" to permission, "allowed" to allowed)
    }

    /** The couple's 14-day Plus trial was started (logged once, by the phone that wrote it). */
    data object TrialStarted : AnalyticsEvent {
        override val name = "trial_started"
    }

    /**
     * A Plus plan screen opened. [source] is a fixed key: "trial_end" (automatic), "lock_<feature>" (P4, the
     * streak protection "lock_streak_day_off" / "lock_streak_repair", or a free limit "lock_notes", "lock_events",
     * "lock_dreams", "lock_capsules", "lock_wheels", "lock_drawings"), "biz_card", "home_card", "management", "takeover".
     */
    data class PaywallShown(val source: String) : AnalyticsEvent {
        override val name = "paywall_shown"
        override val params: Map<String, Any> get() = mapOf("source" to source)
    }

    /** Google Play's purchase window was opened. [product] is "yearly", "monthly" or "gift". */
    data class PurchaseStarted(val product: String) : AnalyticsEvent {
        override val name = "purchase_started"
        override val params: Map<String, Any> get() = mapOf("product" to product)
    }

    /** A Plus purchase was recorded for the couple. [product] is "yearly", "monthly" or "gift". */
    data class PurchaseCompleted(val product: String) : AnalyticsEvent {
        override val name = "purchase_completed"
        override val params: Map<String, Any> get() = mapOf("product" to product)
    }

    /**
     * A Plus purchase did not complete. [reason] is a fixed code: "cancelled", "pending", "not_loaded",
     * "blocked", "record_failed" or "play_<response code>".
     */
    data class PurchaseFailed(val reason: String) : AnalyticsEvent {
        override val name = "purchase_failed"
        override val params: Map<String, Any> get() = mapOf("reason" to reason)
    }

    /** "Satın alımları geri yükle" was tapped. [found] = an owned purchase was found and recorded. */
    data class RestoreTapped(val found: Boolean) : AnalyticsEvent {
        override val name = "restore_tapped"
        override val params: Map<String, Any> get() = mapOf("found" to found)
    }

    /** A Plus gift was bought. [hasNote] only says whether a note was added (never the note). */
    data class GiftSent(val hasNote: Boolean) : AnalyticsEvent {
        override val name = "gift_sent"
        override val params: Map<String, Any> get() = mapOf("has_note" to hasNote)
    }

    /** The "Plus bitiyor, sen devam ettir" sheet (P10) was shown to the partner who did not buy. */
    data object TakeoverShown : AnalyticsEvent {
        override val name = "takeover_shown"
    }

    /**
     * The "Seri nedir?" sheet was shown. [source] is a fixed key: "auto" (by itself, the first time the couple's
     * streak reached 1), "chip" (the Yuva streak chip) or "card" (the Kedi bakımı streak card).
     */
    data class StreakExplainerShown(val source: String) : AnalyticsEvent {
        override val name = "streak_explainer_shown"
        override val params: Map<String, Any> get() = mapOf("source" to source)
    }

    /** The "Seri tehlikede" card (P5a) was shown, at most once a day per phone. [hasPlus]: Plus right now. */
    data class StreakDangerShown(val hasPlus: Boolean) : AnalyticsEvent {
        override val name = "streak_danger_shown"
        override val params: Map<String, Any> get() = mapOf("has_plus" to hasPlus)
    }

    /** A Plus day off (P5b, "izin günü") was used. [left]: days off left this month (0 or 1). */
    data class StreakDayOffUsed(val left: Int) : AnalyticsEvent {
        override val name = "streak_day_off_used"
        override val params: Map<String, Any> get() = mapOf("left" to left.toLong())
    }

    /** The "Seriniz bitti" sheet (P5c) opened by itself. [hasPlus]: it offered the make-up, else Plus. */
    data class StreakRepairShown(val hasPlus: Boolean) : AnalyticsEvent {
        override val name = "streak_repair_shown"
        override val params: Map<String, Any> get() = mapOf("has_plus" to hasPlus)
    }

    /** A missed day was made up with Plus (P5c, "telafi"). [days]: the streak length after it. */
    data class StreakMadeUp(val days: Int) : AnalyticsEvent {
        override val name = "streak_made_up"
        override val params: Map<String, Any> get() = mapOf("days" to days.toLong())
    }

    /** "Seriye 1. günden başla" on the P5c sheet. */
    data object StreakRepairDeclined : AnalyticsEvent {
        override val name = "streak_repair_declined"
    }

    /**
     * A screen was shown. [screenName] is a fixed route name like "home", never anything dynamic.
     * Sent as the standard `screen_view` event, so it shows up in the Screens report.
     */
    data class ScreenView(val screenName: String) : AnalyticsEvent {
        override val name = "screen_view"
        override val params: Map<String, Any> get() = mapOf("screen_name" to screenName, "screen_class" to screenName)
    }
}
