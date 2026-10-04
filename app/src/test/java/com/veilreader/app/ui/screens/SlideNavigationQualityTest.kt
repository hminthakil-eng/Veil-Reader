package com.veilreader.app.ui.screens

import org.junit.Assert.assertTrue
import org.junit.Test

class SlideNavigationQualityTest {

    @Test
    fun `slide tracks the finger closely instead of simulating paper resistance`() {
        val early = slideHorizontalDragResponse(0.05f)
        val middle = slideHorizontalDragResponse(0.50f)
        val late = slideHorizontalDragResponse(0.95f)

        assertTrue(early >= 0.94f)
        assertTrue(middle >= early)
        assertTrue(late >= middle)
        assertTrue(late <= 1.0f)
    }

    @Test
    fun `slide completion remains intentionally low latency`() {
        val slowFromStart = slideCompletionDurationMillis(
            progress = 0f,
            velocityDpPerSec = 0f
        )
        val fastFromStart = slideCompletionDurationMillis(
            progress = 0f,
            velocityDpPerSec = 2_000f
        )
        val nearlyDone = slideCompletionDurationMillis(
            progress = 0.90f,
            velocityDpPerSec = 0f
        )

        assertTrue(slowFromStart <= 168)
        assertTrue(fastFromStart < slowFromStart)
        assertTrue(nearlyDone < slowFromStart)
        assertTrue(nearlyDone >= 72)
    }

    @Test
    fun `slide shadow exists only during transition`() {
        assertTrue(slideEdgeShadowIntensity(0f) <= 0.0001f)
        assertTrue(slideEdgeShadowIntensity(0.5f) > 0.95f)
        assertTrue(slideEdgeShadowIntensity(1f) <= 0.0001f)
    }
}
