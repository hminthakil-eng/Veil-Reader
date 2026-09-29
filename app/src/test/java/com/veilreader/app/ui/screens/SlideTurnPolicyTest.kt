package com.veilreader.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SlideTurnPolicyTest {
    @Test
    fun `short fast flick commits even below distance threshold`() {
        assertTrue(
            shouldCommitSlideTurn(
                inwardDistance = 40f,
                width = 1_000f,
                density = 1f,
                slideProgress = 0.08f,
                releaseVelocityPxPerSec = 1_200f
            )
        )
    }

    @Test
    fun `short slow drag cancels`() {
        assertFalse(
            shouldCommitSlideTurn(
                inwardDistance = 40f,
                width = 1_000f,
                density = 1f,
                slideProgress = 0.08f,
                releaseVelocityPxPerSec = 100f
            )
        )
    }

    @Test
    fun `meaningful distance commits without flick velocity`() {
        assertTrue(
            shouldCommitSlideTurn(
                inwardDistance = 180f,
                width = 1_000f,
                density = 1f,
                slideProgress = 0.18f,
                releaseVelocityPxPerSec = 0f
            )
        )
    }

    @Test
    fun `visual progress threshold can commit a deliberate drag`() {
        assertTrue(
            shouldCommitSlideTurn(
                inwardDistance = 60f,
                width = 1_000f,
                density = 1f,
                slideProgress = 0.35f,
                releaseVelocityPxPerSec = 0f
            )
        )
    }

    @Test
    fun `outward or zero width motion never commits`() {
        assertFalse(
            shouldCommitSlideTurn(
                inwardDistance = -10f,
                width = 1_000f,
                density = 1f,
                slideProgress = 0.5f,
                releaseVelocityPxPerSec = 2_000f
            )
        )
        assertFalse(
            shouldCommitSlideTurn(
                inwardDistance = 120f,
                width = 0f,
                density = 1f,
                slideProgress = 0.5f,
                releaseVelocityPxPerSec = 2_000f
            )
        )
    }
    @Test
    fun `duplicate terminal sample preserves a fresh slide flick`() {
        assertTrue(
            nextSlideReleaseVelocity(
                previousVelocityPxPerSec = 1_400f,
                distanceDeltaPx = 0f,
                elapsedMillis = 14L,
                sinceLastMotionMillis = 14L
            ) == 1_400f
        )
    }

    @Test
    fun `stationary pause expires stale slide flick velocity`() {
        assertTrue(
            nextSlideReleaseVelocity(
                previousVelocityPxPerSec = 1_400f,
                distanceDeltaPx = 0f,
                elapsedMillis = 160L,
                sinceLastMotionMillis = 160L
            ) == 0f
        )
    }

    @Test
    fun `fresh slide movement replaces previous release velocity`() {
        assertTrue(
            nextSlideReleaseVelocity(
                previousVelocityPxPerSec = 200f,
                distanceDeltaPx = 24f,
                elapsedMillis = 12L,
                sinceLastMotionMillis = 12L
            ) == 2_000f
        )
    }

    @Test
    fun `slide intent is density aware and rejects diagonal jitter`() {
        assertFalse(
            hasDeliberateSlideIntent(
                offsetX = 20f,
                offsetY = 2f,
                width = 1_000f,
                density = 3f
            )
        )
        assertFalse(
            hasDeliberateSlideIntent(
                offsetX = 42f,
                offsetY = 40f,
                width = 1_000f,
                density = 3f
            )
        )
        assertTrue(
            hasDeliberateSlideIntent(
                offsetX = 48f,
                offsetY = 12f,
                width = 1_000f,
                density = 3f
            )
        )
    }

    @Test
    fun `slide drag starts weighted and converges toward the finger`() {
        val early = slideHorizontalDragResponse(0.05f)
        val middle = slideHorizontalDragResponse(0.50f)
        val late = slideHorizontalDragResponse(0.95f)

        assertTrue(early < middle)
        assertTrue(middle < late)
        assertTrue(early >= 0.75f)
        assertTrue(late <= 0.98f)
    }

    @Test
    fun `slide edge shadow belongs only to an in-flight page`() {
        assertTrue(slideEdgeShadowIntensity(0f) == 0f)
        assertTrue(slideEdgeShadowIntensity(1f) < 0.0001f)
        assertTrue(slideEdgeShadowIntensity(0.5f) > 0.99f)
    }

    @Test
    fun `slide completion settles faster when most travel is already done`() {
        val earlyRelease = slideCompletionDurationMillis(
            progress = 0.20f,
            velocityDpPerSec = 0f
        )
        val lateRelease = slideCompletionDurationMillis(
            progress = 0.85f,
            velocityDpPerSec = 0f
        )

        assertTrue(lateRelease < earlyRelease)
        assertTrue(lateRelease >= 88)
        assertTrue(earlyRelease <= 220)
    }

    @Test
    fun `fast slide release settles faster than slow release at equal progress`() {
        val slow = slideCompletionDurationMillis(
            progress = 0.40f,
            velocityDpPerSec = 250f
        )
        val fast = slideCompletionDurationMillis(
            progress = 0.40f,
            velocityDpPerSec = 2_200f
        )

        assertTrue(fast < slow)
    }

}
