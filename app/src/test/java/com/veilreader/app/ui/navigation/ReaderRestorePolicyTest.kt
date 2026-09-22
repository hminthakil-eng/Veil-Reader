package com.veilreader.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderRestorePolicyTest {

    @Test
    fun explicitOverride_winsOverCheckpointAndDurableLocator() {
        assertEquals("explicit", chooseReaderRestoreLocator("explicit", "checkpoint", "durable"))
    }

    @Test
    fun processCheckpoint_winsOverOlderDurableLocator() {
        assertEquals("checkpoint", chooseReaderRestoreLocator(null, "checkpoint", "durable"))
    }

    @Test
    fun durableLocator_isFallback_andBlankValuesAreIgnored() {
        assertEquals("durable", chooseReaderRestoreLocator(" ", "", "durable"))
        assertNull(chooseReaderRestoreLocator(" ", "", null))
    }
}
