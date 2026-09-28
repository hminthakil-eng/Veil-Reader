package com.veilreader.app.ui.screens

import android.os.SystemClock
import android.view.HapticFeedbackConstants
import com.veilreader.app.ui.theme.VeilMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.input.DragEvent
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import kotlin.math.abs
import kotlin.math.max

@OptIn(ExperimentalReadiumApi::class)
internal class PageSlideInputListener(
    private val navigator: OverflowableNavigator,
    private val state: PageSlideState,
    private val isEnabled: () -> Boolean,
    private val scope: CoroutineScope,
    private val isReducedMotion: () -> Boolean = { false },
    private val onInteraction: () -> Unit,
    private val onCommittedTurn: () -> Unit
) : InputListener {
    private var activeDrag: TurnSpec? = null
    private var reserved = false
    private var navigationJob: Job? = null
    private var previewSucceeded = false
    private var startLocator: Locator? = null
    private var commitHapticSent = false
    private var lastSampleAt = 0L
    private var lastInward = 0f
    private var releaseVelocity = 0f

    override fun onTap(event: TapEvent): Boolean {
        if (!modeEnabled() || state.active) return state.active
        val spec = resolveEdgeTurn(event.point.x) ?: return false
        val visualReady = state.begin(navigator.publicationView, spec.side, spec.direction)
        onInteraction()

        val moved = navigate(spec.direction)
        if (!moved) {
            if (visualReady) scope.launch {
                state.animateBoundaryBounce()
                state.clear()
            }
            return true
        }

        onCommittedTurn()
        if (visualReady) scope.launch {
            if (!isReducedMotion()) state.animateTapTurn()
            state.clear()
        }
        return true
    }

    override fun onDrag(event: DragEvent): Boolean {
        if (!modeEnabled()) return false
        if (state.active && activeDrag == null) return true
        return when (event.type) {
            DragEvent.Type.Start -> {
                reserved = true
                beginIfReady(event)
                true
            }
            DragEvent.Type.Move -> {
                if (!reserved && activeDrag == null) return false
                val spec = activeDrag ?: run {
                    beginIfReady(event)
                    return true
                }
                if (state.active) {
                    state.updateDrag(event.offset.x)
                    sampleVelocity(spec, event)
                    maybeHaptic(spec, event)
                }
                true
            }
            DragEvent.Type.End -> finishDrag(event)
        }
    }

    private fun finishDrag(event: DragEvent): Boolean {
        val spec = activeDrag
        if (spec == null) {
            reset()
            return true
        }

        state.updateDrag(event.offset.x)
        sampleVelocity(spec, event)
        val view = navigator.publicationView
        val density = view.resources.displayMetrics.density
        val commit = shouldCommitPageSlideTurn(
            inwardDistance = inwardDistance(spec, event),
            width = view.width.toFloat(),
            density = density,
            releaseVelocityPxPerSec = releaseVelocity
        )
        if (commit) signalHaptic()

        scope.launch {
            navigationJob?.join()
            when {
                commit && previewSucceeded -> {
                    onCommittedTurn()
                    if (!isReducedMotion()) {
                        state.animateComplete(releaseVelocity / density.coerceAtLeast(0.1f))
                    }
                }
                previewSucceeded -> {
                    restoreStart(spec)
                    if (!isReducedMotion()) state.animateCancel()
                }
                commit -> if (!isReducedMotion()) state.animateBoundaryBounce()
                else -> if (!isReducedMotion()) state.animateCancel()
            }
            reset()
            state.clear()
        }
        return true
    }

    private fun beginIfReady(event: DragEvent): Boolean {
        if (activeDrag != null || state.active) return activeDrag != null
        if (!isMostlyHorizontal(event)) return false
        val spec = resolveDragTurn(event) ?: return false
        if (!state.begin(navigator.publicationView, spec.side, spec.direction)) return false

        activeDrag = spec
        startLocator = navigator.currentLocator.value
        previewSucceeded = false
        commitHapticSent = false
        lastSampleAt = SystemClock.uptimeMillis()
        lastInward = inwardDistance(spec, event)
        releaseVelocity = 0f
        state.updateDrag(event.offset.x)
        onInteraction()

        navigationJob = scope.launch {
            delay(VeilMotion.FRAME_SETTLE_MS)
            previewSucceeded = navigate(spec.direction)
        }
        return true
    }

    private fun resolveDragTurn(event: DragEvent): TurnSpec? {
        val edge = resolveEdgeTurn(event.start.x)
        if (edge != null && movingInward(edge.side, event.offset.x)) return edge
        val side = when {
            event.offset.x < -DIRECTION_SLOP_PX -> PaperCurlSide.RIGHT
            event.offset.x > DIRECTION_SLOP_PX -> PaperCurlSide.LEFT
            else -> return null
        }
        return TurnSpec(paperTurnDirectionFor(side, navigator.overflow.value.readingProgression), side)
    }

    private fun resolveEdgeTurn(x: Float): TurnSpec? {
        val view = navigator.publicationView
        val width = view.width.toFloat()
        if (width <= 0f) return null
        val edgeSize = max(84f * view.resources.displayMetrics.density, width * EDGE_FRACTION)
        val side = when {
            x <= edgeSize -> PaperCurlSide.LEFT
            x >= width - edgeSize -> PaperCurlSide.RIGHT
            else -> return null
        }
        return TurnSpec(paperTurnDirectionFor(side, navigator.overflow.value.readingProgression), side)
    }

    private fun maybeHaptic(spec: TurnSpec, event: DragEvent) {
        if (commitHapticSent) return
        val view = navigator.publicationView
        if (shouldCommitPageSlideTurn(
                inwardDistance = inwardDistance(spec, event),
                width = view.width.toFloat(),
                density = view.resources.displayMetrics.density,
                releaseVelocityPxPerSec = releaseVelocity
            )
        ) signalHaptic()
    }

    private fun sampleVelocity(spec: TurnSpec, event: DragEvent) {
        val now = SystemClock.uptimeMillis()
        val inward = inwardDistance(spec, event)
        val elapsed = now - lastSampleAt
        if (lastSampleAt > 0L && elapsed in 1L..120L) {
            releaseVelocity = ((inward - lastInward) * 1000f / elapsed.toFloat())
                .coerceIn(-12_000f, 12_000f)
        }
        lastSampleAt = now
        lastInward = inward
    }

    private fun signalHaptic() {
        if (commitHapticSent) return
        commitHapticSent = true
        navigator.publicationView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    private fun restoreStart(spec: TurnSpec) {
        val restored = startLocator?.let { navigator.go(it, animated = false) } ?: false
        if (!restored) navigate(opposite(spec.direction))
    }

    private fun inwardDistance(spec: TurnSpec, event: DragEvent): Float =
        if (spec.side == PaperCurlSide.RIGHT) -event.offset.x else event.offset.x

    private fun navigate(direction: PaperTurnDirection): Boolean =
        if (direction == PaperTurnDirection.FORWARD) navigator.goForward(animated = false)
        else navigator.goBackward(animated = false)

    private fun opposite(direction: PaperTurnDirection): PaperTurnDirection =
        if (direction == PaperTurnDirection.FORWARD) PaperTurnDirection.BACKWARD
        else PaperTurnDirection.FORWARD

    private fun modeEnabled(): Boolean = !navigator.overflow.value.scroll && isEnabled()

    private fun isMostlyHorizontal(event: DragEvent): Boolean {
        val x = abs(event.offset.x)
        val y = abs(event.offset.y)
        if (x < 4f && y < 4f) return true
        return x >= y * HORIZONTAL_BIAS
    }

    private fun movingInward(side: PaperCurlSide, x: Float): Boolean {
        if (abs(x) < DIRECTION_SLOP_PX) return true
        return if (side == PaperCurlSide.RIGHT) x < 0f else x > 0f
    }

    private fun reset() {
        activeDrag = null
        reserved = false
        navigationJob = null
        previewSucceeded = false
        startLocator = null
        commitHapticSent = false
        lastSampleAt = 0L
        lastInward = 0f
        releaseVelocity = 0f
    }

    private data class TurnSpec(val direction: PaperTurnDirection, val side: PaperCurlSide)

    private companion object {
        const val EDGE_FRACTION = 0.22f
        const val HORIZONTAL_BIAS = 0.90f
        const val DIRECTION_SLOP_PX = 4f
    }
}
