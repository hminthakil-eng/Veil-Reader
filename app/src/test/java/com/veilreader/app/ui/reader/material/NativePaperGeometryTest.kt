package com.veilreader.app.ui.reader.material

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.*
import org.junit.Test

class NativePaperGeometryTest {
    @Test fun `free edge follows finger throughout partial and complete roll`() {
        for (travel in listOf(0.0001f, 0.01f, 0.08f, 0.20f, 0.60f, 1f, 1.8f)) {
            val fold = nativePaperFold(travel, 0.5f, 0f, 1.7f, true)
            val distance = 1.0 - fold.x
            val projected = if (fold.radius > 0 && distance <= PI * fold.radius) {
                fold.x + fold.radius * sin(distance / fold.radius)
            } else 2.0 * fold.x - 1.0 + PI * fold.radius
            assertEquals("travel=$travel", 1.0 - travel, projected, 0.00002)
        }
    }

    @Test fun `left and right turns mirror geometry without mirroring book text`() {
        for (travel in listOf(0f, 0.05f, 0.45f, 1f, 2.08f)) {
            val right = nativePaperFold(travel, 0.87f, 0.13f, 1.8f, true)
            val left = nativePaperFold(travel, 0.87f, 0.13f, 1.8f, false)
            assertEquals(1f - right.x, left.x, 0.00001f)
            assertEquals(right.y, left.y, 0.00001f)
            assertEquals(-right.directionX, left.directionX, 0.00001f)
            assertEquals(right.directionY, left.directionY, 0.00001f)
            assertEquals(right.radius, left.radius, 0.00001)
        }
    }

    @Test fun `completed sheet leaves the viewport instead of lingering at binding`() {
        val fold = nativePaperFold(NATIVE_PAPER_COMPLETE_TRAVEL, 0.5f, 0f, 1.7f, true)
        assertEquals(0.0, fold.radius, 0.0)
        assertTrue("Entire reflected sheet must be left of viewport", 2f * fold.x < 0f)
        assertEquals(NATIVE_PAPER_COMPLETE_TRAVEL, nativePaperSettleTravel(1f, 0.4f, 0.35f, true), 0.00001f)
        assertEquals(0f, nativePaperSettleTravel(0f, 0.4f, 0.35f, false), 0f)
        assertEquals(0.35f, nativePaperSettleTravel(0.4f, 0.4f, 0.35f, true), 0.00001f)
        assertEquals(NATIVE_PAPER_COMPLETE_TRAVEL, nativePaperSettleTravel(1f, 1f, 1f, true), 0f)
        assertEquals(0f, nativePaperSettleTravel(0f, 1f, 1f, false), 0f)
    }

    @Test fun `invalid samples never yield nonfinite GL geometry`() {
        for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -1f, 500f)) {
            val fold = nativePaperFold(value, value, value, value, true)
            assertTrue(fold.x.isFinite() && fold.y.isFinite() && fold.directionX.isFinite() && fold.directionY.isFinite() && fold.radius.isFinite())
        }
    }

    @Test fun `saturated gesture still has a continuous release interval`() {
        // A drag can already have progress=1 before the whole leaf has exited.
        // Independent release time must retain its starting geometry at t=0.
        for (start in listOf(0.83f, 1f, 1.3f)) {
            assertEquals(start, nativePaperReleaseTravel(0f, start), 0f)
            val halfway = nativePaperReleaseTravel(0.5f, start)
            assertTrue(halfway > start && halfway < NATIVE_PAPER_COMPLETE_TRAVEL)
            assertEquals(NATIVE_PAPER_COMPLETE_TRAVEL, nativePaperReleaseTravel(1f, start), 0f)
        }
    }
}
