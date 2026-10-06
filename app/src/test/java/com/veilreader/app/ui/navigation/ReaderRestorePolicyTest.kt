package com.veilreader.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderRestorePolicyTest {

    @Test
    fun explicitOverride_winsOverCrashSavedStateAndDurableLocator() {
        assertEquals(
            "explicit",
            chooseReaderRestoreLocator("explicit", "crash", "checkpoint", "durable")
        )
    }

    @Test
    fun crashCheckpoint_winsOverSavedStateAndOlderDurableLocator() {
        assertEquals(
            "crash",
            chooseReaderRestoreLocator(null, "crash", "checkpoint", "durable")
        )
    }

    @Test
    fun processCheckpoint_winsWhenCrashCheckpointIsAbsent() {
        assertEquals(
            "checkpoint",
            chooseReaderRestoreLocator(null, null, "checkpoint", "durable")
        )
    }

    @Test
    fun durableLocator_isFallback_andBlankValuesAreIgnored() {
        assertEquals(
            "durable",
            chooseReaderRestoreLocator(" ", "", "", "durable")
        )
        assertNull(chooseReaderRestoreLocator(" ", "", "", null))
    }
}
