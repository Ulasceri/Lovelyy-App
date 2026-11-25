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

    /**
     * A screen was shown. [screenName] is a fixed route name like "home", never anything dynamic.
     * Sent as the standard `screen_view` event, so it shows up in the Screens report.
     */
    data class ScreenView(val screenName: String) : AnalyticsEvent {
        override val name = "screen_view"
        override val params: Map<String, Any> get() = mapOf("screen_name" to screenName, "screen_class" to screenName)
    }
}
