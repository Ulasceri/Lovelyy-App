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

    /** An accessory was put on the cat ([accessory] is the fixed accessory id, e.g. "Papyon") or taken off (null). */
    data class AccessoryWorn(val accessory: String?) : AnalyticsEvent {
        override val name = "accessory_worn"
        override val params: Map<String, Any> get() = mapOf("accessory" to (accessory ?: "none"))
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

    /**
     * A screen was shown. [screenName] is a fixed route name like "home", never anything dynamic.
     * Sent as the standard `screen_view` event, so it shows up in the Screens report.
     */
    data class ScreenView(val screenName: String) : AnalyticsEvent {
        override val name = "screen_view"
        override val params: Map<String, Any> get() = mapOf("screen_name" to screenName, "screen_class" to screenName)
    }
}
