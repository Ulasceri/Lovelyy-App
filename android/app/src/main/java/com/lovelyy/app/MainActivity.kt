package com.lovelyy.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lovelyy.app.ui.LovelyyRoot
import com.lovelyy.app.ui.theme.LovelyyTheme

/** Screens a widget or a notification can open directly (the Intent extra [EXTRA]). */
object DeepLink {
    const val EXTRA = "com.lovelyy.app.ROUTE"
    const val COUNTER = "counter"
    const val CAT = "cat"
    const val DRAWING = "drawing"
    const val HEARTBEAT = "heartbeat"
    const val MOOD = "mood"
    const val QUESTION = "question"
    const val NOTES = "notes"
    const val WHEEL = "wheel"
    const val PLANS = "tab:plans"

    /** Opens the app on [route] (or just the app when null). */
    fun intent(context: Context, route: String?, requestCode: Int = route?.hashCode() ?: 0): PendingIntent = PendingIntent.getActivity(
        context, requestCode,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .apply { if (route != null) putExtra(EXTRA, route) },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

class MainActivity : ComponentActivity() {
    private fun readRoute(intent: Intent?) {
        val route = intent?.getStringExtra(DeepLink.EXTRA) ?: return
        (application as LovelyyApplication).pendingRoute.value = route
        intent.removeExtra(DeepLink.EXTRA)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readRoute(intent)
    }

    override fun onStart() {
        super.onStart()
        (application as LovelyyApplication).signalCenter.appVisible = true
    }

    override fun onStop() {
        (application as LovelyyApplication).signalCenter.appVisible = false
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) readRoute(intent)
        // Cream background in v1, so always dark system-bar icons (even if the phone is in dark mode).
        val bars = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        setContent {
            LovelyyTheme {
                LovelyyRoot()
            }
        }
    }
}
