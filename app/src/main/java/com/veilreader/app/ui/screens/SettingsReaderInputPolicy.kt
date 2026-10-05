package com.veilreader.app.ui.screens

import com.veilreader.app.domain.ReaderTapAction

internal fun materialPageReviewDebugOverride(enabled: Boolean): Boolean? =
    if (enabled) true else null

internal fun nextReaderTapAction(action: ReaderTapAction): ReaderTapAction =
    when (action) {
        ReaderTapAction.VEIL_DEFAULT -> ReaderTapAction.PREVIOUS_PAGE
        ReaderTapAction.PREVIOUS_PAGE -> ReaderTapAction.TOGGLE_CONTROLS
        ReaderTapAction.TOGGLE_CONTROLS -> ReaderTapAction.NEXT_PAGE
        ReaderTapAction.NEXT_PAGE -> ReaderTapAction.RENDERER
        ReaderTapAction.RENDERER -> ReaderTapAction.VEIL_DEFAULT
    }

internal fun readerTapActionGlyph(action: ReaderTapAction): String =
    when (action) {
        ReaderTapAction.VEIL_DEFAULT -> "V"
        ReaderTapAction.PREVIOUS_PAGE -> "←"
        ReaderTapAction.TOGGLE_CONTROLS -> "◎"
        ReaderTapAction.NEXT_PAGE -> "→"
        ReaderTapAction.RENDERER -> "·"
    }
