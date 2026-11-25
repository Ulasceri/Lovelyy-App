package com.lovelyy.app.analytics

import com.lovelyy.app.ui.AppState

/**
 * Fixed route names for screen_view. The app navigates by [AppState] (there is no NavController),
 * so the screen name comes from the state's type. Null = don't log (the short loading spinner).
 */
val AppState.screenName: String?
    get() = when (this) {
        AppState.Loading -> null
        AppState.FirebaseMissing -> "firebase_setup"
        AppState.SignedOut -> "login"
        is AppState.NeedsName -> "name_setup"
        is AppState.Pairing -> "pairing"
        is AppState.Home -> "home"
        is AppState.Failed -> "error"
    }

/** Inner screens opened on top of the home tabs (logged by HomeScreen when they open). */
object InnerScreens {
    const val DAY_COUNTER = "day_counter"
    const val HEARTBEAT = "heartbeat"
    const val MOOD = "mood"
    const val QUESTION = "daily_question"
    const val NOTES = "notes"
    const val PLANS = "plans"
    const val CAT_CARE = "cat_care"
    const val PROFILE = "profile"
    const val DISTANCE = "distance"
    const val DREAMS = "dreams"
    const val WHEEL = "wheel"
    const val CAPSULE = "time_capsule"
    const val DRAWING = "live_drawing"
    const val MEMORIES = "memories"
    const val WIDGETS = "widgets"
}
