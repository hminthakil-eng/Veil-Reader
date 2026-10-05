package com.veilreader.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HiddenArchiveCompositionTest {
    @Test
    fun populatedArchive_condensesHeader() {
        assertTrue(shouldCondenseHiddenArchive(totalRecords = 1))
    }

    @Test
    fun emptyArchive_keepsFullEntrance() {
        assertFalse(shouldCondenseHiddenArchive(totalRecords = 0))
    }
}
