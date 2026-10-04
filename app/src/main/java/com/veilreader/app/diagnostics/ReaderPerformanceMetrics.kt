package com.veilreader.app.diagnostics

import android.view.View
import androidx.metrics.performance.PerformanceMetricsState

/**
 * Thin UI-state bridge for JankStats. No reading content, locator JSON, titles,
 * highlights or user text are ever written into performance state.
 */
object ReaderPerformanceMetrics {
    const val READER_MODE_KEY = "VeilReaderMode"
    const val PAPER_PHASE_KEY = "VeilPaperPhase"
    const val PAPER_GPU_KEY = "VeilPaperGpu"
    const val PAPER_WORK_KEY = "VeilPaperWork"

    fun putState(root: View, key: String, value: String) {
        PerformanceMetricsState
            .getHolderForHierarchy(root)
            .state
            ?.putState(key, value)
    }

    fun putSingleFrameState(root: View, key: String, value: String) {
        PerformanceMetricsState
            .getHolderForHierarchy(root)
            .state
            ?.putSingleFrameState(key, value)
    }

    fun removeState(root: View, key: String) {
        PerformanceMetricsState
            .getHolderForHierarchy(root)
            .state
            ?.removeState(key)
    }
}
