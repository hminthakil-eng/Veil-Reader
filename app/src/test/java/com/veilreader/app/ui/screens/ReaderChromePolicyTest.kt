package com.veilreader.app.ui.screens

import com.veilreader.app.domain.BookFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderChromePolicyTest {
    @Test
    fun `idle visible chrome can auto-hide`() {
        assertTrue(
            shouldAutoHideReaderChrome(
                controlsVisible = true,
                showNotebook = false,
                showAppearance = false,
                showPdfZoom = false,
                selectionModeActive = false,
                touchExplorationEnabled = false
            )
        )
    }

    @Test
    fun `selection keeps reader chrome available`() {
        assertFalse(
            shouldAutoHideReaderChrome(
                controlsVisible = true,
                showNotebook = false,
                showAppearance = false,
                showPdfZoom = false,
                selectionModeActive = true,
                touchExplorationEnabled = false
            )
        )
    }

    @Test
    fun `touch exploration keeps reader chrome available`() {
        assertFalse(
            shouldAutoHideReaderChrome(
                controlsVisible = true,
                showNotebook = false,
                showAppearance = false,
                showPdfZoom = false,
                selectionModeActive = false,
                touchExplorationEnabled = true
            )
        )
    }

    @Test
    fun `open reader surfaces suspend auto-hide`() {
        assertFalse(
            shouldAutoHideReaderChrome(
                controlsVisible = true,
                showNotebook = true,
                showAppearance = false,
                showPdfZoom = false,
                selectionModeActive = false,
                touchExplorationEnabled = false
            )
        )
    }

    @Test
    fun `Back is swallowed while durable close is in flight`() {
        assertEquals(
            ReaderBackDisposition.SWALLOW,
            readerBackDisposition(
                closeInFlight = true,
                paperPreviewActive = false,
                slidePreviewActive = false
            )
        )
    }

    @Test
    fun `Back cancels active page preview before closing Reader`() {
        assertEquals(
            ReaderBackDisposition.CANCEL_PAPER,
            readerBackDisposition(
                closeInFlight = false,
                paperPreviewActive = true,
                slidePreviewActive = false
            )
        )
        assertEquals(
            ReaderBackDisposition.CANCEL_SLIDE,
            readerBackDisposition(
                closeInFlight = false,
                paperPreviewActive = false,
                slidePreviewActive = true
            )
        )
    }

    @Test
    fun `Back closes only when no guarded Reader transition owns it`() {
        assertEquals(
            ReaderBackDisposition.CLOSE,
            readerBackDisposition(
                closeInFlight = false,
                paperPreviewActive = false,
                slidePreviewActive = false
            )
        )
    }

    @Test
    fun `any active EPUB page preview suppresses navigator locator commits`() {
        assertTrue(
            shouldSuppressNavigatorLocatorDuringPagePreview(
                format = BookFormat.EPUB,
                paperPreviewActive = true,
                slidePreviewActive = false
            )
        )
        assertTrue(
            shouldSuppressNavigatorLocatorDuringPagePreview(
                format = BookFormat.EPUB,
                paperPreviewActive = false,
                slidePreviewActive = true
            )
        )
    }

    @Test
    fun `preview suppression is false after both EPUB preview layers settle`() {
        assertFalse(
            shouldSuppressNavigatorLocatorDuringPagePreview(
                format = BookFormat.EPUB,
                paperPreviewActive = false,
                slidePreviewActive = false
            )
        )
        assertFalse(
            shouldSuppressNavigatorLocatorDuringPagePreview(
                format = BookFormat.PDF,
                paperPreviewActive = true,
                slidePreviewActive = true
            )
        )
    }

    @Test
    fun `final snapshot is skipped after a preview rollback even when visual state is already clear`() {
        assertFalse(
            shouldTakeFinalNavigatorSnapshot(
                format = BookFormat.EPUB,
                paperPreviewActive = false,
                slidePreviewActive = false,
                previewCancelled = true
            )
        )
    }

    @Test
    fun `settled reader can take a final snapshot`() {
        assertTrue(
            shouldTakeFinalNavigatorSnapshot(
                format = BookFormat.EPUB,
                paperPreviewActive = false,
                slidePreviewActive = false,
                previewCancelled = false
            )
        )
        assertFalse(
            shouldTakeFinalNavigatorSnapshot(
                format = BookFormat.EPUB,
                paperPreviewActive = false,
                slidePreviewActive = true,
                previewCancelled = false
            )
        )
    }

    @Test
    fun `boundary feedback throttles key repeat without delaying the first hit`() {
        assertTrue(
            shouldEmitReaderBoundaryFeedback(
                nowMillis = 1_000L,
                lastEmissionMillis = 0L
            )
        )
        assertFalse(
            shouldEmitReaderBoundaryFeedback(
                nowMillis = 1_100L,
                lastEmissionMillis = 1_000L
            )
        )
        assertTrue(
            shouldEmitReaderBoundaryFeedback(
                nowMillis = 1_180L,
                lastEmissionMillis = 1_000L
            )
        )
    }

    @Test
    fun `boundary feedback recovers safely if monotonic clock appears to move backward`() {
        assertTrue(
            shouldEmitReaderBoundaryFeedback(
                nowMillis = 900L,
                lastEmissionMillis = 1_000L
            )
        )
    }

    @Test
    fun `reader jumps disable navigator animation under reduced motion`() {
        assertFalse(shouldAnimateReaderJump(reducedMotion = true))
        assertTrue(shouldAnimateReaderJump(reducedMotion = false))
    }

}
