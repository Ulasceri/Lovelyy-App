package com.lovelyy.app.data

import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.ceil
import kotlin.math.max

/** Where the couple's Plus comes from. The key is what Firestore stores (see firestore.rules `validPlus`). */
enum class PlusSource(val key: String) {
    Trial("trial"),
    Sub("sub"),
    Gift("gift");

    companion object {
        fun of(key: String?): PlusSource = entries.firstOrNull { it.key == key } ?: Trial
    }
}

/** Google Play product ids (Play Console must use exactly these, see docs/architecture.md "Lovelyy Plus"). */
object PlusProducts {
    /** The subscription, with base plans [BASE_YEARLY] and [BASE_MONTHLY]. */
    const val SUB = "lovelyy_plus"
    const val BASE_YEARLY = "yearly"
    const val BASE_MONTHLY = "monthly"
    /** Offer on the yearly base plan: first year cheaper, shown only in the 48 h after the trial ended. */
    const val OFFER_FIRST_YEAR = "first-year"
    /** One-time, non-renewing "1 year of Plus" framed as a gift. Consumed after it is recorded, so it can be bought again. */
    const val GIFT = "lovelyy_plus_gift_year"
}

/** What the user can buy. [key] is stored in the `plus.product` field. */
enum class PlusPlan(val key: String) {
    Yearly("${PlusProducts.SUB}:${PlusProducts.BASE_YEARLY}"),
    Monthly("${PlusProducts.SUB}:${PlusProducts.BASE_MONTHLY}"),
    Gift(PlusProducts.GIFT);

    val isSubscription: Boolean get() = this != Gift

    companion object {
        fun of(key: String?): PlusPlan? = entries.firstOrNull { it.key == key }
    }
}

/**
 * couples/{id}.plus: the couple's Lovelyy Plus. One entitlement covers both partners.
 * Written by the members' phones (no server on the Spark plan; see docs/security.md for the accepted risk).
 */
data class PlusInfo(
    /** When the 14-day trial started (epoch millis). Set once, never changed (firestore.rules). */
    val trialStart: Long? = null,
    /** End of the paid period (epoch millis), or null when nothing was ever bought. */
    val until: Long? = null,
    val source: PlusSource = PlusSource.Trial,
    /** [PlusPlan.key] of what was bought. */
    val product: String? = null,
    /** Who bought it (a member's uid; the person who manages the subscription in Google Play). */
    val buyerUid: String? = null,
    /** Last known auto-renew state of the buyer's subscription (only the buyer's phone can see it). */
    val autoRenew: Boolean? = null,
    /** The Google Play purchase token (stable across renewals); kept for a later server check. */
    val orderToken: String? = null,
    /** Optional short note sent with a gift, shown to the partner once. */
    val giftNote: String? = null,
) {
    val plan: PlusPlan? get() = PlusPlan.of(product)

    fun toMap(): Map<String, Any?> = mapOf(
        "trialStart" to trialStart,
        "until" to until,
        "source" to source.key,
        "product" to product,
        "buyerUid" to buyerUid,
        "autoRenew" to autoRenew,
        "orderToken" to orderToken,
        "giftNote" to giftNote,
    )

    companion object {
        fun fromMap(m: Map<*, *>?): PlusInfo? {
            if (m == null) return null
            return PlusInfo(
                trialStart = (m["trialStart"] as? Number)?.toLong(),
                until = (m["until"] as? Number)?.toLong(),
                source = PlusSource.of(m["source"] as? String),
                product = m["product"] as? String,
                buyerUid = m["buyerUid"] as? String,
                autoRenew = m["autoRenew"] as? Boolean,
                orderToken = m["orderToken"] as? String,
                giftNote = m["giftNote"] as? String,
            )
        }
    }
}

/** What this phone remembers about the automatic trial-end paywall (local prefs, see PlusPrefs). */
data class PaywallMemory(
    /** Couple day (yyyy-MM-dd) the paywall last opened by itself. */
    val lastShownDay: String? = null,
    /** How many times it was closed with the X in a row. */
    val dismissals: Int = 0,
    val lastDismissedAt: Long? = null,
)

/** A subscription Google Play reports as active on this phone (BillingRepository → PlusRules.sync). */
data class StoreSubscription(val plan: PlusPlan, val purchaseTime: Long, val token: String, val autoRenew: Boolean)

/** How the Plus looks to one partner right now (drives the Biz card and the management screen). */
sealed interface PlusStatus {
    /** No Plus map yet (a couple paired before Plus existed, until Home writes the trial). */
    data object None : PlusStatus
    data class Trial(val day: Int, val daysLeft: Int, val endsAt: Long) : PlusStatus
    data class Paid(val plan: PlusPlan?, val source: PlusSource, val until: Long, val autoRenew: Boolean, val boughtByMe: Boolean) : PlusStatus
    /** Trial or paid period is over. [wasPaid]: something was bought before. */
    data class Expired(val wasPaid: Boolean) : PlusStatus
}

/**
 * Pure Lovelyy Plus rules (unit-tested in PlusRulesTest). Time is passed in, so screens, widgets and tests agree.
 * Product rules (approved plan, round 2): one subscription covers both partners, 14-day trial at pairing,
 * yearly (first-year offer for 48 h after the trial) or monthly, a non-renewing 1-year gift, no second purchase
 * while Plus is active and auto-renewing, and the other partner can take over only at the very end.
 */
object PlusRules {
    const val DAY_MS = 86_400_000L
    const val TRIAL_DAYS = 14
    const val TRIAL_MS = TRIAL_DAYS * DAY_MS
    /** The first-year offer is shown for this long after the trial ended. */
    const val OFFER_WINDOW_MS = 48 * 3_600_000L
    /**
     * A renewing subscription stays Plus this long past its stored end, so the partner's phone does not lose Plus
     * before the buyer's phone has opened the app and written the renewed end date.
     */
    const val RENEW_GRACE_MS = 3 * DAY_MS
    /** After [MAX_DISMISSALS] closes in a row, the automatic paywall stays away this long. */
    const val SUPPRESS_MS = 7 * DAY_MS
    const val MAX_DISMISSALS = 2
    const val GIFT_MS = 365 * DAY_MS
    const val GIFT_NOTE_MAX = 140

    fun trialEnd(p: PlusInfo?): Long? = p?.trialStart?.plus(TRIAL_MS)

    /** Something bought is running (with a short grace while it auto-renews). */
    fun paidActive(p: PlusInfo?, now: Long): Boolean {
        val until = p?.until ?: return false
        if (p.source == PlusSource.Trial) return false
        return now < until || (p.autoRenew == true && now < until + RENEW_GRACE_MS)
    }

    fun trialActive(p: PlusInfo?, now: Long): Boolean {
        val end = trialEnd(p) ?: return false
        return now < end
    }

    fun isPlus(p: PlusInfo?, now: Long): Boolean = paidActive(p, now) || trialActive(p, now)

    /** In the free trial and nothing bought yet. */
    fun inTrial(p: PlusInfo?, now: Long): Boolean = !paidActive(p, now) && trialActive(p, now)

    /** 1..14 while the trial runs (day 1 = the pairing day), 0 otherwise. */
    fun trialDay(p: PlusInfo?, now: Long): Int {
        val start = p?.trialStart ?: return 0
        if (!inTrial(p, now)) return 0
        return ((max(0L, now - start) / DAY_MS) + 1).toInt().coerceIn(1, TRIAL_DAYS)
    }

    /** Whole days left in the trial, rounded up (the last day says 1), 0 when not in trial. */
    fun daysLeftInTrial(p: PlusInfo?, now: Long): Int {
        val end = trialEnd(p) ?: return 0
        if (!inTrial(p, now)) return 0
        return ceil((end - now).toDouble() / DAY_MS).toInt().coerceIn(1, TRIAL_DAYS)
    }

    /** Had Plus (trial or paid) and it is over now. */
    fun expired(p: PlusInfo?, now: Long): Boolean = p != null && (p.trialStart != null || p.until != null) && !isPlus(p, now)

    /** P2b: the Home card on trial days 12 and 13, unless closed today. */
    fun showDay12Card(p: PlusInfo?, now: Long, dismissedToday: Boolean): Boolean =
        !dismissedToday && trialDay(p, now) in 12..13

    /** The trial ended and nothing was ever bought. */
    fun trialEndedUnpaid(p: PlusInfo?, now: Long): Boolean {
        val end = trialEnd(p) ?: return false
        return now >= end && p?.until == null && !paidActive(p, now)
    }

    /** Two closes in a row put the automatic paywall to sleep for [SUPPRESS_MS]. */
    fun suppressed(m: PaywallMemory, now: Long): Boolean =
        m.dismissals >= MAX_DISMISSALS && m.lastDismissedAt != null && now < m.lastDismissedAt + SUPPRESS_MS

    /**
     * P3 opens by itself when the trial ended unpaid, at most once per couple day ([today]), and not while
     * [suppressed]. Callers only ask this on a home tab (never during heartbeat, drawing or letter writing).
     */
    fun shouldShowTrialEndPaywall(p: PlusInfo?, now: Long, today: String, m: PaywallMemory): Boolean =
        trialEndedUnpaid(p, now) && m.lastShownDay != today && !suppressed(m, now)

    /** Memory after the paywall was closed with the X. A finished pause starts a new count. */
    fun afterDismiss(m: PaywallMemory, now: Long): PaywallMemory =
        if (m.dismissals >= MAX_DISMISSALS && !suppressed(m, now)) m.copy(dismissals = 1, lastDismissedAt = now)
        else m.copy(dismissals = m.dismissals + 1, lastDismissedAt = now)

    /** The first-year yearly price is offered only in the 48 h after the trial ended (unpaid couples). */
    fun firstYearOfferActive(p: PlusInfo?, now: Long): Boolean {
        val end = trialEnd(p) ?: return false
        return trialEndedUnpaid(p, now) && now < end + OFFER_WINDOW_MS
    }

    /** Millis left of the first-year offer (for the live countdown), 0 when it is not active. */
    fun offerMillisLeft(p: PlusInfo?, now: Long): Long {
        if (!firstYearOfferActive(p, now)) return 0
        return (trialEnd(p)!! + OFFER_WINDOW_MS - now).coerceAtLeast(0)
    }

    /** A second purchase is blocked while Plus is paid, active and auto-renewing. */
    fun purchaseBlocked(p: PlusInfo?, now: Long): Boolean = paidActive(p, now) && p?.autoRenew == true

    /**
     * Whether a new purchase (subscription or gift) may start: free, trial or expired couples can buy; a running
     * non-renewing period (a gift or a cancelled subscription) only on its last day; never while it auto-renews.
     */
    fun canPurchase(p: PlusInfo?, now: Long): Boolean = when {
        purchaseBlocked(p, now) -> false
        !paidActive(p, now) -> true
        else -> now >= p!!.until!! - DAY_MS
    }

    /**
     * P10/P8b: the partner who did not buy may continue Plus on their own account, only when the buyer's period
     * will not renew and it is on its last day or already over. Blocked while the buyer's subscription auto-renews.
     */
    fun canPartnerTakeOver(p: PlusInfo?, me: String, now: Long): Boolean {
        if (p == null || p.source == PlusSource.Trial) return false
        val buyer = p.buyerUid ?: return false
        val until = p.until ?: return false
        if (buyer == me || p.autoRenew == true) return false
        return now >= until - DAY_MS
    }

    /** One key per purchase, so the partner's "Plus açıldı" shows once per purchase (renewals keep the key). */
    fun celebrationKey(p: PlusInfo?): String? {
        if (p == null || p.source == PlusSource.Trial) return null
        val buyer = p.buyerUid ?: return null
        return "${p.source.key}|$buyer|${p.orderToken ?: p.until}"
    }

    /** P6b: show the partner who did not buy that Plus is on, once per purchase. */
    fun shouldCelebrate(p: PlusInfo?, me: String, now: Long, seenKey: String?): Boolean {
        val key = celebrationKey(p) ?: return false
        return p?.buyerUid != me && paidActive(p, now) && key != seenKey
    }

    fun status(p: PlusInfo?, me: String, now: Long): PlusStatus = when {
        p == null -> PlusStatus.None
        paidActive(p, now) -> PlusStatus.Paid(p.plan, p.source, p.until!!, p.autoRenew == true, p.buyerUid == me)
        inTrial(p, now) -> PlusStatus.Trial(trialDay(p, now), daysLeftInTrial(p, now), trialEnd(p)!!)
        else -> PlusStatus.Expired(wasPaid = p.until != null)
    }

    /**
     * The end of the current billing period of a subscription bought at [purchaseTime]: whole years or months
     * (calendar, UTC) after the purchase, the first boundary after [now]. Google Play does not tell the app the
     * expiry, so the buyer's phone recomputes this whenever it sees the subscription still active.
     */
    fun subscriptionEnd(purchaseTime: Long, yearly: Boolean, now: Long): Long {
        val start = Instant.ofEpochMilli(purchaseTime).atZone(ZoneOffset.UTC)
        var n = 1L
        var end = step(start, yearly, n)
        while (end <= now && n < 1200) {
            n++
            end = step(start, yearly, n)
        }
        return end
    }

    private fun step(start: java.time.ZonedDateTime, yearly: Boolean, n: Long): Long =
        (if (yearly) start.plusYears(n) else start.plusMonths(n)).toInstant().toEpochMilli()

    /** A gift adds a year to a running paid period, or starts a year from now. */
    fun giftUntil(p: PlusInfo?, now: Long): Long {
        val base = if (paidActive(p, now)) max(now, p!!.until!!) else now
        return base + GIFT_MS
    }

    fun cleanNote(note: String?): String? = note?.trim()?.take(GIFT_NOTE_MAX)?.ifBlank { null }

    /** The plus map after [me] bought [plan]. Recording the same gift twice (a restore) changes nothing. */
    fun afterPurchase(
        old: PlusInfo?,
        plan: PlusPlan,
        me: String,
        purchaseTime: Long,
        token: String,
        autoRenew: Boolean,
        note: String?,
        now: Long,
    ): PlusInfo {
        val trialStart = old?.trialStart ?: now
        if (plan == PlusPlan.Gift) {
            if (old?.orderToken == token && old.source == PlusSource.Gift) return old
            return PlusInfo(
                trialStart = trialStart, until = giftUntil(old, now), source = PlusSource.Gift, product = plan.key,
                buyerUid = me, autoRenew = false, orderToken = token, giftNote = cleanNote(note),
            )
        }
        return PlusInfo(
            trialStart = trialStart,
            until = subscriptionEnd(purchaseTime, plan == PlusPlan.Yearly, now),
            source = PlusSource.Sub, product = plan.key, buyerUid = me, autoRenew = autoRenew,
            orderToken = token, giftNote = null,
        )
    }

    /**
     * What the buyer's phone writes after asking Google Play which subscription it has ([active], null = none).
     * Returns null when nothing needs to change. Never overwrites the partner's running, renewing subscription.
     */
    fun sync(old: PlusInfo?, me: String, active: StoreSubscription?, now: Long): PlusInfo? {
        if (active == null) {
            if (old != null && old.source == PlusSource.Sub && old.buyerUid == me && old.autoRenew == true) return old.copy(autoRenew = false)
            return null
        }
        if (old != null && old.buyerUid != null && old.buyerUid != me && old.orderToken != active.token && purchaseBlocked(old, now)) return null
        val next = afterPurchase(old, active.plan, me, active.purchaseTime, active.token, active.autoRenew, null, now)
        return if (next == old) null else next
    }
}
