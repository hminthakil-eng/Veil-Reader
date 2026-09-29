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
}
