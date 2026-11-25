package com.lovelyy.app.analytics

/** Sends [AnalyticsEvent]s somewhere. Swap the implementation in tests or for another backend. */
interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)

    fun screen(screenName: String) = track(AnalyticsEvent.ScreenView(screenName))

    /** Used when Firebase isn't configured (no google-services.json) and in unit tests. */
    object NoOp : AnalyticsTracker {
        override fun track(event: AnalyticsEvent) = Unit
    }
}
