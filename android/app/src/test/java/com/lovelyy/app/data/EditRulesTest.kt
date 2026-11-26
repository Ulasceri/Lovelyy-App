package com.lovelyy.app.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditRulesTest {
    private val d = LocalDate.of(2027, 4, 1)

    @Test
    fun unchangedPlanIsNoConflict() {
        val c = EventContent("Sinema", d, false)
        assertFalse(EditRules.isConflict(c, 100L, c.copy(), 100L, "partner", "me"))
    }

    @Test
    fun partnerEditedPlanIsConflict() {
        val opened = EventContent("Sinema", d, false)
        assertTrue(EditRules.isConflict(opened, 100L, opened.copy(date = d.plusDays(1)), 200L, "partner", "me"))
        assertTrue(EditRules.isConflict(opened, 100L, opened, 200L, "partner", "me"))
    }

    @Test
    fun myOwnLaterEditIsNoConflict() {
        val opened = EventContent("Sinema", d, false)
        assertFalse(EditRules.isConflict(opened, 100L, opened.copy(title = "Tiyatro"), 200L, "me", "me"))
    }

    @Test
    fun oldPlanWithoutTimestampsComparesContentOnly() {
        val opened = EventContent("Sinema", d, true)
        assertFalse(EditRules.isConflict(opened, null, opened, null, null, "me"))
        assertTrue(EditRules.isConflict(opened, null, opened.copy(yearly = false), null, null, "me"))
    }

    @Test
    fun dreamDoneToggleIsNotPartOfContent() {
        val a = Dream("1", "Japonya", DreamCategory.Seyahat, "2027", "u", "U", 1L, doneAt = null)
        val b = a.copy(doneAt = 500L)
        assertEquals(a.content, b.content)
        assertFalse(EditRules.isConflict(a.content, a.updatedAt, b.content, b.updatedAt, b.updatedBy, "me"))
    }

    @Test
    fun partnerEditedDreamIsConflict() {
        val a = Dream("1", "Japonya", DreamCategory.Seyahat, "2027", "u", "U", 1L, doneAt = null)
        val b = a.copy(goal = "2028", updatedAt = 300L, updatedBy = "partner")
        assertTrue(EditRules.isConflict(a.content, a.updatedAt, b.content, b.updatedAt, b.updatedBy, "me"))
    }
}
