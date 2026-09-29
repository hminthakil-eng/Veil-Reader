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

}
