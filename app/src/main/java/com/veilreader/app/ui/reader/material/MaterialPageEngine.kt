package com.veilreader.app.ui.reader.material

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

internal enum class MaterialPageSide {
    LEFT,
    RIGHT
}

internal enum class MaterialPageTone {
    LIGHT,
    SEPIA,
    DARK
}

/**
 * Controlled rollout gate. Production stays on the proven legacy curl until source
 * verification and real-device judgment explicitly promote Material Page Engine v1.
 */
internal object MaterialPageEngineRollout {
    const val DEFAULT_ENABLED: Boolean = false

    @Volatile
    private var debugOverride: Boolean? = null

    @Volatile
    private var previewPreset: MaterialPagePreset = MaterialPagePreset.MATTE_BOOK

    fun isEnabled(): Boolean = debugOverride ?: DEFAULT_ENABLED

    fun selectedProfile(): MaterialPageProfile =
        MaterialPageProfiles.canonical(previewPreset)

    internal fun setDebugOverride(enabled: Boolean?) {
        debugOverride = enabled
    }

    internal fun setPreviewPreset(preset: MaterialPagePreset) {
        previewPreset = preset
    }
}

@Stable
internal class MaterialPageEngineState(
    initialProfile: MaterialPageProfile = MaterialPageProfiles.MatteBook,
    private var sensorySink: MaterialPageSensorySink? = null
) {
    var snapshot: Bitmap? by mutableStateOf(null)
        private set

    var progress: Float by mutableFloatStateOf(0f)
        private set

    var verticalBias: Float by mutableFloatStateOf(0f)
        private set

    var pullOriginY: Float by mutableFloatStateOf(0.5f)
        private set

    var visualAlpha: Float by mutableFloatStateOf(1f)
        private set

    var side: MaterialPageSide by mutableStateOf(MaterialPageSide.RIGHT)
        private set

    var profile: MaterialPageProfile by mutableStateOf(initialProfile)
        private set

    var reducedMotion: Boolean by mutableStateOf(false)
        private set

    var patina: Float by mutableFloatStateOf(0.35f)
        private set

    var tone: MaterialPageTone by mutableStateOf(MaterialPageTone.LIGHT)
        private set

    var active: Boolean by mutableStateOf(false)
        private set

    private var width = 0f
    private var height = 0f
    private var snapshotBuffer: Bitmap? = null
    private var liftCueEmitted = false

    fun setProfile(value: MaterialPageProfile) {
        if (!active) profile = value
    }

    fun setSensorySink(value: MaterialPageSensorySink?) {
        sensorySink = value
    }

    fun configureReducedMotion(value: Boolean) {
        reducedMotion = value
    }

    fun setPatina(value: Float) {
        patina = value.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.35f
    }

    fun setTone(value: MaterialPageTone) {
        tone = value
    }

    fun begin(
        view: View,
        side: MaterialPageSide,
        profile: MaterialPageProfile = this.profile
    ): Boolean {
        if (active || view.width <= 0 || view.height <= 0) return false
        val bitmap = capture(view) ?: return false

        width = view.width.toFloat()
        height = view.height.toFloat()
        this.side = side
        this.profile = profile
        snapshot = bitmap
        progress = 0f
        verticalBias = 0f
        pullOriginY = 0.5f
        visualAlpha = 1f
        liftCueEmitted = false
        active = true
        return true
    }

    fun updateDrag(start: PointF, offset: PointF) {
        if (!active || width <= 0f || height <= 0f) return
        val inward = when (side) {
            MaterialPageSide.RIGHT -> -offset.x
            MaterialPageSide.LEFT -> offset.x
        }
        val sample = materialPageDragSample(
            inwardDistancePx = inward.coerceAtLeast(0f),
            verticalDistancePx = offset.y,
            widthPx = width,
            heightPx = height,
            profile = profile
        )

        progress = sample.progress
        verticalBias = sample.verticalBias
        pullOriginY = (start.y / height).coerceIn(0f, 1f)
        visualAlpha = if (reducedMotion) {
            (1f - sample.rawProgress * 0.08f).coerceIn(0.92f, 1f)
        } else {
            1f
        }

        if (
            !liftCueEmitted &&
            sample.rawProgress >= profile.sensory.liftThreshold
        ) {
            liftCueEmitted = true
            sensorySink?.emit(
                materialPageSensoryCue(
                    profile = profile,
                    action = MaterialPageSensoryAction.LIFT_THRESHOLD
                )
            )
        }
    }

    fun dragProgress(): Float = progress.coerceIn(0f, 1f)

    suspend fun animateTapTurn() {
        if (!active) return

        if (reducedMotion) {
            val alpha = Animatable(visualAlpha)
            alpha.animateTo(
                targetValue = 0.08f,
                animationSpec = tween(96)
            ) {
                visualAlpha = value
            }
            progress = 1f
        } else {
            val anim = Animatable(progress)
            anim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = materialPageTapDurationMillis(profile),
                    easing = FastOutSlowInEasing
                )
            ) {
                progress = value
            }
        }

        sensorySink?.emit(
            materialPageSensoryCue(
                profile = profile,
                action = MaterialPageSensoryAction.COMPLETE
            )
        )
    }

    suspend fun animateComplete(
        releaseVelocityDpPerSec: Float = 0f
    ) {
        if (!active) return

        if (reducedMotion) {
            val alpha = Animatable(visualAlpha)
            alpha.animateTo(
                targetValue = 0.08f,
                animationSpec = tween(
                    durationMillis = materialPageSettleDurationMillis(
                        progress = progress,
                        completing = true,
                        velocityDpPerSec = releaseVelocityDpPerSec,
                        profile = profile
                    ).coerceAtMost(110)
                )
            ) {
                visualAlpha = value
            }
            progress = 1f
        } else {
            val anim = Animatable(progress)
            anim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = materialPageSpringDamping(
                        profile = profile,
                        cancelling = false
                    ),
                    stiffness = materialPageSpringStiffness(profile),
                    visibilityThreshold = 0.001f
                )
            ) {
                progress = value.coerceIn(0f, 1f)
            }
        }

        sensorySink?.emit(
            materialPageSensoryCue(
                profile = profile,
                action = MaterialPageSensoryAction.COMPLETE,
                velocityDpPerSec = releaseVelocityDpPerSec
            )
        )
    }

    suspend fun animateCancel() {
        if (!active) return

        if (reducedMotion) {
            val alpha = Animatable(visualAlpha)
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(72)
            ) {
                visualAlpha = value
            }
            progress = 0f
        } else {
            val anim = Animatable(progress)
            anim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = materialPageSpringDamping(
                        profile = profile,
                        cancelling = true
                    ),
                    stiffness = materialPageSpringStiffness(profile) * 1.08f,
                    visibilityThreshold = 0.001f
                )
            ) {
                progress = value.coerceIn(0f, 1f)
            }
        }

        sensorySink?.emit(
            materialPageSensoryCue(
                profile = profile,
                action = MaterialPageSensoryAction.CANCEL
            )
        )
    }

    suspend fun animateBoundaryBounce() {
        if (!active) return

        if (reducedMotion) {
            val alpha = Animatable(visualAlpha)
            alpha.animateTo(0.94f, tween(52)) { visualAlpha = value }
            alpha.animateTo(1f, tween(68)) { visualAlpha = value }
        } else {
            val anim = Animatable(progress)
            anim.animateTo(0.045f, tween(72)) {
                progress = value
            }
            anim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 1f,
                    stiffness = materialPageSpringStiffness(profile) * 1.12f,
                    visibilityThreshold = 0.001f
                )
            ) {
                progress = value.coerceIn(0f, 1f)
            }
        }

        sensorySink?.emit(
            materialPageSensoryCue(
                profile = profile,
                action = MaterialPageSensoryAction.BOUNDARY
            )
        )
    }

    suspend fun clear() {
        snapshot = null
        progress = 0f
        verticalBias = 0f
        pullOriginY = 0.5f
        visualAlpha = 1f
        width = 0f
        height = 0f
        // Preserve one frame of input lock so the snapshot cannot be reused while
        // Compose is still drawing the previous overlay.
        delay(16L)
        active = false
        liftCueEmitted = false
    }

    fun clearImmediately() {
        snapshot = null
        progress = 0f
        verticalBias = 0f
        pullOriginY = 0.5f
        visualAlpha = 1f
        width = 0f
        height = 0f
        active = false
        liftCueEmitted = false
    }

    /**
     * Debug/test inspection hook for production renderer states. It deliberately
     * feeds the real overlay instead of maintaining a separate demo renderer.
     */
    internal fun installInspectableFrame(
        bitmap: Bitmap,
        progress: Float,
        verticalBias: Float = 0f,
        pullOriginY: Float = 0.5f,
        side: MaterialPageSide = MaterialPageSide.RIGHT,
        profile: MaterialPageProfile = this.profile,
        reducedMotion: Boolean = false,
        patina: Float = this.patina,
        tone: MaterialPageTone = this.tone
    ) {
        snapshot = bitmap
        width = bitmap.width.toFloat()
        height = bitmap.height.toFloat()
        this.progress = progress.coerceIn(0f, 1f)
        this.verticalBias = verticalBias.coerceIn(-0.18f, 0.18f)
        this.pullOriginY = pullOriginY.coerceIn(0f, 1f)
        this.side = side
        this.profile = profile
        this.reducedMotion = reducedMotion
        setPatina(patina)
        setTone(tone)
        visualAlpha = if (reducedMotion) {
            materialPageReducedMotionAlpha(
                progress = this.progress,
                completing = true
            )
        } else {
            1f
        }
        active = true
    }

    fun releaseBufferIfIdle() {
        if (active || snapshot != null) return
        snapshotBuffer?.takeIf { !it.isRecycled }?.recycle()
        snapshotBuffer = null
    }

    fun dispose() {
        clearImmediately()
        releaseBufferIfIdle()
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

private class MaterialPageRenderScratch {
    val matrix = Matrix()
    val path = AndroidPath()
    val source = FloatArray(8)
    val destination = FloatArray(8)
    val contentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
        isDither = true
    }
    val shadePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val sourceRect = Rect()
    val destinationRect = RectF()
}

@Composable
internal fun MaterialPageOverlay(
    state: MaterialPageEngineState,
    modifier: Modifier = Modifier
) {
    val bitmap = state.snapshot ?: return
    if (!state.active || bitmap.isRecycled) return

    val scratch = androidx.compose.runtime.remember { MaterialPageRenderScratch() }
    val progress = state.progress
    val verticalBias = state.verticalBias
    val pullOriginY = state.pullOriginY
    val visualAlpha = state.visualAlpha
    val profile = state.profile
    val side = state.side
    val reducedMotion = state.reducedMotion
    val patina = state.patina
    val tone = state.tone

    Canvas(modifier.fillMaxSize()) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas

        val scaleX = size.width / bitmap.width.toFloat().coerceAtLeast(1f)
        val scaleY = size.height / bitmap.height.toFloat().coerceAtLeast(1f)
        val native = drawContext.canvas.nativeCanvas
        native.save()
        native.scale(scaleX, scaleY)

        if (reducedMotion) {
            scratch.contentPaint.alpha = (visualAlpha.coerceIn(0f, 1f) * 255f)
                .roundToInt()
                .coerceIn(0, 255)
            native.drawBitmap(bitmap, 0f, 0f, scratch.contentPaint)

            scratch.detailPaint.color = android.graphics.Color.BLACK
            scratch.detailPaint.alpha = 14
            scratch.detailPaint.strokeWidth = 1.25f
            val edgeX = if (side == MaterialPageSide.RIGHT) {
                bitmap.width.toFloat() - 1f
            } else {
                1f
            }
            native.drawLine(
                edgeX,
                0f,
                edgeX,
                bitmap.height.toFloat(),
                scratch.detailPaint
            )
            native.restore()
            return@Canvas
        }

        val canonical = materialPageGeometry(
            width = bitmap.width.toFloat(),
            height = bitmap.height.toFloat(),
            progress = progress,
            verticalBias = verticalBias,
            profile = profile,
            pullOriginY = pullOriginY
        )
        val frame = if (side == MaterialPageSide.LEFT) {
            mirrorMaterialPageFrame(canonical, bitmap.width.toFloat())
        } else {
            canonical
        }

        if (!isFiniteMaterialPageFrame(frame)) {
            scratch.contentPaint.alpha = 255
            native.drawBitmap(bitmap, 0f, 0f, scratch.contentPaint)
            native.restore()
            return@Canvas
        }

        val flatLeft = frame.flatStartX
            .roundToInt()
            .coerceIn(0, bitmap.width)
        val flatRight = ceil(frame.flatEndX.toDouble())
            .toInt()
            .coerceIn(flatLeft, bitmap.width)
        if (flatRight > flatLeft) {
            scratch.sourceRect.set(flatLeft, 0, flatRight, bitmap.height)
            scratch.destinationRect.set(
                flatLeft.toFloat(),
                0f,
                flatRight.toFloat(),
                bitmap.height.toFloat()
            )
            scratch.contentPaint.alpha = 255
            native.drawBitmap(
                bitmap,
                scratch.sourceRect,
                scratch.destinationRect,
                scratch.contentPaint
            )
        }

        frame.strips.forEachIndexed { index, strip ->
            if (strip.sourceRight <= strip.sourceLeft) return@forEachIndexed

            scratch.source[0] = strip.sourceLeft
            scratch.source[1] = 0f
            scratch.source[2] = strip.sourceRight
            scratch.source[3] = 0f
            scratch.source[4] = strip.sourceRight
            scratch.source[5] = bitmap.height.toFloat()
            scratch.source[6] = strip.sourceLeft
            scratch.source[7] = bitmap.height.toFloat()

            scratch.destination[0] = strip.topLeft.x
            scratch.destination[1] = strip.topLeft.y
            scratch.destination[2] = strip.topRight.x
            scratch.destination[3] = strip.topRight.y
            scratch.destination[4] = strip.bottomRight.x
            scratch.destination[5] = strip.bottomRight.y
            scratch.destination[6] = strip.bottomLeft.x
            scratch.destination[7] = strip.bottomLeft.y

            scratch.path.reset()
            scratch.path.moveTo(strip.topLeft.x, strip.topLeft.y)
            scratch.path.lineTo(strip.topRight.x, strip.topRight.y)
            scratch.path.lineTo(strip.bottomRight.x, strip.bottomRight.y)
            scratch.path.lineTo(strip.bottomLeft.x, strip.bottomLeft.y)
            scratch.path.close()

            scratch.matrix.reset()
            if (
                scratch.matrix.setPolyToPoly(
                    scratch.source,
                    0,
                    scratch.destination,
                    0,
                    4
                )
            ) {
                native.save()
                native.clipPath(scratch.path)
                native.concat(scratch.matrix)
                scratch.contentPaint.alpha = 255
                native.drawBitmap(bitmap, 0f, 0f, scratch.contentPaint)
                native.restore()
            }

            val optics = profile.optics
            val baseShadeAlpha = if (strip.backFacing) {
                (
                    0.86f +
                        optics.roughness * 0.035f +
                        patina * optics.patinaResponse * 0.035f -
                        optics.translucency * 0.10f -
                        optics.inkGhosting * 0.16f
                    ).coerceIn(0.76f, 0.92f)
            } else {
                0f
            }

            if (baseShadeAlpha > 0f) {
                scratch.shadePaint.color = Color(
                    materialPageToneAdjustedArgb(optics.backArgb, tone)
                ).toArgb()
                scratch.shadePaint.alpha =
                    (baseShadeAlpha * 255f).roundToInt().coerceIn(0, 255)
                scratch.shadePaint.style = Paint.Style.FILL
                native.drawPath(scratch.path, scratch.shadePaint)
            }

            // Deterministic micro-tonal variation: enough to communicate grain and age,
            // never enough to compete with publication text.
            val variationUnit = (((index * 37) % 11) - 5) / 5f
            val tonalAlpha = (
                kotlin.math.abs(variationUnit) *
                    optics.grain *
                    (0.25f + patina * optics.patinaResponse * 0.75f) *
                    0.035f *
                    255f
                ).roundToInt().coerceIn(0, 9)
            if (tonalAlpha > 0) {
                scratch.shadePaint.color =
                    if (variationUnit >= 0f) android.graphics.Color.WHITE
                    else android.graphics.Color.BLACK
                scratch.shadePaint.alpha = tonalAlpha
                native.drawPath(scratch.path, scratch.shadePaint)
            }

            val light = strip.lightResponse
            if (light > 0.01f) {
                scratch.shadePaint.color = android.graphics.Color.WHITE
                scratch.shadePaint.alpha = (
                    light *
                        (0.06f + optics.specularResponse * 0.18f) *
                        255f
                    ).roundToInt().coerceIn(0, 78)
                native.drawPath(scratch.path, scratch.shadePaint)
            } else if (light < -0.01f) {
                scratch.shadePaint.color = android.graphics.Color.BLACK
                scratch.shadePaint.alpha = (
                    -light *
                        (0.06f + optics.roughness * 0.12f) *
                        255f
                    ).roundToInt().coerceIn(0, 54)
                native.drawPath(scratch.path, scratch.shadePaint)
            }

            if (
                strip.backFacing &&
                optics.directionalFiber > 0.35f &&
                index % 2 == 0
            ) {
                val centerTopX = (strip.topLeft.x + strip.topRight.x) * 0.5f
                val centerBottomX =
                    (strip.bottomLeft.x + strip.bottomRight.x) * 0.5f
                scratch.detailPaint.color = android.graphics.Color.BLACK
                scratch.detailPaint.alpha = (
                    optics.directionalFiber *
                        strip.lift *
                        (8f + patina * optics.patinaResponse * 8f)
                    ).roundToInt().coerceIn(0, 14)
                scratch.detailPaint.strokeWidth =
                    (0.35f + optics.grain * 0.50f)
                native.drawLine(
                    centerTopX,
                    bitmap.height * 0.10f,
                    centerBottomX,
                    bitmap.height * 0.90f,
                    scratch.detailPaint
                )
            }
        }

        // The binding/contact shadow is deliberately restrained; it communicates
        // attachment and thickness without turning the page into theatrical 3D.
        val creaseX = frame.creaseTop.x
        val edgeBody = (
            profile.optics.edgeBody *
                (0.94f + patina * profile.optics.patinaResponse * 0.12f)
            ).coerceIn(0f, 1f)
        val contactAlpha =
            (frame.lift * (0.08f + edgeBody * 0.12f) * 255f)
                .roundToInt()
                .coerceIn(0, 52)
        scratch.detailPaint.color = android.graphics.Color.BLACK
        scratch.detailPaint.alpha = contactAlpha
        scratch.detailPaint.strokeWidth = 2.2f + edgeBody * 2.4f
        native.drawLine(
            creaseX,
            frame.creaseTop.y,
            creaseX,
            frame.creaseBottom.y,
            scratch.detailPaint
        )

        val edgeAlpha =
            (frame.lift * (0.08f + profile.optics.specularResponse * 0.14f) * 255f)
                .roundToInt()
                .coerceIn(0, 46)
        scratch.detailPaint.color = Color(
            materialPageToneAdjustedArgb(profile.optics.edgeArgb, tone)
        ).toArgb()
        scratch.detailPaint.alpha = edgeAlpha
        scratch.detailPaint.strokeWidth = 0.9f + edgeBody * 0.9f
        val edgeOffset =
            if (side == MaterialPageSide.RIGHT) -1.2f else 1.2f
        native.drawLine(
            creaseX + edgeOffset,
            frame.creaseTop.y,
            creaseX + edgeOffset,
            frame.creaseBottom.y,
            scratch.detailPaint
        )

        native.restore()
    }
}


internal fun materialPageToneAdjustedArgb(
    argb: Long,
    tone: MaterialPageTone
): Long {
    val a = ((argb ushr 24) and 0xFF).toInt()
    val r = ((argb ushr 16) and 0xFF).toInt()
    val g = ((argb ushr 8) and 0xFF).toInt()
    val b = (argb and 0xFF).toInt()

    fun channel(value: Int, scale: Float, bias: Int): Int =
        (value * scale + bias).roundToInt().coerceIn(0, 255)

    val adjusted = when (tone) {
        MaterialPageTone.LIGHT -> intArrayOf(r, g, b)
        MaterialPageTone.SEPIA -> intArrayOf(
            channel(r, 0.98f, 2),
            channel(g, 0.94f, 1),
            channel(b, 0.84f, 0)
        )
        MaterialPageTone.DARK -> intArrayOf(
            channel(r, 0.16f, 14),
            channel(g, 0.15f, 13),
            channel(b, 0.17f, 16)
        )
    }

    return (
        (a.toLong() shl 24) or
            (adjusted[0].toLong() shl 16) or
            (adjusted[1].toLong() shl 8) or
            adjusted[2].toLong()
        )
}
