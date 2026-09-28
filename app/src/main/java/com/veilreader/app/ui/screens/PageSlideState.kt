package com.veilreader.app.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.veilreader.app.ui.theme.VeilMotion
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max

@Stable
internal class PageSlideState {
    var snapshot: Bitmap? by mutableStateOf(null); private set
    var translationX: Float by mutableFloatStateOf(0f); private set
    var side: PaperCurlSide by mutableStateOf(PaperCurlSide.RIGHT); private set
    var direction: PaperTurnDirection by mutableStateOf(PaperTurnDirection.FORWARD); private set
    var active: Boolean by mutableStateOf(false); private set
    private var width = 0f
    private var snapshotBuffer: Bitmap? = null

    fun begin(view: View, side: PaperCurlSide, direction: PaperTurnDirection): Boolean {
        if (active || view.width <= 0 || view.height <= 0) return false
        val bitmap = capture(view) ?: return false
        width = view.width.toFloat()
        this.side = side
        this.direction = direction
        snapshot = bitmap
        translationX = 0f
        active = true
        return true
    }

    fun updateDrag(offsetX: Float) {
        if (!active || width <= 0f) return
        val inward = if (side == PaperCurlSide.RIGHT) (-offsetX).coerceAtLeast(0f) else offsetX.coerceAtLeast(0f)
        val moved = inward * pageSlideDragResponse((inward / width).coerceIn(0f, 1f))
        translationX = if (side == PaperCurlSide.RIGHT) -moved else moved
    }

    fun progress(): Float = if (!active || width <= 0f) 0f else (abs(translationX) / width).coerceIn(0f, 1f)

    suspend fun animateTapTurn() {
        if (!active) return
        val target = completionTarget()
        val anim = Animatable(translationX)
        anim.animateTo(target, keyframes {
            durationMillis = 300
            0f at 0
            target * 0.16f at 70
            target * 0.72f at 210
            target at 300
        }) { translationX = value }
    }

    suspend fun animateComplete(releaseVelocityDpPerSec: Float) {
        if (!active) return
        val target = completionTarget()
        val speed = abs(releaseVelocityDpPerSec)
        val anim = Animatable(translationX)
        if (speed >= 900f) {
            val duration = (220f - ((speed - 900f) / 2200f).coerceIn(0f, 1f) * 90f).toInt()
            anim.animateTo(target, tween(duration, easing = FastOutSlowInEasing)) { translationX = value }
        } else {
            anim.animateTo(target, spring(dampingRatio = 0.92f, stiffness = Spring.StiffnessMediumLow)) { translationX = value }
        }
    }

    suspend fun animateCancel() {
        if (!active) return
        val anim = Animatable(translationX)
        anim.animateTo(0f, spring(dampingRatio = 0.92f, stiffness = Spring.StiffnessMedium)) { translationX = value }
    }

    suspend fun animateBoundaryBounce() {
        if (!active || width <= 0f) return
        val sign = if (side == PaperCurlSide.RIGHT) -1f else 1f
        val anim = Animatable(translationX)
        anim.animateTo(sign * width * 0.055f, tween(VeilMotion.PAPER_BOUNDARY_IN_MS, easing = FastOutSlowInEasing)) { translationX = value }
        anim.animateTo(0f, tween(VeilMotion.PAPER_BOUNDARY_OUT_MS, easing = FastOutSlowInEasing)) { translationX = value }
    }

    suspend fun clear() {
        snapshot = null
        width = 0f
        translationX = 0f
        delay(VeilMotion.FRAME_SETTLE_MS)
        active = false
    }

    fun dispose() {
        snapshot = null
        snapshotBuffer?.takeIf { !it.isRecycled }?.recycle()
        snapshotBuffer = null
        translationX = 0f
        width = 0f
        active = false
    }

    private fun completionTarget(): Float = if (side == PaperCurlSide.RIGHT) -width else width

    private fun capture(view: View): Bitmap? = runCatching {
        val w = max(1, view.width)
        val h = max(1, view.height)
        val reusable = snapshotBuffer?.takeIf { !it.isRecycled && it.width == w && it.height == h && it.config == Bitmap.Config.ARGB_8888 }
        val bitmap = reusable ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
            snapshotBuffer?.takeIf { old -> !old.isRecycled }?.recycle()
            snapshotBuffer = it
        }
        bitmap.eraseColor(android.graphics.Color.TRANSPARENT)
        view.draw(AndroidCanvas(bitmap))
        bitmap
    }.getOrNull()
}

internal fun pageSlideDragResponse(inwardFraction: Float): Float {
    val t = inwardFraction.coerceIn(0f, 1f)
    val smooth = t * t * (3f - 2f * t)
    return 0.80f + smooth * 0.17f
}

internal fun shouldCommitPageSlideTurn(inwardDistance: Float, width: Float, density: Float, releaseVelocityPxPerSec: Float): Boolean {
    if (inwardDistance <= 0f || width <= 0f) return false
    val d = density.coerceAtLeast(0.1f)
    val distance = max(88f * d, width * 0.20f)
    val flickDistance = max(30f * d, width * 0.03f)
    return inwardDistance >= distance || (inwardDistance >= flickDistance && releaseVelocityPxPerSec >= 820f * d)
}

@Composable
internal fun PageSlideOverlay(state: PageSlideState, modifier: Modifier = Modifier) {
    val bitmap = state.snapshot ?: return
    if (!state.active || bitmap.isRecycled) return
    val progress = state.progress()
    val edgeX = if (state.translationX <= 0f) bitmap.width + state.translationX else state.translationX
    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val shadow = (18f + progress * 28f).dp.toPx()
            val start = (edgeX - shadow * 0.5f).coerceIn(0f, size.width)
            val widthPx = shadow.coerceAtMost((size.width - start).coerceAtLeast(0f))
            if (widthPx > 0f) drawRect(
                brush = Brush.horizontalGradient(
                    if (state.translationX <= 0f) listOf(Color.Transparent, Color.Black.copy(alpha = 0.18f * progress))
                    else listOf(Color.Black.copy(alpha = 0.18f * progress), Color.Transparent),
                    startX = start,
                    endX = start + widthPx
                ),
                topLeft = Offset(start, 0f),
                size = Size(widthPx, size.height)
            )
        }
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                translationX = state.translationX
                scaleX = 1f - progress * 0.012f
                alpha = 1f - progress * 0.035f
            }
        )
    }
}
