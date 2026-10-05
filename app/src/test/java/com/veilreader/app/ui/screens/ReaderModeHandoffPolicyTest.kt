package com.veilreader.app.ui.screens

import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderNavigationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderModeHandoffPolicyTest {
    @Test
    fun `fixed-layout EPUB cannot enter single-sheet Paper or Slide transitions`() {
        listOf(PageTurnStyle.PAPER, PageTurnStyle.SLIDE).forEach { style ->
            val effective = effectiveReaderAppearanceForPublication(
                appearance = ReaderAppearance(
                    scroll = true,
                    pageTurnStyle = style
                ),
                fixedLayout = true
            )
            assertFalse(effective.scroll)
            assertEquals(PageTurnStyle.NONE, effective.pageTurnStyle)
        }
    }

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
    fun `initial navigator attach does not resubmit identical preferences`() {
        val appearance = ReaderAppearance(
            scroll = false,
            pageTurnStyle = PageTurnStyle.PAPER
        )
        assertFalse(
            readerPreferencesNeedSubmission(
                previousPresented = appearance,
                previousAccepted = appearance,
                previousSpread = com.veilreader.app.domain.ReaderFixedLayoutSpread.AUTO,
                requestedPresented = appearance,
                requestedSource = appearance,
                requestedSpread = com.veilreader.app.domain.ReaderFixedLayoutSpread.AUTO
            )
        )
    }

    @Test
    fun `real appearance or spread changes still submit preferences`() {
        val before = ReaderAppearance(
            scroll = false,
            pageTurnStyle = PageTurnStyle.PAGED
        )
        val after = before.copy(pageTurnStyle = PageTurnStyle.SLIDE)
        assertTrue(
            readerPreferencesNeedSubmission(
                previousPresented = before,
                previousAccepted = before,
                previousSpread = com.veilreader.app.domain.ReaderFixedLayoutSpread.AUTO,
                requestedPresented = after,
                requestedSource = after,
                requestedSpread = com.veilreader.app.domain.ReaderFixedLayoutSpread.AUTO
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
