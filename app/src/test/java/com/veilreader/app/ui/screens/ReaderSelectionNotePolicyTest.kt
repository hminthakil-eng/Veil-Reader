package com.veilreader.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderSelectionNotePolicyTest {

    @Test
    fun `fresh note requires text while existing highlight may clear its annotation`() {
        assertFalse(
            canSavePendingSelectionNote(
                isNewNote = true,
                note = ""
            )
        )
        assertFalse(
            canSavePendingSelectionNote(
                isNewNote = true,
                note = "   "
            )
        )
        assertTrue(
            canSavePendingSelectionNote(
                isNewNote = true,
                note = "A durable margin note"
            )
        )
        assertTrue(
            canSavePendingSelectionNote(
                isNewNote = false,
                note = ""
            )
        )
    }
}
