package com.lovelyy.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lovelyy.app.signals.SignalCenter

/**
 * Widgets are RemoteViews built from string resources, so they keep the old language until redrawn.
 * When the system or per-app language changes, redraw all of them and refresh the notification channel
 * name and description, which Android stores as plain text.
 */
class LocaleChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_LOCALE_CHANGED) return
        CatWidget.updateAll(context)
        DaysWidget.updateAll(context)
        DrawingWidget.updateAll(context)
        SignalCenter.ensureChannel(context)
    }
}
