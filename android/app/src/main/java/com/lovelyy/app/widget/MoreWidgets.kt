package com.lovelyy.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.lovelyy.app.DeepLink
import com.lovelyy.app.LovelyyApplication
import com.lovelyy.app.R
import com.lovelyy.app.analytics.AnalyticsEvent
import com.lovelyy.app.data.DayCounter
import com.lovelyy.app.data.Emotion
import com.lovelyy.app.data.SignalKind
import com.lovelyy.app.data.Stroke
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.telemetry.Telemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/** Who the couple is, saved for the widgets (they can't read Firestore while the app is closed). */
object WidgetData {
    private const val PREFS = "widget_couple"

    data class CoupleInfo(
        val coupleId: String,
        val myUid: String,
        val myName: String,
        val partnerUid: String?,
        val partnerName: String,
        val startMillis: Long?,
    ) {
        fun days(now: Long = System.currentTimeMillis()): Int = startMillis?.let { DayCounter.together(it, now).days.toInt() } ?: 0
    }

    fun saveCouple(context: Context, info: CoupleInfo) {
        if (couple(context) == info) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString("coupleId", info.coupleId)
            putString("myUid", info.myUid)
            putString("myName", info.myName)
            putString("partnerUid", info.partnerUid)
            putString("partnerName", info.partnerName)
            putLong("start", info.startMillis ?: 0L)
        }
        DaysWidget.updateAll(context)
        CatWidget.updateAll(context)
    }

    fun couple(context: Context): CoupleInfo? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = p.getString("coupleId", null) ?: return null
        return CoupleInfo(
            id, p.getString("myUid", "").orEmpty(), p.getString("myName", "").orEmpty(),
            p.getString("partnerUid", null), p.getString("partnerName", "").orEmpty(),
            p.getLong("start", 0L).takeIf { it > 0 },
        )
    }

    fun clear(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { clear() }

    fun anyWidgets(context: Context): Boolean {
        val m = AppWidgetManager.getInstance(context)
        return listOf(CatWidgetProvider::class.java, CatWideWidgetProvider::class.java, DaysWidgetProvider::class.java, DrawingWidgetProvider::class.java)
            .any { m.getAppWidgetIds(ComponentName(context, it)).isNotEmpty() }
    }

    /** How many Lovelyy widgets are on the home screen (Biz → Ana ekran widget'ları). */
    fun count(context: Context): Int {
        val m = AppWidgetManager.getInstance(context)
        return listOf(CatWidgetProvider::class.java, CatWideWidgetProvider::class.java, DaysWidgetProvider::class.java, DrawingWidgetProvider::class.java)
            .sumOf { m.getAppWidgetIds(ComponentName(context, it)).size }
    }
}

/** "Gün sayacı + tek dokunuşla kalp": days together and a heart that sends a heartbeat. */
object DaysWidget {
    const val ACTION_HEART = "com.lovelyy.app.SEND_HEART"

    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, DaysWidgetProvider::class.java))
        if (ids.isEmpty()) return
        val views = render(context)
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    private fun render(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_days)
        val c = WidgetData.couple(context)
        views.setOnClickPendingIntent(R.id.days_root, CatWidget.openApp(context, DeepLink.COUNTER))
        views.setTextViewText(R.id.days_me, c?.myName?.trim()?.firstOrNull()?.uppercase() ?: "")
        views.setTextViewText(R.id.days_partner, c?.partnerName?.trim()?.firstOrNull()?.uppercase() ?: "")
        views.setTextViewText(
            R.id.days_count,
            if (c?.startMillis != null) context.resources.getQuantityString(R.plurals.widget_days, (c.days()).toInt(), c.days()) else context.getString(R.string.widget_open_app),
        )
        val heart = PendingIntent.getBroadcast(
            context, 1, Intent(context, HeartReceiver::class.java).setAction(ACTION_HEART),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        views.setOnClickPendingIntent(R.id.days_heart, heart)
        return views
    }
}

class DaysWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        DaysWidget.updateAll(context)
        CatWidget.schedule(context)
    }
}

/** The widget's heart: sends a "Sevgi" heartbeat to the partner without opening the app. */
class HeartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DaysWidget.ACTION_HEART) return
        val app = context.applicationContext as LovelyyApplication
        val c = WidgetData.couple(app)
        val user = if (app.isFirebaseConfigured) FirebaseAuth.getInstance().currentUser else null
        if (c == null || c.partnerUid == null || user == null || user.uid != c.myUid) {
            Toast.makeText(app, R.string.widget_heart_failed, Toast.LENGTH_SHORT).show()
            return
        }
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val result = runCatching {
                val me = UserProfile(uid = c.myUid, displayName = c.myName, coupleId = c.coupleId)
                app.signalRepository.send(c.coupleId, me, c.partnerUid, SignalKind.Heartbeat, Emotion.Sevgi)
            }.onFailure { Timber.w(it, "Widget heartbeat failed") }
            val ok = result.isSuccess
            if (ok) Telemetry.track(AnalyticsEvent.HeartbeatSent(Emotion.Sevgi.key))
            val message = when {
                ok -> R.string.widget_heart_sent
                result.exceptionOrNull() is com.lovelyy.app.data.SignalLimitException -> R.string.signal_limit
                else -> R.string.widget_heart_failed
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(app, message, Toast.LENGTH_SHORT).show()
            }
            pending.finish()
        }
    }
}

/** "Canlı çizim" widget: the shared canvas as a picture, and who is drawing right now. */
object DrawingWidget {
    private const val PREFS = "drawing_widget"
    const val FILE = "drawing_widget.png"
    private const val SIZE = 420

    fun isEnabled(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("enabled", true)

    fun setEnabled(context: Context, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean("enabled", on) }
        updateAll(context)
    }

    fun hasWidgets(context: Context) =
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, DrawingWidgetProvider::class.java)).isNotEmpty()

    /** Renders the canvas to a small picture (off the main thread) and refreshes the widgets. */
    suspend fun save(context: Context, strokes: List<Stroke>, liveName: String?) {
        if (!hasWidgets(context)) return
        withContext(Dispatchers.Default) {
            val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(android.graphics.Color.WHITE)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            strokes.forEach { s ->
                if (s.points.size < 2) return@forEach
                paint.color = s.color.toInt()
                paint.strokeWidth = s.width * SIZE
                val path = Path().apply {
                    moveTo(s.points[0] * SIZE, s.points[1] * SIZE)
                    var i = 2
                    while (i + 1 < s.points.size) { lineTo(s.points[i] * SIZE, s.points[i + 1] * SIZE); i += 2 }
                }
                canvas.drawPath(path, paint)
            }
            runCatching { File(context.filesDir, FILE).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
                .onFailure { Timber.w(it, "Saving the drawing widget picture failed") }
            bmp.recycle()
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putBoolean("empty", strokes.isEmpty())
            putString("live", liveName)
        }
        updateAll(context)
    }

    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, DrawingWidgetProvider::class.java))
        if (ids.isEmpty()) return
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val views = RemoteViews(context.packageName, R.layout.widget_drawing)
        views.setOnClickPendingIntent(R.id.drawing_root, CatWidget.openApp(context, DeepLink.DRAWING))
        val file = File(context.filesDir, FILE)
        val show = isEnabled(context) && !p.getBoolean("empty", true) && file.exists()
        if (show) {
            BitmapFactory.decodeFile(file.path)?.let { views.setImageViewBitmap(R.id.drawing_image, it) }
        }
        views.setViewVisibility(R.id.drawing_image, if (show) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.drawing_empty, if (show) View.GONE else View.VISIBLE)
        val live = p.getString("live", null)
        views.setViewVisibility(R.id.drawing_live, if (show && live != null) View.VISIBLE else View.GONE)
        if (live != null) views.setTextViewText(R.id.drawing_live_text, context.getString(R.string.lv_partner_now, live))
        ids.forEach { manager.updateAppWidget(it, views) }
    }
}

class DrawingWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        DrawingWidget.updateAll(context)
        CatWidget.schedule(context)
    }
}
