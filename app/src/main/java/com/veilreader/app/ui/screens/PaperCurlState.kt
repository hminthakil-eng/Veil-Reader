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
        GpuMaterialPageRendererStatus.READY
    )
        private set

    internal val materialEngine = MaterialPageEngineState(
        initialProfile = MaterialPageEngineRollout.selectedProfile()
    )

    fun configureReducedMotion(value: Boolean) {
        materialEngine.configureReducedMotion(value)
    }

    fun updateRendererStatus(value: GpuMaterialPageRendererStatus) {
        rendererStatus = value
    }

    internal fun usingMaterialEngine(): Boolean = active

    fun prepareBuffer(view: View): Boolean {
        if (active || view.width <= 0 || view.height <= 0) return false
        return materialEngine.prepareBuffer(view)
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
            rendererStatus == GpuMaterialPageRendererStatus.FAILED ||
            rendererStatus == GpuMaterialPageRendererStatus.UNSUPPORTED ||
            active ||
            view.width <= 0 ||
            view.height <= 0
        ) {
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
        }
        return started
    }

    fun captureMaterialBack(view: View): Boolean =
        active && materialEngine.captureBack(view)

    fun prepareMaterialTapGrip() {
        if (active) {
            materialEngine.prepareTapGrip()
        }
    }

    fun updateDrag(start: PointF, offset: PointF) {
        if (active) {
            materialEngine.updateDrag(start, offset)
        }
    }

    fun dragProgress(): Float =
        if (active) materialEngine.dragProgress() else 0f

    suspend fun animateTapTurn() {
        if (active) materialEngine.animateTapTurn()
    }

    suspend fun animateComplete(releaseVelocityDpPerSec: Float = 0f) {
        if (active) materialEngine.animateComplete(releaseVelocityDpPerSec)
    }

    suspend fun animateCancel(releaseVelocityDpPerSec: Float = 0f) {
        if (active) materialEngine.animateCancel(releaseVelocityDpPerSec)
    }

    suspend fun animateBoundaryBounce() {
        if (active) materialEngine.animateBoundaryBounce()
    }

    suspend fun clear() {
        if (!active) return
        materialEngine.clear()
        active = false
    }

    fun clearImmediately() {
        materialEngine.clearImmediately()
        active = false
    }

    fun releaseBufferIfIdle() {
        materialEngine.releaseBufferIfIdle()
    }

    fun dispose() {
        materialEngine.dispose()
        active = false
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
                    "PAPER · GPU v2 · DISABLED"
                state.rendererStatus == GpuMaterialPageRendererStatus.UNSUPPORTED ->
                    "PAPER · GPU v2 · GPU UNSUPPORTED · A${state.debugBeginAttempts}"
                state.rendererStatus == GpuMaterialPageRendererStatus.FAILED ->
                    "PAPER · GPU v2 · GPU FAILED · A${state.debugBeginAttempts}"
                state.rendererStatus == GpuMaterialPageRendererStatus.REDUCED_MOTION ->
                    "PAPER · GPU v2 · REDUCED MOTION · A${state.debugBeginAttempts}"
                state.lastBeginFailed ->
                    "PAPER · GPU v2 · CAPTURE FAILED · A${state.debugBeginAttempts}"
                state.active ->
                    "PAPER · GPU v2 · ACTIVE · A${state.debugBeginAttempts}"
                else ->
                    "PAPER · GPU v2 · READY · A${state.debugBeginAttempts}"
            }
            Text(
                text = label,
                color = Color(0xFFFFC857),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
            )
        }
    }
}
