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

    fun putState(root: View, key: String, value: String) {
        PerformanceMetricsState
            .getHolderForHierarchy(root)
            .state
            ?.putState(key, value)
    }

    fun removeState(root: View, key: String) {
        PerformanceMetricsState
            .getHolderForHierarchy(root)
            .state
            ?.removeState(key)
    }
}
