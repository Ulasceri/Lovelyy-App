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
import com.lovelyy.app.MainActivity
import com.lovelyy.app.R
import com.lovelyy.app.data.CatDoc
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
        )
    }

    fun coupleId(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("coupleId", null)

    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, CatWidgetProvider::class.java))
        if (ids.isEmpty()) return
        val views = render(context)
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    private fun render(context: Context): RemoteViews {
        val doc = load(context)
        val views = RemoteViews(context.packageName, R.layout.widget_cat)
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        views.setOnClickPendingIntent(R.id.widget_root, open)
        if (doc == null) {
            views.setImageViewResource(R.id.widget_cat, R.drawable.cat_w_idle)
            views.setTextViewText(R.id.widget_name, context.getString(R.string.app_name))
            views.setTextViewText(R.id.widget_status, context.getString(R.string.widget_open_app))
            return views
        }
        val mood = CatRules.levels(doc).mood
        views.setImageViewResource(R.id.widget_cat, mood.picture())
        views.setTextViewText(R.id.widget_name, doc.name?.takeIf { it.isNotBlank() } ?: context.getString(R.string.cat_generic))
        views.setTextViewText(R.id.widget_status, context.getString(mood.text()))
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

    private fun CatMood.text() = when (this) {
        CatMood.Sleeping -> R.string.mood_sleeping
        CatMood.Hungry -> R.string.mood_hungry
        CatMood.Thirsty -> R.string.mood_thirsty
        CatMood.Sleepy -> R.string.mood_sleepy
        CatMood.WantsLove -> R.string.mood_love
        CatMood.Happy -> R.string.mood_happy
        CatMood.Idle -> R.string.mood_idle
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
        CatWidget.cancel(context)
    }
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
            Result.success()
        } catch (e: Exception) {
            Timber.w(e, "Widget refresh failed, will retry")
            CatWidget.updateAll(app)
            Result.retry()
        }
    }
}
