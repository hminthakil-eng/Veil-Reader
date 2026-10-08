package com.veilreader.app.ui.screens

import kotlin.math.max
import kotlin.math.min

internal data class ReaderImageTransform(
    val scale: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f
)

/** Fit uses the image's aspect ratio; letterboxing is never treated as pannable image. */
internal fun clampReaderImageTransform(
    requested: ReaderImageTransform,
    viewportWidth: Int,
    viewportHeight: Int,
    imageWidth: Int,
    imageHeight: Int
): ReaderImageTransform {
    val scale = requested.scale.takeIf { it.isFinite() }?.coerceIn(1f, 5f) ?: 1f
    if (scale == 1f || viewportWidth <= 0 || viewportHeight <= 0 ||
        imageWidth <= 0 || imageHeight <= 0
    ) return ReaderImageTransform(scale)
    val fit = min(viewportWidth.toFloat() / imageWidth, viewportHeight.toFloat() / imageHeight)
    val maxX = max(0f, (imageWidth * fit * scale - viewportWidth) / 2f)
    val maxY = max(0f, (imageHeight * fit * scale - viewportHeight) / 2f)
    return ReaderImageTransform(
        scale,
        requested.panX.takeIf { it.isFinite() }?.coerceIn(-maxX, maxX) ?: 0f,
        requested.panY.takeIf { it.isFinite() }?.coerceIn(-maxY, maxY) ?: 0f
    )
}

/** Keeps the content under the gesture centroid stationary while zooming. */
internal fun transformReaderImage(
    current: ReaderImageTransform,
    viewportWidth: Int,
    viewportHeight: Int,
    imageWidth: Int,
    imageHeight: Int,
    centroidX: Float,
    centroidY: Float,
    zoomFactor: Float,
    gesturePanX: Float = 0f,
    gesturePanY: Float = 0f
): ReaderImageTransform {
    val origin = clampReaderImageTransform(current, viewportWidth, viewportHeight, imageWidth, imageHeight)
    val zoom = zoomFactor.takeIf { it.isFinite() && it > 0f } ?: 1f
    val nextScale = (origin.scale * zoom).coerceIn(1f, 5f)
    val ratio = nextScale / origin.scale
    val x = (centroidX - viewportWidth / 2f).takeIf { it.isFinite() } ?: 0f
    val y = (centroidY - viewportHeight / 2f).takeIf { it.isFinite() } ?: 0f
    return clampReaderImageTransform(
        ReaderImageTransform(
            nextScale,
            x + (origin.panX - x) * ratio + gesturePanX,
            y + (origin.panY - y) * ratio + gesturePanY
        ),
        viewportWidth, viewportHeight, imageWidth, imageHeight
    )
}
