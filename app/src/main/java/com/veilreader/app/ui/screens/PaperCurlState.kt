package com.veilreader.app.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.veilreader.app.ui.theme.VeilMotion
import kotlinx.coroutines.delay
import kotlin.math.max

internal enum class PaperCurlSide { LEFT, RIGHT }
internal enum class PaperTurnDirection { FORWARD, BACKWARD }

@Stable
internal class PaperCurlState {
    var snapshot: Bitmap? by mutableStateOf(null)
        private set
    var edge: PaperCurlEdge by mutableStateOf(
        PaperCurlEdge(Offset.Zero, Offset.Zero)
    )
        private set

    var side: PaperCurlSide by mutableStateOf(PaperCurlSide.RIGHT)
        private set

    var direction: PaperTurnDirection by mutableStateOf(
        PaperTurnDirection.FORWARD
    )
        private set

    var active: Boolean by mutableStateOf(false)
        private set

    private var width = 0f
    private var height = 0f
    private var snapshotBuffer: Bitmap? = null
    private var captureGeneration = 0L

    fun begin(
        view: View,
        side: PaperCurlSide,
        direction: PaperTurnDirection
    ): Boolean {
        if (active || view.width <= 0 || view.height <= 0) return false
        val bitmap = capture(view) ?: return false

        width = view.width.toFloat()
        height = view.height.toFloat()
        this.side = side
        this.direction = direction
        snapshot = bitmap
        captureGeneration += 1L
        edge = rightEdge()
        active = true
        return true
    }
    fun updateDrag(start: PointF, offset: PointF) {
        if (!active || width <= 0f || height <= 0f) return

        val actualStart = Offset(start.x, start.y)
        val actualCurrent = Offset(
            start.x + offset.x,
            start.y + offset.y
        )
        val canonicalStart = canonical(actualStart)
        val rawCanonicalCurrent = canonical(actualCurrent)
        val inwardFraction = paperInwardDragFraction(
            start = canonicalStart,
            current = rawCanonicalCurrent,
            pageWidth = width
        )
        val canonicalCurrent = paperWeightedDragCurrent(
            start = canonicalStart,
            current = rawCanonicalCurrent,
            response = paperHorizontalDragResponse(inwardFraction),
            verticalResponse = paperVerticalDragResponse(inwardFraction)
        ).let {
            Offset(
                it.x.coerceIn(-width * 0.25f, width * 1.25f),
                it.y.coerceIn(-height * 0.25f, height * 1.25f)
            )
        }

        edge = paperCurlPageEdge(
            width = width,
            start = canonicalStart,
            current = canonicalCurrent
        )
    }

    fun dragProgress(): Float {
        if (!active || width <= 0f) return 0f
        val centerX = (edge.top.x + edge.bottom.x) * 0.5f
        return (1f - centerX / width).coerceIn(0f, 1f)
    }

    suspend fun animateTapTurn() {
        if (!active) return
        val anim = Animatable(
            edge,
            PaperCurlEdge.VectorConverter,
            PaperCurlEdge.VisibilityThreshold
        )
        anim.animateTo(
            targetValue = leftEdge(),
            animationSpec = keyframes {
                durationMillis = VeilMotion.PAPER_TAP_TURN_MS
                rightEdge() at 0
                PaperCurlEdge(
                    top = Offset(width * 0.96f, height * 0.16f),
                    bottom = Offset(width * 0.72f, height * 0.94f)
                ) at 105
                PaperCurlEdge(
                    top = Offset(width * 0.76f, height * 0.08f),
                    bottom = Offset(width * 0.32f, height)
                ) at 260
                leftEdge() at VeilMotion.PAPER_TAP_TURN_MS
            }
        ) {
            edge = value
        }
    }

    suspend fun animateComplete(releaseVelocityDpPerSec: Float = 0f) {
        if (!active) return
        val profile = paperReleaseProfile(releaseVelocityDpPerSec)
        if (profile.regime == PaperReleaseRegime.FLING) {
            // A flick is a release regime, not the slow manipulation animation sped up.
            // The detached sheet snaps into a tighter fold before settling off the book.
            val anim = Animatable(
                edge,
                PaperCurlEdge.VectorConverter,
                PaperCurlEdge.VisibilityThreshold
            )
            val bendX = width * (1f - profile.completionBias)
            val bend = PaperCurlEdge(
                top = Offset(bendX * 1.10f, height * 0.035f),
                bottom = Offset(bendX * 0.48f, height)
            )
            anim.animateTo(
                targetValue = leftEdge(),
                animationSpec = keyframes {
                    durationMillis = profile.durationMillis
                    edge at 0
                    bend at (profile.durationMillis * 0.46f).toInt()
                    leftEdge() at profile.durationMillis
                }
            ) {
                edge = value
            }
        } else {
            animateTo(
                target = leftEdge(),
                dampingRatio = 0.88f,
                stiffness = Spring.StiffnessMediumLow
            )
        }
    }

    suspend fun animateCancel() {
        if (!active) return
        animateTo(
            target = rightEdge(),
            dampingRatio = 0.96f,
            stiffness = Spring.StiffnessMedium
        )
    }

    suspend fun animateBoundaryBounce() {
        if (!active) return
        val anim = Animatable(
            edge,
            PaperCurlEdge.VectorConverter,
            PaperCurlEdge.VisibilityThreshold
        )
        val peek = PaperCurlEdge(
            top = Offset(width * 0.94f, height * 0.16f),
            bottom = Offset(width * 0.86f, height * 0.90f)
        )
        anim.animateTo(
            peek,
            tween(VeilMotion.PAPER_BOUNDARY_IN_MS, easing = FastOutSlowInEasing)
        ) {
            edge = value
        }
        anim.animateTo(
            rightEdge(),
            tween(VeilMotion.PAPER_BOUNDARY_OUT_MS, easing = FastOutSlowInEasing)
        ) {
            edge = value
        }
    }

    suspend fun clear() {
        val generationAtClear = captureGeneration
        snapshot = null
        width = 0f
        height = 0f
        edge = PaperCurlEdge(Offset.Zero, Offset.Zero)

        // Keep one frame of input lock so Compose fully drops the overlay
        // before the reusable bitmap can be drawn into again.
        delay(VeilMotion.FRAME_SETTLE_MS)
        active = false

        // Full-resolution snapshots can exceed 15–20 MB on modern phones. Keep the buffer
        // briefly so rapid page turns stay allocation-free, then recycle it if no newer turn
        // has claimed the buffer. The generation check prevents an older cleanup coroutine
        // from recycling a bitmap reused by a newer gesture.
        delay(VeilMotion.PAPER_SNAPSHOT_BUFFER_RETENTION_MS)
        if (
            !active &&
            snapshot == null &&
            captureGeneration == generationAtClear
        ) {
            snapshotBuffer?.takeIf { !it.isRecycled }?.recycle()
            snapshotBuffer = null
        }
    }

    fun dispose() {
        snapshot = null
        snapshotBuffer?.takeIf { !it.isRecycled }?.recycle()
        snapshotBuffer = null
        captureGeneration += 1L
        active = false
    }
    private suspend fun animateTo(
        target: PaperCurlEdge,
        dampingRatio: Float,
        stiffness: Float
    ) {
        val anim = Animatable(
            edge,
            PaperCurlEdge.VectorConverter,
            PaperCurlEdge.VisibilityThreshold
        )
        anim.animateTo(
            targetValue = target,
            animationSpec = spring(
                dampingRatio = dampingRatio,
                stiffness = stiffness,
                visibilityThreshold = PaperCurlEdge.VisibilityThreshold
            )
        ) {
            edge = value
        }
    }

    private fun canonical(point: Offset): Offset =
        if (side == PaperCurlSide.RIGHT) {
            point
        } else {
            Offset(width - point.x, point.y)
        }

    private fun rightEdge(): PaperCurlEdge =
        PaperCurlEdge(
            Offset(width, 0f),
            Offset(width, height)
        )

    private fun leftEdge(): PaperCurlEdge =
        PaperCurlEdge(
            Offset(0f, 0f),
            Offset(0f, height)
        )
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
            view.draw(Canvas(bitmap))
            bitmap
        }.getOrNull()
}

internal fun paperCurlPageEdge(
    width: Float,
    start: Offset,
    current: Offset
): PaperCurlEdge {
    // Body swipes must start with an uncurled page, just like edge swipes.
    // Anchor the fold to the page edge and apply only the finger displacement;
    // using the absolute finger position pre-curls the page before it moves.
    val draggedEdge = Offset(width + current.x - start.x, current.y)
    val vector = Offset(width, start.y) - draggedEdge
    val rotated = Offset(-vector.y, vector.x)
    return PaperCurlEdge(
        draggedEdge - rotated + vector / 2f,
        draggedEdge + rotated + vector / 2f
    )
}

@Composable
internal fun PaperCurlOverlay(
    state: PaperCurlState,
    config: PaperCurlVisualConfig,
    modifier: Modifier = Modifier
) {
    val bitmap = state.snapshot ?: return
    if (!state.active || bitmap.isRecycled) return

    val mirror = state.side == PaperCurlSide.LEFT
    val mirrorScale = if (mirror) -1f else 1f
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = mirrorScale
            }
            .paperCurl(
                config = config,
                edgeProvider = { state.edge }
            )
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = mirrorScale
                }
        )
    }
}
