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
