package com.lovelyy.app.data

/** What to show as a count: [count] notes, "[count]+" when [atLeast]. */
data class CountLabel(val count: Int, val atLeast: Boolean)

/** Pure rules for capped Firestore lists (unit-tested in ListLimitsTest). */
object ListLimits {
    /** Notes loaded per page; "load more" adds another page. */
    const val NOTES_PAGE = 200

    fun nextLimit(current: Int, page: Int = NOTES_PAGE): Int = current + page

    /** A full window means older documents may exist. */
    fun hasMore(loaded: Int, limit: Int): Boolean = loaded >= limit

    /** The count query only matters when the window is full; otherwise [loaded] is already the total. */
    fun needsCount(loaded: Int, limit: Int): Boolean = hasMore(loaded, limit)

    /** [total] is the real count (null if unknown): shown exactly; else "[loaded]+" when the window is full. */
    fun countLabel(loaded: Int, limit: Int, total: Long?): CountLabel = when {
        total != null -> CountLabel(maxOf(total, loaded.toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), false)
        hasMore(loaded, limit) -> CountLabel(loaded, true)
        else -> CountLabel(loaded, false)
    }
}
