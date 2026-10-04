package com.veilreader.app.diagnostics

import android.view.Window
import androidx.metrics.performance.FrameDataApi31
import androidx.metrics.performance.JankStats

/**
 * Activity-owned JankStats lifecycle shared by the production shell and benchmark shell.
 *
 * ReaderTrace gates installation to debug/benchmark builds, so release builds pay no
 * frame-listener overhead and emit no diagnostic metadata.
 */
class ReaderJankMonitor(
    private val window: Window
) {
    private var tracker: JankStats? = null

    fun install() {
        if (!ReaderTrace.isEnabled() || tracker != null) return

        // Force hierarchy creation first so Reader composables can publish
        // PerformanceMetricsState from their first observable frame.
        window.decorView
        tracker = JankStats.createAndTrack(window) { frame ->
            if (!frame.isJank) return@createAndTrack
            val stateSummary =
                frame.states.joinToString(separator = ",") { state ->
                    "${state.key}=${state.value}"
                }
            val overrunUs =
                (frame as? FrameDataApi31)
                    ?.frameOverrunNanos
                    ?.div(1_000L)
            ReaderTrace.event(
                name = "ui_jank",
                details =
                    "uiUs=${frame.frameDurationUiNanos / 1_000L} " +
                        "overrunUs=${overrunUs ?: -1L} " +
                        "states=$stateSummary"
            )
        }
    }

    fun resume() {
        tracker?.isTrackingEnabled = true
    }

    fun pause() {
        tracker?.isTrackingEnabled = false
    }
}
