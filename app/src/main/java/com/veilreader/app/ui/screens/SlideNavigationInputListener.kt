package com.veilreader.app.ui.screens

import android.os.SystemClock
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.veilreader.app.ui.reader.awaitReaderVisualNavigationDeparture
import com.veilreader.app.ui.reader.readerNavigationIdentityMatchesTarget
import com.veilreader.app.ui.reader.toReaderNavigationIdentity
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.input.DragEvent
import org.readium.r2.navigator.input.InputListener
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
    private val onCommittedTurn: () -> Unit,
    private val onBoundaryHit: (PaperCurlSide) -> Unit = {}
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
    private var lastMotionAtMillis = 0L
    private var lastDistance = 0f
    private var releaseVelocityPxPerSec = 0f
    private var operationGeneration = 0L
    private var activeOperationGeneration = 0L

    private fun beginOperation(): Long {
        operationGeneration =
            nextSlideTurnOperationGeneration(operationGeneration)
        activeOperationGeneration = operationGeneration
        return activeOperationGeneration
    }

    private fun invalidateOperation() {
        operationGeneration =
            nextSlideTurnOperationGeneration(operationGeneration)
        activeOperationGeneration = 0L
    }

    private fun operationIsCurrent(token: Long): Boolean =
        slideTurnOperationIsCurrent(
            operationToken = token,
            activeOperationToken = activeOperationGeneration
        )

    override fun onTap(event: TapEvent): Boolean {
        if (!slideModeEnabled()) return cancelPendingTurn()
        val spec = resolveEdgeTurn(event.point.x) ?: return false
        return performDiscreteTurn(spec.direction)
    }

    fun performDiscreteTurn(direction: PaperTurnDirection): Boolean {
        if (!slideModeEnabled()) return cancelPendingTurn()
        if (completionJob != null || state.active || reserved) return true
        performDiscreteTurn(
            TurnSpec(
                direction = direction,
                side = paperTurnSideFor(direction, navigator.overflow.value.readingProgression)
            )
        )
        return true
    }

    override fun onKey(event: KeyEvent): Boolean {
        if (!slideModeEnabled()) return cancelPendingTurn()
        if (completionJob != null || state.active || reserved) return true
        if (event.type != KeyEvent.Type.Down) return false

        val turn = readerKeyTurn(
            key = event.key,
            modifiers = event.modifiers,
            progression = navigator.overflow.value.readingProgression
        ) ?: return false
        performDiscreteTurn(
            TurnSpec(
                direction = turn.direction,
                side = turn.side
            )
        )
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
        beginOperation()
        reserved = true
        activeSpec = null
        previewNavigationSucceeded = false
        dragStartLocator = navigator.currentLocator.value
        cancellationRequested = false
        turnCommitted = false
        completionJob = null
        lastSampleAtMillis = SystemClock.uptimeMillis()
        lastMotionAtMillis = 0L
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

        val spec = activeSpec ?: resolveDragTurn(event)?.also { resolved ->
            activeSpec = resolved
            if (state.active) {
                val operationToken = activeOperationGeneration
                val origin = dragStartLocator
                navigationJob = scope.launch {
                    delay(com.veilreader.app.ui.theme.VeilMotion.FRAME_SETTLE_MS)
                    if (
                        origin != null &&
                        !cancellationRequested &&
                        operationIsCurrent(operationToken)
                    ) {
                        val accepted = navigate(resolved.direction)
                        val moved =
                            accepted &&
                                awaitReaderVisualNavigationDeparture(
                                    currentLocator = navigator.currentLocator,
                                    origin = origin
                                )
                        if (
                            accepted &&
                            (
                                !moved ||
                                    cancellationRequested ||
                                    !operationIsCurrent(operationToken)
                                )
                        ) {
                            restoreDragStart(resolved, forceRequest = true)
                        } else {
                            previewNavigationSucceeded = moved
                        }
                    }
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
            val operationToken = activeOperationGeneration
            completionJob = scope.launch {
                if (state.active && !isReducedMotion()) state.animateCancel()
                if (state.active) state.clear()
                if (operationIsCurrent(operationToken)) {
                    activeOperationGeneration = 0L
                }
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

        val operationToken = activeOperationGeneration
        completionJob = scope.launch {
            navigationJob?.join()

            if (!operationIsCurrent(operationToken)) {
                if (previewNavigationSucceeded) {
                    restoreDragStart(spec, forceRequest = true)
                }
                if (state.active) state.clearImmediately()
                resetDrag()
                return@launch
            }

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
                        val origin =
                            dragStartLocator ?: navigator.currentLocator.value
                        val accepted = navigate(spec.direction)
                        val moved =
                            accepted &&
                                awaitReaderVisualNavigationDeparture(
                                    currentLocator = navigator.currentLocator,
                                    origin = origin
                                )
                        if (!operationIsCurrent(operationToken)) {
                            if (accepted) {
                                navigator.go(origin, animated = false)
                            }
                            if (state.active) state.clearImmediately()
                            resetDrag()
                            return@launch
                        }
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
                        } else {
                            if (accepted) {
                                navigator.go(origin, animated = false)
                            }
                            onBoundaryHit(spec.side)
                            if (!isReducedMotion()) {
                                state.animateBoundaryBounce(
                                    visualDirectionSign(spec.side)
                                )
                            }
                        }
                    }

                    else -> if (!isReducedMotion()) state.animateCancel()
                }
            } else if (commit && !cancellationRequested) {
                val origin =
                    dragStartLocator ?: navigator.currentLocator.value
                val accepted = navigate(spec.direction)
                val moved =
                    accepted &&
                        awaitReaderVisualNavigationDeparture(
                            currentLocator = navigator.currentLocator,
                            origin = origin
                        )
                if (!operationIsCurrent(operationToken)) {
                    if (accepted) {
                        navigator.go(origin, animated = false)
                    }
                    resetDrag()
                    return@launch
                }
                if (moved) {
                    turnCommitted = true
                    onCommittedTurn()
                } else if (
                    shouldEmitSlideTerminalBoundary(
                        commitRequested = commit,
                        cancellationRequested = cancellationRequested,
                        navigationMoved = moved
                    )
                ) {
                    // Reduced-motion and failed-snapshot paths still owe the same semantic boundary
                    // response even though there is no visual sheet available to bounce.
                    onBoundaryHit(spec.side)
                }
            }

            if (state.active) state.clear()
            if (operationIsCurrent(operationToken)) {
                activeOperationGeneration = 0L
            }
            resetDrag()
        }
        return true
    }

    /**
     * Restores a drag-preview locator when SLIDE loses ownership because the app pauses,
     * closes, or the user changes navigation mode. A committed turn is never rolled back.
     */
    fun cancelPendingTurn(): Boolean {
        if (!reserved && activeSpec == null) {
            if (completionJob != null || state.active) {
                invalidateOperation()
                cancellationRequested = true
                completionJob?.cancel()
                state.clearImmediately()
                resetDrag()
                return true
            }
            return false
        }
        if (turnCommitted) {
            invalidateOperation()
            completionJob?.cancel()
            state.clearImmediately()
            resetDrag()
            return true
        }

        invalidateOperation()
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

    /**
     * Cancels an uncommitted preview and waits until any preview navigation has been restored.
     * This is the only safe path before taking a durable close snapshot.
     */
    suspend fun cancelPendingTurnAndAwait(): Boolean {
        val targetIdentity =
            if (!turnCommitted) {
                dragStartLocator?.toReaderNavigationIdentity()
            } else {
                null
            }
        val existingCompletion = completionJob
        val requested = cancelPendingTurn()
        if (!requested) return false
        (existingCompletion ?: completionJob)?.join()

        targetIdentity ?: return true
        if (
            readerNavigationIdentityMatchesTarget(
                observed = navigator.currentLocator.value.toReaderNavigationIdentity(),
                target = targetIdentity
            )
        ) {
            return true
        }

        return withTimeoutOrNull(RESTORE_SETTLE_TIMEOUT_MS) {
            navigator.currentLocator.first { locator ->
                readerNavigationIdentityMatchesTarget(
                    observed = locator.toReaderNavigationIdentity(),
                    target = targetIdentity
                )
            }
            true
        } ?: false
    }

    fun hasPendingTurn(): Boolean =
        reserved ||
            activeSpec != null ||
            completionJob != null ||
            state.active

    /**
     * Synchronous teardown for configuration changes and composition disposal. The reader's
     * composition scope can disappear immediately, so locator restoration cannot depend on it.
     */
    fun forceCancelPendingTurn(): Boolean {
        if (!reserved && activeSpec == null) {
            if (completionJob != null || state.active) {
                invalidateOperation()
                cancellationRequested = true
                completionJob?.cancel()
                state.clearImmediately()
                resetDrag()
                return true
            }
            return false
        }
        if (turnCommitted) {
            invalidateOperation()
            completionJob?.cancel()
            state.clearImmediately()
            resetDrag()
            return true
        }

        invalidateOperation()
        cancellationRequested = true
        navigationJob?.cancel()
        completionJob?.cancel()

        val spec = activeSpec
        if (spec != null && dragStartLocator != null) {
            // Navigation can complete just before coroutine cancellation but
            // before previewNavigationSucceeded becomes observable. Restore the
            // exact origin whenever a preview transaction had a start locator.
            restoreDragStart(spec, forceRequest = true)
        }
        state.clearImmediately()
        resetDrag()
        return true
    }

    private fun performDiscreteTurn(spec: TurnSpec) {
        val operationToken = beginOperation()
        val originLocator = navigator.currentLocator.value
        val reducedMotion = isReducedMotion()
        val visualReady = !reducedMotion && state.begin(navigator.publicationView)
        onInteraction()

        completionJob = scope.launch {
            if (visualReady) {
                delay(com.veilreader.app.ui.theme.VeilMotion.FRAME_SETTLE_MS)
            }
            if (cancellationRequested || !operationIsCurrent(operationToken)) {
                if (state.active) state.clear()
                resetDrag()
                return@launch
            }

            val accepted = navigate(spec.direction)
            val moved =
                accepted &&
                    awaitReaderVisualNavigationDeparture(
                        currentLocator = navigator.currentLocator,
                        origin = originLocator
                    )
            if (!operationIsCurrent(operationToken)) {
                if (accepted) {
                    navigator.go(originLocator, animated = false)
                }
                if (state.active) state.clearImmediately()
                resetDrag()
                return@launch
            }
            if (!moved) {
                if (accepted) {
                    navigator.go(originLocator, animated = false)
                }
                onBoundaryHit(spec.side)
                if (visualReady && state.active) {
                    state.animateBoundaryBounce(visualDirectionSign(spec.side))
                    state.clear()
                }
                resetDrag()
                return@launch
            }

            turnCommitted = true
            onCommittedTurn()
            if (visualReady && state.active) {
                delay(com.veilreader.app.ui.theme.VeilMotion.PAGE_REVEAL_MS)
                state.animateComplete(visualDirectionSign(spec.side))
                state.clear()
            }
            if (operationIsCurrent(operationToken)) {
                activeOperationGeneration = 0L
            }
            resetDrag()
        }
    }

    private fun resolveDragTurn(event: DragEvent): TurnSpec? {
        val view = navigator.publicationView
        if (
            !hasDeliberateSlideIntent(
                offsetX = event.offset.x,
                offsetY = event.offset.y,
                width = view.width.toFloat(),
                density = view.resources.displayMetrics.density
            )
        ) {
            return null
        }
        val side = if (event.offset.x < 0f) {
            PaperCurlSide.RIGHT
        } else {
            PaperCurlSide.LEFT
        }
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
        val edge = pageTurnTapZonePx(
            width = width,
            density = density,
            preferredFraction = EDGE_FRACTION
        )
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
        val delta = distance - lastDistance
        val elapsed = now - lastSampleAtMillis
        val sinceMotion = if (lastMotionAtMillis > 0L) {
            now - lastMotionAtMillis
        } else {
            Long.MAX_VALUE
        }
        releaseVelocityPxPerSec = nextSlideReleaseVelocity(
            previousVelocityPxPerSec = releaseVelocityPxPerSec,
            distanceDeltaPx = delta,
            elapsedMillis = elapsed,
            sinceLastMotionMillis = sinceMotion
        )
        if (abs(delta) >= 1f) lastMotionAtMillis = now
        lastSampleAtMillis = now
        lastDistance = distance
    }

    private fun restoreDragStart(
        spec: TurnSpec,
        forceRequest: Boolean = false
    ) {
        val exact = dragStartLocator ?: return
        val targetIdentity = exact.toReaderNavigationIdentity()
        val alreadyAtOrigin =
            readerNavigationIdentityMatchesTarget(
                observed = navigator.currentLocator.value.toReaderNavigationIdentity(),
                target = targetIdentity
            )
        if (alreadyAtOrigin && !forceRequest) {
            return
        }

        val accepted = navigator.go(exact, animated = false)
        if (
            !accepted &&
            previewNavigationSucceeded &&
            !alreadyAtOrigin
        ) {
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
        // ReaderScreen already supplies the accepted SLIDE contract. Do not
        // re-check Readium's overflow flow here; it can lag preference changes
        // and create the same visible-mode/input-owner split that broke Paper.
        isEnabled()

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
        lastMotionAtMillis = 0L
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
        const val EDGE_FRACTION = 0.22f
        const val RESTORE_SETTLE_TIMEOUT_MS = 1_500L
    }
}

internal fun nextSlideTurnOperationGeneration(current: Long): Long =
    if (current == Long.MAX_VALUE) 1L else current + 1L

internal fun slideTurnOperationIsCurrent(
    operationToken: Long,
    activeOperationToken: Long
): Boolean =
    operationToken > 0L &&
        operationToken == activeOperationToken

internal fun hasDeliberateSlideIntent(
    offsetX: Float,
    offsetY: Float,
    width: Float,
    density: Float
): Boolean {
    if (width <= 0f) return false
    val x = abs(offsetX)
    val y = abs(offsetY)
    val safeDensity = density.coerceAtLeast(0.1f)
    val intentDistance = max(10f * safeDensity, width * 0.012f)
    return x >= intentDistance && x >= y * 1.15f
}

internal fun shouldEmitSlideTerminalBoundary(
    commitRequested: Boolean,
    cancellationRequested: Boolean,
    navigationMoved: Boolean
): Boolean =
    commitRequested &&
        !cancellationRequested &&
        !navigationMoved

internal fun shouldUseVeilSlideNavigation(
    format: com.veilreader.app.domain.BookFormat,
    scroll: Boolean,
    pageTurnStyle: com.veilreader.app.domain.PageTurnStyle
): Boolean =
    format == com.veilreader.app.domain.BookFormat.EPUB &&
        !scroll &&
        pageTurnStyle == com.veilreader.app.domain.PageTurnStyle.SLIDE

/**
 * Drag End often repeats the final Move offset. Preserve a genuinely fresh flick through that
 * duplicate sample, but expire it once the finger has actually paused.
 */
internal fun nextSlideReleaseVelocity(
    previousVelocityPxPerSec: Float,
    distanceDeltaPx: Float,
    elapsedMillis: Long,
    sinceLastMotionMillis: Long
): Float {
    val previous =
        previousVelocityPxPerSec
            .takeIf { it.isFinite() }
            ?.coerceIn(-12_000f, 12_000f)
            ?: 0f
    val delta = distanceDeltaPx.takeIf { it.isFinite() } ?: 0f

    if (abs(delta) >= 1f) {
        if (elapsedMillis in 1L..120L) {
            val instantaneous =
                (delta * 1000f / elapsedMillis.toFloat())
                    .coerceIn(-12_000f, 12_000f)
            val sampleTrust = when {
                elapsedMillis <= 12L -> 0.42f
                elapsedMillis <= 28L -> 0.58f
                elapsedMillis <= 60L -> 0.72f
                else -> 0.82f
            }
            val sameDirection =
                previous == 0f ||
                    kotlin.math.sign(previous) ==
                    kotlin.math.sign(instantaneous)
            val trust =
                if (sameDirection) sampleTrust
                else kotlin.math.max(sampleTrust, 0.76f)
            return (
                previous * (1f - trust) +
                    instantaneous * trust
                ).coerceIn(-12_000f, 12_000f)
        }
        return if (elapsedMillis == 0L && sinceLastMotionMillis <= 100L) {
            previous
        } else {
            0f
        }
    }
    return if (sinceLastMotionMillis <= 100L) previous else 0f
}

internal fun shouldCommitSlideTurn(
    inwardDistance: Float,
    width: Float,
    density: Float,
    slideProgress: Float,
    releaseVelocityPxPerSec: Float = 0f
): Boolean {
    if (inwardDistance <= 0f || width <= 0f) return false
    val safeDensity = density.coerceAtLeast(0.1f)
    val distanceThreshold = max(60f * safeDensity, width * 0.13f)
    val flickDistance = max(24f * safeDensity, width * 0.026f)
    val fastFlick =
        inwardDistance >= flickDistance &&
            releaseVelocityPxPerSec >= 760f * safeDensity

    return inwardDistance >= distanceThreshold ||
        slideProgress >= 0.26f ||
        fastFlick
}
