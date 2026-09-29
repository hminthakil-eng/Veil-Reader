package com.veilreader.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun `renderer zoom mirror clamps invalid values`() {
        assertEquals(1f, normalizedPdfZoom(Float.NaN, 1f, 4f), 0.001f)
        assertEquals(1f, normalizedPdfZoom(0.2f, 1f, 4f), 0.001f)
        assertEquals(4f, normalizedPdfZoom(8f, 1f, 4f), 0.001f)
    }

    @Test
    fun `zoom step normalizes renderer value before scaling`() {
        assertEquals(1.25f, nextPdfZoom(Float.NaN, 1f, 4f, 1.25f), 0.001f)
   
    @Test
    fun `PDF zoom animation follows reduced motion policy`() {
        assertFalse(shouldAnimatePdfZoom(reducedMotion = true))
        assertTrue(shouldAnimatePdfZoom(reducedMotion = false))
    }
     assertEquals(2f, nextPdfZoom(2f, 1f, 4f, Float.NaN), 0.001f)
    }
}
