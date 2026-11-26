package com.lovelyy.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Accessories the cat unlocks with the shared care streak (approved design 14). Pure, unit-tested. */
object CareRewards {
    enum class Accessory(val day: Int) { Kurdele(7), Papyon(14), Sapka(30), Gozluk(60) }

    /** An accessory stays unlocked once the best streak reached it. */
    fun unlocked(a: Accessory, best: Int): Boolean = best >= a.day

    /** What the cat shows: the chosen accessory, only while it is unlocked. */
    fun worn(wearing: String?, best: Int): Accessory? =
        Accessory.entries.firstOrNull { it.name == wearing }?.takeIf { unlocked(it, best) }

    /**
     * Off until the accessories are baked into the cat's video clips (Ulaş, 2026-11-26): a drawing on top of the
     * video stays still while the cat breathes and moves, which looks broken. The streak stays; the badges and wardrobe are hidden.
     */
    const val WEAR_ON_CAT = false

    /** What the cat actually shows: [worn] while [WEAR_ON_CAT] is on, otherwise nothing. */
    fun shown(wearing: String?, best: Int, enabled: Boolean = WEAR_ON_CAT): Accessory? =
        if (enabled) worn(wearing, best) else null

    /** The next accessory to work towards, or null when all are unlocked. */
    fun next(current: Int, best: Int): Accessory? =
        Accessory.entries.firstOrNull { it.day > current && !unlocked(it, best) }

    /** 0..1 progress towards [next] (1 when everything is unlocked). */
    fun progress(current: Int, best: Int): Float {
        val n = next(current, best) ?: return 1f
        return (current.toFloat() / n.day).coerceIn(0f, 1f)
    }

    /** Each person's latest action today, mine first ("Bugün: sen sevdin · Elif su verdi"). Naming isn't care. */
    fun todayLatest(recent: List<CatAction>, myUid: String, today: LocalDate, zone: ZoneId): List<CatAction> =
        recent
            .filter { it.type != CatActionType.Name && Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate() == today }
            .sortedByDescending { it.at }
            .distinctBy { it.uid }
            .sortedBy { if (it.uid == myUid) 0 else 1 }
}
