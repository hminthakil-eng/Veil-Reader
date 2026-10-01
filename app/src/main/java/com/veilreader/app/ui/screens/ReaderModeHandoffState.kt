package com.veilreader.app.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.ReaderNavigationMode
import com.veilreader.app.ui.theme.VeilMotion
import kotlin.math.max

/**
 * Keeps the last fully painted Reader frame above Readium while a navigation-mode preference
 * handoff reflows the renderer underneath it. This is deliberately short lived and only exists to
 * eliminate single-frame PAPER/SLIDE/SCROLL flashes.
 */
@Stable
internal class ReaderModeHandoffState {
    var snapshot: Bitmap? by mutableStateOf(null)
        private set

    var alpha: Float by mutableFloatStateOf(0f)
        private set

    private var snapshotBuffer: Bitmap? = null

    fun capture(view: View): Boolean {
        if (view.width <= 0 || view.height <= 0) return false
        val bitmap = captureBitmap(view) ?: return false
        snapshot = bitmap
        alpha = 1f
        return true
    }

    suspend fun release(reducedMotion: Boolean) {
        if (snapshot == null) return
        if (reducedMotion) {
            clearImmediately()
            return
        }

        val anim = Animatable(alpha)
        anim.animateTo(
            targetValue = 0f,
            animationSpec = tween(VeilMotion.MICRO_FAST_MS.coerceAtMost(110))
        ) {
            alpha = value
        }
        snapshot = null
        alpha = 0f
    }

    fun clearImmediately() {
        snapshot = null
        alpha = 0f
    }

    fun dispose() {
        clearImmediately()
        snapshotBuffer?.takeIf { !it.isRecycled }?.recycle()
        snapshotBuffer = null
    }

    private fun captureBitmap(view: View): Bitmap? =
        runCatching {
            val targetWidth = max(1, view.width)
            val targetHeight = max(1, view.height)
            val reusable = snapshotBuffer?.takeIf {
                !it.isRecycled &&
                    it.width == targetWidth &&
                    it.height == targetHeight &&
                    it.config == Bitmap.Config.ARGB_8888
            }
            val bitmap = reusable ?: Bitmap.createBitmap(
                targetWidth,
                targetHeight,
                Bitmap.Config.ARGB_8888
            ).also { created ->
                snapshotBuffer?.takeIf { !it.isRecycled }?.recycle()
                snapshotBuffer = created
            }

            bitmap.eraseColor(android.graphics.Color.TRANSPARENT)
            view.draw(AndroidCanvas(bitmap))
            bitmap
        }.getOrNull()
}

@Composable
internal fun ReaderModeHandoffOverlay(
    state: ReaderModeHandoffState,
    modifier: Modifier = Modifier
) {
    val bitmap = state.snapshot ?: return
    if (bitmap.isRecycled || state.alpha <= 0.001f) return

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = state.alpha }
    )
}

internal fun shouldCaptureReaderModeHandoff(
    format: BookFormat,
    previousMode: ReaderNavigationMode,
    requestedMode: ReaderNavigationMode,
    fixedLayoutSpreadChanged: Boolean = false
): Boolean =
    format == BookFormat.EPUB &&
        (previousMode != requestedMode || fixedLayoutSpreadChanged)

internal fun readerPreferenceSettleFrames(
    previousMode: ReaderNavigationMode,
    requestedMode: ReaderNavigationMode
): Int =
    if (previousMode != requestedMode) 2 else 1
