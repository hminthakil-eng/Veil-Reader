package com.veilreader.app.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
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
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.roundToInt

@Stable
internal class SlidePageState {
    var snapshot: Bitmap? by mutableStateOf(null)
        private set
    var offsetPx: Float by mutableFloatStateOf(0f)
        private set
    var active: Boolean by mutableStateOf(false)
        private set

    private var width = 0f
    private var snapshotBuffer: Bitmap? = null

    fun begin(view: View): Boolean {
        if (active || view.width <= 0 || view.height <= 0) return false
        val bitmap = capture(view) ?: return false
        width = view.width.toFloat()
        snapshot = bitmap
        offsetPx = 0f
        active = true
        return true
    }

    fun updateDrag(rawOffsetX: Float) {
        if (!active || width <= 0f) return
        val fraction = (abs(rawOffsetX) / width).coerceIn(0f, 1f)
        val response = slideHorizontalDragResponse(fraction)
        offsetPx = (rawOffsetX * response)
            .coerceIn(-width * 1.04f, width * 1.04f)
    }

    fun dragProgress(): Float =
        if (!active || width <= 0f) 0f
        else (abs(offsetPx) / width).coerceIn(0f, 1f)

    suspend fun animateComplete(directionSign: Float, velocityDpPerSec: Float = 0f) {
        if (!active || width <= 0f) return
        val target = width * directionSign.coerceIn(-1f, 1f)
        val duration = slideCompletionDurationMillis(
            progress = dragProgress(),
            velocityDpPerSec = velocityDpPerSec
        )
        val anim = Animatable(offsetPx)
        anim.animateTo(
            targetValue = target,
            animationSpec = tween(duration)
        ) {
            offsetPx = value
        }
    }

    suspend fun animateCancel() {
        if (!active) return
        val anim = Animatable(offsetPx)
        anim.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = 0.90f,
                stiffness = Spring.StiffnessMediumLow
            )
        ) {
            offsetPx = value
        }
    }

    suspend fun animateBoundaryBounce(directionSign: Float) {
        if (!active || width <= 0f) return
        val anim = Animatable(offsetPx)
        val peek = width * 0.055f * directionSign.coerceIn(-1f, 1f)
        anim.animateTo(peek, tween(95)) { offsetPx = value }
        anim.animateTo(
            0f,
            spring(
                dampingRatio = 0.82f,
                stiffness = Spring.StiffnessMedium
            )
        ) { offsetPx = value }
    }

    suspend fun clear() {
        clearVisual(keepInputLock = true)
    }

    /**
     * Lifecycle/disposal escape hatch. This intentionally skips the one-frame lock used by normal
     * animation completion because teardown must leave no stale page snapshot behind.
     */
    fun clearImmediately() {
        snapshot = null
        offsetPx = 0f
        width = 0f
        active = false
    }

    private suspend fun clearVisual(keepInputLock: Boolean) {
        snapshot = null
        offsetPx = 0f
        width = 0f
        if (keepInputLock) delay(VeilMotion.FRAME_SETTLE_MS)
        active = false
    }

    fun dispose() {
        clearImmediately()
        snapshotBuffer?.takeIf { !it.isRecycled }?.recycle()
        snapshotBuffer = null
    }

    private fun capture(view: View): Bitmap? =
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
internal fun SlidePageOverlay(
    state: SlidePageState,
    modifier: Modifier = Modifier
) {
    val bitmap = state.snapshot ?: return
    if (!state.active || bitmap.isRecycled) return
    val progress = state.dragProgress()
    val shadowIntensity = slideEdgeShadowIntensity(progress)
    val direction = when {
        state.offsetPx < 0f -> -1f
        state.offsetPx > 0f -> 1f
        else -> 0f
    }

    Box(modifier.fillMaxSize()) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = state.offsetPx
                    alpha = 1f - progress * 0.04f
                }
        )

        if (direction != 0f && progress > 0.001f) {
            Canvas(Modifier.fillMaxSize()) {
                val edgeX = if (direction < 0f) {
                    size.width + state.offsetPx
                } else {
                    state.offsetPx
                }.coerceIn(0f, size.width)

                val shadowWidth =
                    (16.dp.toPx() + 38.dp.toPx() * shadowIntensity)
                val rawStartX = if (direction < 0f) {
                    edgeX
                } else {
                    edgeX - shadowWidth
                }
                val rawEndX = if (direction < 0f) {
                    edgeX + shadowWidth
                } else {
                    edgeX
                }
                val startX = rawStartX.coerceIn(0f, size.width)
                val endX = rawEndX.coerceIn(0f, size.width)
                val visibleWidth = (endX - startX).coerceAtLeast(0f)
                if (visibleWidth > 0.5f) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colorStops = if (direction < 0f) {
                                arrayOf(
                                    0f to Color.Black.copy(
                                        alpha = 0.26f * shadowIntensity
                                    ),
                                    1f to Color.Transparent
                                )
                            } else {
                                arrayOf(
                                    0f to Color.Transparent,
                                    1f to Color.Black.copy(
                                        alpha = 0.26f * shadowIntensity
                                    )
                                )
                            },
                            startX = startX,
                            endX = endX
                        ),
                        topLeft = Offset(startX, 0f),
                        size = androidx.compose.ui.geometry.Size(
                            visibleWidth,
                            size.height
                        )
                    )
                }
            }
        }
    }
}


/**
 * A weighted slide should feel attached to the finger without looking like a native renderer
 * swipe. It starts with mass, then progressively catches up as the turn becomes intentional.
 */
internal fun slideHorizontalDragResponse(progress: Float): Float {
    val t = progress.coerceIn(0f, 1f)
    val smooth = t * t * (3f - 2f * t)
    return 0.76f + smooth * 0.22f
}

/**
 * Contact shadow belongs to the lifted/moving edge, so it disappears both at rest and when the
 * source page has fully left the viewport.
 */
internal fun slideEdgeShadowIntensity(progress: Float): Float =
    sin(progress.coerceIn(0f, 1f).toDouble() * PI)
        .toFloat()
        .coerceIn(0f, 1f)

internal fun slideCompletionDurationMillis(
    progress: Float,
    velocityDpPerSec: Float
): Int {
    val remaining = 1f - progress.coerceIn(0f, 1f)
    val speed = abs(velocityDpPerSec)
    val fullTravelMillis = when {
        speed >= 1_800f -> 140f
        speed >= 900f -> 175f
        else -> 220f
    }
    // Even a nearly completed gesture needs a perceptible settle frame, but it must not crawl.
    return (88f + (fullTravelMillis - 88f) * remaining)
        .roundToInt()
        .coerceIn(88, 220)
}
