package com.veilreader.rd

enum class ReadingRulerStyle { SINGLE_LINE, THREE_LINE, BAND, WINDOW }

data class ReadingRulerConfig(
    val style: ReadingRulerStyle,
    val centerFraction: Double = 0.5,
    val heightFraction: Double = 0.12,
    val opacity: Double = 0.55
) {
    fun normalized(): ReadingRulerConfig = copy(
        centerFraction = centerFraction.coerceIn(0.0, 1.0),
        heightFraction = heightFraction.coerceIn(0.02, 0.8),
        opacity = opacity.coerceIn(0.0, 1.0)
    )
}

data class FocusWindow(val topPx: Float, val bottomPx: Float)

object ReadingRulerGeometry {
    fun window(viewportHeightPx: Float, config: ReadingRulerConfig): FocusWindow {
        if (viewportHeightPx <= 0f) return FocusWindow(0f, 0f)
        val safe = config.normalized()
        val height = viewportHeightPx * safe.heightFraction.toFloat()
        val center = viewportHeightPx * safe.centerFraction.toFloat()
        val top = (center - height / 2f).coerceIn(0f, viewportHeightPx)
        val bottom = (center + height / 2f).coerceIn(top, viewportHeightPx)
        return FocusWindow(top, bottom)
    }
}
