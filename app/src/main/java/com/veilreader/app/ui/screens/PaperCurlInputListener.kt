package com.veilreader.app.ui.screens

import android.os.SystemClock
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.veilreader.app.ui.theme.VeilMotion
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
    private val isReducedMotion: () -> Boolean = { false },
    private val onInteraction: () -> Unit,
    private val onCommittedTurn: () -> Unit
) : InputListener {
    private var activeDrag: TurnSpec? = null
    private var dragReserved = false
    private var navigationJob: Job? = null
    private var completionJob: Job? = null
    private var cancellationRequested = false
    private var turnCommitted = false
    private var previewNavigationSucceeded = false
    private var dragStartLocator: Locator? = null
    private var lastDragSampleAtMillis = 0L
    private var lastMotionAtMillis = 0L
    private var lastInwardDistance = 0f
    private var releaseVelocityPxPerSec = 0f

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

        // The Reader's sensory layer gates feedback with the user's settings.
        onCommittedTurn()

        if (visualReady) {
            scope.launch {
                if (isReducedMotion()) {
                    state.clear()
                } else {
                    delay(VeilMotion.PAGE_REVEAL_MS)
                    state.animateTapTurn()
                    state.clear()
                }
            }
        }
        return true
    }

    override fun onDrag(event: DragEvent): Boolean {
        if (!paperModeEnabled()) {
            // The mode may change while a sheet is lifted. Restore its starting
            // locator before allowing the new navigation mode to own later drags.
            return cancelPendingTurn()
        }

        // Lock out overlapping gestures while a turn finishes or a tap animates.
        if (completionJob != null || (state.active && activeDrag == null)) return true

        return when (event.type) {
            DragEvent.Type.Start -> onDragStart(event)
            DragEvent.Type.Move -> onDragMove(event)
            DragEvent.Type.End -> onDragEnd(event)
        }
    }

    private fun onDragStart(event: DragEvent): Boolean {
        if (state.active) return true

        // Reserve the whole drag sequence for PAPER mode immediately.
        //
        // Readium sends Start before a body swipe has enough offset to know its
        // direction. Returning false here lets the renderer start its native slide,
        // then our curl can begin on Move — visually mixing SLIDE and CURL.
        //
        // PAPER mode is paginated, so we intentionally claim the gesture at Start
        // and decide the curl direction once horizontal intent is measurable.
        dragReserved = true
        beginReservedDragIfReady(event)
        return true
    }

    private fun onDragMove(event: DragEvent): Boolean {
        if (!dragReserved && activeDrag == null) return false

        val spec = activeDrag ?: run {
            beginReservedDragIfReady(event)
            return true
        }

        if (!state.active) return true
        state.updateDrag(event.start, event.offset)
        sampleReleaseVelocity(spec, event)
        return true
    }

    private fun onDragEnd(event: DragEvent): Boolean {
        if (completionJob != null) return true
        val spec = activeDrag
        if (spec == null) {
            // A reserved gesture that never became a horizontal turn is still
            // consumed so the native renderer cannot finish it as a slide.
            resetDrag()
            return true
        }

        state.updateDrag(event.start, event.offset)
        sampleReleaseVelocity(spec, event)

        val view = navigator.publicationView
        val width = view.width.toFloat()
        val density = view.resources.displayMetrics.density
        val inward = inwardDistance(spec, event)
        val commit = shouldCommitPaperTurn(
            inwardDistance = inward,
            width = width,
            density = density,
            curlProgress = state.dragProgress(),
            releaseVelocityPxPerSec = releaseVelocityPxPerSec
        )
        completionJob = scope.launch {
            navigationJob?.join()

            when {
                cancellationRequested -> {
                    if (previewNavigationSucceeded) restoreDragStart(spec)
                }

                commit && previewNavigationSucceeded -> {
                    // Persist/count and emit sensory feedback only after a real commit.
                    turnCommitted = true
                    onCommittedTurn()
                    if (!isReducedMotion()) {
                        state.animateComplete(
                            releaseVelocityDpPerSec =
                                releaseVelocityPxPerSec / density.coerceAtLeast(0.1f)
                        )
                    }
                }

                previewNavigationSucceeded -> {
                    restoreDragStart(spec)
                    if (!isReducedMotion()) {
                        delay(VeilMotion.PAGE_REVEAL_MS)
                        state.animateCancel()
                    }
                }

                commit -> {
                    // End-of-book / navigation refusal should still feel intentional.
                    if (!isReducedMotion()) state.animateBoundaryBounce()
                }

                else -> {
                    if (!isReducedMotion()) state.animateCancel()
                }
            }

            state.clear()
            resetDrag()
        }
        return true
    }

    /**
     * Called when the Reader pauses or exits PAPER mode. The preview navigator may
     * already be on the next page, so let that navigation finish before restoring
     * the exact locator captured at drag start. A committed turn stays committed.
     */
    fun cancelPendingTurn(): Boolean {
        if (!dragReserved && activeDrag == null) return false
        if (turnCommitted) return false
        cancellationRequested = true
        val spec = activeDrag
        if (spec == null) {
            resetDrag()
            return true
        }
        if (completionJob == null) {
            completionJob = scope.launch {
                navigationJob?.join()
                if (previewNavigationSucceeded) restoreDragStart(spec)
                state.clear()
                resetDrag()
            }
        }
        return true
    }

    /**
     * Cancels an uncommitted preview and does not return until the source locator is restored.
     * Use this before taking a durable locator snapshot.
     */
    suspend fun cancelPendingTurnAndAwait(): Boolean {
        val requested = cancelPendingTurn()
        if (!requested) return false
        completionJob?.join()
        return true
    }

    /**
     * Synchronous teardown for composition/lifecycle disposal where the composition scope may be
     * cancelled before an asynchronous restoration job can run.
     */
    fun forceCancelPendingTurn(): Boolean {
        if (!dragReserved && activeDrag == null) return false
        if (turnCommitted) return false

        cancellationRequested = true
        navigationJob?.cancel()
        completionJob?.cancel()

        val spec = activeDrag
        if (spec != null && previewNavigationSucceeded) {
            restoreDragStart(spec)
        }
        state.clearImmediately()
        resetDrag()
        return true
    }

    private fun beginReservedDragIfReady(event: DragEvent): Boolean {
        if (activeDrag != null || state.active) return activeDrag != null
        if (!isMostlyHorizontal(event)) return false

        val spec = resolveDragTurn(event) ?: return false
        if (!state.begin(navigator.publicationView, spec.side, spec.direction)) {
            // Keep the gesture reserved even if the visual snapshot cannot start;
            // falling through would re-enable native slide in PAPER mode.
            return false
        }

        activeDrag = spec
        dragStartLocator = navigator.currentLocator.value
        previewNavigationSucceeded = false
        lastDragSampleAtMillis = SystemClock.uptimeMillis()
        lastMotionAtMillis = 0L
        lastInwardDistance = inwardDistance(spec, event)
        releaseVelocityPxPerSec = 0f
        state.updateDrag(event.start, event.offset)
        onInteraction()

        // Let the captured source page become visible first, then reveal the live
        // destination underneath it. This stays inside Readium's input pipeline.
        navigationJob = scope.launch {
            delay(VeilMotion.FRAME_SETTLE_MS)
            previewNavigationSucceeded = navigate(spec.direction)
        }
        return true
    }

    private fun inwardDistance(spec: TurnSpec, event: DragEvent): Float =
        when (spec.side) {
            PaperCurlSide.RIGHT -> -event.offset.x
            PaperCurlSide.LEFT -> event.offset.x
        }

    private fun sampleReleaseVelocity(spec: TurnSpec, event: DragEvent) {
        val now = SystemClock.uptimeMillis()
        val inward = inwardDistance(spec, event)
        val delta = inward - lastInwardDistance
        val elapsed = now - lastDragSampleAtMillis
        val sinceMotion = if (lastMotionAtMillis > 0L) {
            now - lastMotionAtMillis
        } else {
            Long.MAX_VALUE
        }
        releaseVelocityPxPerSec = nextPaperReleaseVelocity(
            previousVelocityPxPerSec = releaseVelocityPxPerSec,
            distanceDeltaPx = delta,
            elapsedMillis = elapsed,
            sinceLastMotionMillis = sinceMotion
        )
        if (abs(delta) >= 1f) lastMotionAtMillis = now
        lastDragSampleAtMillis = now
        lastInwardDistance = inward
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
        val edgeSize = pageTurnTapZonePx(
            width = width,
            density = density,
            preferredFraction = EDGE_FRACTION
        )
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
        val view = navigator.publicationView
        val width = view.width.toFloat()
        val density = view.resources.displayMetrics.density
        val edgeZone = pageTurnTapZonePx(
            width = width,
            density = density,
            preferredFraction = EDGE_FRACTION
        )
        val startsAtEdge =
            event.start.x <= edgeZone ||
                event.start.x >= width - edgeZone
        return hasDeliberatePaperIntent(
            offsetX = event.offset.x,
            offsetY = event.offset.y,
            width = width,
            density = density,
            startsAtEdge = startsAtEdge
        )
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
        dragReserved = false
        navigationJob = null
        completionJob = null
        cancellationRequested = false
        turnCommitted = false
        previewNavigationSucceeded = false
        dragStartLocator = null
        lastDragSampleAtMillis = 0L
        lastMotionAtMillis = 0L
        lastInwardDistance = 0f
        releaseVelocityPxPerSec = 0f
    }

    private data class TurnSpec(
        val direction: PaperTurnDirection,
        val side: PaperCurlSide
    )

    private companion object {
        const val EDGE_FRACTION = 0.22f
        const val DRAG_DIRECTION_SLOP_PX = 4f
    }
}

internal fun hasDeliberatePaperIntent(
    offsetX: Float,
    offsetY: Float,
    width: Float,
    density: Float,
    startsAtEdge: Boolean = false
): Boolean {
    if (width <= 0f) return false
    val x = abs(offsetX)
    val y = abs(offsetY)
    val safeDensity = density.coerceAtLeast(0.1f)
    val intentDistance = if (startsAtEdge) {
        max(6f * safeDensity, width * 0.006f)
    } else {
        max(8f * safeDensity, width * 0.009f)
    }
    val horizontalBias = if (startsAtEdge) 0.72f else 1.08f
    return x >= intentDistance && x >= y * horizontalBias
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
    curlProgress: Float,
    releaseVelocityPxPerSec: Float = 0f
): Boolean {
    // Returning to the origin or dragging outward must never commit a turn.
    if (inwardDistance <= 0f) return false
    val safeDensity = density.coerceAtLeast(0.1f)
    val commitDistance = max(96f * safeDensity, width * 0.22f)
    val flickDistance = max(36f * safeDensity, width * 0.035f)
    val fastInwardFlick =
        inwardDistance >= flickDistance &&
            releaseVelocityPxPerSec >= 900f * safeDensity
    return inwardDistance >= commitDistance ||
        curlProgress >= 0.36f ||
        fastInwardFlick
}

/**
 * A terminal drag event commonly repeats the last Move offset. Keep a fresh flick
 * through that duplicate sample, but expire it when the finger has actually paused.
 */
internal fun nextPaperReleaseVelocity(
    previousVelocityPxPerSec: Float,
    distanceDeltaPx: Float,
    elapsedMillis: Long,
    sinceLastMotionMillis: Long
): Float {
    if (abs(distanceDeltaPx) >= 1f) {
        if (elapsedMillis in 1L..120L) {
            return (distanceDeltaPx * 1000f / elapsedMillis.toFloat())
                .coerceIn(-12_000f, 12_000f)
        }
        return if (elapsedMillis == 0L && sinceLastMotionMillis <= 100L) {
            previousVelocityPxPerSec
        } else {
            0f
        }
    }
    return if (sinceLastMotionMillis <= 100L) previousVelocityPxPerSec else 0f
}
