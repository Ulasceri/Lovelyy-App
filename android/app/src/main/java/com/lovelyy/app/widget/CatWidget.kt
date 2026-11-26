package com.lovelyy.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.lovelyy.app.LovelyyApplication
import com.lovelyy.app.DeepLink
import com.lovelyy.app.R
import com.lovelyy.app.data.displayCatName
import com.lovelyy.app.data.CareStreak
import com.lovelyy.app.data.CatDoc
import com.lovelyy.app.ui.cat.bubbleRes
import com.lovelyy.app.data.CatMood
import com.lovelyy.app.data.CatRules
import com.lovelyy.app.data.Need
import java.util.concurrent.TimeUnit
import timber.log.Timber

/**
 * Home-screen widget: the cat's picture for its current mood, its name and how it feels.
 *
 * Needs decay with time, so the widget can compute the mood from the last known cat on its
 * own (no network): the app saves the cat here whenever it changes, and a background job
 * refreshes it from Firestore every 30 minutes so the partner's care shows up too.
 */
object CatWidget {
    private const val PREFS = "cat_widget"
    private const val WORK = "cat-widget-refresh"

    fun save(context: Context, coupleId: String, doc: CatDoc) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString("coupleId", coupleId)
            putString("name", doc.name)
            putNeed("hunger", doc.hunger)
            putNeed("thirst", doc.thirst)
            putNeed("energy", doc.energy)
            putNeed("love", doc.love)
            putLong("sleepingSince", doc.sleepingSince ?: 0L)
            putInt("streak.count", doc.streak.count)
            putString("streak.last", doc.streak.lastCompleteDay)
        }
        updateAll(context)
    }

    private fun android.content.SharedPreferences.Editor.putNeed(key: String, n: Need) {
        putFloat("$key.level", n.level.toFloat())
        putLong("$key.at", n.at)
    }

    private fun load(context: Context): CatDoc? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.contains("coupleId")) return null
        fun need(key: String) = Need(p.getFloat("$key.level", 80f).toDouble(), p.getLong("$key.at", System.currentTimeMillis()))
        return CatDoc(
            name = p.getString("name", null),
            hunger = need("hunger"),
            thirst = need("thirst"),
            energy = need("energy"),
            love = need("love"),
            sleepingSince = p.getLong("sleepingSince", 0L).takeIf { it > 0 },
            streak = CareStreak(count = p.getInt("streak.count", 0), lastCompleteDay = p.getString("streak.last", null)),
        )
    }

    fun coupleId(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("coupleId", null)

    /** Refreshes every cat widget (small and wide). */
    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, CatWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            val views = render(context)
            ids.forEach { manager.updateAppWidget(it, views) }
        }
        val wide = manager.getAppWidgetIds(ComponentName(context, CatWideWidgetProvider::class.java))
        if (wide.isNotEmpty()) {
            val views = renderWide(context)
            wide.forEach { manager.updateAppWidget(it, views) }
        }
    }

    /** "Kedi · geniş": picture, name + streak, the four needs and "Ulaş & Elif · 412. gün". */
    private fun renderWide(context: Context): RemoteViews {
        val doc = load(context)
        val views = RemoteViews(context.packageName, R.layout.widget_cat_wide)
        views.setOnClickPendingIntent(R.id.wide_root, openApp(context, DeepLink.CAT))
        val couple = WidgetData.couple(context)
        views.setTextViewText(
            R.id.wide_couple,
            if (couple != null && couple.startMillis != null) {
                context.getString(R.string.widget_couple_day, context.getString(R.string.home_us, couple.myName, couple.partnerName), couple.days())
            } else couple?.let { context.getString(R.string.home_us, it.myName, it.partnerName) } ?: context.getString(R.string.widget_open_app),
        )
        if (doc == null) {
            views.setImageViewResource(R.id.wide_cat, R.drawable.cat_w_idle)
            views.setTextViewText(R.id.wide_name, context.getString(R.string.app_name))
            views.setTextViewText(R.id.wide_streak, context.resources.getQuantityString(R.plurals.widget_streak, 0, 0))
            return views
        }
        val levels = CatRules.levels(doc)
        views.setImageViewResource(R.id.wide_cat, levels.mood.picture())
        views.setTextViewText(R.id.wide_name, displayCatName(doc.name))
        views.setTextViewText(R.id.wide_streak, context.resources.getQuantityString(R.plurals.widget_streak, (doc.streak.current(com.lovelyy.app.data.CoupleClock.today())).toInt(), doc.streak.current(com.lovelyy.app.data.CoupleClock.today())))
        fun bar(ok: Int, low: Int, value: Double) {
            val v = value.toInt().coerceIn(0, 100)
            val isLow = value < CatRules.LOW
            views.setViewVisibility(ok, if (isLow) android.view.View.GONE else android.view.View.VISIBLE)
            views.setViewVisibility(low, if (isLow) android.view.View.VISIBLE else android.view.View.GONE)
            views.setProgressBar(if (isLow) low else ok, 100, v, false)
        }
        bar(R.id.wide_hunger_ok, R.id.wide_hunger_low, levels.hunger)
        bar(R.id.wide_thirst_ok, R.id.wide_thirst_low, levels.thirst)
        bar(R.id.wide_energy_ok, R.id.wide_energy_low, levels.energy)
        bar(R.id.wide_love_ok, R.id.wide_love_low, levels.love)
        return views
    }

    internal fun openApp(context: Context, route: String? = null): PendingIntent = DeepLink.intent(context, route)

    private fun render(context: Context): RemoteViews {
        val doc = load(context)
        val views = RemoteViews(context.packageName, R.layout.widget_cat)
        views.setOnClickPendingIntent(R.id.widget_root, openApp(context, DeepLink.CAT))
        if (doc == null) {
            views.setImageViewResource(R.id.widget_cat, R.drawable.cat_w_idle)
            views.setTextViewText(R.id.widget_name, context.getString(R.string.app_name))
            views.setTextViewText(R.id.widget_status, context.getString(R.string.widget_open_app))
            return views
        }
        val mood = CatRules.levels(doc).mood
        views.setImageViewResource(R.id.widget_cat, mood.picture())
        views.setTextViewText(R.id.widget_name, displayCatName(doc.name))
        views.setTextViewText(R.id.widget_status, context.getString(mood.bubbleRes()))
        return views
    }

    private fun CatMood.picture() = when (this) {
        CatMood.Sleeping -> R.drawable.cat_w_sleeping
        CatMood.Hungry -> R.drawable.cat_w_hungry
        CatMood.Thirsty -> R.drawable.cat_w_thirsty
        CatMood.Sleepy -> R.drawable.cat_w_sleepy
        CatMood.WantsLove -> R.drawable.cat_w_love
        CatMood.Happy -> R.drawable.cat_w_happy
        CatMood.Idle -> R.drawable.cat_w_idle
    }

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<CatWidgetWorker>(30, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(WORK)
}

class CatWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        CatWidget.updateAll(context)
        CatWidget.schedule(context)
    }

    override fun onEnabled(context: Context) = CatWidget.schedule(context)

    override fun onDisabled(context: Context) {
        if (!WidgetData.anyWidgets(context)) CatWidget.cancel(context)
    }
}

/** "Kedi · geniş" widget. Rendered together with the small one by [CatWidget.updateAll]. */
class CatWideWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        CatWidget.updateAll(context)
        CatWidget.schedule(context)
    }

    override fun onEnabled(context: Context) = CatWidget.schedule(context)
}

/** Fetches the latest cat so the widget also shows what the partner did. */
class CatWidgetWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as LovelyyApplication
        if (!app.isFirebaseConfigured || FirebaseAuth.getInstance().currentUser == null) {
            CatWidget.updateAll(app)
            return Result.success()
        }
        val coupleId = CatWidget.coupleId(app) ?: return Result.success()
        return try {
            app.catRepository.get(coupleId)?.let { CatWidget.save(app, coupleId, it) } ?: CatWidget.updateAll(app)
            DaysWidget.updateAll(app)
            if (DrawingWidget.hasWidgets(app)) {
                runCatching { DrawingWidget.save(app, app.drawingRepository.current(coupleId), null) }
            }
            Result.success()
        } catch (e: Exception) {
            Timber.w(e, "Widget refresh failed, will retry")
            CatWidget.updateAll(app)
            Result.retry()
        }
    }
}
