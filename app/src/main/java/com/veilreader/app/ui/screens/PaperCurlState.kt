package com.veilreader.app.ui.screens

import android.graphics.PointF
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.veilreader.app.BuildConfig
import com.veilreader.app.ui.reader.material.GpuMaterialPageOverlay
import com.veilreader.app.ui.reader.material.NativeMaterialPageOverlay
import com.veilreader.app.ui.reader.material.GpuMaterialPageRendererStatus
import com.veilreader.app.ui.reader.material.MaterialPageEngineRollout
import com.veilreader.app.ui.reader.material.MaterialPageEngineState
import com.veilreader.app.ui.reader.material.MaterialPageSide
import com.veilreader.app.ui.reader.material.MaterialPageTone
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import org.readium.r2.shared.publication.Locator

internal enum class PaperCurlSide { LEFT, RIGHT }
internal enum class PaperTurnDirection { FORWARD, BACKWARD }

internal enum class PaperPerformancePhase {
    IDLE,
    LIFT,
    DRAG,
    RELEASE,
    CANCEL,
    BOUNDARY,
    GL_RECREATE
}

/**
 * One Paper transaction with an explicitly selected renderer backend.
 * Debug/benchmark use the licensed native cylinder; release keeps GPU v2 behind
 * its existing rollout gate. Renderer failure never silently selects another
 * backend or disguises a failed fold as a successful slide.
 */
@Stable
internal class PaperCurlState {
    var side: PaperCurlSide by mutableStateOf(PaperCurlSide.RIGHT)
        private set

    var direction: PaperTurnDirection by mutableStateOf(PaperTurnDirection.FORWARD)
        private set

    var active: Boolean by mutableStateOf(false)
        private set

    var lastBeginFailed: Boolean by mutableStateOf(false)
        private set

    var debugBeginAttempts: Int by mutableIntStateOf(0)
        private set

    var rendererStatus: GpuMaterialPageRendererStatus by mutableStateOf(
        GpuMaterialPageRendererStatus.INITIALIZING
    )
        private set

    var performancePhase: PaperPerformancePhase by mutableStateOf(
        PaperPerformancePhase.IDLE
    )
        private set

    private var rendererEverReady = false

    internal val materialEngine = MaterialPageEngineState(
        initialProfile = MaterialPageEngineRollout.selectedProfile()
    )

    internal val destinationPreview = PaperDestinationPreviewState()

    val snapshotSourceRevision: Long
        get() = materialEngine.snapshotSourceRevision

    fun configureReducedMotion(value: Boolean) {
        materialEngine.configureReducedMotion(value)
    }

    fun updateRendererStatus(value: GpuMaterialPageRendererStatus) {
        rendererStatus = value
        if (value != GpuMaterialPageRendererStatus.READY) materialEngine.invalidateSheetPresentation()
        when (value) {
            GpuMaterialPageRendererStatus.READY -> {
                rendererEverReady = true
                if (!active && performancePhase == PaperPerformancePhase.GL_RECREATE) {
                    performancePhase = PaperPerformancePhase.IDLE
                }
            }
            GpuMaterialPageRendererStatus.INITIALIZING,
            GpuMaterialPageRendererStatus.FAILED -> {
                if (rendererEverReady && !active) {
                    performancePhase = PaperPerformancePhase.GL_RECREATE
                }
            }
            GpuMaterialPageRendererStatus.UNSUPPORTED,
            GpuMaterialPageRendererStatus.REDUCED_MOTION -> {
                if (!active) {
                    performancePhase = PaperPerformancePhase.IDLE
                }
            }
        }
    }

    internal fun usingMaterialEngine(): Boolean = active

    suspend fun prepareSnapshot(view: View, isSourceCurrent: () -> Boolean = { true }): Boolean {
        if (active || view.width <= 0 || view.height <= 0) return false
        return materialEngine.prepareSnapshot(view, isSourceCurrent)
    }

    fun invalidateSnapshotSource() {
        materialEngine.invalidateSnapshotSource()
        destinationPreview.invalidate()
    }

    fun begin(
        view: View,
        side: PaperCurlSide,
        direction: PaperTurnDirection,
        origin: Locator
    ): Boolean {
        if (BuildConfig.DEBUG) {
            debugBeginAttempts += 1
        }
        if (
            rendererStatus != GpuMaterialPageRendererStatus.READY ||
            active
        ) {
            return false
        }
        if (view.width <= 0 || view.height <= 0) {
            lastBeginFailed = true
            return false
        }

        materialEngine.configureProfile(MaterialPageEngineRollout.selectedProfile())
        val started = materialEngine.begin(
            view = view,
            side = when (side) {
                PaperCurlSide.LEFT -> MaterialPageSide.LEFT
                PaperCurlSide.RIGHT -> MaterialPageSide.RIGHT
            }
        )
        lastBeginFailed = !started
        if (started) {
            this.side = side
            this.direction = direction
            active = true
            destinationPreview.begin(direction, snapshotSourceRevision, origin, view.width, view.height)
            performancePhase = PaperPerformancePhase.DRAG
        } else {
            performancePhase = PaperPerformancePhase.IDLE
        }
        return started
    }

    fun prepareMaterialTapGrip() {
        if (active) {
            performancePhase = PaperPerformancePhase.LIFT
            materialEngine.prepareTapGrip()
        }
    }

    fun updateDrag(start: PointF, offset: PointF) {
        if (active) {
            performancePhase = PaperPerformancePhase.DRAG
            materialEngine.updateDrag(start, offset)
        }
    }

    fun dragProgress(): Float =
        if (active) materialEngine.dragProgress() else 0f

    suspend fun animateTapTurn() {
        if (active) {
            performancePhase = PaperPerformancePhase.RELEASE
            materialEngine.animateTapTurn()
        }
    }

    suspend fun animateComplete(releaseVelocityDpPerSec: Float = 0f) {
        if (active) {
            performancePhase = PaperPerformancePhase.RELEASE
            materialEngine.animateComplete(releaseVelocityDpPerSec)
        }
    }

    suspend fun animateCancel(releaseVelocityDpPerSec: Float = 0f) {
        if (active) {
            performancePhase = PaperPerformancePhase.CANCEL
            materialEngine.animateCancel(releaseVelocityDpPerSec)
        }
    }

    suspend fun animateBoundaryBounce() {
        if (active) {
            performancePhase = PaperPerformancePhase.BOUNDARY
            materialEngine.animateBoundaryBounce()
        }
    }

    suspend fun clear() {
        if (!active) return
        try {
            materialEngine.clear()
        } finally {
            active = false
            destinationPreview.end()
            performancePhase = PaperPerformancePhase.IDLE
        }
    }

    fun clearImmediately() {
        materialEngine.clearImmediately()
        active = false
        destinationPreview.end()
        performancePhase = PaperPerformancePhase.IDLE
    }

    fun releaseBufferIfIdle() {
        materialEngine.releaseBufferIfIdle()
        if (!active) destinationPreview.release()
    }

    fun dispose() {
        materialEngine.dispose()
        destinationPreview.release()
        active = false
        performancePhase = PaperPerformancePhase.IDLE
    }
}

@Composable
internal fun PaperCurlOverlay(
    state: PaperCurlState,
    modifier: Modifier = Modifier,
    patina: Float = 0.35f,
    tone: MaterialPageTone = MaterialPageTone.LIGHT
) {
    val reducedMotion = LocalVeilReducedMotion.current
    LaunchedEffect(state, patina, tone, reducedMotion) {
        state.configureReducedMotion(reducedMotion)
        state.materialEngine.configurePatina(patina)
        state.materialEngine.configureTone(tone)
    }

    val presentedEpoch by state.materialEngine.presentedSheetEpoch.collectAsState()
    // A nested GLTextureView must not occlude Readium's native touch recipient.
    // Share sibling hit-testing at the full visual overlay boundary.
    Box(modifier = modifier.readerVisualOnlyInput()) {
        // The adjacent page lies UNDER the actual source leaf. Never upload it
        // as the reverse of that leaf. Pin it for this gesture, including release.
        val destination = state.destinationPreview.activeSnapshot
        if (BuildConfig.NATIVE_PAPER_REBUILD && state.active &&
            presentedEpoch == state.materialEngine.sheetEpoch && destination != null &&
            !destination.bitmap.isRecycled) {
            val ink = androidx.compose.runtime.remember(destination) { destination.bitmap.asImageBitmap() }
            Canvas(Modifier.fillMaxSize().readerVisualOnlyAccessibility()) {
                if (size.width.toInt() == ink.width && size.height.toInt() == ink.height) {
                    drawImage(ink, dstSize = IntSize(ink.width, ink.height))
                }
            }
        }
        // Explicit build selection, never an automatic fallback after failure.
        // The release gate and original renderer remain unchanged for rollback.
        if (BuildConfig.NATIVE_PAPER_REBUILD) {
            NativeMaterialPageOverlay(
                state = state.materialEngine,
                modifier = Modifier.fillMaxSize().readerVisualOnlyAccessibility(),
                onRendererStatus = state::updateRendererStatus
            )
        } else {
            GpuMaterialPageOverlay(
                state = state.materialEngine,
                modifier = Modifier.fillMaxSize().readerVisualOnlyAccessibility(),
                onRendererStatus = state::updateRendererStatus
            )
        }

        // The rebuilt reader keeps diagnostics in traces, leaving book ink clear.
        // Preserve the original HUD only for the explicit legacy backend.
        if (BuildConfig.DEBUG && !BuildConfig.NATIVE_PAPER_REBUILD) {
            val label = when {
                !MaterialPageEngineRollout.isEnabled() ->
                    "PAPER · GPU CANONICAL · DISABLED"
                state.rendererStatus == GpuMaterialPageRendererStatus.INITIALIZING ->
                    "PAPER · GPU CANONICAL · INITIALIZING · A${state.debugBeginAttempts}"
                state.rendererStatus == GpuMaterialPageRendererStatus.UNSUPPORTED ->
                    "PAPER · GPU CANONICAL · GPU UNSUPPORTED · A${state.debugBeginAttempts}"
                state.rendererStatus == GpuMaterialPageRendererStatus.FAILED ->
                    "PAPER · GPU CANONICAL · GPU FAILED · A${state.debugBeginAttempts}"
                state.rendererStatus == GpuMaterialPageRendererStatus.REDUCED_MOTION ->
                    "PAPER · GPU CANONICAL · REDUCED MOTION · A${state.debugBeginAttempts}"
                state.materialEngine.presentationTimedOut ->
                    "PAPER · GPU CANONICAL · PRESENTATION TIMED OUT · A${state.debugBeginAttempts}"
                state.lastBeginFailed ->
                    "PAPER · GPU CANONICAL · CAPTURE FAILED " +
                        "(${state.materialEngine.lastSnapshotFailureReason ?: "UNKNOWN"}) " +
                        "· A${state.debugBeginAttempts}"
                state.active && presentedEpoch != state.materialEngine.sheetEpoch ->
                    "PAPER · GPU CANONICAL · WAITING FOR FRAME · A${state.debugBeginAttempts}"
                state.active ->
                    "PAPER · GPU CANONICAL · ACTIVE · A${state.debugBeginAttempts}"
                else ->
                    "PAPER · GPU CANONICAL · READY · A${state.debugBeginAttempts}"
            }
            Text(
                text = label,
                color = Color(0xFFFFC857),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clearAndSetSemantics { }
            )
        }
    }
}
