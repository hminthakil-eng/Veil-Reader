package com.veilreader.app.ui.screens

import com.veilreader.app.domain.ReaderTapAction
import com.veilreader.app.domain.ReaderTapGrid
import com.veilreader.app.domain.readerTapZoneAt
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.ExperimentalReadiumApi

internal enum class ReaderTapZoneDisposition {
    CONSUMED,
    RENDERER,
    DEFER
}

/**
 * Veil-owned 3×3 tap matrix.
 *
 * RENDERER is a first-class disposition, not merely an unhandled tap. ReaderInputArbiter
 * can therefore bypass every Veil page-turn delegate and return the event directly to the
 * publication. This prevents a customized "book interaction" zone from being stolen by
 * paper-curl, slide or static edge navigation.
 */
@OptIn(ExperimentalReadiumApi::class)
internal class ReaderTapZoneInputListener(
    private val navigator: OverflowableNavigator,
    private val grid: () -> ReaderTapGrid,
    private val isEnabled: () -> Boolean,
    private val canTurnPages: () -> Boolean,
    private val onPreviousPage: () -> Boolean,
    private val onNextPage: () -> Boolean,
    private val onToggleControls: () -> Boolean
) {
    fun routeTap(event: TapEvent): ReaderTapZoneDisposition {
        if (!isEnabled()) return ReaderTapZoneDisposition.DEFER

        val view = navigator.publicationView
        val zone = readerTapZoneAt(
            x = event.point.x,
            y = event.point.y,
            width = view.width.toFloat(),
            height = view.height.toFloat()
        ) ?: return ReaderTapZoneDisposition.DEFER

        return when (grid()[zone]) {
            ReaderTapAction.VEIL_DEFAULT ->
                ReaderTapZoneDisposition.DEFER

            ReaderTapAction.PREVIOUS_PAGE -> {
                if (!canTurnPages()) {
                    ReaderTapZoneDisposition.RENDERER
                } else if (onPreviousPage()) {
                    ReaderTapZoneDisposition.CONSUMED
                } else {
                    ReaderTapZoneDisposition.DEFER
                }
            }
            ReaderTapAction.NEXT_PAGE -> {
                if (!canTurnPages()) {
                    ReaderTapZoneDisposition.RENDERER
                } else if (onNextPage()) {
                    ReaderTapZoneDisposition.CONSUMED
                } else {
                    ReaderTapZoneDisposition.DEFER
                }
            }
            ReaderTapAction.TOGGLE_CONTROLS ->
                if (onToggleControls()) {
                    ReaderTapZoneDisposition.CONSUMED
                } else {
                    ReaderTapZoneDisposition.DEFER
                }
            ReaderTapAction.RENDERER ->
                ReaderTapZoneDisposition.RENDERER
        }
    }
}
