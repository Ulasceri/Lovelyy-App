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
        is AppState.JustPaired -> "paired_onboarding"
        is AppState.Home -> "home"
        is AppState.Failed -> "error"
    }

/** Inner screens opened on top of the home tabs (logged by HomeScreen when they open). */
object InnerScreens {
    /** The try-it intro between Karşılama and Giriş (part of the signed-out state). */
    const val INTRO = "onboarding_intro"
    const val DAY_COUNTER = "day_counter"
    const val HEARTBEAT = "heartbeat"
    const val MOOD = "mood"
    const val QUESTION = "daily_question"
    const val NOTES = "notes"
    const val PLANS = "plans"
    const val CAT_CARE = "cat_care"
    /** P7a, Mırmır's items (from Kedi bakımı). */
    const val CAT_ITEMS = "cat_items"
    const val PROFILE = "profile"
    const val DISTANCE = "distance"
    const val DREAMS = "dreams"
    const val WHEEL = "wheel"
    const val CAPSULE = "time_capsule"
    const val DRAWING = "live_drawing"
    const val MEMORIES = "memories"
    const val WIDGETS = "widgets"
    const val MOOD_HISTORY = "mood_history"
    const val QUESTION_HISTORY = "question_history"
    const val HEARTBEAT_HISTORY = "heartbeat_history"
    const val WHEEL_HISTORY = "wheel_history"
    const val SEARCH = "search"
    const val DRAWINGS = "saved_drawings"
    const val LETTERS = "opened_letters"
    const val INBOX = "inbox"
    const val ACCOUNT = "account"
    const val UNPAIR = "unpair"
    /** Lovelyy Plus. P1 is a step of the post-pairing flow; P4, P6b and P10 are overlays on Home. */
    const val PLUS_TRIAL_GIFT = "plus_trial_gift"
    const val PLUS_MANAGEMENT = "plus_management"
    const val PLUS_PAYWALL = "plus_paywall"
    const val PLUS_GIFT = "plus_gift"
    const val PLUS_LOCK = "plus_lock"
    const val PLUS_CELEBRATION = "plus_celebration"
    const val PLUS_TAKEOVER = "plus_takeover"
    /** Streak protection sheets (P5b day off, P5c make-up), overlays on Home and Kedi bakımı. */
    const val STREAK_DAY_OFF = "streak_day_off"
    const val STREAK_REPAIR = "streak_repair"
}
