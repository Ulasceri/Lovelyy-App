package com.lovelyy.app.data

import com.lovelyy.app.data.CareRewards.Accessory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class CareRewardsTest {
    @Test
    fun nextAccessoryFollowsTheStreak() {
        assertEquals(Accessory.Kurdele, CareRewards.next(0, 0))
        assertEquals(Accessory.Papyon, CareRewards.next(12, 12))
        assertEquals(12f / 14f, CareRewards.progress(12, 12), 0.0001f)
    }

    @Test
    fun unlockedAccessoriesStayUnlockedAfterABreak() {
        assertEquals(true, CareRewards.unlocked(Accessory.Papyon, best = 20))
        // Streak broke (3) but the best was 20: next goal is Şapka, not Kurdele again.
        assertEquals(Accessory.Sapka, CareRewards.next(3, 20))
    }

    @Test
    fun allUnlocked() {
        assertNull(CareRewards.next(60, 60))
        assertEquals(1f, CareRewards.progress(60, 60), 0f)
    }

    @Test
    fun todayLatestKeepsOneActionPerPersonMineFirst() {
        val today = LocalDate.of(2026, 11, 25)
        val noon = today.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val recent = listOf(
            CatAction(CatActionType.Water, "p", "Elif", noon + 3000),
            CatAction(CatActionType.Pet, "me", "Ulaş", noon + 2000),
            CatAction(CatActionType.Feed, "me", "Ulaş", noon + 1000),
            CatAction(CatActionType.Play, "p", "Elif", noon - 86_400_000L),
        )
        val out = CareRewards.todayLatest(recent, "me", today, ZoneOffset.UTC)
        assertEquals(listOf(CatActionType.Pet, CatActionType.Water), out.map { it.type })
    }

    @Test
    fun catShowsNoAccessoryWhileWearingIsOff() {
        org.junit.Assert.assertNull(CareRewards.shown("Kurdele", best = 99, enabled = false))
        org.junit.Assert.assertEquals(CareRewards.Accessory.Kurdele, CareRewards.shown("Kurdele", best = 7, enabled = true))
        org.junit.Assert.assertNull(CareRewards.shown("Sapka", best = 7, enabled = true))
    }

    @org.junit.Test
    fun wornAccessoryMustBeUnlocked() {
        org.junit.Assert.assertEquals(CareRewards.Accessory.Kurdele, CareRewards.worn("Kurdele", best = 7))
        org.junit.Assert.assertNull(CareRewards.worn("Sapka", best = 7))
        org.junit.Assert.assertNull(CareRewards.worn(null, best = 99))
        org.junit.Assert.assertNull(CareRewards.worn("Bilinmeyen", best = 99))
    }
}
