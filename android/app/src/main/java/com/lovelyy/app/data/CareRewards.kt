package com.lovelyy.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Care helpers for Kedi bakımı. Pure, unit-tested. */
object CareRewards {
    /** Each person's latest action today, mine first ("Bugün: sen sevdin · Elif su verdi"). Naming isn't care. */
    fun todayLatest(recent: List<CatAction>, myUid: String, today: LocalDate, zone: ZoneId): List<CatAction> =
        recent
            .filter { it.type != CatActionType.Name && Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate() == today }
            .sortedByDescending { it.at }
            .distinctBy { it.uid }
            .sortedBy { if (it.uid == myUid) 0 else 1 }
}
