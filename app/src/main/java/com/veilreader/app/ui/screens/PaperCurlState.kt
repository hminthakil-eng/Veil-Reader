package com.veilreader.app.ui.screens

import android.graphics.PointF
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.veilreader.app.BuildConfig
import com.veilreader.app.ui.reader.material.GpuMaterialPageOverlay
import com.veilreader.app.ui.reader.material.GpuMaterialPageRendererStatus
import com.veilreader.app.ui.reader.material.MaterialPageEngineRollout
import com.veilreader.app.ui.reader.material.MaterialPageEngineState
import com.veilreader.app.ui.reader.material.MaterialPageSide
import com.veilreader.app.ui.reader.material.MaterialPageTone

internal enum class PaperCurlSide { LEFT, RIGHT }
internal enum class PaperTurnDirection { FORWARD, BACKWARD }

internal enum class PaperPerformancePhase {
    IDLE,
    CAPTURE,
    DRAG,
    RELEASE,
    CANCEL,
    GL_RECREATE
}

/**
 * Single Paper runtime for the GPU Material Page Engine.
 *
 * Legacy Canvas curl and Material Canvas v1 were intentionally removed from the
 * v2 build. Git history/PR #372 remains the rollback source; this runtime has only
 * one visual Paper implementation so a device test can never silently fall back to
 * an older curl and disguise renderer/gesture defects.
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

    var debugBeginAttempts: Int by mutableStateOf(0)
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

    val snapshotSourceRevision: Long
        get() = materialEngine.snapshotSourceRevision

    fun configureReducedMotion(value: Boolean) {
        materialEngine.configureReducedMotion(value)
    }

    fun updateRendererStatus(value: GpuMaterialPageRendererStatus) {
        rendererStatus = value
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

    suspend fun prepareSnapshot(view: View): Boolean {
        if (active || view.width <= 0 || view.height <= 0) return false
        performancePhase = PaperPerformancePhase.CAPTURE
        return try {
            materialEngine.prepareSnapshot(view)
        } finally {
            if (!active) {
                performancePhase = PaperPerformancePhase.IDLE
            }
        }
    }

    fun invalidateSnapshotSource() {
        materialEngine.invalidateSnapshotSource()
    }

    fun begin(
        view: View,
        side: PaperCurlSide,
        direction: PaperTurnDirection
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

        performancePhase = PaperPerformancePhase.CAPTURE
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
            performancePhase = PaperPerformancePhase.DRAG
        } else {
            performancePhase = PaperPerformancePhase.IDLE
        }
        return started
    }

    fun prepareMaterialTapGrip() {
        if (active) {
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
            performancePhase = PaperPerformancePhase.CANCEL
            materialEngine.animateBoundaryBounce()
        }
    }

    suspend fun clear() {
        if (!active) return
        materialEngine.clear()
        active = false
        performancePhase = PaperPerformancePhase.IDLE
    }

    fun clearImmediately() {
        materialEngine.clearImmediately()
        active = false
        performancePhase = PaperPerformancePhase.IDLE
    }

    fun releaseBufferIfIdle() {
        materialEngine.releaseBufferIfIdle()
    }

    fun dispose() {
        materialEngine.dispose()
        active = false
        performancePhase = PaperPerformancePhase.IDLE
    }
}

@Composable
internal fun PaperCurlOverlay(
    state: PaperCurlState,
    patina: Float = 0.35f,
    tone: MaterialPageTone = MaterialPageTone.LIGHT,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(state, patina, tone) {
        state.materialEngine.configurePatina(patina)
        state.materialEngine.configureTone(tone)
    }

    Box(modifier = modifier) {
        GpuMaterialPageOverlay(
            state = state.materialEngine,
            modifier = Modifier.fillMaxSize(),
            onRendererStatus = state::updateRendererStatus
        )

        if (BuildConfig.DEBUG) {
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
                state.lastBeginFailed ->
                    "PAPER · GPU CANONICAL · CAPTURE FAILED " +
                        "(${state.materialEngine.lastSnapshotFailureReason ?: "UNKNOWN"}) " +
                        "· A${state.debugBeginAttempts}"
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
