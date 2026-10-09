@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.veilreader.app.ui.screens

import android.os.SystemClock
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.veilreader.app.ui.reader.awaitReaderVisualNavigationDeparture
import com.veilreader.app.ui.reader.readerNavigationIdentityMatchesTarget
import com.veilreader.app.ui.reader.toReaderNavigationIdentity
import com.veilreader.app.ui.reader.material.GpuMaterialPageRendererStatus
import com.veilreader.app.ui.reader.material.MaterialPageEngineRollout
import com.veilreader.app.ui.reader.material.MaterialPageReleaseDecision
import com.veilreader.app.ui.reader.material.materialPageReleaseDecision
import com.veilreader.app.ui.reader.material.materialPageRendererCanPresent
import com.veilreader.app.ui.theme.VeilMotion
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.input.DragEvent
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.KeyEvent
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
internal fun shouldCapturePaperTurnSnapshot(
    reducedMotion: Boolean
): Boolean =
    MaterialPageEngineRollout.isEnabled() && !reducedMotion

/**
 * Paper is a visual interaction contract, not merely a navigation label.
 *
 * Normal-motion input may commit only while a Paper renderer can actually present the
 * source sheet. GPU remains preferred; the clean-room software mesh backend is a reliability
 * fallback. Reduced Motion deliberately permits a static navigation path by accessibility policy.
 */
internal fun paperRendererCanOwnNavigationInput(
    reducedMotion: Boolean,
    rendererStatus: GpuMaterialPageRendererStatus
): Boolean =
    MaterialPageEngineRollout.isEnabled() &&
        (reducedMotion || materialPageRendererCanPresent(rendererStatus))

/**
 * Paper owns its drag sequence whenever the Paper engine rollout is selected.
 *
 * Renderer readiness controls whether the page may move, not who owns the gesture. Returning false
 * for FAILED/UNSUPPORTED let Readium/native paged gestures take over and made a broken Paper engine
 * look like a valid static/slide turn on real devices.
 */
@Suppress("UNUSED_PARAMETER")
internal fun paperRendererCanReserveDrag(
    reducedMotion: Boolean,
    rendererStatus: GpuMaterialPageRendererStatus
): Boolean =
    MaterialPageEngineRollout.isEnabled()

internal fun shouldAllowPaperNavigation(
    reducedMotion: Boolean,
    rendererStatus: GpuMaterialPageRendererStatus,
    visualActive: Boolean
): Boolean =
    paperRendererCanOwnNavigationInput(
        reducedMotion = reducedMotion,
        rendererStatus = rendererStatus
    ) &&
        (reducedMotion || visualActive)

internal class PaperCurlInputListener(
    private val navigator: OverflowableNavigator,
    private val state: PaperCurlState,
    private val isEnabled: () -> Boolean,
    private val scope: CoroutineScope,
    private val isReducedMotion: () -> Boolean = { false },
    private val onInteraction: () -> Unit,
    private val onCommittedTurn: () -> Unit,
    private val onBoundaryHit: (PaperCurlSide) -> Unit = {},
    private val isDragEnabled: () -> Boolean = isEnabled,
    private val onVisualFailure: (PaperTurnVisualFailure) -> Unit = {}
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
    private var visualStartAttempted = false
    private var operationGeneration = 0L
    private var activeOperationGeneration = 0L
    private var cancelledOperationAwaitingCleanup = 0L

    override fun onTap(event: TapEvent): Boolean {
        if (!paperModeEnabled()) return false
        if (paperInputBusy()) return true

        val spec = resolveEdgeTurn(event.point.x) ?: return false
        performDiscreteTurn(spec)
        return true
    }

    override fun onKey(event: KeyEvent): Boolean {
        if (!paperModeEnabled()) return cancelPendingTurn()
        if (paperInputBusy()) return true
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

    private fun paperInputBusy(): Boolean =
        completionJob != null ||
            dragReserved ||
            activeDrag != null ||
            state.active

    /** Semantic page actions reuse the same keyboard/tap transaction and busy guard. */
    fun performDiscreteTurn(direction: PaperTurnDirection): Boolean {
        if (!paperModeEnabled()) return false
        if (paperInputBusy()) return true
        performDiscreteTurn(TurnSpec(
            direction = direction,
            side = paperTurnSideFor(direction, navigator.overflow.value.readingProgression)
        ))
        return true
    }

    private fun beginOperation(): Long {
        cancelledOperationAwaitingCleanup = 0L
        operationGeneration =
            nextPaperTurnOperationGeneration(operationGeneration)
        activeOperationGeneration = operationGeneration
        return activeOperationGeneration
    }

    private fun invalidateOperation() {
        cancelledOperationAwaitingCleanup = activeOperationGeneration
        operationGeneration =
            nextPaperTurnOperationGeneration(operationGeneration)
        activeOperationGeneration = 0L
    }

    private fun operationIsCurrent(token: Long): Boolean =
        paperTurnOperationIsCurrent(
            operationToken = token,
            activeOperationToken = activeOperationGeneration
        )

    private fun mayCleanCancelledOperation(token: Long): Boolean =
        cancelledOperationAwaitingCleanup == token &&
            readerTurnMayCleanCancelledOperation(token, operationGeneration, activeOperationGeneration)

    private fun performDiscreteTurn(spec: TurnSpec) {
        val operationToken = beginOperation()
        val originLocator = navigator.currentLocator.value
        // Discrete turns are preview transactions too. Lifecycle cancellation
        // must retain an exact origin while Readium accepts but has not settled.
        activeDrag = spec
        dragStartLocator = originLocator
        previewNavigationSucceeded = false
        val reducedMotion = isReducedMotion()
        state.configureReducedMotion(reducedMotion)
        val visualReady =
            shouldCapturePaperTurnSnapshot(reducedMotion) &&
                state.begin(
                    view = navigator.publicationView,
                    side = spec.side,
                    direction = spec.direction
                )

        if (visualReady) {
            state.prepareMaterialTapGrip()
        }

        onInteraction()
        if (
            !shouldAllowPaperNavigation(
                reducedMotion = reducedMotion,
                rendererStatus = state.rendererStatus,
                visualActive = visualReady
            )
        ) {
            // Consume the Paper action rather than silently degrading to a static
            // page turn when GPU/capture readiness is missing.
            invalidateOperation()
            resetDrag()
            if (!reducedMotion && materialPageRendererCanPresent(state.rendererStatus)) {
                onVisualFailure(PaperTurnVisualFailure.SNAPSHOT)
            }
            return
        }

        // Tap/key turns must obey the same visual transaction as drag turns.
        // Wait for the presented Paper source sheet before changing Readium underneath
        // it. READY/upload/draw and fixed delays do not prove screen presentation.
        completionJob = launchCompletion {
            if (visualReady && !reducedMotion) {
                if (!state.materialEngine.awaitSheetPresented()) {
                    if (operationIsCurrent(operationToken)) {
                        state.clearImmediately()
                        activeOperationGeneration = 0L
                        resetDrag()
                        onVisualFailure(PaperTurnVisualFailure.PRESENTATION)
                    }
                    return@launchCompletion
                }
            }
            if (cancellationRequested || !operationIsCurrent(operationToken) ||
                !shouldAllowPaperNavigation(reducedMotion, state.rendererStatus, state.active)) {
                if (state.active) state.clear()
                resetDrag()
                return@launchCompletion
            }

            val accepted = navigate(spec.direction)
            previewNavigationSucceeded = accepted
            val moved =
                accepted &&
                    awaitReaderVisualNavigationDeparture(
                        currentLocator = navigator.currentLocator,
                        origin = originLocator
                    )
            if (!operationIsCurrent(operationToken)) {
                if (!mayCleanCancelledOperation(operationToken)) return@launchCompletion
                if (accepted) {
                    navigator.go(originLocator, animated = false)
                }
                if (state.active) state.clearImmediately()
                resetDrag()
                return@launchCompletion
            }
            if (!moved) {
                if (accepted) {
                    navigator.go(originLocator, animated = false)
                }
                onBoundaryHit(spec.side)
                if (visualReady && state.active) {
                    state.animateBoundaryBounce()
                    state.clear()
                }
                resetDrag()
                return@launchCompletion
            }

            turnCommitted = true
            onCommittedTurn()
            if (visualReady && state.active) {
                if (!reducedMotion) {
                    // Let the destination paint underneath the captured source leaf
                    // before the sheet exposes it through the release animation.
                    delay(VeilMotion.PAGE_REVEAL_MS)
                }
                state.animateTapTurn()
                state.clear()
            }
            if (operationIsCurrent(operationToken)) {
                activeOperationGeneration = 0L
            }
            resetDrag()
        }
        completionJob?.start()
    }

    override fun onDrag(event: DragEvent): Boolean {
        if (!isDragEnabled() || !scope.isActive) {
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

        if (!state.active && navigationJob == null) {
            tryStartReservedVisual(spec, event)
        }
        if (state.active) {
            state.updateDrag(event.start, event.offset)
        }
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

        if (!state.active && navigationJob == null) {
            tryStartReservedVisual(spec, event)
        }
        if (state.active) {
            state.updateDrag(event.start, event.offset)
        }
        sampleReleaseVelocity(spec, event)

        val view = navigator.publicationView
        val width = view.width.toFloat()
        val density = view.resources.displayMetrics.density
        val inward = inwardDistance(spec, event)
        val reducedMotion = isReducedMotion()
        val navigationAllowed = shouldAllowPaperNavigation(
            reducedMotion = reducedMotion,
            rendererStatus = state.rendererStatus,
            visualActive = state.active
        )
        val commit = when {
            !navigationAllowed -> false
            state.usingMaterialEngine() ->
                inward > 0f &&
                    materialPageReleaseDecision(
                        progress = state.dragProgress(),
                        inwardVelocityDpPerSec =
                            releaseVelocityPxPerSec / density.coerceAtLeast(0.1f),
                        profile = state.materialEngine.profile
                    ) == MaterialPageReleaseDecision.COMPLETE
            else ->
                // Reduced Motion intentionally has no curl visual. It keeps the
                // same deliberate distance/flick threshold for functional paging.
                shouldCommitPaperTurn(
                    inwardDistance = inward,
                    width = width,
                    density = density,
                    curlProgress = state.dragProgress(),
                    releaseVelocityPxPerSec = releaseVelocityPxPerSec
                )
        }
        val operationToken = activeOperationGeneration
        completionJob = launchCompletion {
            navigationJob?.join()

            if (!operationIsCurrent(operationToken)) {
                if (!mayCleanCancelledOperation(operationToken)) return@launchCompletion
                if (previewNavigationSucceeded) {
                    restoreDragStart(spec, forceRequest = true)
                }
                if (state.active) state.clearImmediately()
                resetDrag()
                return@launchCompletion
            }

            when {
                cancellationRequested -> {
                    if (previewNavigationSucceeded) restoreDragStart(spec)
                }

                commit && previewNavigationSucceeded -> {
                    // Persist/count and emit sensory feedback only after a real commit.
                    turnCommitted = true
                    onCommittedTurn()
                    if (shouldAnimatePaperVisual()) {
                        state.animateComplete(
                            releaseVelocityDpPerSec =
                                releaseVelocityPxPerSec / density.coerceAtLeast(0.1f)
                        )
                    }
                }

                previewNavigationSucceeded -> {
                    restoreDragStart(spec)
                    if (shouldAnimatePaperVisual()) {
                        if (!isReducedMotion()) delay(VeilMotion.PAGE_REVEAL_MS)
                        state.animateCancel(
                            releaseVelocityDpPerSec =
                                releaseVelocityPxPerSec / density.coerceAtLeast(0.1f)
                        )
                    }
                }

                commit && reducedMotion -> {
                    // This branch is reachable without a visual only for Reduced Motion.
                    // Normal-motion Paper fails closed before a static navigation fallback.
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
                        if (!mayCleanCancelledOperation(operationToken)) return@launchCompletion
                        if (accepted) {
                            navigator.go(origin, animated = false)
                        }
                        if (state.active) state.clearImmediately()
                        resetDrag()
                        return@launchCompletion
                    }
                    if (!moved && accepted) {
                        navigator.go(origin, animated = false)
                    }
                    if (moved) {
                        turnCommitted = true
                        onCommittedTurn()
                        if (state.active && shouldAnimatePaperVisual()) {
                            state.animateComplete(
                                releaseVelocityDpPerSec =
                                    releaseVelocityPxPerSec / density.coerceAtLeast(0.1f)
                            )
                        }
                    } else {
                        onBoundaryHit(spec.side)
                        if (state.active && shouldAnimatePaperVisual()) {
                            state.animateBoundaryBounce()
                        }
                    }
                }

                else -> {
                    if (state.active && shouldAnimatePaperVisual()) {
                        state.animateCancel(
                            releaseVelocityDpPerSec =
                                releaseVelocityPxPerSec / density.coerceAtLeast(0.1f)
                        )
                    }
                }
            }

            if (state.active) state.clear()
            if (operationIsCurrent(operationToken)) {
                activeOperationGeneration = 0L
            }
            resetDrag()
        }
        completionJob?.start()
        return true
    }

    /**
     * Called when the Reader pauses or exits PAPER mode. An uncommitted preview is
     * restored to its exact start locator; a committed turn keeps its navigation
     * result but any remaining Paper visual coroutine is cancelled and cleared.
     */
    fun cancelPendingTurn(): Boolean {
        if (!dragReserved && activeDrag == null) {
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
        val spec = activeDrag
        if (spec == null) {
            resetDrag()
            return true
        }
        if (completionJob == null) {
            completionJob = launchCompletion {
                navigationJob?.join()
                if (previewNavigationSucceeded) restoreDragStart(spec)
                state.clear()
                resetDrag()
            }
            completionJob?.start()
        }
        return true
    }

    /**
     * Cancels an uncommitted preview and does not return until the source locator is restored.
     * Use this before taking a durable locator snapshot.
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

        // navigator.go() only acknowledges that a navigation request was accepted.
        // The authoritative restoration point is currentLocator.
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
        dragReserved ||
            activeDrag != null ||
            completionJob != null ||
            state.active

    /**
     * Synchronous teardown for composition/lifecycle disposal where the composition scope may be
     * cancelled before an asynchronous restoration job can run.
     */
    fun forceCancelPendingTurn(): Boolean {
        if (!dragReserved && activeDrag == null) {
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

        val spec = activeDrag
        if (spec != null && dragStartLocator != null) {
            // Restore the exact origin even if cancellation races the navigation job
            // before previewNavigationSucceeded becomes observable.
            restoreDragStart(spec, forceRequest = true)
        }
        state.clearImmediately()
        resetDrag()
        return true
    }

    private fun beginReservedDragIfReady(event: DragEvent): Boolean {
        if (activeDrag != null || state.active) return activeDrag != null
        if (!isMostlyHorizontal(event)) return false

        val spec = resolveDragTurn(event) ?: return false
        if (activeOperationGeneration == 0L) {
            beginOperation()
        }
        activeDrag = spec
        dragStartLocator = navigator.currentLocator.value
        previewNavigationSucceeded = false
        lastDragSampleAtMillis = SystemClock.uptimeMillis()
        lastMotionAtMillis = 0L
        lastInwardDistance = inwardDistance(spec, event)
        releaseVelocityPxPerSec = 0f

        tryStartReservedVisual(spec, event)
        onInteraction()
        return true
    }

    /**
     * A PAPER drag is reserved before GL readiness is guaranteed. If the first
     * deliberate Move arrives while the renderer is INITIALIZING, keep the same
     * transaction and retry on later Move/End events instead of sacrificing the
     * whole first turn. Native/Slide never receives the reserved gesture.
     */
    private fun tryStartReservedVisual(
        spec: TurnSpec,
        event: DragEvent
    ): Boolean {
        if (state.active) return true
        if (!shouldCapturePaperTurnSnapshot(isReducedMotion())) return false
        if (!materialPageRendererCanPresent(state.rendererStatus)) return false

        val view = navigator.publicationView
        if (view.width <= 0 || view.height <= 0) return false
        // Once GL is READY, make at most one synchronous capture attempt for this
        // gesture. A real capture failure must not trigger View.draw() on every Move.
        if (visualStartAttempted) return false
        visualStartAttempted = true

        val visualReady = state.begin(
            view,
            spec.side,
            spec.direction
        )
        if (!visualReady) {
            onVisualFailure(PaperTurnVisualFailure.SNAPSHOT)
            return false
        }

        state.updateDrag(event.start, event.offset)
        val operationToken = activeOperationGeneration
        navigationJob = launchPreview {
            if (!state.materialEngine.awaitSheetPresented()) {
                // An unpresented sheet must never enable the release-time static branch.
                if (operationIsCurrent(operationToken)) {
                    state.clearImmediately()
                    onVisualFailure(PaperTurnVisualFailure.PRESENTATION)
                }
                return@launchPreview
            }
            if (
                !cancellationRequested &&
                operationIsCurrent(operationToken) &&
                shouldAllowPaperNavigation(isReducedMotion(), state.rendererStatus, state.active)
            ) {
                val origin = dragStartLocator
                val accepted = navigate(spec.direction)
                val moved =
                    accepted &&
                        origin != null &&
                        awaitReaderVisualNavigationDeparture(
                        currentLocator = navigator.currentLocator,
                        origin = origin
                            )
                if (!operationIsCurrent(operationToken) && !mayCleanCancelledOperation(operationToken)) {
                    return@launchPreview
                }
                if (
                    accepted &&
                    (
                        !moved ||
                            cancellationRequested ||
                            !operationIsCurrent(operationToken)
                        )
                ) {
                    restoreDragStart(spec, forceRequest = true)
                } else {
                    previewNavigationSucceeded = moved
                }
            }
            // Keep the previewed destination underneath the curl. The reverse
            // face remains source-derived until a true opposite-leaf provider exists.
        }
        navigationJob?.start()
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
            // Cancellation can beat the delayed preview navigation. In that race the
            // source is already authoritative and any directional fallback would
            // incorrectly move one extra page.
            return
        }

        val accepted = navigator.go(exact, animated = false)
        if (
            !accepted &&
            previewNavigationSucceeded &&
            !alreadyAtOrigin
        ) {
            // Use a directional fallback only after evidence that this transaction
            // actually moved away from the origin. A forced no-op restore while
            // origin is still current must never manufacture a reverse page turn.
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
        // The presented Reader appearance is the accepted interaction contract.
        // Readium's overflow StateFlow can lag preference application by a frame;
        // consulting it here creates a split-brain state where the UI says PAPER
        // but the JS drag is not prevented and native swipe wins.
        isEnabled() && scope.isActive

    private fun shouldAnimatePaperVisual(): Boolean =
        state.active

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

    /** A failed/cancelled coroutine must not leave the next gesture permanently busy. */
    private fun launchCompletion(block: suspend CoroutineScope.() -> Unit): Job {
        val token = cleanupOperationToken()
        return scope.launch(start = CoroutineStart.LAZY) {
            try {
                block()
            } finally {
                releaseInterruptedOperation(token)
            }
        }
    }

    private fun launchPreview(block: suspend CoroutineScope.() -> Unit): Job {
        val token = cleanupOperationToken()
        return scope.launch(start = CoroutineStart.LAZY) {
            try {
                block()
            } catch (failure: Throwable) {
                releaseInterruptedOperation(token)
                throw failure
            }
        }
    }

    private fun cleanupOperationToken(): Long =
        activeOperationGeneration.takeIf { it != 0L } ?: cancelledOperationAwaitingCleanup

    private fun releaseInterruptedOperation(token: Long) {
        // A late finally from the previous gesture must never clear a newer sheet or locator.
        if (!operationIsCurrent(token) && !mayCleanCancelledOperation(token)) return
        navigationJob?.cancel()
        try {
            if (!turnCommitted) activeDrag?.let { restoreDragStart(it, forceRequest = true) }
        } finally {
            try {
                state.clearImmediately()
            } finally {
                activeOperationGeneration = 0L
                resetDrag()
            }
        }
    }

    private fun resetDrag() {
        cancelledOperationAwaitingCleanup = 0L
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
        visualStartAttempted = false
    }

    private data class TurnSpec(
        val direction: PaperTurnDirection,
        val side: PaperCurlSide
    )

    private companion object {
        const val EDGE_FRACTION = 0.22f
        const val DRAG_DIRECTION_SLOP_PX = 4f
        const val RESTORE_SETTLE_TIMEOUT_MS = 1_500L
    }
}

internal fun nextPaperTurnOperationGeneration(current: Long): Long =
    if (current == Long.MAX_VALUE) 1L else current + 1L

internal fun paperTurnOperationIsCurrent(
    operationToken: Long,
    activeOperationToken: Long
): Boolean =
    operationToken > 0L &&
        operationToken == activeOperationToken

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

internal fun paperTurnSideFor(
    direction: PaperTurnDirection,
    progression: ReadingProgression
): PaperCurlSide =
    when (direction) {
        PaperTurnDirection.FORWARD ->
            if (progression == ReadingProgression.RTL) {
                PaperCurlSide.LEFT
            } else {
                PaperCurlSide.RIGHT
            }
        PaperTurnDirection.BACKWARD ->
            if (progression == ReadingProgression.RTL) {
                PaperCurlSide.RIGHT
            } else {
                PaperCurlSide.LEFT
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
    curlProgress: Float,
    releaseVelocityPxPerSec: Float = 0f
): Boolean {
    // Returning to the origin or dragging outward must never commit a turn.
    if (inwardDistance <= 0f) return false
    val safeDensity = density.coerceAtLeast(0.1f)
    val commitDistance = max(82f * safeDensity, width * 0.18f)
    val flickDistance = max(32f * safeDensity, width * 0.032f)
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
                if (sameDirection) {
                    sampleTrust
                } else {
                    kotlin.math.max(sampleTrust, 0.76f)
                }
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

/** A canceled worker may clean up only until another turn has acquired ownership. */
internal fun readerTurnMayCleanCancelledOperation(
    token: Long,
    latestGeneration: Long,
    activeToken: Long
): Boolean = token > 0L && activeToken == 0L &&
    latestGeneration == nextPaperTurnOperationGeneration(token)
