package com.lovelyy.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CatRulesTest {
    private val hour = 3_600_000L
    private val t0 = 1_800_000_000_000L
    private fun doc(level: Double = 100.0) =
        CatDoc(name = "Mırmır", hunger = Need(level, t0), thirst = Need(level, t0), energy = Need(level, t0), love = Need(level, t0))

    @Test fun needsDecayWithTime() {
        val l = CatRules.levels(doc(), t0 + 6 * hour)
        assertEquals(50.0, l.hunger, 0.01)            // 12h to empty
        assertEquals(40.0, l.thirst, 0.01)            // 10h
        assertEquals(62.5, l.energy, 0.01)            // 16h
        assertEquals(25.0, l.love, 0.01)              // 8h
        assertEquals(CatMood.WantsLove, l.mood)
        assertEquals(0.0, CatRules.levels(doc(), t0 + 100 * hour).hunger, 0.0)
    }

    @Test fun feedingFillsHungerAndIsCapped() {
        val fed = CatRules.apply(doc(), CatActionType.Feed, t0 + 6 * hour)!!
        assertEquals(90.0, CatRules.levels(fed, t0 + 6 * hour).hunger, 0.01)
        val full = CatRules.apply(doc(), CatActionType.Feed, t0)!!
        assertEquals(100.0, CatRules.levels(full, t0).hunger, 0.01)
    }

    @Test fun sleepRefillsEnergyAndWakesUpByItself() {
        val tired = doc(20.0)
        val asleep = CatRules.apply(tired, CatActionType.Sleep, t0)!!
        assertTrue(CatRules.levels(asleep, t0 + hour / 4).sleeping)
        assertEquals(45.0, CatRules.levels(asleep, t0 + hour / 4).energy, 0.01)
        assertEquals(CatRules.Refusal.Asleep, CatRules.refusal(asleep, CatActionType.Feed, t0 + hour / 4))
        // 80 points at 100/h -> awake after 48 min, then decaying.
        val later = CatRules.levels(asleep, t0 + 2 * hour)
        assertFalse(later.sleeping)
        assertTrue(later.energy in 92.0..100.0)
    }

    @Test fun refusals() {
        assertEquals(CatRules.Refusal.NotSleepy, CatRules.refusal(doc(), CatActionType.Sleep, t0))
        assertEquals(CatRules.Refusal.TooTired, CatRules.refusal(doc(10.0), CatActionType.Play, t0))
        assertNull(CatRules.apply(doc(10.0), CatActionType.Play, t0))
        assertNotNull(CatRules.apply(doc(10.0), CatActionType.Pet, t0, strokes = 3))
    }

    @Test fun streakNeedsBothPartnersEveryDay() {
        val members = listOf("a", "b")
        val d1 = LocalDate.of(2026, 11, 25)
        var s = CareStreak().afterCare("a", members, d1)
        assertEquals(0, s.current(d1))
        s = s.afterCare("a", members, d1)
        assertEquals(0, s.current(d1))
        s = s.afterCare("b", members, d1)
        assertEquals(1, s.current(d1))
        s = s.afterCare("b", members, d1) // caring twice the same day changes nothing
        assertEquals(1, s.count)
        val d2 = d1.plusDays(1)
        assertEquals(1, s.current(d2)) // still alive the next day
        s = s.afterCare("b", members, d2).afterCare("a", members, d2)
        assertEquals(2, s.current(d2))
        assertEquals(0, s.current(d2.plusDays(2))) // a whole day missed: broken
        s = s.afterCare("a", members, d2.plusDays(2)).afterCare("b", members, d2.plusDays(2))
        assertEquals(1, s.count)
        assertEquals(2, s.best)
    }
}
