package com.lovelyy.app.ui.plus

import android.app.Activity
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lovelyy.app.BuildConfig
import com.lovelyy.app.LovelyyApplication
import com.lovelyy.app.R
import com.lovelyy.app.analytics.AnalyticsEvent
import com.lovelyy.app.billing.PlusCatalog
import com.lovelyy.app.billing.PurchaseEvent
import com.lovelyy.app.billing.StorePurchase
import com.lovelyy.app.data.PlusClock
import com.lovelyy.app.data.PlusInfo
import com.lovelyy.app.data.PlusPlan
import com.lovelyy.app.data.PlusRules
import com.lovelyy.app.data.PlusSource
import com.lovelyy.app.data.StoreSubscription
import com.lovelyy.app.telemetry.Telemetry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

/** One-time results for the Plus screens. */
sealed interface PlusEffect {
    data class Message(@StringRes val text: Int) : PlusEffect
    /** This phone bought [plan]: the paywall / gift screen closes. */
    data class Purchased(val plan: PlusPlan) : PlusEffect
}

/** The analytics key of a plan ("yearly", "monthly", "gift"); fixed values, never personal. */
val PlusPlan.analyticsKey: String get() = name.lowercase()

/**
 * Lovelyy Plus actions for one couple: start the trial, buy (subscription or gift), restore, keep the buyer's
 * subscription state in sync, and the debug-only fake purchase. One keyed instance ("plus-<coupleId>") is shared
 * by Home and every Plus screen. The current plus map is pushed in by Home ([update]) from the observed couple.
 */
class PlusViewModel(
    private val app: LovelyyApplication,
    private val coupleId: String,
    private val me: String,
) : ViewModel() {

    private val billing = app.billingRepository
    val catalog: StateFlow<PlusCatalog> = billing.catalog

    private val _busy = MutableStateFlow(false)
    /** A purchase or restore is running (buttons show a spinner). */
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _effects = Channel<PlusEffect>(Channel.BUFFERED)
    val effects: Flow<PlusEffect> = _effects.receiveAsFlow()

    private val _clockTick = MutableStateFlow(0)
    /** Bumped when debug time moves, so Home re-reads PlusClock at once. */
    val clockTick: StateFlow<Int> = _clockTick.asStateFlow()

    private var current: PlusInfo? = null
    private var pendingPlan: PlusPlan? = null
    private var pendingNote: String? = null
    private var synced = false

    init {
        viewModelScope.launch { billing.loadCatalog() }
        viewModelScope.launch { billing.events.collect { onEvent(it) } }
    }

    /** The couple's plus map as Home last saw it. */
    fun update(plus: PlusInfo?) {
        current = plus
    }

    /**
     * Debug builds fake the purchase (no Google Play window) when forced with -Pplus.fakeBilling=true or when the
     * products can't be loaded (emulator, no Play listing yet). Release builds never do.
     */
    val fakeBilling: Boolean get() = BuildConfig.DEBUG && (BuildConfig.PLUS_FAKE_BILLING || !catalog.value.available)

    /** Starts the 14-day trial if the couple has none (P1 "Başlayalım", or Home for couples paired before Plus). */
    fun ensureTrial() {
        viewModelScope.launch {
            try {
                if (app.plusRepository.ensureTrial(coupleId, System.currentTimeMillis())) Telemetry.track(AnalyticsEvent.TrialStarted)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Starting the Plus trial failed")
            }
        }
    }

    fun paywallShown(source: String) = Telemetry.track(AnalyticsEvent.PaywallShown(source))

    fun takeoverShown() = Telemetry.track(AnalyticsEvent.TakeoverShown)

    /** Opens Google Play's purchase window for [plan] ([note] only for a gift). */
    fun buy(activity: Activity?, plan: PlusPlan, note: String? = null) {
        if (_busy.value) return
        val now = PlusClock.now(app)
        if (!PlusRules.canPurchase(current, now)) {
            failed("blocked", R.string.plus_err_blocked)
            return
        }
        Telemetry.track(AnalyticsEvent.PurchaseStarted(plan.analyticsKey))
        pendingPlan = plan
        pendingNote = note
        _busy.value = true
        viewModelScope.launch {
            if (fakeBilling) {
                delay(700)
                record(StorePurchase(plan, "debug-${UUID.randomUUID()}", System.currentTimeMillis(), plan.isSubscription, acknowledged = true), restore = false)
                return@launch
            }
            val offer = plan == PlusPlan.Yearly && PlusRules.firstYearOfferActive(current, now)
            val opened = activity != null && billing.launch(activity, plan, offer)
            if (!opened) {
                _busy.value = false
                pendingPlan = null
                failed("not_loaded", R.string.plus_err_unavailable)
            }
        }
    }

    private suspend fun onEvent(e: PurchaseEvent) {
        when (e) {
            is PurchaseEvent.Purchased -> record(e.purchase, restore = false)
            PurchaseEvent.Pending -> {
                _busy.value = false
                Telemetry.track(AnalyticsEvent.PurchaseFailed("pending"))
                _effects.send(PlusEffect.Message(R.string.plus_pending))
            }
            PurchaseEvent.Cancelled -> {
                _busy.value = false
                pendingPlan = null
                Telemetry.track(AnalyticsEvent.PurchaseFailed("cancelled"))
            }
            is PurchaseEvent.Failed -> {
                _busy.value = false
                pendingPlan = null
                failed("play_${e.code}", R.string.plus_err_failed)
            }
        }
    }

    /**
     * Writes the purchase into the couple's plus map, then acknowledges / consumes it in Google Play. In that
     * order on purpose: if the write fails, Google refunds an unacknowledged purchase instead of the couple
     * paying for nothing, and the next restore or sync can still record it.
     */
    private suspend fun record(p: StorePurchase, restore: Boolean): Boolean {
        val note = if (pendingPlan == p.plan) pendingNote else null
        return try {
            val next = PlusRules.afterPurchase(current, p.plan, me, p.purchaseTime, p.token, p.autoRenew, note, PlusClock.now(app))
            if (next != current) app.plusRepository.write(coupleId, next)
            current = next
            if (!p.token.startsWith("debug-")) billing.finish(p)
            if (!restore) {
                Telemetry.track(AnalyticsEvent.PurchaseCompleted(p.plan.analyticsKey))
                if (p.plan == PlusPlan.Gift) Telemetry.track(AnalyticsEvent.GiftSent(hasNote = PlusRules.cleanNote(note) != null))
                _effects.send(PlusEffect.Purchased(p.plan))
            }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Recording the Plus purchase failed")
            if (!restore) failed("record_failed", R.string.plus_err_record)
            false
        } finally {
            if (!restore) {
                _busy.value = false
                pendingPlan = null
                pendingNote = null
            }
        }
    }

    /** "Satın alımları geri yükle": records whatever this Google account owns for the couple. */
    fun restore() {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                val owned = billing.owned()
                if (owned == null) {
                    Telemetry.track(AnalyticsEvent.RestoreTapped(found = false))
                    _effects.send(PlusEffect.Message(R.string.plus_err_unavailable))
                    return@launch
                }
                val found = apply(owned)
                Telemetry.track(AnalyticsEvent.RestoreTapped(found))
                _effects.send(PlusEffect.Message(if (found) R.string.plus_restored else R.string.plus_restore_none))
            } finally {
                _busy.value = false
            }
        }
    }

    /** Once per app run on Home: keeps the buyer's renewal state current and records purchases that were never finished. */
    fun syncOnce() {
        if (synced || current == null) return
        synced = true
        viewModelScope.launch { billing.owned()?.let { apply(it) } }
    }

    /** True when something owned was recorded. */
    private suspend fun apply(owned: List<StorePurchase>): Boolean {
        var found = false
        val sub = owned.firstOrNull { it.plan.isSubscription }
        try {
            val now = PlusClock.now(app)
            val next = PlusRules.sync(current, me, sub?.let { StoreSubscription(it.plan, it.purchaseTime, it.token, it.autoRenew) }, now)
            if (next != null) {
                app.plusRepository.write(coupleId, next)
                current = next
            }
            if (sub != null && current?.orderToken == sub.token) {
                found = true
                billing.finish(sub)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Syncing the Plus subscription failed")
        }
        // A gift is consumed once recorded, so one still owned was never written.
        owned.filter { it.plan == PlusPlan.Gift }.forEach { if (record(it, restore = true)) found = true }
        return found
    }

    private fun failed(reason: String, @StringRes text: Int) {
        Telemetry.track(AnalyticsEvent.PurchaseFailed(reason))
        _effects.trySend(PlusEffect.Message(text))
    }

    // ---------- debug only (P8 shows these buttons in debug builds) ----------

    /** Moves Plus time [days] forward (0 = back to real time) to test trial days and the paywall. */
    fun debugShiftDays(days: Int) {
        if (!BuildConfig.DEBUG) return
        val now = com.lovelyy.app.data.PlusPrefs.debugOffset(app)
        com.lovelyy.app.data.PlusPrefs.setDebugOffset(app, if (days == 0) 0L else now + days * PlusRules.DAY_MS)
        _clockTick.value++
    }

    /** Turns off auto-renew of my (fake) subscription, so the takeover and second-purchase rules can be tried. */
    fun debugStopRenewing() = debugWrite { it.copy(autoRenew = false) }

    /** Drops what was bought (the trial start stays, the rules never let it move). */
    fun debugResetPurchase() = debugWrite {
        PlusInfo(trialStart = it.trialStart, buyerUid = me, source = PlusSource.Trial)
    }

    private fun debugWrite(change: (PlusInfo) -> PlusInfo) {
        if (!BuildConfig.DEBUG) return
        val old = current ?: return
        viewModelScope.launch {
            runCatching { app.plusRepository.write(coupleId, change(old).copy(buyerUid = me)) }
                .onFailure { Timber.w(it, "Debug Plus write failed") }
        }
    }
}
