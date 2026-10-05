package com.veilreader.app.ui.screens

import androidx.compose.ui.unit.IntSize
import com.veilreader.app.domain.BookFormat

/**
 * Pure Reader interaction policy.
 *
 * These decisions intentionally contain no Compose state, navigator implementation,
 * persistence, animation or gesture side effects.
 */
internal fun shouldEmitReaderBoundaryFeedback(
    nowMillis: Long,
    lastEmissionMillis: Long,
    minimumIntervalMillis: Long = 180L
): Boolean =
    lastEmissionMillis <= 0L ||
        nowMillis < lastEmissionMillis ||
        nowMillis - lastEmissionMillis >= minimumIntervalMillis.coerceAtLeast(0L)

internal fun shouldSuppressNavigatorLocatorDuringPagePreview(
    format: BookFormat,
    paperPreviewActive: Boolean,
    slidePreviewActive: Boolean
): Boolean =
    format == BookFormat.EPUB &&
        (paperPreviewActive || slidePreviewActive)

internal fun shouldTakeFinalNavigatorSnapshot(
    format: BookFormat,
    paperPreviewActive: Boolean,
    slidePreviewActive: Boolean,
    previewCancelled: Boolean,
    programmaticNavigationInFlight: Boolean = false
): Boolean =
    !previewCancelled &&
        !programmaticNavigationInFlight &&
        !shouldSuppressNavigatorLocatorDuringPagePreview(
            format = format,
            paperPreviewActive = paperPreviewActive,
            slidePreviewActive = slidePreviewActive
        )

internal enum class ReaderContextControl {
    APPEARANCE,
    PDF_VIEW
}

internal fun readerContextControlFor(format: BookFormat): ReaderContextControl =
    when (format) {
        BookFormat.EPUB -> ReaderContextControl.APPEARANCE
        BookFormat.PDF -> ReaderContextControl.PDF_VIEW
        else -> error("Unsupported Reader context-control format: $format")
    }

internal enum class ReaderBackDisposition {
    SWALLOW,
    CANCEL_PAPER,
    CANCEL_SLIDE,
    CLOSE
}

internal fun readerBackDisposition(
    closeInFlight: Boolean,
    paperPreviewActive: Boolean,
    slidePreviewActive: Boolean
): ReaderBackDisposition = when {
    closeInFlight -> ReaderBackDisposition.SWALLOW
    paperPreviewActive -> ReaderBackDisposition.CANCEL_PAPER
    slidePreviewActive -> ReaderBackDisposition.CANCEL_SLIDE
    else -> ReaderBackDisposition.CLOSE
}

internal fun shouldAutoHideReaderChrome(
    controlsVisible: Boolean,
    showNotebook: Boolean,
    showAppearance: Boolean,
    showPdfZoom: Boolean,
    selectionModeActive: Boolean,
    touchExplorationEnabled: Boolean,
    autoHideEnabled: Boolean = true
): Boolean =
    autoHideEnabled && controlsVisible &&
        !showNotebook &&
        !showAppearance &&
        !showPdfZoom &&
        !selectionModeActive &&
        !touchExplorationEnabled


internal fun shouldCancelReaderPreviewForViewportChange(
    previousSize: IntSize,
    newSize: IntSize,
    paperPreviewActive: Boolean,
    slidePreviewActive: Boolean
): Boolean =
    previousSize != IntSize.Zero &&
        newSize != previousSize &&
        (paperPreviewActive || slidePreviewActive)
