package com.veilreader.app.ui.reader

import kotlin.math.abs

internal data class EpubPageMotionFrame(
    val phase: Float,
    val rotationYDegrees: Float,
    val scale: Float,
    val alpha: Float,
    val pivotAtStart: Boolean
)

internal fun calculateEpubPageMotion(
    scrollX: Int,
    oldScrollX: Int,
    pageWidth: Int
): EpubPageMotionFrame {
    if (pageWidth <= 0 || scrollX == oldScrollX) return IDENTITY_EPUB_PAGE_MOTION

    val normalized = abs(scrollX.toFloat() / pageWidth.toFloat())
    val pageFraction = normalized % 1f
    val phase = (1f - abs((pageFraction * 2f) - 1f)).coerceIn(0f, 1f)
    if (phase <= MOTION_EPSILON) return IDENTITY_EPUB_PAGE_MOTION

    val movingForwardOnScreen = scrollX > oldScrollX
    val direction = if (movingForwardOnScreen) -1f else 1f
    return EpubPageMotionFrame(
        phase = phase,
        rotationYDegrees = direction * MAX_ROTATION_DEGREES * phase,
        scale = 1f - (MAX_SCALE_DROP * phase),
        alpha = 1f - (MAX_ALPHA_DROP * phase),
        pivotAtStart = movingForwardOnScreen
    )
}

private val IDENTITY_EPUB_PAGE_MOTION = EpubPageMotionFrame(
    phase = 0f,
    rotationYDegrees = 0f,
    scale = 1f,
    alpha = 1f,
    pivotAtStart = true
)

private const val MAX_ROTATION_DEGREES = 3.0f
private const val MAX_SCALE_DROP = 0.008f
private const val MAX_ALPHA_DROP = 0.03f
private const val MOTION_EPSILON = 0.001f
