package com.lovelyy.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatItemsTest {
    private val hour = 3_600_000L
    private val t0 = 1_800_000_000_000L
    private val plusItems = CatItems(CatItem.MamaSut, CatItem.SuCesme, CatItem.OyuncakTuy, CatItem.YatakKutu)

    private fun doc(level: Double = 20.0, items: CatItems? = null) = CatDoc(
        hunger = Need(level, t0), thirst = Need(level, t0), energy = Need(level, t0), love = Need(level, t0), items = items,
    )

    @Test fun everySlotHasOneFreeModelFirst() {
        assertEquals(12, CatItem.entries.size)
        CareSlot.entries.forEach { slot ->
            val models = CatItem.forSlot(slot)
            assertFalse(models.first().plus)
            assertEquals(1, models.count { !it.plus })
        }
        assertEquals(listOf(3, 2, 4, 3), CareSlot.entries.map { CatItem.forSlot(it).size })
    }

    @Test fun withoutPlusPlusModelsFallBackToFree() {
        assertEquals(CatItems(), plusItems.effective(plus = false))
        assertEquals(plusItems, plusItems.effective(plus = true))
        assertTrue(plusItems.needsPlus)
        assertFalse(CatItems().needsPlus)
        assertFalse(CatItems().with(CatItem.OyuncakFare).effective(false).needsPlus)
    }

    @Test fun mapRoundTripAndUnknownIds() {
        assertEquals(plusItems, CatItems.fromMap(plusItems.toMap()))
        assertNull(CatItems.fromMap(null))
        // A future item id, or a model in the wrong slot, falls back to the free model.
        assertEquals(CatItems(), CatItems.fromMap(mapOf("feed" to "mama_yeni", "water" to "oyuncak_fare")))
    }

    @Test fun plusFoodWaterAndToyGiveTenMore() {
        val free = doc(items = plusItems)
        val now = t0
        fun after(type: CatActionType, plus: Boolean) = CatRules.levels(CatRules.apply(free, type, now, plus = plus)!!, now)
        assertEquals(60.0, after(CatActionType.Feed, plus = false).hunger, 0.01)
        assertEquals(70.0, after(CatActionType.Feed, plus = true).hunger, 0.01)
        assertEquals(65.0, after(CatActionType.Water, plus = false).thirst, 0.01)
        assertEquals(75.0, after(CatActionType.Water, plus = true).thirst, 0.01)
        assertEquals(45.0, after(CatActionType.Play, plus = false).love, 0.01)
        assertEquals(55.0, after(CatActionType.Play, plus = true).love, 0.01)
        // The free model gives no bonus, even with Plus. Caps stay at 100.
        assertEquals(60.0, CatRules.levels(CatRules.apply(doc(items = CatItems()), CatActionType.Feed, now, plus = true)!!, now).hunger, 0.01)
        assertEquals(100.0, CatRules.levels(CatRules.apply(doc(95.0, plusItems), CatActionType.Feed, now, plus = true)!!, now).hunger, 0.01)
    }

    @Test fun plusBedFillsEnergyIn45Minutes() {
        val asleep = CatRules.apply(doc(items = plusItems), CatActionType.Sleep, t0, plus = true)!!
        assertEquals(45, asleep.sleepRefillMin)
        // 80 points at 100 per 45 min: awake after 36 min.
        assertTrue(CatRules.levels(asleep, t0 + 35 * 60_000L).sleeping)
        assertFalse(CatRules.levels(asleep, t0 + 37 * 60_000L).sleeping)
        // Free bed: one hour (awake after 48 min).
        val free = CatRules.apply(doc(items = plusItems), CatActionType.Sleep, t0, plus = false)!!
        assertNull(free.sleepRefillMin)
        assertTrue(CatRules.levels(free, t0 + 47 * 60_000L).sleeping)
        // A care write during the sleep keeps the bed's speed; waking up clears it.
        val petted = CatRules.apply(asleep, CatActionType.Pet, t0 + 10 * 60_000L, strokes = 2)!!
        assertEquals(45, petted.sleepRefillMin)
        assertFalse(CatRules.levels(petted, t0 + 37 * 60_000L).sleeping)
        assertNull(CatRules.apply(asleep, CatActionType.Wake, t0 + 10 * 60_000L)!!.sleepRefillMin)
        assertNull(CatRules.apply(asleep, CatActionType.Feed, t0 + 2 * hour)!!.sleepRefillMin)
    }

    @Test fun newlyPlacedPicksTheFirstChangedSlot() {
        assertEquals(CatItem.MamaSut, CatItemRules.newlyPlaced(null, plusItems))
        assertEquals(CatItem.YatakMinder, CatItemRules.newlyPlaced(null, CatItems()))
        assertEquals(CatItem.YatakKutu, CatItemRules.newlyPlaced(CatItems(), CatItems().with(CatItem.YatakKutu)))
        assertEquals(CatItem.SuSade, CatItemRules.newlyPlaced(plusItems, plusItems.with(CatItem.SuSade)))
        assertNull(CatItemRules.newlyPlaced(plusItems, plusItems))
    }

    @Test fun reactionShowsOnceAndNotWhenOld() {
        assertTrue(CatItemRules.shouldReact(t0, CatItem.YatakKutu, seenAt = 0, now = t0 + hour))
        assertFalse(CatItemRules.shouldReact(t0, CatItem.YatakKutu, seenAt = t0, now = t0 + hour))
        assertFalse(CatItemRules.shouldReact(t0, null, seenAt = 0, now = t0 + hour))
        assertFalse(CatItemRules.shouldReact(t0, CatItem.YatakKutu, seenAt = 0, now = t0 + 3 * 24 * hour))
    }

    @Test fun bonusItemOnlyForCareWithAPlusModel() {
        assertEquals(CatItem.OyuncakTuy, CatItemRules.bonusItem(plusItems, CatActionType.Play, plus = true))
        assertNull(CatItemRules.bonusItem(plusItems, CatActionType.Play, plus = false))
        assertNull(CatItemRules.bonusItem(plusItems, CatActionType.Pet, plus = true))
        assertNull(CatItemRules.bonusItem(null, CatActionType.Feed, plus = true))
    }
}
