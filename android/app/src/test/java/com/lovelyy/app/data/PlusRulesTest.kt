package com.lovelyy.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class PlusRulesTest {
    private val day = PlusRules.DAY_MS
    private val hour = 3_600_000L
    private val t0 = 1_800_000_000_000L
    private val trial = PlusInfo(trialStart = t0)

    private fun paid(until: Long, autoRenew: Boolean?, buyer: String = "ada", source: PlusSource = PlusSource.Sub, token: String = "tok") =
        PlusInfo(trialStart = t0, until = until, source = source, product = PlusPlan.Monthly.key, buyerUid = buyer, autoRenew = autoRenew, orderToken = token)

    @Test fun trialLastsFourteenDays() {
        assertTrue(PlusRules.isPlus(trial, t0))
        assertTrue(PlusRules.inTrial(trial, t0 + 14 * day - 1))
        assertFalse(PlusRules.isPlus(trial, t0 + 14 * day))
        assertTrue(PlusRules.expired(trial, t0 + 14 * day))
        assertFalse(PlusRules.isPlus(null, t0))
        assertFalse(PlusRules.expired(null, t0))
    }

    @Test fun trialDayAndDaysLeft() {
        assertEquals(1, PlusRules.trialDay(trial, t0))
        assertEquals(14, PlusRules.daysLeftInTrial(trial, t0))
        assertEquals(5, PlusRules.trialDay(trial, t0 + 4 * day + hour))
        assertEquals(10, PlusRules.daysLeftInTrial(trial, t0 + 4 * day + hour))
        assertEquals(14, PlusRules.trialDay(trial, t0 + 13 * day + hour))
        assertEquals(1, PlusRules.daysLeftInTrial(trial, t0 + 13 * day + hour))
        assertEquals(0, PlusRules.trialDay(trial, t0 + 15 * day))
        assertEquals(0, PlusRules.daysLeftInTrial(trial, t0 + 15 * day))
    }

    @Test fun day12CardOnDays12And13Only() {
        assertFalse(PlusRules.showDay12Card(trial, t0 + 10 * day + hour, false)) // day 11
        assertTrue(PlusRules.showDay12Card(trial, t0 + 11 * day + hour, false))  // day 12
        assertTrue(PlusRules.showDay12Card(trial, t0 + 12 * day + hour, false))  // day 13
        assertFalse(PlusRules.showDay12Card(trial, t0 + 12 * day + hour, true))  // closed today
        assertFalse(PlusRules.showDay12Card(trial, t0 + 13 * day + hour, false)) // day 14
        // Bought during the trial: no card.
        assertFalse(PlusRules.showDay12Card(paid(t0 + 40 * day, true), t0 + 11 * day + hour, false))
    }

    @Test fun trialEndPaywallOncePerDayAndSuppressedAfterTwoCloses() {
        val end = t0 + 14 * day
        val m = PaywallMemory()
        assertFalse(PlusRules.shouldShowTrialEndPaywall(trial, end - 1, "d14", m))
        assertTrue(PlusRules.shouldShowTrialEndPaywall(trial, end, "d15", m))
        assertFalse(PlusRules.shouldShowTrialEndPaywall(trial, end, "d15", m.copy(lastShownDay = "d15")))
        val once = PlusRules.afterDismiss(m.copy(lastShownDay = "d15"), end)
        assertTrue(PlusRules.shouldShowTrialEndPaywall(trial, end + day, "d16", once))
        val twice = PlusRules.afterDismiss(once.copy(lastShownDay = "d16"), end + day)
        assertEquals(2, twice.dismissals)
        assertFalse(PlusRules.shouldShowTrialEndPaywall(trial, end + 2 * day, "d17", twice))
        assertFalse(PlusRules.shouldShowTrialEndPaywall(trial, end + 7 * day + day - 1, "d22", twice))
        assertTrue(PlusRules.shouldShowTrialEndPaywall(trial, end + 8 * day, "d23", twice))
        // A close after the pause starts counting again.
        assertEquals(1, PlusRules.afterDismiss(twice, end + 8 * day).dismissals)
        // Paid couples never get it.
        assertFalse(PlusRules.shouldShowTrialEndPaywall(paid(end + 30 * day, true), end, "d15", m))
        // Expired paid couples are not "trial ended" either.
        assertFalse(PlusRules.shouldShowTrialEndPaywall(paid(end - day, false), end + 10 * day, "x", m))
    }

    @Test fun firstYearOfferOnlyFor48HoursAfterTrial() {
        val end = t0 + 14 * day
        assertFalse(PlusRules.firstYearOfferActive(trial, end - 1))
        assertTrue(PlusRules.firstYearOfferActive(trial, end))
        assertEquals(48 * hour, PlusRules.offerMillisLeft(trial, end))
        assertEquals(hour, PlusRules.offerMillisLeft(trial, end + 47 * hour))
        assertFalse(PlusRules.firstYearOfferActive(trial, end + 48 * hour))
        assertEquals(0L, PlusRules.offerMillisLeft(trial, end + 48 * hour))
    }

    @Test fun renewingSubKeepsPlusThroughGraceAndBlocksSecondPurchase() {
        val until = t0 + 30 * day
        val sub = paid(until, autoRenew = true)
        assertTrue(PlusRules.isPlus(sub, until + 2 * day))
        assertFalse(PlusRules.isPlus(sub, until + 3 * day))
        assertTrue(PlusRules.purchaseBlocked(sub, until - 5 * day))
        assertFalse(PlusRules.canPurchase(sub, until - 5 * day))
        val cancelled = sub.copy(autoRenew = false)
        assertFalse(PlusRules.isPlus(cancelled, until))
        assertFalse(PlusRules.canPurchase(cancelled, until - 2 * day))
        assertTrue(PlusRules.canPurchase(cancelled, until - day))
        assertTrue(PlusRules.canPurchase(trial, t0 + day))
        assertTrue(PlusRules.canPurchase(null, t0))
    }

    @Test fun takeoverOnlyOnLastDayOfNonRenewingPeriodOrAfter() {
        val until = t0 + 30 * day
        assertFalse(PlusRules.canPartnerTakeOver(paid(until, true), "bora", until - 1))
        assertFalse(PlusRules.canPartnerTakeOver(paid(until, false), "bora", until - day - 1))
        assertTrue(PlusRules.canPartnerTakeOver(paid(until, false), "bora", until - day))
        assertTrue(PlusRules.canPartnerTakeOver(paid(until, false), "bora", until + 5 * day))
        assertTrue(PlusRules.canPartnerTakeOver(paid(until, null, source = PlusSource.Gift), "bora", until - hour))
        assertFalse(PlusRules.canPartnerTakeOver(paid(until, false), "ada", until - hour)) // the buyer
        assertFalse(PlusRules.canPartnerTakeOver(trial, "bora", t0 + 20 * day))
        // A renewing sub stays blocked even after its stored end (grace).
        assertFalse(PlusRules.canPartnerTakeOver(paid(until, true), "bora", until + day))
    }

    @Test fun celebrationOncePerPurchaseForTheOtherPartner() {
        val sub = paid(t0 + 30 * day, true)
        val key = PlusRules.celebrationKey(sub)
        assertEquals("sub|ada|tok", key)
        assertTrue(PlusRules.shouldCelebrate(sub, "bora", t0, null))
        assertFalse(PlusRules.shouldCelebrate(sub, "bora", t0, key))
        assertFalse(PlusRules.shouldCelebrate(sub, "ada", t0, null))
        assertFalse(PlusRules.shouldCelebrate(trial, "bora", t0, null))
        // A renewal keeps the key, a takeover makes a new one.
        assertEquals(key, PlusRules.celebrationKey(sub.copy(until = t0 + 60 * day)))
        assertTrue(PlusRules.shouldCelebrate(sub.copy(buyerUid = "bora", orderToken = "t2"), "ada", t0, key))
    }

    @Test fun subscriptionEndStepsWholePeriods() {
        val bought = Instant.parse("2026-01-31T10:00:00Z").toEpochMilli()
        val month = PlusRules.subscriptionEnd(bought, yearly = false, now = bought)
        assertEquals(Instant.parse("2026-02-28T10:00:00Z").toEpochMilli(), month)
        val later = PlusRules.subscriptionEnd(bought, yearly = false, now = Instant.parse("2026-04-01T00:00:00Z").toEpochMilli())
        assertEquals(Instant.parse("2026-04-30T10:00:00Z").toEpochMilli(), later)
        val year = PlusRules.subscriptionEnd(bought, yearly = true, now = bought + day)
        assertEquals(Instant.parse("2027-01-31T10:00:00Z").atZone(ZoneOffset.UTC).toInstant().toEpochMilli(), year)
    }

    @Test fun purchaseWritesTheBuyerAndGiftsAddAYear() {
        val now = t0 + 3 * day
        val sub = PlusRules.afterPurchase(trial, PlusPlan.Yearly, "ada", now, "tok", true, "ignored", now)
        assertEquals(t0, sub.trialStart)
        assertEquals(PlusSource.Sub, sub.source)
        assertEquals("ada", sub.buyerUid)
        assertEquals(PlusPlan.Yearly, sub.plan)
        assertNull(sub.giftNote)
        val gift = PlusRules.afterPurchase(trial, PlusPlan.Gift, "ada", now, "g1", false, "  İyi ki varsın  ", now)
        assertEquals(now + PlusRules.GIFT_MS, gift.until)
        assertEquals("İyi ki varsın", gift.giftNote)
        assertEquals(false, gift.autoRenew)
        // Recording the same gift again changes nothing; a new gift on a running one adds a year.
        assertEquals(gift, PlusRules.afterPurchase(gift, PlusPlan.Gift, "ada", now, "g1", false, null, now + day))
        val second = PlusRules.afterPurchase(gift, PlusPlan.Gift, "bora", now, "g2", false, null, now + day)
        assertEquals(gift.until!! + PlusRules.GIFT_MS, second.until)
        assertEquals(140, PlusRules.cleanNote("x".repeat(300))!!.length)
        assertNull(PlusRules.cleanNote("   "))
        // No plus map yet: the trial start becomes now.
        assertEquals(now, PlusRules.afterPurchase(null, PlusPlan.Monthly, "ada", now, "t", true, null, now).trialStart)
    }

    @Test fun syncMarksCancelledAndNeverOverwritesPartnersRenewingSub() {
        val now = t0 + 10 * day
        val mine = paid(t0 + 30 * day, true, buyer = "ada")
        assertEquals(false, PlusRules.sync(mine, "ada", null, now)?.autoRenew)
        assertNull(PlusRules.sync(mine.copy(autoRenew = false), "ada", null, now))
        assertNull(PlusRules.sync(mine, "bora", null, now))
        val store = StoreSubscription(PlusPlan.Monthly, t0, "other", true)
        assertNull(PlusRules.sync(mine, "bora", store, now))
        // Same subscription, still renewing: nothing to write when the end date did not change.
        val synced = PlusRules.sync(trial, "ada", StoreSubscription(PlusPlan.Monthly, t0, "tok", true), now)!!
        assertNull(PlusRules.sync(synced, "ada", StoreSubscription(PlusPlan.Monthly, t0, "tok", true), now))
    }

    @Test fun mapRoundTrip() {
        val p = paid(t0 + day, true).copy(giftNote = "not")
        assertEquals(p, PlusInfo.fromMap(p.toMap()))
        assertNull(PlusInfo.fromMap(null))
        assertEquals(PlusSource.Trial, PlusInfo.fromMap(mapOf("trialStart" to 5L))!!.source)
    }

    @Test fun statusForEachPhase() {
        assertEquals(PlusStatus.None, PlusRules.status(null, "ada", t0))
        assertTrue(PlusRules.status(trial, "ada", t0) is PlusStatus.Trial)
        assertEquals(PlusStatus.Expired(false), PlusRules.status(trial, "ada", t0 + 20 * day))
        val s = PlusRules.status(paid(t0 + 30 * day, true), "bora", t0) as PlusStatus.Paid
        assertFalse(s.boughtByMe)
        assertTrue(s.autoRenew)
    }
}
