package com.veilreader.app.ui.reader.material

import android.app.ActivityManager
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.PointF
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.veilreader.app.BuildConfig
import kotlinx.coroutines.delay
import kotlin.math.hypot
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
 * Controlled rollout gate for the single GPU Material Page Engine.
 * Release remains disabled until build/device verification explicitly promotes v2.
 */
internal object MaterialPageEngineRollout {
    const val DEFAULT_ENABLED: Boolean = false
    @Volatile
    private var debugOverride: Boolean? = null

    @Volatile
    private var previewPreset: MaterialPagePreset = MaterialPagePreset.MATTE_BOOK

    fun isEnabled(): Boolean =
        debugOverride ?: if (BuildConfig.DEBUG) true else DEFAULT_ENABLED

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

    var backSnapshot: Bitmap? by mutableStateOf(null)
        private set

    var progress: Float by mutableFloatStateOf(0f)
        private set

    var verticalBias: Float by mutableFloatStateOf(0f)
        private set

    var pullOriginY: Float by mutableFloatStateOf(0.5f)
        private set

    var diagonalPull: Float by mutableFloatStateOf(0f)
        private set

    var pointerTravel: Float by mutableFloatStateOf(0f)
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
    private val snapshotBuffers = arrayOfNulls<Bitmap>(2)
    private val backSnapshotBuffers = arrayOfNulls<Bitmap>(2)
    private var snapshotBufferCursor = -1
    private var backSnapshotBufferCursor = -1
    private var backSnapshotAllowed = true
    private var liftCueEmitted = false

    fun configureProfile(value: MaterialPageProfile) {
        if (!active) profile = value
    }

    fun setSensorySink(value: MaterialPageSensorySink?) {
        sensorySink = value
    }

    fun configureReducedMotion(value: Boolean) {
        reducedMotion = value
    }

    fun configurePatina(value: Float) {
        patina = value.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.35f
    }

    fun configureTone(value: MaterialPageTone) {
        tone = value
    }

    fun prepareBuffer(view: View): Boolean {
        if (active || view.width <= 0 || view.height <= 0) return false
        val nextSlot = (snapshotBufferCursor + 1) and 1
        val warmed = obtainReusableBuffer(
            current = snapshotBuffers[nextSlot],
            view = view
        ) ?: return false
        snapshotBuffers[nextSlot] = warmed
        // Do not advance/reset the cursor here. begin() claims exactly this next
        // slot, preserving ping-pong separation from the bitmap the GL thread
        // may still be uploading from the previous turn.
        return true
    }

    fun begin(
        view: View,
        side: MaterialPageSide,
        profile: MaterialPageProfile = this.profile
    ): Boolean {
        if (active || view.width <= 0 || view.height <= 0) return false
        val bitmap = captureIntoSourceBuffer(view) ?: return false

        width = view.width.toFloat()
        height = view.height.toFloat()
        density = view.resources.displayMetrics.density
            .takeIf { it.isFinite() }
            ?.coerceIn(0.75f, 4f)
            ?: 1f
        val memory = view.context.getSystemService(
            android.content.Context.ACTIVITY_SERVICE
        ) as? ActivityManager
        backSnapshotAllowed =
            shouldCaptureMaterialBackSnapshot(
                lowMemoryDevice = memory?.isLowRamDevice == true,
                memoryClassMb = memory?.memoryClass ?: 256,
                pageWidthPx = view.width,
                pageHeightPx = view.height
            )
        this.side = side
        this.profile = profile
        snapshot = bitmap
        backSnapshot = null
        progress = 0f
        verticalBias = 0f
        pullOriginY = 0.5f
        diagonalPull = 0f
        pointerTravel = 0f
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
        pullOriginY = materialPageStablePullOrigin(
            startY = start.y,
            heightPx = height
        )
        val slopeDenominator =
            inward.coerceAtLeast(width * 0.08f)
        diagonalPull =
            (offset.y / slopeDenominator)
                .takeIf { it.isFinite() }
                ?.coerceIn(-1f, 1f)
                ?.let { slope ->
                    slope * (sample.rawProgress / 0.14f).coerceIn(0f, 1f)
                }
                ?: 0f
        pointerTravel =
            (
                hypot(
                    inward.coerceAtLeast(0f).toDouble(),
                    offset.y.toDouble()
                ) / width.toDouble().coerceAtLeast(1.0)
                ).toFloat()
                .takeIf { it.isFinite() }
                ?.coerceIn(0f, 1.5f)
                ?: sample.rawProgress
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

    private fun acceptAnimatedProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
        pointerTravel = progress
    }

    fun prepareTapGrip() {
        if (!active) return
        pullOriginY = materialPageTapPullOrigin(profile)
        diagonalPull = materialPageTapDiagonalPull(profile)
        pointerTravel = progress.coerceAtLeast(
            materialPageTapLiftFraction(profile)
        )
    }

    /**
     * Captures the previewed destination after Readium navigation has settled.
     * This becomes the physical back-side texture of the lifted leaf. Failure is
     * non-fatal: the GPU renderer falls back to mirrored ink-through from the
     * source snapshot.
     */
    fun captureBack(view: View): Boolean {
        if (
            !active ||
            !backSnapshotAllowed ||
            view.width <= 0 ||
            view.height <= 0
        ) {
            return false
        }
        val bitmap = captureBackSnapshot(view) ?: return false
        backSnapshot = bitmap
        return true
    }

    suspend fun animateTapTurn() {
        if (!active) return
        sensorySink?.emit(
            materialPageSensoryCue(
                profile = profile,
                action = MaterialPageSensoryAction.COMPLETE
            )
        )


        if (reducedMotion) {
            visualAlpha = 0.08f
            acceptAnimatedProgress(1f)
        } else {
            val anim = Animatable(progress)
            val liftTarget =
                materialPageTapLiftFraction(profile)
                    .coerceAtLeast(progress)
            if (progress + 0.001f < liftTarget) {
                anim.animateTo(
                    targetValue = liftTarget,
                    animationSpec = tween(
                        durationMillis = materialPageTapLiftDurationMillis(profile),
                        easing = FastOutSlowInEasing
                    )
                ) {
                    acceptAnimatedProgress(value)
                }
            }

            val launchVelocity =
                (
                    0.78f /
                        profile.physics.apparentMass.coerceIn(0.6f, 1.5f)
                    ).coerceIn(0.52f, 1.10f)
            anim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = materialPageSpringDamping(
                        profile = profile,
                        cancelling = false
                    ),
                    stiffness =
                        materialPageSpringStiffness(profile) * 0.94f,
                    visibilityThreshold = 0.001f
                ),
                initialVelocity = launchVelocity
            ) {
                acceptAnimatedProgress(value)
            }
        }

    }

    suspend fun animateComplete(
        releaseVelocityDpPerSec: Float = 0f
    ) {
        if (!active) return
        sensorySink?.emit(
            materialPageSensoryCue(
                profile = profile,
                action = MaterialPageSensoryAction.COMPLETE,
                velocityDpPerSec = releaseVelocityDpPerSec
            )
        )


        if (reducedMotion) {
            visualAlpha = 0.08f
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
                acceptAnimatedProgress(value)
            }
        }

    }

    suspend fun animateCancel(
        releaseVelocityDpPerSec: Float = 0f
    ) {
        if (!active) return
        sensorySink?.emit(
            materialPageSensoryCue(
                profile = profile,
                action = MaterialPageSensoryAction.CANCEL
            )
        )


        if (reducedMotion) {
            visualAlpha = 1f
            acceptAnimatedProgress(0f)
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
                acceptAnimatedProgress(value)
            }
        }

    }

    suspend fun animateBoundaryBounce() {
        if (!active) return
        sensorySink?.emit(
            materialPageSensoryCue(
                profile = profile,
                action = MaterialPageSensoryAction.BOUNDARY
            )
        )


        if (reducedMotion) {
            visualAlpha = 1f
            progress = 0f
        } else {
            val anim = Animatable(progress)
            anim.animateTo(0.045f, tween(72)) {
                acceptAnimatedProgress(value)
            }
            anim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 1f,
                    stiffness = materialPageSpringStiffness(profile) * 1.12f,
                    visibilityThreshold = 0.001f
                )
            ) {
                acceptAnimatedProgress(value)
            }
        }

    }

    suspend fun clear() {
        snapshot = null
        backSnapshot = null
        progress = 0f
        verticalBias = 0f
        pullOriginY = 0.5f
        diagonalPull = 0f
        pointerTravel = 0f
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
        backSnapshot = null
        progress = 0f
        verticalBias = 0f
        pullOriginY = 0.5f
        diagonalPull = 0f
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
        diagonalPull: Float = 0f,
        pointerTravel: Float = progress,
        side: MaterialPageSide = MaterialPageSide.RIGHT,
        profile: MaterialPageProfile = this.profile,
        reducedMotion: Boolean = false,
        patina: Float = this.patina,
        tone: MaterialPageTone = this.tone
    ) {
        snapshot = bitmap
        backSnapshot = null
        width = bitmap.width.toFloat()
        height = bitmap.height.toFloat()
        this.progress =
            progress.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
        this.verticalBias =
            verticalBias.takeIf { it.isFinite() }?.coerceIn(-0.18f, 0.18f) ?: 0f
        val safePullOrigin =
            pullOriginY.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.5f
        this.pullOriginY = materialPageStablePullOrigin(
            startY = safePullOrigin * height,
            heightPx = height
        )
        this.diagonalPull =
            diagonalPull.takeIf { it.isFinite() }?.coerceIn(-1f, 1f) ?: 0f
        this.pointerTravel =
            pointerTravel.takeIf { it.isFinite() }?.coerceIn(0f, 1.5f)
                ?: this.progress
        this.side = side
        this.profile = profile
        this.reducedMotion = reducedMotion
        configurePatina(patina)
        configureTone(tone)
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
        val safeVelocity =
            velocityDpPerSec.takeIf { it.isFinite() } ?: 0f
        return safeVelocity / widthDp
    }

    fun releaseBufferIfIdle() {
        if (active || snapshot != null || backSnapshot != null) return
        // Do not manually recycle buffers that may still be referenced by the GL
        // render thread. Dropping ownership lets Android reclaim them once all
        // in-flight frame references are gone.
        snapshotBuffers[0] = null
        snapshotBuffers[1] = null
        backSnapshotBuffers[0] = null
        backSnapshotBuffers[1] = null
        snapshotBufferCursor = -1
        backSnapshotBufferCursor = -1
    }

    fun dispose() {
        clearImmediately()
        releaseBufferIfIdle()
    }

    private fun captureIntoSourceBuffer(view: View): Bitmap? =
        runCatching {
            val bitmap = obtainSnapshotBuffer(view)
                ?: return@runCatching null
            bitmap.eraseColor(android.graphics.Color.TRANSPARENT)
            view.draw(AndroidCanvas(bitmap))
            bitmap
        }.getOrNull()

    private fun obtainSnapshotBuffer(view: View): Bitmap? {
        snapshotBufferCursor = (snapshotBufferCursor + 1) and 1
        val slot = snapshotBufferCursor
        return obtainReusableBuffer(
            current = snapshotBuffers[slot],
            view = view
        ).also { resolved ->
            if (resolved != null) {
                snapshotBuffers[slot] = resolved
            }
        }
    }

    private fun captureBackSnapshot(view: View): Bitmap? =
        runCatching {
            val bitmap = obtainBackSnapshotBuffer(view)
                ?: return@runCatching null
            bitmap.eraseColor(android.graphics.Color.TRANSPARENT)
            view.draw(AndroidCanvas(bitmap))
            bitmap
        }.getOrNull()

    private fun obtainBackSnapshotBuffer(view: View): Bitmap? {
        backSnapshotBufferCursor = (backSnapshotBufferCursor + 1) and 1
        val slot = backSnapshotBufferCursor
        return obtainReusableBuffer(
            current = backSnapshotBuffers[slot],
            view = view
        ).also { resolved ->
            if (resolved != null) {
                backSnapshotBuffers[slot] = resolved
            }
        }
    }

    private fun obtainReusableBuffer(
        current: Bitmap?,
        view: View
    ): Bitmap? =
        runCatching {
            val targetWidth = max(1, view.width)
            val targetHeight = max(1, view.height)
            current?.takeIf {
                !it.isRecycled &&
                    it.width == targetWidth &&
                    it.height == targetHeight &&
                    it.config == Bitmap.Config.ARGB_8888
            } ?: Bitmap.createBitmap(
                targetWidth,
                targetHeight,
                Bitmap.Config.ARGB_8888
            )
        }.getOrNull()
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
