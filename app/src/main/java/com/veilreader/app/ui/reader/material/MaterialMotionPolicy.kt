package com.veilreader.app.ui.reader.material

import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.PageTurnStyle

internal enum class MaterialMotionPath { LEGACY, LIVE_EDGE, DEFORMED_SHEET }
internal fun materialMotionPath(
    enabled: Boolean, format: BookFormat, fixedLayout: Boolean, scroll: Boolean,
    style: PageTurnStyle, reducedMotion: Boolean, sdkInt: Int = 29
): MaterialMotionPath = when {
    !enabled || format != BookFormat.EPUB || fixedLayout || scroll || style != PageTurnStyle.PAPER -> MaterialMotionPath.LEGACY
    // Canvas.drawVertices is hardware accelerated from API 29. Older phones keep live text
    // and the premium edge path, without forcing expensive software rendering every frame.
    reducedMotion || sdkInt < 29 -> MaterialMotionPath.LIVE_EDGE
    else -> MaterialMotionPath.DEFORMED_SHEET
}
