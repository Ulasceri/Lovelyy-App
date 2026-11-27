package com.lovelyy.app.ui.cat

/**
 * Sizes of the cat card (approved boards "V2 · Anasayfa kedi kartı v2" and "14 · Kedi bakımı v2"), in dp.
 * Pure so it can be unit-tested (CatCardGeometryTest).
 *
 * Nothing sits on the video any more: the name and streak live in a strip above it and the speech bubble in a
 * slot below it. The card keeps the height it had when everything was drawn over a 1.12 : 1 video: the video
 * gives up the strip and the bubble slot and gets back the 4 dp the need bars lost on top (12 -> 8, as on the
 * board). At 350 dp wide that is 36 + ~244 + 36, close to the board's 36/236/36, and it keeps the whole cat in
 * view down to a 320 dp card (360 dp phones).
 */
object CatCardGeometry {
    /** Width : height of the old full-card video stage. The total card height is kept from it. */
    const val STAGE_RATIO = 1.12f
    const val STRIP_DP = 36f
    const val SPEECH_DP = 36f

    /** The need bars' top padding went from 12 to 8 dp; the video gets those 4 dp. */
    const val NEEDS_TRIM_DP = 4f

    /**
     * Video clips are square; this part of their height (top, bottom) holds the cat, ear tips to paws, in the
     * clips' poses (measured on frames of assets/cat: ears from ~0.16, paws down to ~0.84; the mat below may be cut).
     */
    const val CAT_TOP = 0.16f
    const val CAT_BOTTOM = 0.84f

    /** Height of the video area for a card [widthDp] wide. */
    fun videoHeightDp(widthDp: Float): Float =
        (widthDp / STAGE_RATIO - STRIP_DP - SPEECH_DP + NEEDS_TRIM_DP).coerceAtLeast(0f)

    /**
     * The visible window of the square video (top, bottom as fractions of its height) when it fills a
     * [widthDp] × [heightDp] area and [cropBias] of the cut-off height is taken from the top (0.5 = even).
     */
    fun visibleWindow(widthDp: Float, heightDp: Float, cropBias: Float = 0.5f): Pair<Float, Float> {
        if (widthDp <= 0f || heightDp >= widthDp) return 0f to 1f
        val cut = (widthDp - heightDp) / widthDp
        val top = cut * cropBias
        return top to top + heightDp / widthDp
    }
}
