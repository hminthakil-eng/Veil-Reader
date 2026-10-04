package com.veilreader.app.ui.reader.material

import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.PageTurnStyle

internal enum class MaterialMotionPath { LEGACY, LIVE_EDGE, DEFORMED_SHEET }
internal fun materialMotionPath(
    enabled: Boolean, format: BookFormat, fixedLayout: Boolean, scroll: Boolean,
    style: PageTurnStyle, reducedMotion: Boolean
): MaterialMotionPath = when {
    !enabled || format != BookFormat.EPUB || fixedLayout || scroll || style != PageTurnStyle.PAPER -> MaterialMotionPath.LEGACY
    reducedMotion -> MaterialMotionPath.LIVE_EDGE
    else -> MaterialMotionPath.DEFORMED_SHEET
}
