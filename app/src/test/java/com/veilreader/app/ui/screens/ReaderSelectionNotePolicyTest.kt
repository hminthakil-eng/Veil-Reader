package com.veilreader.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderSelectionNotePolicyTest {

    @Test
    fun `new note highlight is discarded when draft is abandoned`() {
        assertTrue(
            shouldDiscardPendingSelectionNoteHighlight(
                createdForNote = true,
                noteSaving = false
            )
        )
        assertFalse(
            shouldDiscardPendingSelectionNoteHighlight(
                createdForNote = false,
                noteSaving = false
            )
        )
        assertFalse(
            shouldDiscardPendingSelectionNoteHighlight(
                createdForNote = true,
                noteSaving = true
            )
        )
    }

    @Test
    fun `new note requires text while existing highlight may clear its annotation`() {
        assertFalse(
            canSavePendingSelectionNote(
                createdForNote = true,
                note = ""
            )
        )
        assertFalse(
            canSavePendingSelectionNote(
                createdForNote = true,
                note = "   "
            )
        )
        assertTrue(
            canSavePendingSelectionNote(
                createdForNote = true,
                note = "A durable margin note"
            )
        )
        assertTrue(
            canSavePendingSelectionNote(
                createdForNote = false,
                note = ""
            )
        )
    }
}
