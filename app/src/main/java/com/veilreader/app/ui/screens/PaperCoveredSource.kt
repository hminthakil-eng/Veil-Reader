package com.veilreader.app.ui.screens

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.IntSize
import com.veilreader.app.ui.reader.material.GpuMaterialPageRendererStatus

/** Only draw opacity is leased. The publication stays laid out, visible and touch-owned. */
internal class PaperCoveredSource(private val source: View) : AutoCloseable {
    private val originalAlpha = source.alpha
    private val ownsOpacity = originalAlpha == 1f

    init { if (ownsOpacity) source.alpha = 0f }

    override fun close() {
        // Do not overwrite another owner's later fade/visibility policy.
        if (ownsOpacity && source.alpha == 0f) source.alpha = originalAlpha
    }
}

@Composable
internal fun PaperCoveredSourceEffect(
    state: PaperCurlState,
    source: View?,
    viewport: IntSize,
    enabled: Boolean
) {
    val acquiredEpoch by state.materialEngine.presentedSheetEpoch.collectAsState()
    val adjacent = state.destinationPreview.activeSnapshot
    // The live page remains the underlay on cold/missing/stale destinations.
    // Only a real acquired source leaf AND full-size pinned adjacent pixels can
    // cover it. Alpha changes no touch/focus/layout/navigation ownership.
    val covered = enabled && source != null && state.active &&
        state.rendererStatus == GpuMaterialPageRendererStatus.READY &&
        acquiredEpoch > 0L && acquiredEpoch == state.materialEngine.sheetEpoch &&
        adjacent != null && !adjacent.bitmap.isRecycled &&
        viewport.width > 0 && viewport.height > 0 &&
        source.width == viewport.width && source.height == viewport.height &&
        adjacent.bitmap.width == viewport.width && adjacent.bitmap.height == viewport.height

    DisposableEffect(source, covered) {
        val lease = if (covered) PaperCoveredSource(source) else null
        onDispose { lease?.close() }
    }
}
