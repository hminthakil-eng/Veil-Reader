package com.veilreader.app.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EpubPageMotionTest {
    @Test
    fun `page boundary is identity`() {
        val frame = calculateEpubPageMotion(scrollX = 1000, oldScrollX = 900, pageWidth = 1000)

        assertEquals(0f, frame.phase, 0.0001f)
        assertEquals(0f, frame.rotationYDegrees, 0.0001f)
        assertEquals(1f, frame.scale, 0.0001f)
        assertEquals(1f, frame.alpha, 0.0001f)
    }

    @Test
    fun `forward midpoint adds restrained depth`() {
        val frame = calculateEpubPageMotion(scrollX = 500, oldScrollX = 400, pageWidth = 1000)

        assertEquals(1f, frame.phase, 0.0001f)
        assertEquals(-3f, frame.rotationYDegrees, 0.0001f)
        assertEquals(0.992f, frame.scale, 0.0001f)
        assertEquals(0.97f, frame.alpha, 0.0001f)
        assertTrue(frame.pivotAtStart)
    }

    @Test
    fun `backward midpoint mirrors the hinge`() {
        val frame = calculateEpubPageMotion(scrollX = 500, oldScrollX = 600, pageWidth = 1000)

        assertEquals(1f, frame.phase, 0.0001f)
        assertEquals(3f, frame.rotationYDegrees, 0.0001f)
        assertFalse(frame.pivotAtStart)
    }

    @Test
    fun `invalid width stays identity`() {
        val frame = calculateEpubPageMotion(scrollX = 500, oldScrollX = 400, pageWidth = 0)

        assertEquals(0f, frame.phase, 0.0001f)
    }
}
