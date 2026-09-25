import android.view.HapticFeedbackConstants
package com.veilreader.app.ui.screens

import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.input.DragEvent
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator

/**
 * Interactive paper turn driven through Readium's own input pipeline.
 *
 * No Android overlay owns touch input. Readium remains authoritative for gestures.
 * During a drag, the destination may be previewed underneath the captured source page;
 * persistence/counting is deferred until commit and an exact start locator is restored on cancel.
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
    private var activeDrag: TurnSpec? = null
    private var navigationJob: Job? = null
    private var previewNavigationSucceeded = false
    private var dragStartLocator: Locator? = null
    private var commitHapticSent = false

    override fun onTap(event: TapEvent): Boolean {
        if (!paperModeEnabled()) return false
        if (state.active) return true

        val spec = resolveEdgeTurn(event.point.x) ?: return false
        val visualReady = state.begin(
            view = navigator.publicationView,
            side = spec.side,
            direction = spec.direction
        )

        onInteraction()

        // Navigation must never depend on the visual layer succeeding.
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
                delay(PAGE_REVEAL_DELAY_MS)
                state.animateTapTurn()
                state.clear()
            }
        }
        return true
    }

    override fun onDrag(event: DragEvent): Boolean {
        if (!paperModeEnabled()) return false

        // Lock out overlapping gestures while a tap-turn animation is running.
        if (state.active && activeDrag == null) return true

        return when (event.type) {
            DragEvent.Type.Start -> onDragStart(event)
            DragEvent.Type.Move -> onDragMove(event)
            DragEvent.Type.End -> onDragEnd(event)
        }
    }

    private fun onDragStart(event: DragEvent): Boolean {
        if (state.active) return true
        if (!isMostlyHorizontal(event)) return false

        val spec = resolveDragTurn(event) ?: return false
        if (!state.begin(navigator.publicationView, spec.side, spec.direction)) {
            return false
        }

        activeDrag = spec
        dragStartLocator = navigator.currentLocator.value
        previewNavigationSucceeded = false
        commitHapticSent = false
        state.updateDrag(event.start, event.offset)
        onInteraction()

        // Let the captured source page become visible first, then reveal the live
        // destination underneath it. This stays inside Readium's input pipeline.
        navigationJob = scope.launch {
            delay(FRAME_DELAY_MS)
            previewNavigationSucceeded = navigate(spec.direction)
        }
        return true
    }

    private fun onDragMove(event: DragEvent): Boolean {
        val spec = activeDrag ?: return false
        if (!state.active) return false
        state.updateDrag(event.start, event.offset)
        maybeSignalCommitThreshold(spec, event)
        return true
    }

    private fun onDragEnd(event: DragEvent): Boolean {
        val spec = activeDrag ?: return false
        state.updateDrag(event.start, event.offset)

        val view = navigator.publicationView
        val width = view.width.toFloat()
        val density = view.resources.displayMetrics.density
        val inward = when (spec.side) {
            PaperCurlSide.RIGHT -> -event.offset.x
            PaperCurlSide.LEFT -> event.offset.x
        }
        val commit = shouldCommitPaperTurn(
            inwardDistance = inward,
            width = width,
            density = density,
            curlProgress = state.dragProgress()
        )
        if (commit) signalCommitThreshold()

        scope.launch {
            navigationJob?.join()

            when {
                commit && previewNavigationSucceeded -> {
                    // Persist/count the committed destination before finishing the visual tail.
                    onCommittedTurn()
                    state.animateComplete()
                }

                previewNavigationSucceeded -> {
                    restoreDragStart(spec)
                    delay(PAGE_REVEAL_DELAY_MS)
                    state.animateCancel()
                }

                commit -> {
                    // End-of-book / navigation refusal should still feel intentional.
                    state.animateBoundaryBounce()
                }

                else -> {
                    state.animateCancel()
                }
            }

            resetDrag()
            state.clear()
        }
        return true
    }

    private fun maybeSignalCommitThreshold(
        spec: TurnSpec,
        event: DragEvent
    ) {
        if (commitHapticSent) return
        val view = navigator.publicationView
        val width = view.width.toFloat()
        if (width <= 0f) return
        val inward = when (spec.side) {
            PaperCurlSide.RIGHT -> -event.offset.x
            PaperCurlSide.LEFT -> event.offset.x
        }
        val crossed = shouldCommitPaperTurn(
            inwardDistance = inward,
            width = width,
            density = view.resources.displayMetrics.density,
            curlProgress = state.dragProgress()
        )
        if (crossed) signalCommitThreshold()
    }

    private fun signalCommitThreshold() {
        if (commitHapticSent) return
        commitHapticSent = true
        navigator.publicationView.performHapticFeedback(
            HapticFeedbackConstants.CLOCK_TICK
        )
    }

    private fun restoreDragStart(spec: TurnSpec) {
        val exact = dragStartLocator
        val restored = exact?.let {
            navigator.go(it, animated = false)
        } ?: false

        if (!restored) {
            // Defensive fallback if a transient locator cannot be restored.
            navigate(opposite(spec.direction))
        }
    }

    private fun resolveDragTurn(event: DragEvent): TurnSpec? {
        val edgeTurn = resolveEdgeTurn(event.start.x)
        if (edgeTurn != null && isMovingInward(edgeTurn.side, event.offset.x)) {
            return edgeTurn
        }

        // Allow a natural horizontal swipe from the page body, not only a tiny edge target.
        val side = when {
            event.offset.x < -DRAG_DIRECTION_SLOP_PX -> PaperCurlSide.RIGHT
            event.offset.x > DRAG_DIRECTION_SLOP_PX -> PaperCurlSide.LEFT
            else -> return null
        }
        return TurnSpec(
            direction = paperTurnDirectionFor(
                side = side,
                progression = navigator.overflow.value.readingProgression
            ),
            side = side
        )
    }

    private fun resolveEdgeTurn(x: Float): TurnSpec? {
        val view = navigator.publicationView
        val width = view.width.toFloat()
        if (width <= 0f) return null

        val density = view.resources.displayMetrics.density
        val edgeSize = max(84f * density, width * EDGE_FRACTION)
        val side = when {
            x <= edgeSize -> PaperCurlSide.LEFT
            x >= width - edgeSize -> PaperCurlSide.RIGHT
            else -> return null
        }
        return TurnSpec(
            direction = paperTurnDirectionFor(
                side = side,
                progression = navigator.overflow.value.readingProgression
            ),
            side = side
        )
    }

    private fun paperModeEnabled(): Boolean =
        !navigator.overflow.value.scroll && isEnabled()

    private fun isMostlyHorizontal(event: DragEvent): Boolean {
        val x = abs(event.offset.x)
        val y = abs(event.offset.y)
        if (x < 4f && y < 4f) return true
        return x >= y * HORIZONTAL_BIAS
    }

    private fun isMovingInward(
        side: PaperCurlSide,
        offsetX: Float
    ): Boolean {
        if (abs(offsetX) < DRAG_DIRECTION_SLOP_PX) return true
        return when (side) {
            PaperCurlSide.RIGHT -> offsetX < 0f
            PaperCurlSide.LEFT -> offsetX > 0f
        }
    }

    private fun navigate(direction: PaperTurnDirection): Boolean =
        when (direction) {
            PaperTurnDirection.FORWARD ->
                navigator.goForward(animated = false)
            PaperTurnDirection.BACKWARD ->
                navigator.goBackward(animated = false)
        }

    private fun opposite(direction: PaperTurnDirection): PaperTurnDirection =
        when (direction) {
            PaperTurnDirection.FORWARD -> PaperTurnDirection.BACKWARD
            PaperTurnDirection.BACKWARD -> PaperTurnDirection.FORWARD
        }

    private fun resetDrag() {
        activeDrag = null
        navigationJob = null
        previewNavigationSucceeded = false
        dragStartLocator = null
        commitHapticSent = false
    }

    private data class TurnSpec(
        val direction: PaperTurnDirection,
        val side: PaperCurlSide
    )

    private companion object {
        const val EDGE_FRACTION = 0.24f
        const val HORIZONTAL_BIAS = 0.90f
        const val DRAG_DIRECTION_SLOP_PX = 4f
        const val FRAME_DELAY_MS = 18L
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

internal fun shouldCommitPaperTurn(
    inwardDistance: Float,
    width: Float,
    density: Float,
    curlProgress: Float
): Boolean {
    // Returning to the origin or dragging outward must never commit a turn.
    if (inwardDistance <= 0f) return false
    val commitDistance = max(92f * density, width * 0.20f)
    return inwardDistance >= commitDistance ||
        curlProgress >= 0.34f
}
