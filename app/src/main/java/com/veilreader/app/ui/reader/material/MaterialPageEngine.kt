package com.veilreader.app.ui.reader.material

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.PointF
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
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.delay
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

    fun selectedPreset(): MaterialPagePreset = previewPreset

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
    private var density = 1f
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
        density = view.resources.displayMetrics.density.coerceAtLeast(0.1f)
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
                ),
                initialVelocity = normalizedReleaseVelocity(
                    releaseVelocityDpPerSec
                ).coerceIn(-1f, 5f)
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

    suspend fun animateCancel(
        releaseVelocityDpPerSec: Float = 0f
    ) {
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
                ),
                initialVelocity = normalizedReleaseVelocity(
                    releaseVelocityDpPerSec
                ).coerceIn(-5f, 3f)
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
        density = 1f
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
        density = 1f
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

    private fun normalizedReleaseVelocity(
        velocityDpPerSec: Float
    ): Float {
        val widthDp = (width / density.coerceAtLeast(0.1f))
            .coerceAtLeast(1f)
        return velocityDpPerSec / widthDp
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
    val mesh = MaterialPageMeshBuffer(36)
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

        val pageWidth = bitmap.width.toFloat()
        val pageHeight = bitmap.height.toFloat()
        updateMaterialPageMesh(
            buffer = scratch.mesh,
            width = pageWidth,
            height = pageHeight,
            progress = progress,
            verticalBias = verticalBias,
            profile = profile,
            pullOriginY = pullOriginY
        )
        val mesh = scratch.mesh
        val mirror = side == MaterialPageSide.LEFT

        val creaseTopX =
            if (mirror) pageWidth - mesh.creaseTopX else mesh.creaseTopX
        val creaseBottomX =
            if (mirror) pageWidth - mesh.creaseBottomX else mesh.creaseBottomX

        scratch.path.reset()
        if (mirror) {
            scratch.path.moveTo(pageWidth, 0f)
            scratch.path.lineTo(creaseTopX, 0f)
            scratch.path.lineTo(creaseBottomX, pageHeight)
            scratch.path.lineTo(pageWidth, pageHeight)
        } else {
            scratch.path.moveTo(0f, 0f)
            scratch.path.lineTo(creaseTopX, 0f)
            scratch.path.lineTo(creaseBottomX, pageHeight)
            scratch.path.lineTo(0f, pageHeight)
        }
        scratch.path.close()
        native.save()
        native.clipPath(scratch.path)
        scratch.contentPaint.alpha = 255
        native.drawBitmap(bitmap, 0f, 0f, scratch.contentPaint)
        native.restore()

        val optics = profile.optics
        val backColor =
            materialPageToneAdjustedArgb(optics.backArgb, tone).toInt()

        for (index in 0 until mesh.segmentCount) {
            val sourceLeft: Float
            val sourceRight: Float
            val topLeftX: Float
            val topLeftY: Float
            val topRightX: Float
            val topRightY: Float
            val bottomLeftX: Float
            val bottomLeftY: Float
            val bottomRightX: Float
            val bottomRightY: Float

            if (mirror) {
                sourceLeft = pageWidth - mesh.sourceRight[index]
                sourceRight = pageWidth - mesh.sourceLeft[index]
                topLeftX = pageWidth - mesh.topRightX[index]
                topLeftY = mesh.topRightY[index]
                topRightX = pageWidth - mesh.topLeftX[index]
                topRightY = mesh.topLeftY[index]
                bottomLeftX = pageWidth - mesh.bottomRightX[index]
                bottomLeftY = mesh.bottomRightY[index]
                bottomRightX = pageWidth - mesh.bottomLeftX[index]
                bottomRightY = mesh.bottomLeftY[index]
            } else {
                sourceLeft = mesh.sourceLeft[index]
                sourceRight = mesh.sourceRight[index]
                topLeftX = mesh.topLeftX[index]
                topLeftY = mesh.topLeftY[index]
                topRightX = mesh.topRightX[index]
                topRightY = mesh.topRightY[index]
                bottomLeftX = mesh.bottomLeftX[index]
                bottomLeftY = mesh.bottomLeftY[index]
                bottomRightX = mesh.bottomRightX[index]
                bottomRightY = mesh.bottomRightY[index]
            }

            if (sourceRight <= sourceLeft) continue

            scratch.source[0] = sourceLeft
            scratch.source[1] = 0f
            scratch.source[2] = sourceRight
            scratch.source[3] = 0f
            scratch.source[4] = sourceRight
            scratch.source[5] = pageHeight
            scratch.source[6] = sourceLeft
            scratch.source[7] = pageHeight

            scratch.destination[0] = topLeftX
            scratch.destination[1] = topLeftY
            scratch.destination[2] = topRightX
            scratch.destination[3] = topRightY
            scratch.destination[4] = bottomRightX
            scratch.destination[5] = bottomRightY
            scratch.destination[6] = bottomLeftX
            scratch.destination[7] = bottomLeftY

            scratch.path.reset()
            scratch.path.moveTo(topLeftX, topLeftY)
            scratch.path.lineTo(topRightX, topRightY)
            scratch.path.lineTo(bottomRightX, bottomRightY)
            scratch.path.lineTo(bottomLeftX, bottomLeftY)
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

            val isBackFacing = mesh.backFacing[index]
            val baseShadeAlpha = if (isBackFacing) {
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
                scratch.shadePaint.color = backColor
                scratch.shadePaint.alpha =
                    (baseShadeAlpha * 255f).roundToInt().coerceIn(0, 255)
                scratch.shadePaint.style = Paint.Style.FILL
                native.drawPath(scratch.path, scratch.shadePaint)
            }

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

            val light = mesh.lightResponse[index]
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
                isBackFacing &&
                optics.directionalFiber > 0.35f &&
                index % 2 == 0
            ) {
                val centerTopX = (topLeftX + topRightX) * 0.5f
                val centerTopY = (topLeftY + topRightY) * 0.5f
                val centerBottomX = (bottomLeftX + bottomRightX) * 0.5f
                val centerBottomY = (bottomLeftY + bottomRightY) * 0.5f
                scratch.detailPaint.color = android.graphics.Color.BLACK
                scratch.detailPaint.alpha = (
                    optics.directionalFiber *
                        mesh.stripLift[index] *
                        (8f + patina * optics.patinaResponse * 8f)
                    ).roundToInt().coerceIn(0, 14)
                scratch.detailPaint.strokeWidth =
                    (0.35f + optics.grain * 0.50f)
                native.drawLine(
                    centerTopX,
                    centerTopY,
                    centerBottomX,
                    centerBottomY,
                    scratch.detailPaint
                )
            }
        }

        // The binding/contact shadow is deliberately restrained; it communicates
        // attachment and thickness without turning the page into theatrical 3D.
        val creaseTopY = mesh.creaseTopY
        val creaseBottomY = mesh.creaseBottomY
        val edgeBody = (
            profile.optics.edgeBody *
                (0.94f + patina * profile.optics.patinaResponse * 0.12f)
            ).coerceIn(0f, 1f)
        val contactAlpha =
            (mesh.lift * (0.08f + edgeBody * 0.12f) * 255f)
                .roundToInt()
                .coerceIn(0, 52)
        scratch.detailPaint.color = android.graphics.Color.BLACK
        scratch.detailPaint.alpha = contactAlpha
        scratch.detailPaint.strokeWidth = 2.2f + edgeBody * 2.4f
        native.drawLine(
            creaseTopX,
            creaseTopY,
            creaseBottomX,
            creaseBottomY,
            scratch.detailPaint
        )

        // Restrained lifted-sheet cast shadow. Multiple cheap lines approximate a
        // soft falloff without allocating a moving gradient shader each frame.
        val shadowDirection =
            if (side == MaterialPageSide.RIGHT) 1f else -1f
        for (step in 1..3) {
            val distance = step * step * 3.2f
            val falloff = 1f / (step.toFloat() * step.toFloat())
            scratch.detailPaint.color = android.graphics.Color.BLACK
            scratch.detailPaint.alpha = (
                mesh.lift *
                    falloff *
                    (0.035f + (1f - profile.optics.roughness) * 0.025f) *
                    255f
                ).roundToInt().coerceIn(0, 18)
            scratch.detailPaint.strokeWidth = 2.5f + step * 2.5f
            native.drawLine(
                creaseTopX + shadowDirection * distance,
                creaseTopY,
                creaseBottomX + shadowDirection * distance,
                creaseBottomY,
                scratch.detailPaint
            )
        }

        val edgeAlpha =
            (mesh.lift * (0.08f + profile.optics.specularResponse * 0.14f) * 255f)
                .roundToInt()
                .coerceIn(0, 46)
        scratch.detailPaint.color =
            materialPageToneAdjustedArgb(profile.optics.edgeArgb, tone).toInt()
        scratch.detailPaint.alpha = edgeAlpha
        scratch.detailPaint.strokeWidth = 0.9f + edgeBody * 0.9f
        val edgeOffset =
            if (side == MaterialPageSide.RIGHT) -1.2f else 1.2f
        native.drawLine(
            creaseTopX + edgeOffset,
            creaseTopY,
            creaseBottomX + edgeOffset,
            creaseBottomY,
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

    if (tone == MaterialPageTone.LIGHT) return argb

    val adjustedR: Int
    val adjustedG: Int
    val adjustedB: Int
    when (tone) {
        MaterialPageTone.LIGHT -> {
            adjustedR = r
            adjustedG = g
            adjustedB = b
        }
        MaterialPageTone.SEPIA -> {
            adjustedR = channel(r, 0.98f, 2)
            adjustedG = channel(g, 0.94f, 1)
            adjustedB = channel(b, 0.84f, 0)
        }
        MaterialPageTone.DARK -> {
            adjustedR = channel(r, 0.16f, 14)
            adjustedG = channel(g, 0.15f, 13)
            adjustedB = channel(b, 0.17f, 16)
        }
    }

    return (
        (a.toLong() shl 24) or
            (adjustedR.toLong() shl 16) or
            (adjustedG.toLong() shl 8) or
            adjustedB.toLong()
        )
}
