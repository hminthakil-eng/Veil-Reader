package com.veilreader.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfZoomPolicyTest {
    @Test
    fun `PDF view probing stops immediately once renderer view exists`() {
        assertFalse(
            shouldProbePdfView(
                attempt = 0,
                maxAttempts = 40,
                hasView = true
            )
        )
    }

    @Test
    fun `PDF view probing respects bounded retry budget`() {
        assertTrue(shouldProbePdfView(attempt = 0, maxAttempts = 40, hasView = false))
        assertTrue(shouldProbePdfView(attempt = 39, maxAttempts = 40, hasView = false))
        assertFalse(shouldProbePdfView(attempt = 40, maxAttempts = 40, hasView = false))
        assertFalse(shouldProbePdfView(attempt = -1, maxAttempts = 40, hasView = false))
        assertFalse(shouldProbePdfView(attempt = 0, maxAttempts = 0, hasView = false))
    }

    @Test
    fun `PDF zoom normalization rejects non finite renderer values`() {
        assertEquals(1f, normalizedPdfZoom(Float.NaN, 1f, 4f), 0.0001f)
        assertEquals(1f, normalizedPdfZoom(0.1f, 1f, 4f), 0.0001f)
        assertEquals(4f, normalizedPdfZoom(9f, 1f, 4f), 0.0001f)
        assertEquals(1f, normalizedPdfZoom(2f, Float.NaN, Float.NaN), 0.0001f)
    }

    @Test
    fun `PDF zoom stepping remains bounded and ignores invalid factors`() {
        assertEquals(2.5f, nextPdfZoom(2f, 1f, 4f, 1.25f), 0.0001f)
        assertEquals(4f, nextPdfZoom(3.5f, 1f, 4f, 1.25f), 0.0001f)
        assertEquals(1f, nextPdfZoom(1.1f, 1f, 4f, 0.8f), 0.0001f)
        assertEquals(2f, nextPdfZoom(2f, 1f, 4f, Float.NaN), 0.0001f)
        assertEquals(2f, nextPdfZoom(2f, 1f, 4f, -2f), 0.0001f)
    }
}
