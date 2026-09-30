package com.veilreader.app.ui.reader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderStartupHandshakePolicyTest {

    @Test
    fun navigatorCannotPublishBeforeDurableSessionIsReady() {
        assertFalse(
            shouldCollectReaderLocator(
                sessionReady = false,
                navigatorAttached = true
            )
        )
        assertTrue(
            shouldCollectReaderLocator(
                sessionReady = true,
                navigatorAttached = true
            )
        )
    }

    @Test
    fun foregroundSessionResumesImmediatelyAfterOpen() {
        assertTrue(
            shouldResumeReaderAfterOpen(
                sessionReady = true,
                lifecycleResumed = true
            )
        )
        assertFalse(
            shouldResumeReaderAfterOpen(
                sessionReady = true,
                lifecycleResumed = false
            )
        )
    }

    @Test
    fun backgroundStartupFlushesOnlyAfterInitialLocatorWasAccepted() {
        assertTrue(
            shouldFlushStartupLocatorInBackground(
                sessionReady = true,
                lifecycleResumed = false,
                initialLocatorCommitAccepted = true
            )
        )
        assertFalse(
            shouldFlushStartupLocatorInBackground(
                sessionReady = true,
                lifecycleResumed = true,
                initialLocatorCommitAccepted = true
            )
        )
        assertFalse(
            shouldFlushStartupLocatorInBackground(
                sessionReady = true,
                lifecycleResumed = false,
                initialLocatorCommitAccepted = false
            )
        )
    }
}
