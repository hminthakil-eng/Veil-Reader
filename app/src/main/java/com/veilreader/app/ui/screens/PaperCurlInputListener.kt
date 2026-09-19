package com.veilreader.app.ui.screens

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Reliable visual-only paper turn.
 *
 * Navigation is committed by Readium first. The captured old page is then curled
 * away on top of the already-live destination page. If the visual layer fails,
 * navigation still succeeds.
 *
 * Drag/swipe is intentionally not intercepted here; Readium owns that path.
 */
@OptIn(ExperimentalReadiumApi::class)
internal class PaperCurlInputListener(
    private val navigator: OverflowableNavigator,
    private val state: PaperCurlState,
    private val isEnabled: () -> Boolean,
    private val scope: CoroutineScope,
    private val onInteraction: () -> Unit,
    private val onCommittedTurn: () -> Unit
) : InputListener {

    override fun onTap(event: TapEvent): Boolean {
        if (navigator.overflow.value.scroll) return false
        if (!isEnabled()) return false
        if (state.active) return true

        val spec = resolveTurn(event.point.x) ?: return false

        // Snapshot is optional. Never let visual capture block navigation.
        val visualReady = state.begin(
            view = navigator.publicationView,
            side = spec.side,
            direction = spec.direction
        )

        onInteraction()

        // Commit the real reader navigation first.
        val moved = navigate(spec.direction)
        if (!moved) {
            if (visualReady) {
                scope.launch {
                    state.animateBoundaryBounce()
                    state.clear()
                }
            }
            return true
        }

        onCommittedTurn()

        if (visualReady) {
            scope.launch {
                // One short frame gives Readium time to paint the destination
                // underneath the captured old page.
                delay(PAGE_REVEAL_DELAY_MS)
                state.animateTapTurn()
                state.clear()
            }
        }

        return true
    }

    private fun resolveTurn(x: Float): TurnSpec? {
        val view = navigator.publicationView
        val width = view.width.toFloat()
        if (width <= 0f) return null

        val density = view.resources.displayMetrics.density
        val edgeSize = kotlin.math.max(
            84f * density,
            width * EDGE_FRACTION
        )
        val side = when {
            x <= edgeSize -> PaperCurlSide.LEFT
            x >= width - edgeSize -> PaperCurlSide.RIGHT
            else -> return null
        }

        val direction = paperTurnDirectionFor(
            side = side,
            progression = navigator.overflow.value.readingProgression
        )
        return TurnSpec(direction, side)
    }

    private fun navigate(direction: PaperTurnDirection): Boolean =
        when (direction) {
            PaperTurnDirection.FORWARD ->
                navigator.goForward(animated = false)

            PaperTurnDirection.BACKWARD ->
                navigator.goBackward(animated = false)
        }

    private data class TurnSpec(
        val direction: PaperTurnDirection,
        val side: PaperCurlSide
    )

    private companion object {
        const val EDGE_FRACTION = 0.24f
        const val PAGE_REVEAL_DELAY_MS = 28L
    }
}

internal fun paperTurnDirectionFor(
    side: PaperCurlSide,
    progression: ReadingProgression
): PaperTurnDirection =
    when (side) {
        PaperCurlSide.RIGHT ->
            if (progression == ReadingProgression.LTR) {
                PaperTurnDirection.FORWARD
            } else {
                PaperTurnDirection.BACKWARD
            }

        PaperCurlSide.LEFT ->
            if (progression == ReadingProgression.LTR) {
                PaperTurnDirection.BACKWARD
            } else {
                PaperTurnDirection.FORWARD
            }
    }
