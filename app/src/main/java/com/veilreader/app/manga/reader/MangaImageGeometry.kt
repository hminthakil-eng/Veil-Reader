package com.veilreader.app.manga.reader

import kotlin.math.min

enum class MangaImageFitMode {
    FIT_INSIDE,
    FIT_WIDTH
}

data class MangaImageGeometry(
    val sourceWidthPx: Int,
    val sourceHeightPx: Int,
    val viewportWidthPx: Int,
    val viewportHeightPx: Int,
    val fitScale: Double,
    val renderedWidthPx: Double,
    val renderedHeightPx: Double
) {
    init {
        require(sourceWidthPx > 0 && sourceHeightPx > 0)
        require(viewportWidthPx > 0 && viewportHeightPx > 0)
        require(fitScale.isFinite() && fitScale > 0.0)
        require(renderedWidthPx.isFinite() && renderedWidthPx > 0.0)
        require(renderedHeightPx.isFinite() && renderedHeightPx > 0.0)
    }
}

/**
 * Decode-ready geometry guard.
 *
 * Returns null until both source and viewport dimensions are known and positive. This prevents
 * divide-by-zero, Infinity/NaN scale and zero-height measurement for tall webtoon pages.
 */
object MangaImageGeometryCalculator {

    fun calculate(
        sourceWidthPx: Int,
        sourceHeightPx: Int,
        viewportWidthPx: Int,
        viewportHeightPx: Int,
        fitMode: MangaImageFitMode
    ): MangaImageGeometry? {
        if (
            sourceWidthPx <= 0 ||
            sourceHeightPx <= 0 ||
            viewportWidthPx <= 0 ||
            viewportHeightPx <= 0
        ) {
            return null
        }

        val widthScale = viewportWidthPx.toDouble() / sourceWidthPx.toDouble()
        val heightScale = viewportHeightPx.toDouble() / sourceHeightPx.toDouble()
        val scale = when (fitMode) {
            MangaImageFitMode.FIT_INSIDE -> min(widthScale, heightScale)
            MangaImageFitMode.FIT_WIDTH -> widthScale
        }

        if (!scale.isFinite() || scale <= 0.0) return null

        val renderedWidth = sourceWidthPx * scale
        val renderedHeight = sourceHeightPx * scale
        if (
            !renderedWidth.isFinite() ||
            !renderedHeight.isFinite() ||
            renderedWidth <= 0.0 ||
            renderedHeight <= 0.0
        ) {
            return null
        }

        return MangaImageGeometry(
            sourceWidthPx = sourceWidthPx,
            sourceHeightPx = sourceHeightPx,
            viewportWidthPx = viewportWidthPx,
            viewportHeightPx = viewportHeightPx,
            fitScale = scale,
            renderedWidthPx = renderedWidth,
            renderedHeightPx = renderedHeight
        )
    }
}
