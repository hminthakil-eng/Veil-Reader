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

@OptIn(ExperimentalReadiumApi::class)
internal class PaperCurlInputListener(
    private val navigator: OverflowableNavigator,
    private val state: PaperCurlState,
    private val scope: CoroutineScope,
    private val onInteraction: () -> Unit,
    private val onCommittedTurn: () -> Unit
) : InputListener {
    private var activeDrag: TurnSpec? = null
    private var navigationJob: Job? = null
    private var navigationSucceeded = false

    override fun onTap(event: TapEvent): Boolean {
        if (navigator.overflow.value.scroll) return false
        if (state.active) return true
        val spec = resolveTurn(event.point.x) ?: return false
        if (!state.begin(navigator.publicationView, spec.side, spec.direction)) {
            return false
        }

        onInteraction()
        scope.launch {
            delay(FRAME_DELAY_MS)
            val moved = navigate(spec.direction)
            if (!moved) {
                state.animateBoundaryBounce()
                state.clear()
                return@launch
            }

            delay(PAGE_REVEAL_DELAY_MS)
            state.animateTapTurn()
            onCommittedTurn()
            state.clear()
        }
        return true
    }

    override fun onDrag(event: DragEvent): Boolean {
        if (navigator.overflow.value.scroll) return false
        if (state.active && activeDrag == null) return true
        return when (event.type) {
            DragEvent.Type.Start -> onDragStart(event)
            DragEvent.Type.Move -> onDragMove(event)
            DragEvent.Type.End -> onDragEnd(event)
        }
    }

    private fun onDragStart(event: DragEvent): Boolean {
        if (state.active) return true
        val spec = resolveTurn(event.start.x) ?: return false
        if (!isMostlyHorizontal(event)) return false
        if (!isMovingInward(spec.side, event.offset.x)) return false

        if (!state.begin(navigator.publicationView, spec.side, spec.direction)) {
            return false
        }
        activeDrag = spec
        navigationSucceeded = false
        state.updateDrag(event.start, event.offset)
        onInteraction()

        navigationJob = scope.launch {
            delay(FRAME_DELAY_MS)
            navigationSucceeded = navigate(spec.direction)
        }
        return true
    }

    private fun onDragMove(event: DragEvent): Boolean {
        if (activeDrag == null || !state.active) return false
        state.updateDrag(event.start, event.offset)
        return true
    }

    private fun onDragEnd(event: DragEvent): Boolean {
        val spec = activeDrag ?: return false
        state.updateDrag(event.start, event.offset)

        val width = navigator.publicationView.width.toFloat()
        val density = navigator.publicationView.resources.displayMetrics.density
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

        scope.launch {
            navigationJob?.join()

            when {
                commit && navigationSucceeded -> {
                    state.animateComplete()
                    onCommittedTurn()
                }

                navigationSucceeded -> {
                    navigate(opposite(spec.direction))
                    delay(PAGE_REVEAL_DELAY_MS)
                    state.animateCancel()
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

    private fun resolveTurn(x: Float): TurnSpec? {
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
        val progression = navigator.overflow.value.readingProgression
        val direction = paperTurnDirectionFor(
            side = side,
            progression = progression
        )
        return TurnSpec(direction, side)
    }

    private fun isMostlyHorizontal(event: DragEvent): Boolean {
        val x = abs(event.offset.x)
        val y = abs(event.offset.y)
        if (x < 4f && y < 4f) return true
        return x >= y * 0.85f
    }

    private fun isMovingInward(
        side: PaperCurlSide,
        offsetX: Float
    ): Boolean {
        if (abs(offsetX) < 4f) return true
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

    private fun opposite(
        direction: PaperTurnDirection
    ): PaperTurnDirection =
        when (direction) {
            PaperTurnDirection.FORWARD -> PaperTurnDirection.BACKWARD
            PaperTurnDirection.BACKWARD -> PaperTurnDirection.FORWARD
        }

    private fun resetDrag() {
        activeDrag = null
        navigationJob = null
        navigationSucceeded = false
    }

    private data class TurnSpec(
        val direction: PaperTurnDirection,
        val side: PaperCurlSide
    )

    private companion object {
        const val EDGE_FRACTION = 0.24f
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
    val commitDistance = max(92f * density, width * 0.20f)
    return inwardDistance >= commitDistance ||
        curlProgress >= 0.34f
}
