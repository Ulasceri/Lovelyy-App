package com.lovelyy.app.ui.history

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class GridDayLabelTest {
    private val abbr = mapOf(8 to "Ağu", 6 to "Haz", 10 to "Eki", 11 to "Kas")
    private val short: (LocalDate) -> String = { "${it.dayOfMonth} ${abbr.getValue(it.monthValue)}" }

    @Test fun plainDayNumberInsideMonth() {
        assertEquals("15", gridDayLabel(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 3), short))
    }

    @Test fun firstCellShowsDayAndAbbreviatedMonth() {
        assertEquals("3 Eki", gridDayLabel(LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 3), short))
    }

    @Test fun firstOfMonthShowsMonthAndNeverCutsAWord() {
        assertEquals("1 Kas", gridDayLabel(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 10, 3), short))
        assertEquals("1 Ağu", gridDayLabel(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 7, 5), short))
        assertEquals("1 Haz", gridDayLabel(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 5, 5), short))
    }
}
