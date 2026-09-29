package com.veilreader.app.ui.screens

import android.os.SystemClock
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.input.DragEvent
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.Key
import org.readium.r2.navigator.input.KeyEvent
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator

/**
 * Veil-owned weighted slide transition.
 *
 * The source page is captured and moved as a physical sheet while the destination is previewed
 * beneath it. Preview locators are not committed by ReaderScreen until this listener confirms the
 * turn. Cancelling restores the exact starting locator.
 */
@OptIn(ExperimentalReadiumApi::class)
internal class SlideNavigationInputListener(
    private val navigator: OverflowableNavigator,
    private val state: SlidePageState,
    private val isEnabled: () -> Boolean,
    private val scope: CoroutineScope,
    private val isReducedMotion: () -> Boolean = { false },
    private val onInteraction: () -> Unit,
    private val onCommittedTurn: () -> Unit
) : InputListener {
    private var reserved = false
    private var activeSpec: TurnSpec? = null
    private var navigationJob: Job? = null
    private var completionJob: Job? = null
    private var previewNavigationSucceeded = false
    private var dragStartLocator: Locator? = null
    private var cancellationRequested = false
    private var turnCommitted = false
    private var lastSampleAtMillis = 0L
    private var lastDistance = 0f
    private var releaseVelocityPxPerSec = 0f

    override fun onTap(event: TapEvent): Boolean {
        if (!slideModeEnabled()) return cancelPendingTurn()
        if (completionJob != null || state.active || reserved) return true
        val spec = resolveEdgeTurn(event.point.x) ?: return false
        performDiscreteTurn(spec)
        return true
    }

    override fun onKey(event: KeyEvent): Boolean {
        if (!slideModeEnabled()) return cancelPendingTurn()
        if (completionJob != null || state.active || reserved) return true
        if (event.type != KeyEvent.Type.Down || event.modifiers.isNotEmpty()) return false

        val progression = navigator.overflow.value.readingProgression
        val spec = when (event.key) {
            Key.ArrowUp -> turnSpecFor(PaperTurnDirection.BACKWARD, progression)
            Key.ArrowDown, Key.Space -> turnSpecFor(PaperTurnDirection.FORWARD, progression)
            Key.ArrowLeft -> TurnSpec(
                paperTurnDirectionFor(PaperCurlSide.LEFT, progression),
                PaperCurlSide.LEFT
            )
            Key.ArrowRight -> TurnSpec(
                paperTurnDirectionFor(PaperCurlSide.RIGHT, progression),
                PaperCurlSide.RIGHT
            )
            else -> return false
        }
        performDiscreteTurn(spec)
        return true
    }

    override fun onDrag(event: DragEvent): Boolean {
        if (!slideModeEnabled()) {
            // Mode ownership can change after the destination preview has already navigated.
            // Restore the captured source before allowing another mode to see this gesture.
            return cancelPendingTurn()
        }
        if (completionJob != null || (state.active && !reserved)) return true

        return when (event.type) {
            DragEvent.Type.Start -> onDragStart()
            DragEvent.Type.Move -> onDragMove(event)
            DragEvent.Type.End -> onDragEnd(event)
        }
    }

    private fun onDragStart(): Boolean {
        reserved = true
        activeSpec = null
        previewNavigationSucceeded = false
        dragStartLocator = navigator.currentLocator.value
        cancellationRequested = false
        turnCommitted = false
        completionJob = null
        lastSampleAtMillis = SystemClock.uptimeMillis()
        lastDistance = 0f
        releaseVelocityPxPerSec = 0f
        if (!isReducedMotion()) {
            state.begin(navigator.publicationView)
        }
        onInteraction()
        return true
    }

    private fun onDragMove(event: DragEvent): Boolean {
        if (!reserved) return false

        val spec = activeSpec ?: resolveDragTurn(event)?.also {
            activeSpec = it
            if (state.active) {
                navigationJob = scope.launch {
                    delay(com.veilreader.app.ui.theme.VeilMotion.FRAME_SETTLE_MS)
                    previewNavigationSucceeded = navigate(it.direction)
                }
            }
        }

        if (spec != null) {
            state.updateDrag(constrainedOffset(spec.side, event.offset.x))
            sampleVelocity(spec, event)
        }
        return true
    }

    private fun onDragEnd(event: DragEvent): Boolean {
        if (!reserved) return false
        val spec = activeSpec ?: resolveDragTurn(event)

        if (spec == null) {
            completionJob = scope.launch {
                if (state.active && !isReducedMotion()) state.animateCancel()
                if (state.active) state.clear()
                resetDrag()
            }
            return true
        }

        state.updateDrag(constrainedOffset(spec.side, event.offset.x))
        sampleVelocity(spec, event)

        val width = navigator.publicationView.width.toFloat()
        val density = navigator.publicationView.resources.displayMetrics.density
        val distance = inwardDistance(spec.side, event.offset.x)
        val commit = shouldCommitSlideTurn(
            inwardDistance = distance,
            width = width,
            density = density,
            slideProgress = state.dragProgress(),
            releaseVelocityPxPerSec = releaseVelocityPxPerSec
        )

        completionJob = scope.launch {
            navigationJob?.join()

            if (cancellationRequested && !turnCommitted) {
                if (previewNavigationSucceeded) restoreDragStart(spec)
                if (state.active && !isReducedMotion()) state.animateCancel()
                if (state.active) state.clear()
                resetDrag()
                return@launch
            }

            if (state.active) {
                when {
                    commit && previewNavigationSucceeded && !cancellationRequested -> {
                        turnCommitted = true
                        onCommittedTurn()
                        if (!isReducedMotion()) {
                            state.animateComplete(
                                directionSign = visualDirectionSign(spec.side),
                                velocityDpPerSec =
                                    releaseVelocityPxPerSec / density.coerceAtLeast(0.1f)
                            )
                        }
                    }

                    previewNavigationSucceeded -> {
                        restoreDragStart(spec)
                        if (!isReducedMotion()) state.animateCancel()
                    }

                    commit && !cancellationRequested -> {
                        val moved = navigate(spec.direction)
                        if (moved) {
                            turnCommitted = true
                            onCommittedTurn()
                            if (!isReducedMotion()) {
                                state.animateComplete(
                                    directionSign = visualDirectionSign(spec.side),
                                    velocityDpPerSec =
                                        releaseVelocityPxPerSec / density.coerceAtLeast(0.1f)
                                )
                            }
                        } else if (!isReducedMotion()) {
                            state.animateBoundaryBounce(visualDirectionSign(spec.side))
                        }
                    }

                    else -> if (!isReducedMotion()) state.animateCancel()
                }
            } else if (commit && !cancellationRequested) {
                if (navigate(spec.direction)) {
                    turnCommitted = true
                    onCommittedTurn()
                }
            }

            if (state.active) state.clear()
            resetDrag()
        }
        return true
    }

    /**
     * Restores a drag-preview locator when SLIDE loses ownership because the app pauses,
     * closes, or the user changes navigation mode. A committed turn is never rolled back.
     */
    fun cancelPendingTurn(): Boolean {
        if (!reserved && activeSpec == null) return false
        if (turnCommitted) return false

        cancellationRequested = true
        val spec = activeSpec
        if (spec == null) {
            completionJob = scope.launch {
                if (state.active) state.clear()
                resetDrag()
            }
            return true
        }

        if (completionJob == null) {
            completionJob = scope.launch {
                navigationJob?.join()
                if (previewNavigationSucceeded) restoreDragStart(spec)
                if (state.active) state.clear()
                resetDrag()
            }
        }
        return true
    }

    private fun performDiscreteTurn(spec: TurnSpec) {
        val visualReady = !isReducedMotion() && state.begin(navigator.publicationView)
        onInteraction()
        val moved = navigate(spec.direction)
        if (!moved) {
            if (visualReady) {
                completionJob = scope.launch {
                    state.animateBoundaryBounce(visualDirectionSign(spec.side))
                    state.clear()
                    resetDrag()
                }
            }
            return
        }

        turnCommitted = true
        onCommittedTurn()
        if (visualReady) {
            completionJob = scope.launch {
                delay(com.veilreader.app.ui.theme.VeilMotion.PAGE_REVEAL_MS)
                state.animateComplete(visualDirectionSign(spec.side))
                state.clear()
                resetDrag()
            }
        } else {
            resetDrag()
        }
    }

    private fun resolveDragTurn(event: DragEvent): TurnSpec? {
        val x = abs(event.offset.x)
        val y = abs(event.offset.y)
        if (x < DRAG_SLOP_PX || x < y * HORIZONTAL_BIAS) return null
        val side = if (event.offset.x < 0f) PaperCurlSide.RIGHT else PaperCurlSide.LEFT
        return TurnSpec(
            direction = paperTurnDirectionFor(
                side,
                navigator.overflow.value.readingProgression
            ),
            side = side
        )
    }

    private fun resolveEdgeTurn(x: Float): TurnSpec? {
        val width = navigator.publicationView.width.toFloat()
        if (width <= 0f) return null
        val density = navigator.publicationView.resources.displayMetrics.density
        val edge = max(84f * density, width * EDGE_FRACTION)
        val side = when {
            x <= edge -> PaperCurlSide.LEFT
            x >= width - edge -> PaperCurlSide.RIGHT
            else -> return null
        }
        return TurnSpec(
            paperTurnDirectionFor(side, navigator.overflow.value.readingProgression),
            side
        )
    }

    private fun turnSpecFor(
        direction: PaperTurnDirection,
        progression: ReadingProgression
    ): TurnSpec {
        val side = when (direction) {
            PaperTurnDirection.FORWARD ->
                if (progression == ReadingProgression.RTL) PaperCurlSide.LEFT
                else PaperCurlSide.RIGHT
            PaperTurnDirection.BACKWARD ->
                if (progression == ReadingProgression.RTL) PaperCurlSide.RIGHT
                else PaperCurlSide.LEFT
        }
        return TurnSpec(direction, side)
    }

    private fun constrainedOffset(side: PaperCurlSide, rawOffsetX: Float): Float =
        when (side) {
            PaperCurlSide.RIGHT -> rawOffsetX.coerceAtMost(0f)
            PaperCurlSide.LEFT -> rawOffsetX.coerceAtLeast(0f)
        }

    private fun inwardDistance(side: PaperCurlSide, offsetX: Float): Float =
        when (side) {
            PaperCurlSide.RIGHT -> -offsetX
            PaperCurlSide.LEFT -> offsetX
        }.coerceAtLeast(0f)

    private fun sampleVelocity(spec: TurnSpec, event: DragEvent) {
        val now = SystemClock.uptimeMillis()
        val distance = inwardDistance(spec.side, event.offset.x)
        val elapsed = now - lastSampleAtMillis
        if (lastSampleAtMillis > 0L && elapsed in 1L..120L) {
            releaseVelocityPxPerSec =
                ((distance - lastDistance) * 1000f / elapsed.toFloat())
                    .coerceIn(-12_000f, 12_000f)
        }
        lastSampleAtMillis = now
        lastDistance = distance
    }

    private fun restoreDragStart(spec: TurnSpec) {
        val restored = dragStartLocator?.let {
            navigator.go(it, animated = false)
        } ?: false
        if (!restored) {
            navigate(
                when (spec.direction) {
                    PaperTurnDirection.FORWARD -> PaperTurnDirection.BACKWARD
                    PaperTurnDirection.BACKWARD -> PaperTurnDirection.FORWARD
                }
            )
        }
    }

    private fun navigate(direction: PaperTurnDirection): Boolean =
        when (direction) {
            PaperTurnDirection.FORWARD -> navigator.goForward(animated = false)
            PaperTurnDirection.BACKWARD -> navigator.goBackward(animated = false)
        }

    private fun slideModeEnabled(): Boolean =
        isEnabled() && !navigator.overflow.value.scroll

    private fun resetDrag() {
        reserved = false
        activeSpec = null
        navigationJob = null
        completionJob = null
        previewNavigationSucceeded = false
        dragStartLocator = null
        cancellationRequested = false
        turnCommitted = false
        lastSampleAtMillis = 0L
        lastDistance = 0f
        releaseVelocityPxPerSec = 0f
    }

    private fun visualDirectionSign(side: PaperCurlSide): Float =
        if (side == PaperCurlSide.RIGHT) -1f else 1f

    private data class TurnSpec(
        val direction: PaperTurnDirection,
        val side: PaperCurlSide
    )

    private companion object {
        const val EDGE_FRACTION = 0.24f
        const val HORIZONTAL_BIAS = 1.05f
        const val DRAG_SLOP_PX = 6f
    }
}

internal fun shouldUseVeilSlideNavigation(
    format: com.veilreader.app.domain.BookFormat,
    scroll: Boolean,
    pageTurnStyle: com.veilreader.app.domain.PageTurnStyle
): Boolean =
    format == com.veilreader.app.domain.BookFormat.EPUB &&
        !scroll &&
        pageTurnStyle == com.veilreader.app.domain.PageTurnStyle.SLIDE

internal fun shouldCommitSlideTurn(
    inwardDistance: Float,
    width: Float,
    density: Float,
    slideProgress: Float,
    releaseVelocityPxPerSec: Float = 0f
): Boolean {
    if (inwardDistance <= 0f || width <= 0f) return false
    val safeDensity = density.coerceAtLeast(0.1f)
    val distanceThreshold = max(72f * safeDensity, width * 0.16f)
    val flickDistance = max(28f * safeDensity, width * 0.03f)
    val fastFlick =
        inwardDistance >= flickDistance &&
            releaseVelocityPxPerSec >= 850f * safeDensity

    return inwardDistance >= distanceThreshold ||
        slideProgress >= 0.30f ||
        fastFlick
}
