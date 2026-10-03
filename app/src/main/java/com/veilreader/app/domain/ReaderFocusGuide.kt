package com.veilreader.app.domain

enum class ReaderFocusGuideMode {
    OFF,
    WINDOW,
    LINE
}

data class ReaderFocusGuideSettings(
    val mode: ReaderFocusGuideMode = ReaderFocusGuideMode.OFF,
    val lastActiveMode: ReaderFocusGuideMode = ReaderFocusGuideMode.WINDOW,
    val verticalPosition: Double = 0.50,
    val bandFraction: Double = 0.18,
    val dimStrength: Double = 0.30
) {
    fun normalized(): ReaderFocusGuideSettings {
        val safeLastActive = when {
            mode != ReaderFocusGuideMode.OFF -> mode
            lastActiveMode == ReaderFocusGuideMode.OFF -> ReaderFocusGuideMode.WINDOW
            else -> lastActiveMode
        }
        return copy(
            lastActiveMode = safeLastActive,
            verticalPosition = verticalPosition
                .takeIf { it.isFinite() }
                ?.coerceIn(0.20, 0.80)
                ?: 0.50,
            bandFraction = bandFraction
                .takeIf { it.isFinite() }
                ?.coerceIn(0.06, 0.36)
                ?: 0.18,
            dimStrength = dimStrength
                .takeIf { it.isFinite() }
                ?.coerceIn(0.08, 0.68)
                ?: 0.30
        )
    }

    fun toggled(): ReaderFocusGuideSettings =
        if (mode == ReaderFocusGuideMode.OFF) {
            copy(mode = lastActiveMode).normalized()
        } else {
            copy(
                mode = ReaderFocusGuideMode.OFF,
                lastActiveMode = mode
            ).normalized()
        }
}

data class ReaderFocusGuideBand(
    val top: Float,
    val bottom: Float
)

fun readerFocusGuideBand(
    viewportHeightPx: Float,
    settings: ReaderFocusGuideSettings
): ReaderFocusGuideBand? {
    if (!viewportHeightPx.isFinite() || viewportHeightPx <= 0f) return null
    val normalized = settings.normalized()
    if (normalized.mode == ReaderFocusGuideMode.OFF) return null

    val heightFraction = when (normalized.mode) {
        ReaderFocusGuideMode.OFF -> return null
        ReaderFocusGuideMode.WINDOW -> normalized.bandFraction
        ReaderFocusGuideMode.LINE -> normalized.bandFraction.coerceAtMost(0.12)
    }
    val bandHeight = viewportHeightPx * heightFraction.toFloat()
    val center = viewportHeightPx * normalized.verticalPosition.toFloat()
    val half = bandHeight / 2f
    val top = (center - half).coerceIn(0f, viewportHeightPx)
    val bottom = (center + half).coerceIn(top, viewportHeightPx)
    return ReaderFocusGuideBand(top = top, bottom = bottom)
}
