package com.lovelyy.app.analytics

/** Debug builds only: an event to check the analytics pipeline without using any screen. */
data object DebugPing : AnalyticsEvent {
    override val name = "debug_ping"
}
