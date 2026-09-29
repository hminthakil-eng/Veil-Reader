package com.veilreader.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaperReleaseVelocityTest {
    @Test
    fun `duplicate end offset preserves a fresh flick and commits`() {
        val velocity = nextPaperReleaseVelocity(
            previousVelocityPxPerSec = 1_600f,
            distanceDeltaPx = 0f,
            elapsedMillis = 16L,
            sinceLastMotionMillis = 16L
        )

        assertTrue(
            shouldCommitPaperTurn(
                inwardDistance = 48f,
                width = 1_000f,
                density = 1f,
                curlProgress = 0.10f,
                releaseVelocityPxPerSec = velocity
            )
        )
    }

    @Test
    fun `holding the sheet still before release expires the flick`() {
        val velocity = nextPaperReleaseVelocity(
            previousVelocityPxPerSec = 1_600f,
            distanceDeltaPx = 0f,
            elapsedMillis = 16L,
            sinceLastMotionMillis = 150L
        )

        assertFalse(
            shouldCommitPaperTurn(
                inwardDistance = 48f,
                width = 1_000f,
                density = 1f,
                curlProgress = 0.10f,
                releaseVelocityPxPerSec = velocity
            )
        )
    }
}
