package com.veilreader.app.ui.screens

import com.veilreader.app.ui.reader.material.GpuMaterialPageRendererStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class PaperPerformancePhaseTest {

    @Test
    fun `initial gpu startup is not mislabeled as context recreation`() {
        val state = PaperCurlState()

        state.updateRendererStatus(GpuMaterialPageRendererStatus.INITIALIZING)

        assertEquals(PaperPerformancePhase.IDLE, state.performancePhase)
    }

    @Test
    fun `gpu loss after readiness is labeled until recovery completes`() {
        val state = PaperCurlState()

        state.updateRendererStatus(GpuMaterialPageRendererStatus.READY)
        state.updateRendererStatus(GpuMaterialPageRendererStatus.FAILED)
        assertEquals(
            PaperPerformancePhase.GL_RECREATE,
            state.performancePhase
        )

        state.updateRendererStatus(GpuMaterialPageRendererStatus.INITIALIZING)
        assertEquals(
            PaperPerformancePhase.GL_RECREATE,
            state.performancePhase
        )

        state.updateRendererStatus(GpuMaterialPageRendererStatus.READY)
        assertEquals(PaperPerformancePhase.IDLE, state.performancePhase)
    }

    @Test
    fun `unsupported renderer does not leave stale recovery label`() {
        val state = PaperCurlState()

        state.updateRendererStatus(GpuMaterialPageRendererStatus.READY)
        state.updateRendererStatus(GpuMaterialPageRendererStatus.FAILED)
        state.updateRendererStatus(GpuMaterialPageRendererStatus.UNSUPPORTED)

        assertEquals(PaperPerformancePhase.IDLE, state.performancePhase)
    }
}
