package com.lovelyy.app.ui.cat

import com.lovelyy.app.data.CareSlot
import com.lovelyy.app.data.CatActionType
import com.lovelyy.app.data.CatItem
import com.lovelyy.app.data.CatItems
import com.lovelyy.app.data.CatMood

/**
 * The key poses every clip starts and ends on. Two clips can be chained with a hard cut
 * when the first one ends on the pose the second one starts on: the frames are identical,
 * so the cut is invisible. Different poses are joined with a short cross-fade
 * (e.g. the food bowl fading in) or with a bridge clip (lying down, waking up).
 */
enum class Pose {
    Sit, Sleep, Food, Water, Toy,
    // Item clips (tools/item-art): each starts and ends on the cat with its item in place, so a hard cut only
    // joins clips of the same item; anything else cross-fades (the item fades in and out like the bowls).
    FishPlate, Milk, Fountain, Mouse, Scratcher,
    // The Plus beds: sitting in the bed, asleep in it.
    BoxAwake, BoxSleep, BasketAwake, BasketSleep;

    val isSleep: Boolean get() = this == Sleep || this == BoxSleep || this == BasketSleep
}

/** One video in assets/cat/ (see tools/cat-animation). */
enum class CatClip(val file: String, val start: Pose, val end: Pose) {
    Idle("idle", Pose.Sit, Pose.Sit),
    Happy("happy", Pose.Sit, Pose.Sit),
    Hungry("hungry", Pose.Sit, Pose.Sit),
    Thirsty("thirsty", Pose.Sit, Pose.Sit),
    WantsLove("wants_love", Pose.Sit, Pose.Sit),
    Petted("petted", Pose.Sit, Pose.Sit),
    Surprised("surprised", Pose.Sit, Pose.Sit),
    Sleepy("sleepy", Pose.Sit, Pose.Sit),
    LieDown("lie_down", Pose.Sit, Pose.Sleep),
    Sleeping("sleeping", Pose.Sleep, Pose.Sleep),
    WakeUp("wake_up", Pose.Sleep, Pose.Sit),
    Eating("eating", Pose.Food, Pose.Food),
    Drinking("drinking", Pose.Water, Pose.Water),
    Playing("playing", Pose.Toy, Pose.Toy),

    // Mırmır's Plus items (assets/cat/<item id>*.mp4). The feather wand comes in and leaves the frame itself.
    FishPlate("mama_balik_tabagi", Pose.FishPlate, Pose.FishPlate),
    Milk("mama_sut", Pose.Milk, Pose.Milk),
    Fountain("su_cesme", Pose.Fountain, Pose.Fountain),
    Mouse("oyuncak_fare", Pose.Mouse, Pose.Mouse),
    Feather("oyuncak_tuy", Pose.Sit, Pose.Sit),
    Scratcher("oyuncak_tirmalama", Pose.Scratcher, Pose.Scratcher),
    BoxLieDown("yatak_kutu_lie_down", Pose.BoxAwake, Pose.BoxSleep),
    BoxSleeping("yatak_kutu_sleeping", Pose.BoxSleep, Pose.BoxSleep),
    BoxWakeUp("yatak_kutu_wake_up", Pose.BoxSleep, Pose.BoxAwake),
    BasketLieDown("yatak_sepet_lie_down", Pose.BasketAwake, Pose.BasketSleep),
    BasketSleeping("yatak_sepet_sleeping", Pose.BasketSleep, Pose.BasketSleep),
    BasketWakeUp("yatak_sepet_wake_up", Pose.BasketSleep, Pose.BasketAwake);

    val uri: String get() = "asset:///cat/$file.mp4"

    /** Bridges are never interrupted (they would look broken half way). */
    val isBridge: Boolean get() = Bed.entries.any { this == it.lieDown || this == it.wakeUp }

    /** The care slot whose item is inside this clip: Home hides that item's picture while it plays. */
    val slot: CareSlot?
        get() = when (this) {
            Eating, FishPlate, Milk -> CareSlot.Feed
            Drinking, Fountain -> CareSlot.Water
            Playing, Mouse, Feather, Scratcher -> CareSlot.Play
            else -> if (Bed.entries.any { this == it.lieDown || this == it.sleeping || this == it.wakeUp }) CareSlot.Sleep else null
        }
}

/** The three sleep clips of each bed. The free cushion (minder) uses the cat's original clips. */
enum class Bed(val lieDown: CatClip, val sleeping: CatClip, val wakeUp: CatClip) {
    Minder(CatClip.LieDown, CatClip.Sleeping, CatClip.WakeUp),
    Kutu(CatClip.BoxLieDown, CatClip.BoxSleeping, CatClip.BoxWakeUp),
    Sepet(CatClip.BasketLieDown, CatClip.BasketSleeping, CatClip.BasketWakeUp);

    companion object {
        fun of(item: CatItem): Bed = when (item) {
            CatItem.YatakKutu -> Kutu
            CatItem.YatakSepet -> Sepet
            else -> Minder
        }
    }
}

/** The animation that shows an action with the couple's items ([items] = what is in use). Sleep/Wake: the mood. */
fun actionClip(type: CatActionType, items: CatItems): CatClip? = when (type) {
    CatActionType.Feed -> when (items.feed) {
        CatItem.MamaBalikTabagi -> CatClip.FishPlate
        CatItem.MamaSut -> CatClip.Milk
        else -> CatClip.Eating
    }
    CatActionType.Water -> if (items.water == CatItem.SuCesme) CatClip.Fountain else CatClip.Drinking
    CatActionType.Play -> when (items.play) {
        CatItem.OyuncakFare -> CatClip.Mouse
        CatItem.OyuncakTuy -> CatClip.Feather
        CatItem.OyuncakTirmalama -> CatClip.Scratcher
        else -> CatClip.Playing
    }
    CatActionType.Pet -> CatClip.Petted
    CatActionType.Name -> CatClip.Happy
    CatActionType.Sleep, CatActionType.Wake -> null
}

/**
 * The looping "mood" playlist: a need is shown, then a calm idle loop, so the cat doesn't
 * look stuck repeating one clip. [index] counts loops played in the current mood.
 */
fun moodClip(mood: CatMood, index: Int, bed: Bed = Bed.Minder): CatClip = when (mood) {
    CatMood.Sleeping -> bed.sleeping
    CatMood.Idle -> CatClip.Idle
    CatMood.Happy -> if (index % 2 == 0) CatClip.Happy else CatClip.Idle
    CatMood.Hungry -> if (index % 3 == 2) CatClip.Idle else CatClip.Hungry
    CatMood.Thirsty -> if (index % 3 == 2) CatClip.Idle else CatClip.Thirsty
    CatMood.Sleepy -> if (index % 3 == 2) CatClip.Idle else CatClip.Sleepy
    CatMood.WantsLove -> if (index % 3 == 2) CatClip.Idle else CatClip.WantsLove
}

/**
 * The clip that should follow [current] to reach [target], inserting a bridge when the
 * cat has to lie down or get up first (in the bed it is asleep in, or the bed of [target]).
 */
fun bridgeTo(current: Pose?, target: CatClip): CatClip = when {
    current != null && current.isSleep && !target.start.isSleep ->
        (Bed.entries.firstOrNull { it.sleeping.start == current } ?: Bed.Minder).wakeUp
    current != null && !current.isSleep && target.start.isSleep ->
        (Bed.entries.firstOrNull { it.sleeping.start == target.start } ?: Bed.Minder).lieDown
    else -> target
}
