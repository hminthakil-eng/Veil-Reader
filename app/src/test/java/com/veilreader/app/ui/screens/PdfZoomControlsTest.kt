package com.veilreader.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfZoomControlsTest {
    @Test
    fun `zoom in respects maximum`() {
        assertEquals(4f, nextPdfZoom(3.8f, 1f, 4f, 1.25f), 0.001f)
    }

    @Test
    fun `zoom out respects minimum`() {
        assertEquals(1f, nextPdfZoom(1.1f, 1f, 4f, 0.8f), 0.001f)
    }

    @Test
    fun `zoom step scales inside range`() {
        assertEquals(2.5f, nextPdfZoom(2f, 1f, 4f, 1.25f), 0.001f)
    }
}
