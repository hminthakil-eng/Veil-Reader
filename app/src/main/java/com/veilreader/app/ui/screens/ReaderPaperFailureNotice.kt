package com.veilreader.app.ui.screens

import com.veilreader.app.ui.reader.material.GpuMaterialPageRendererStatus

internal enum class PaperTurnVisualFailure {
    SNAPSHOT,
    PRESENTATION,
    RENDERER_UNAVAILABLE
}

/** Identity distinguishes successive notices so cancelled display jobs cannot clear newer ones. */
internal class ReaderPaperFailureNotice(val failure: PaperTurnVisualFailure)

internal fun paperRendererFailureNotice(
    status: GpuMaterialPageRendererStatus
): PaperTurnVisualFailure? = when (status) {
    GpuMaterialPageRendererStatus.FAILED,
    GpuMaterialPageRendererStatus.UNSUPPORTED -> PaperTurnVisualFailure.RENDERER_UNAVAILABLE
    else -> null
}
