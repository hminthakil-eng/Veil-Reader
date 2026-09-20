package com.veilreader.app.ui

import androidx.window.core.layout.WindowSizeClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VeilAppAdaptiveLayoutTest {
    @Test
    fun `compact width keeps bottom dock`() {
        assertFalse(shouldUseNavigationRail(size(width = 599, height = 800)))
    }

    @Test
    fun `short landscape keeps bottom dock even when width is expanded`() {
        assertFalse(shouldUseNavigationRail(size(width = 900, height = 479)))
    }

    @Test
    fun `medium width and height switch to navigation rail`() {
        assertTrue(shouldUseNavigationRail(size(width = 600, height = 480)))
    }

    @Test
    fun `window resize crosses rail breakpoint without changing policy`() {
        val foldedNarrow = size(width = 599, height = 800)
        val unfoldedWide = size(width = 700, height = 800)

        assertFalse(shouldUseNavigationRail(foldedNarrow))
        assertTrue(shouldUseNavigationRail(unfoldedWide))
    }

    @Test
    fun `expanded breakpoint selects wider content cap`() {
        assertEquals(1040, contentMaxWidthDp(size(width = 839, height = 800)))
        assertEquals(1280, contentMaxWidthDp(size(width = 840, height = 800)))
        assertEquals(1280, contentMaxWidthDp(size(width = 1200, height = 800)))
    }

    private fun size(width: Int, height: Int): WindowSizeClass =
        WindowSizeClass(width.toFloat(), height.toFloat())
}
