package com.veilreader.app.ui.screens

import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.ReaderNavigationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderModeHandoffPolicyTest {
    @Test
    fun `EPUB navigation mode change captures one continuity frame`() {
        assertTrue(
            shouldCaptureReaderModeHandoff(
                format = BookFormat.EPUB,
                previousMode = ReaderNavigationMode.PAPER_CURL,
                requestedMode = ReaderNavigationMode.SLIDE
            )
        )
        assertTrue(
            shouldCaptureReaderModeHandoff(
                format = BookFormat.EPUB,
                previousMode = ReaderNavigationMode.SLIDE,
                requestedMode = ReaderNavigationMode.SCROLL
            )
        )
    }

    @Test
    fun `fixed-layout spread change captures continuity even when navigation mode is unchanged`() {
        assertTrue(
            shouldCaptureReaderModeHandoff(
                format = BookFormat.EPUB,
                previousMode = ReaderNavigationMode.PAGED,
                requestedMode = ReaderNavigationMode.PAGED,
                fixedLayoutSpreadChanged = true
            )
        )
        assertFalse(
            shouldCaptureReaderModeHandoff(
                format = BookFormat.PDF,
                previousMode = ReaderNavigationMode.PAGED,
                requestedMode = ReaderNavigationMode.PAGED,
                fixedLayoutSpreadChanged = true
            )
        )
    }

    @Test
    fun `same mode and non EPUB formats do not allocate a handoff snapshot`() {
        assertFalse(
            shouldCaptureReaderModeHandoff(
                format = BookFormat.EPUB,
                previousMode = ReaderNavigationMode.PAGED,
                requestedMode = ReaderNavigationMode.PAGED
            )
        )
        assertFalse(
            shouldCaptureReaderModeHandoff(
                format = BookFormat.PDF,
                previousMode = ReaderNavigationMode.PAGED,
                requestedMode = ReaderNavigationMode.SLIDE
            )
        )
    }

    @Test
    fun `mode changes receive an extra renderer settle frame`() {
        assertEquals(
            2,
            readerPreferenceSettleFrames(
                previousMode = ReaderNavigationMode.PAPER_CURL,
                requestedMode = ReaderNavigationMode.SCROLL
            )
        )
        assertEquals(
            1,
            readerPreferenceSettleFrames(
                previousMode = ReaderNavigationMode.PAPER_CURL,
                requestedMode = ReaderNavigationMode.PAPER_CURL
            )
        )
        assertEquals(
            2,
            readerPreferenceSettleFrames(
                previousMode = ReaderNavigationMode.PAGED,
                requestedMode = ReaderNavigationMode.PAGED,
                fixedLayoutSpreadChanged = true
            )
        )
    }
}
