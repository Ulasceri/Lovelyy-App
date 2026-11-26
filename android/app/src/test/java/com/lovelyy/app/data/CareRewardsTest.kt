package com.lovelyy.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class CareRewardsTest {
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

}
