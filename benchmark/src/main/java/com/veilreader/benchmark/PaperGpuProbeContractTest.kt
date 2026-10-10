package com.veilreader.benchmark

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Regression: live GPU/motion fields are not adjacent in the real accessibility probe. */
@RunWith(AndroidJUnit4::class)
class PaperGpuProbeContractTest {
    private val acquiredProbe =
        "benchmark-reader-locator:OEBPS/c1.xhtml|0.0" +
            ";gpuEpoch=2;gpuSubmitted=2;gpuActive=false" +
            ";gpuHost=true;gpuAttached=true;gpuSurface=true" +
            ";gpuSize=1080x2400;gpuHistory=12ms:c1.xhtml@0.0:2/2/0;motion=true"

    @Test fun separateProbeFieldsStillProveAnAcquiredAndSettledGpuTurn() {
        assertTrue(gpuPaperTurnSettled(1L, acquiredProbe))
    }

    @Test fun noNewAcquiredSheetCannotBeMistakenForMotion() {
        assertFalse(gpuPaperTurnSettled(2L, acquiredProbe))
        assertFalse(gpuPaperTurnSettled(1L, acquiredProbe.replace("gpuEpoch=2", "gpuEpoch=1")))
        assertFalse(gpuPaperTurnSettled(1L, acquiredProbe.replace("gpuEpoch=2", "gpuEpoch=missing")))
    }

    @Test fun activeCurlMissingGpuSurfaceOrDisabledMotionMustStayRed() {
        assertFalse(gpuPaperTurnSettled(1L, acquiredProbe.replace("gpuActive=false", "gpuActive=true")))
        assertFalse(gpuPaperTurnSettled(1L, acquiredProbe.replace("gpuHost=true", "gpuHost=false")))
        assertFalse(gpuPaperTurnSettled(1L, acquiredProbe.replace("gpuAttached=true", "gpuAttached=false")))
        assertFalse(gpuPaperTurnSettled(1L, acquiredProbe.replace("gpuSurface=true", "gpuSurface=false")))
        assertFalse(gpuPaperTurnSettled(1L, acquiredProbe.replace("motion=true", "motion=false")))
    }
}
