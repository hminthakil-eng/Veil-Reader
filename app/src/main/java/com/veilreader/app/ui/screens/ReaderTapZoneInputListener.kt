package com.veilreader.app.ui.screens

import com.veilreader.app.domain.ReaderTapAction
import com.veilreader.app.domain.ReaderTapGrid
import com.veilreader.app.domain.readerTapZoneAt
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Veil-owned 3×3 tap matrix.
 *
 * It is renderer-safe by design: zones mapped to RENDERER return false so links,
 * selection and publication-owned interactions retain control. Page actions are
 * delegated back to ReaderScreen so PAPER_CURL and SLIDE keep their native Veil
 * transitions instead of being replaced by raw locator jumps.
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
) : InputListener {
    override fun onTap(event: TapEvent): Boolean {
        if (!isEnabled()) return false

        val view = navigator.publicationView
        val zone = readerTapZoneAt(
            x = event.point.x,
            y = event.point.y,
            width = view.width.toFloat(),
            height = view.height.toFloat()
        ) ?: return false

        return when (grid()[zone]) {
            ReaderTapAction.PREVIOUS_PAGE ->
                if (canTurnPages()) onPreviousPage() else false
            ReaderTapAction.NEXT_PAGE ->
                if (canTurnPages()) onNextPage() else false
            ReaderTapAction.TOGGLE_CONTROLS ->
                onToggleControls()
            ReaderTapAction.RENDERER ->
                false
        }
    }
}
