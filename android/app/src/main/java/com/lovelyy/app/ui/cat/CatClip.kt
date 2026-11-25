package com.lovelyy.app.ui.cat

import com.lovelyy.app.data.CatMood

/**
 * The key poses every clip starts and ends on. Two clips can be chained with a hard cut
 * when the first one ends on the pose the second one starts on: the frames are identical,
 * so the cut is invisible. Different poses are joined with a short cross-fade
 * (e.g. the food bowl fading in) or with a bridge clip (lying down, waking up).
 */
enum class Pose { Sit, Sleep, Food, Water, Toy }

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
    Playing("playing", Pose.Toy, Pose.Toy);

    val uri: String get() = "asset:///cat/$file.mp4"

    /** Bridges are never interrupted (they would look broken half way). */
    val isBridge: Boolean get() = this == LieDown || this == WakeUp
}

/**
 * The looping "mood" playlist: a need is shown, then a calm idle loop, so the cat doesn't
 * look stuck repeating one clip. [index] counts loops played in the current mood.
 */
fun moodClip(mood: CatMood, index: Int): CatClip = when (mood) {
    CatMood.Sleeping -> CatClip.Sleeping
    CatMood.Idle -> CatClip.Idle
    CatMood.Happy -> if (index % 2 == 0) CatClip.Happy else CatClip.Idle
    CatMood.Hungry -> if (index % 3 == 2) CatClip.Idle else CatClip.Hungry
    CatMood.Thirsty -> if (index % 3 == 2) CatClip.Idle else CatClip.Thirsty
    CatMood.Sleepy -> if (index % 3 == 2) CatClip.Idle else CatClip.Sleepy
    CatMood.WantsLove -> if (index % 3 == 2) CatClip.Idle else CatClip.WantsLove
}

/**
 * The clip that should follow [current] to reach [target], inserting a bridge when the
 * cat has to lie down or get up first.
 */
fun bridgeTo(current: Pose?, target: CatClip): CatClip = when {
    current == Pose.Sleep && target.start != Pose.Sleep -> CatClip.WakeUp
    current != null && current != Pose.Sleep && target.start == Pose.Sleep -> CatClip.LieDown
    else -> target
}
