package com.veilreader.app.ui.screens

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
}
