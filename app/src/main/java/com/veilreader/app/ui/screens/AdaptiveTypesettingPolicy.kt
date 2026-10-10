package com.veilreader.app.ui.screens

import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderColumnMode

/** Effective layout only: never persist viewport constraints over the user's preference. */
internal object AdaptiveTypesettingPolicy {
    // Two reading columns need at least two comfortable phone-width text measures.
    private const val MIN_COLUMN_WIDTH_DP = 320.0

    fun resolve(
        appearance: ReaderAppearance,
        format: BookFormat,
        fixedLayout: Boolean,
        viewportWidthDp: Double,
        accessibilityFontScale: Double
    ): ReaderAppearance {
        if (format != BookFormat.EPUB || fixedLayout) return appearance
        val safe = appearance.normalized()
        val systemScale = accessibilityFontScale
            .takeIf { it.isFinite() && it > 0.0 } ?: 1.0
        val width = viewportWidthDp.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
        val requiredWidth = 2 * MIN_COLUMN_WIDTH_DP *
            maxOf(1.0, safe.fontScale) * maxOf(1.0, systemScale)
        val columns = when {
            safe.scroll || safe.columnMode == ReaderColumnMode.ONE -> ReaderColumnMode.ONE
            width < requiredWidth -> ReaderColumnMode.ONE
            else -> ReaderColumnMode.TWO
        }
        return safe.copy(columnMode = columns)
    }
}
