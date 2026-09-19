package com.veilreader.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.veilreader.app.ui.paperturn.gpu.GpuPaperCurlView

@Stable
internal class GpuPaperCurlBridge {
    private var view: GpuPaperCurlView? = null

    var ready by mutableStateOf(false)
        private set

    var active by mutableStateOf(false)
        private set

    private var progress by mutableFloatStateOf(0f)

    fun attach(target: GpuPaperCurlView) {
        if (view === target) return
        view = target
        target.setSupportListener { supported ->
            ready = supported
            if (!supported && active) {
                active = false
                progress = 0f
            }
        }
    }

    fun detach(target: GpuPaperCurlView) {
        if (view !== target) return
        if (active) target.endCurl()
        view = null
        ready = false
        active = false
        progress = 0f
    }
    fun begin(
        snapshot: Bitmap?,
        side: PaperCurlSide,
        startY: Float,
        backColor: Int
    ): Boolean {
        val target = view ?: return false
        val bitmap = snapshot ?: return false
        if (!ready || bitmap.isRecycled) return false

        val gpuSide = when (side) {
            PaperCurlSide.LEFT -> GpuPaperCurlView.SIDE_LEFT
            PaperCurlSide.RIGHT -> GpuPaperCurlView.SIDE_RIGHT
        }
        val started = target.begin(
            bitmap,
            gpuSide,
            startY,
            backColor
        )
        active = started
        if (started) progress = 0f
        return started
    }

    fun updateDrag(
        startX: Float,
        startY: Float,
        offsetX: Float,
        offsetY: Float,
        curlProgress: Float
    ) {
        if (!active) return
        view?.updateDrag(startX, startY, offsetX, offsetY)
        progress = curlProgress.coerceIn(0f, 1f)
    }

    suspend fun animateTapTurn() {
        animateTo(1f, 430)
    }

    suspend fun animateComplete() {
        animateTo(1f, 165)
    }

    suspend fun animateCancel() {
        animateTo(0f, 145)
    }

    suspend fun animateBoundaryBounce() {
        animateTo(0.11f, 80)
        animateTo(0f, 105)
    }
    fun end() {
        if (active) view?.endCurl()
        active = false
        progress = 0f
    }

    private suspend fun animateTo(
        target: Float,
        durationMs: Int
    ) {
        if (!active) return
        val animator = Animatable(progress)
        animator.animateTo(
            targetValue = target,
            animationSpec = tween(
                durationMillis = durationMs,
                easing = FastOutSlowInEasing
            )
        ) {
            progress = value
            view?.setProgress(value)
        }
    }
}

@Composable
internal fun AdaptivePaperCurlOverlay(
    state: PaperCurlState,
    gpu: GpuPaperCurlBridge,
    config: PaperCurlVisualConfig,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                GpuPaperCurlView(context).also(gpu::attach)
            },
            update = gpu::attach,
            onRelease = { view ->
                gpu.detach(view)
                view.releaseGpu()
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!gpu.active) {
            PaperCurlOverlay(
                state = state,
                config = config,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
