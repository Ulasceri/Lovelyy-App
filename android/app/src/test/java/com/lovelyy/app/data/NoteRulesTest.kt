package com.lovelyy.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteRulesTest {
    @Test
    fun unchangedNoteIsNoConflict() {
        assertFalse(NoteRules.isConflict("a", 100L, "a", 100L, "partner", "me"))
    }

    @Test
    fun partnerEditMeanwhileIsConflict() {
        assertTrue(NoteRules.isConflict("a", 100L, "b", 200L, "partner", "me"))
        // Same text again but a newer edit (e.g. a color change) still counts.
        assertTrue(NoteRules.isConflict("a", 100L, "a", 200L, "partner", "me"))
    }

    @Test
    fun myOwnLaterEditIsNoConflict() {
        assertFalse(NoteRules.isConflict("a", 100L, "b", 200L, "me", "me"))
    }

    @Test
    fun oldNotesWithoutUpdatedAtCompareText() {
        assertFalse(NoteRules.isConflict("a", null, "a", null, null, "me"))
        assertTrue(NoteRules.isConflict("a", null, "b", null, null, "me"))
        // The first edit with the new fields after opening an old note.
        assertTrue(NoteRules.isConflict("a", null, "b", 200L, "partner", "me"))
    }
}
