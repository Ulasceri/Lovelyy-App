package com.lovelyy.app.data

/**
 * Mırmır's items (approved boards E1, P7a, P7b): one item per care action. The plain ("sade") model of each is free;
 * the others are Lovelyy Plus models with a small care bonus ("hafif etki", Ulaş 2026-11-27): food +10 fullness,
 * water +10, play +10 love, and the bed fills energy in 45 minutes instead of an hour. Free couples lose nothing:
 * decay, caps and the streak are the same for everyone.
 *
 * Stored in cat/main as `items` ({feed, water, play, sleep} -> item id); firestore.rules `validItems` has the same
 * lists. The ids are also the file names of the pictures (res/drawable-nodpi) and of the item clips (assets/cat).
 */
enum class CareSlot(val key: String) {
    Feed("feed"),
    Water("water"),
    Play("play"),
    Sleep("sleep"),
}

enum class CatItem(val id: String, val slot: CareSlot, val plus: Boolean) {
    MamaSade("mama_sade", CareSlot.Feed, false),
    MamaBalikTabagi("mama_balik_tabagi", CareSlot.Feed, true),
    MamaSut("mama_sut", CareSlot.Feed, true),
    SuSade("su_sade", CareSlot.Water, false),
    SuCesme("su_cesme", CareSlot.Water, true),
    OyuncakYumak("oyuncak_yumak", CareSlot.Play, false),
    OyuncakFare("oyuncak_fare", CareSlot.Play, true),
    OyuncakTuy("oyuncak_tuy", CareSlot.Play, true),
    OyuncakTirmalama("oyuncak_tirmalama", CareSlot.Play, true),
    YatakMinder("yatak_minder", CareSlot.Sleep, false),
    YatakKutu("yatak_kutu", CareSlot.Sleep, true),
    YatakSepet("yatak_sepet", CareSlot.Sleep, true);

    companion object {
        fun of(id: String?): CatItem? = entries.firstOrNull { it.id == id }

        /** The free model of [slot]. */
        fun free(slot: CareSlot): CatItem = entries.first { it.slot == slot && !it.plus }

        /** All models of [slot], free one first (the order of the P7a row). */
        fun forSlot(slot: CareSlot): List<CatItem> = entries.filter { it.slot == slot }
    }
}

/** The couple's chosen item for every care action. */
data class CatItems(
    val feed: CatItem = CatItem.MamaSade,
    val water: CatItem = CatItem.SuSade,
    val play: CatItem = CatItem.OyuncakYumak,
    val sleep: CatItem = CatItem.YatakMinder,
) {
    init {
        require(feed.slot == CareSlot.Feed && water.slot == CareSlot.Water && play.slot == CareSlot.Play && sleep.slot == CareSlot.Sleep)
    }

    operator fun get(slot: CareSlot): CatItem = when (slot) {
        CareSlot.Feed -> feed
        CareSlot.Water -> water
        CareSlot.Play -> play
        CareSlot.Sleep -> sleep
    }

    fun with(item: CatItem): CatItems = when (item.slot) {
        CareSlot.Feed -> copy(feed = item)
        CareSlot.Water -> copy(water = item)
        CareSlot.Play -> copy(play = item)
        CareSlot.Sleep -> copy(sleep = item)
    }

    val all: List<CatItem> get() = listOf(feed, water, play, sleep)

    /** True when at least one chosen model is a Plus model (saving it needs Plus). */
    val needsPlus: Boolean get() = all.any { it.plus }

    /**
     * What is really in use: without Plus every Plus model falls back to the free one (no bonus, the plain picture
     * and clip). The choice stays stored, so it comes back when the couple has Plus again.
     */
    fun effective(plus: Boolean): CatItems =
        if (plus) this else CatItems(free(feed), free(water), free(play), free(sleep))

    private fun free(item: CatItem) = if (item.plus) CatItem.free(item.slot) else item

    /** Field names match firestore.rules `validItems`. */
    fun toMap(): Map<String, String> = CareSlot.entries.associate { it.key to get(it).id }

    companion object {
        /** Unknown or missing ids fall back to the free model, so an older app never breaks on a newer item. */
        fun fromMap(m: Map<*, *>?): CatItems? {
            m ?: return null
            fun pick(slot: CareSlot) = CatItem.of(m[slot.key] as? String)?.takeIf { it.slot == slot } ?: CatItem.free(slot)
            return CatItems(pick(CareSlot.Feed), pick(CareSlot.Water), pick(CareSlot.Play), pick(CareSlot.Sleep))
        }
    }
}

/** Numbers and pure decisions about the items (unit-tested in CatItemsTest). */
object CatItemRules {
    /** Extra fullness per feed with a Plus food model (40 -> 50). */
    const val FEED_BONUS = 10.0
    /** Extra water per drink with a Plus water model (45 -> 55). */
    const val WATER_BONUS = 10.0
    /** Extra love per play with a Plus toy (25 -> 35). */
    const val PLAY_LOVE_BONUS = 10.0
    /** Minutes from empty to full energy in a Plus bed (60 in the free one). */
    const val PLUS_SLEEP_REFILL_MIN = 45
    const val FREE_SLEEP_REFILL_MIN = 60

    /** The item whose bonus counts for [type] right now, or null when the action has no item or no bonus. */
    fun bonusItem(items: CatItems?, type: CatActionType, plus: Boolean): CatItem? {
        val slot = slotOf(type) ?: return null
        val item = items?.effective(plus)?.get(slot) ?: return null
        return item.takeIf { it.plus }
    }

    fun slotOf(type: CatActionType): CareSlot? = when (type) {
        CatActionType.Feed -> CareSlot.Feed
        CatActionType.Water -> CareSlot.Water
        CatActionType.Play -> CareSlot.Play
        CatActionType.Sleep, CatActionType.Wake -> CareSlot.Sleep
        CatActionType.Pet, CatActionType.Name -> null
    }

    /**
     * The item Mırmır reacts to after a save (P7b "Yeni yatağımı çok sevdim"): the first chosen model that was not
     * chosen before, in the order feed, water, play, sleep. Null when nothing new was placed. [before] is null the
     * first time items are placed; then the first non-free model counts, or the bed when everything is free.
     */
    fun newlyPlaced(before: CatItems?, after: CatItems): CatItem? {
        if (before == null) return after.all.firstOrNull { it.plus } ?: after.sleep
        return CareSlot.entries.map { after[it] }.firstOrNull { it != before[it.slot] }
    }

    /** A reaction older than this is not shown any more (e.g. after a reinstall, the save is old news). */
    const val REACTION_MAX_AGE_MS = 2 * 86_400_000L

    /** Whether a reaction for a save at [itemsAt] still has to be shown on this phone (it saw up to [seenAt]). */
    fun shouldReact(itemsAt: Long?, newItem: CatItem?, seenAt: Long, now: Long): Boolean =
        itemsAt != null && newItem != null && itemsAt > seenAt && now - itemsAt < REACTION_MAX_AGE_MS
}
