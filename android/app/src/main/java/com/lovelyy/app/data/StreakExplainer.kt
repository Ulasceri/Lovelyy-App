package com.lovelyy.app.data

/** Where the "Seri nedir?" sheet was opened from. [key] is the fixed analytics value. */
enum class StreakExplainerSource(val key: String) {
    /** Shown by itself once, the first time the couple's streak is at least 1. */
    Auto("auto"),

    /** The streak chip on the Yuva cat card. */
    Chip("chip"),

    /** The streak card on Kedi bakımı. */
    Card("card"),
}

/** Pure rules of the "Seri nedir?" sheet (approved board S1). Unit-tested in StreakExplainerTest. */
object StreakExplainer {
    /** Which heading the sheet shows. */
    enum class Heading {
        /** No streak yet: "Seri nedir?" */
        Intro,

        /** Day one: "Seriniz başladı: 1. gün" (the board's copy). */
        Started,

        /** Day two and later: "Seriniz: N. gün". */
        Running,
    }

    /** The sheet opens by itself once per couple on this phone, as soon as the shown streak reaches 1. */
    fun shouldAutoShow(currentStreak: Int, seen: Boolean): Boolean = currentStreak >= 1 && !seen

    fun heading(currentStreak: Int): Heading = when {
        currentStreak <= 0 -> Heading.Intro
        currentStreak == 1 -> Heading.Started
        else -> Heading.Running
    }
}
