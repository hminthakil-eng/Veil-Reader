package com.veilreader.app.ui.reader.material

import android.graphics.Bitmap
import android.graphics.PointF
import android.os.SystemClock
import android.os.Trace
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.veilreader.app.BuildConfig
import com.veilreader.app.diagnostics.ReaderTrace
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

internal data class MaterialPageGpuUploadLease(
    val bitmap: Bitmap,
    val rendererGeneration: Long
)

@Stable
internal class MaterialPageEngineState(
    initialProfile: MaterialPageProfile = MaterialPageProfiles.MatteBook,
    private var sensorySink: MaterialPageSensorySink? = null,
    private val snapshotProvider: MaterialPageImmediateSnapshotProvider =
        ViewDrawImmediateMaterialPageSnapshotProvider
) {
    var snapshot: Bitmap? by mutableStateOf(null)
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

    var edgeTravel: Float by mutableFloatStateOf(0f)
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

    var lastSnapshotFailureReason: MaterialPageSnapshotFailureReason? by mutableStateOf(null)
        private set

    private var width = 0f
    private var height = 0f
    private var density = 1f
    private val snapshotBuffers = arrayOfNulls<Bitmap>(2)
    private var snapshotBufferCursor = -1
    var snapshotSourceRevision: Long by mutableLongStateOf(1L)
        private set
    private var preparedSnapshot: MaterialPagePreparedSnapshot? = null
    private var preparedSnapshotBufferSlot = -1
    private val gpuUploadLeases = mutableListOf<MaterialPageGpuUploadLease>()
    private var releaseBuffersWhenUploadsSettle = false
    private var liftCueEmitted = false

    fun configureProfile(value: MaterialPageProfile) {
        if (!active) profile = value
    }

    fun invalidateSnapshotSource() {
        snapshotSourceRevision =
            nextMaterialPageSnapshotRevision(snapshotSourceRevision)
        preparedSnapshot = null
        preparedSnapshotBufferSlot = -1
    }

    fun setSensorySink(value: MaterialPageSensorySink?) {
        sensorySink = value
    }

    /**
     * Called immediately before a snapshot bitmap is handed to the GL renderer.
     * Identity semantics are intentional: two bitmaps with identical pixels are
     * still independent CPU storage and must have independent upload leases.
     */
    fun markSnapshotSubmittedForGpu(
        bitmap: Bitmap,
        rendererGeneration: Long
    ) {
        if (bitmap.isRecycled || rendererGeneration <= 0L) return
        if (
            gpuUploadLeases.none {
                it.bitmap === bitmap &&
                    it.rendererGeneration == rendererGeneration
            }
        ) {
            gpuUploadLeases += MaterialPageGpuUploadLease(
                bitmap = bitmap,
                rendererGeneration = rendererGeneration
            )
        }
    }

    /**
     * GL calls this only after texImage2D/texSubImage2D returned without a GL error.
     * Until this acknowledgement, capture code is forbidden from overwriting the bitmap.
     */
    fun acknowledgeSnapshotUploaded(
        bitmap: Bitmap,
        rendererGeneration: Long
    ) {
        gpuUploadLeases.removeAll {
            it.bitmap === bitmap &&
                it.rendererGeneration == rendererGeneration
        }
        releaseDeferredBuffersIfPossible()
    }

    /**
     * Releases only leases owned by the GL generation which has definitively died.
     * A newer context may already have leased the same bitmap by the time this
     * callback reaches the UI thread.
     */
    fun abandonGpuUploadLeases(rendererGeneration: Long) {
        if (rendererGeneration <= 0L) return
        gpuUploadLeases.removeAll {
            it.rendererGeneration == rendererGeneration
        }
        releaseDeferredBuffersIfPossible()
    }

    private fun releaseDeferredBuffersIfPossible() {
        if (
            releaseBuffersWhenUploadsSettle &&
            !active &&
            snapshot == null
        ) {
            releaseUnleasedBuffers()
            if (gpuUploadLeases.isEmpty()) {
                snapshotBufferCursor = -1
                releaseBuffersWhenUploadsSettle = false
            }
        }
    }

    internal fun snapshotHasPendingGpuUpload(bitmap: Bitmap?): Boolean =
        bitmap != null &&
            gpuUploadLeases.any { it.bitmap === bitmap }

    fun configureReducedMotion(value: Boolean) {
        reducedMotion = value
    }

    fun configurePatina(value: Float) {
        patina = value.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.35f
    }

    fun configureTone(value: MaterialPageTone) {
        tone = value
    }

    suspend fun prepareSnapshot(view: View): Boolean {
        if (
            active ||
            view.width <= 0 ||
            view.height <= 0 ||
            !view.isAttachedToWindow
        ) {
            return false
        }
        releaseBuffersWhenUploadsSettle = false

        val revision = snapshotSourceRevision
        val widthAtRequest = view.width
        val heightAtRequest = view.height
        val visualWaitStarted = SystemClock.elapsedRealtimeNanos()
        if (
            !awaitMaterialPageSourceVisualReady(
                root = view,
                requestId = revision
            )
        ) {
            ReaderTrace.event(
                name = "paper_snapshot_prepare_not_ready",
                details = "revision=$revision"
            )
            return false
        }
        val visualWaitNanos =
            (SystemClock.elapsedRealtimeNanos() - visualWaitStarted)
                .coerceAtLeast(0L)
        if (
            active ||
            revision != snapshotSourceRevision ||
            widthAtRequest != view.width ||
            heightAtRequest != view.height
        ) {
            return false
        }

        val nextSlot = nextWritableSnapshotBufferSlot()
            ?: return false
        val target = obtainReusableBuffer(
            current = snapshotBuffers[nextSlot],
            view = view
        ) ?: return false
        snapshotBuffers[nextSlot] = target

        val totalStarted = SystemClock.elapsedRealtimeNanos()
        Trace.beginSection("paper.capture.prepare")
        val capture = try {
            snapshotProvider.capture(
                view = view,
                target = target,
                sourceRevision = revision
            )
        } finally {
            Trace.endSection()
        }
        val totalNanos =
            (SystemClock.elapsedRealtimeNanos() - totalStarted)
                .coerceAtLeast(0L)
        val ready = capture as? MaterialPageSnapshotCapture.Ready
            ?: return false
        if (
            active ||
            !materialPageSnapshotCaptureIsCurrent(
                captureRevision = ready.sourceRevision,
                expectedRevision = snapshotSourceRevision
            ) ||
            ready.bitmap.width != view.width ||
            ready.bitmap.height != view.height
        ) {
            return false
        }

        preparedSnapshot = MaterialPagePreparedSnapshot(
            bitmap = ready.bitmap,
            sourceRevision = ready.sourceRevision,
            width = ready.bitmap.width,
            height = ready.bitmap.height,
            capturedAtElapsedNanos = SystemClock.elapsedRealtimeNanos(),
            provider = ready.provider
        )
        preparedSnapshotBufferSlot = nextSlot
        ReaderTrace.event(
            name = "paper_snapshot_prepared",
            details =
                "provider=${ready.provider} revision=${ready.sourceRevision} " +
                    "width=${ready.bitmap.width} height=${ready.bitmap.height} " +
                    "visualWaitUs=${visualWaitNanos / 1_000L} " +
                    "captureUs=${ready.elapsedNanos / 1_000L} " +
                    "totalUs=${totalNanos / 1_000L}"
        )
        return true
    }

    fun begin(
        view: View,
        side: MaterialPageSide,
        profile: MaterialPageProfile = this.profile
    ): Boolean {
        if (active || view.width <= 0 || view.height <= 0) return false
        releaseBuffersWhenUploadsSettle = false
        val prepared = preparedSnapshot
        val usePrepared =
            materialPagePreparedSnapshotIsCurrent(
                prepared = prepared,
                expectedRevision = snapshotSourceRevision,
                expectedWidth = view.width,
                expectedHeight = view.height,
                nowElapsedNanos = SystemClock.elapsedRealtimeNanos()
            ) &&
                preparedSnapshotBufferSlot in snapshotBuffers.indices &&
                prepared != null &&
                snapshotBuffers[preparedSnapshotBufferSlot] === prepared.bitmap
        val capture =
            if (usePrepared && prepared != null) {
                snapshotBufferCursor = preparedSnapshotBufferSlot
                preparedSnapshot = null
                preparedSnapshotBufferSlot = -1
                ReaderTrace.event(
                    name = "paper_snapshot_prepared_consumed",
                    details =
                        "provider=${prepared.provider} " +
                            "revision=${prepared.sourceRevision} " +
                            "ageUs=${
                                (
                                    SystemClock.elapsedRealtimeNanos() -
                                        prepared.capturedAtElapsedNanos
                                    ).coerceAtLeast(0L) / 1_000L
                            }"
                )
                MaterialPageSnapshotCapture.Ready(
                    bitmap = prepared.bitmap,
                    sourceRevision = prepared.sourceRevision,
                    provider = prepared.provider,
                    elapsedNanos = 0L
                )
            } else {
                preparedSnapshot = null
                preparedSnapshotBufferSlot = -1
                captureIntoSourceBuffer(view)
            }
        val ready = when (capture) {
            is MaterialPageSnapshotCapture.Ready -> capture
            is MaterialPageSnapshotCapture.NotReady -> {
                lastSnapshotFailureReason = capture.reason
                return false
            }
            is MaterialPageSnapshotCapture.Failed -> {
                lastSnapshotFailureReason = capture.reason
                return false
            }
        }
        if (
            !materialPageSnapshotCaptureIsCurrent(
                captureRevision = ready.sourceRevision,
                expectedRevision = snapshotSourceRevision
            )
        ) {
            lastSnapshotFailureReason =
                MaterialPageSnapshotFailureReason.STALE_REVISION
            ReaderTrace.event(
                name = "paper_snapshot_stale_rejected",
                details =
                    "captureRevision=${ready.sourceRevision} " +
                        "expectedRevision=$snapshotSourceRevision"
            )
            return false
        }
        lastSnapshotFailureReason = null
        val bitmap = ready.bitmap
        width = view.width.toFloat()
        height = view.height.toFloat()
        density = view.resources.displayMetrics.density
            .takeIf { it.isFinite() }
            ?.coerceIn(0.75f, 4f)
            ?: 1f
        this.side = side
        this.profile = profile
        snapshot = bitmap
        progress = 0f
        verticalBias = 0f
        pullOriginY = 0.5f
        diagonalPull = 0f
        pointerTravel = 0f
        edgeTravel = 0f
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
        edgeTravel = sample.rawProgress
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
        edgeTravel = progress
    }

    fun prepareTapGrip() {
        if (!active) return
        pullOriginY = materialPageTapPullOrigin(profile)
        diagonalPull = materialPageTapDiagonalPull(profile)
        pointerTravel = progress.coerceAtLeast(
            materialPageTapLiftFraction(profile)
        )
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
        progress = 0f
        verticalBias = 0f
        pullOriginY = 0.5f
        diagonalPull = 0f
        pointerTravel = 0f
        edgeTravel = 0f
        visualAlpha = 1f
        width = 0f
        height = 0f
        density = 1f
        // Keep the one-frame visual/input settling barrier, but do not rely on time
        // for memory safety. CPU buffer reuse is protected by explicit GPU upload leases.
        delay(16L)
        active = false
        liftCueEmitted = false
    }

    fun clearImmediately() {
        snapshot = null
        progress = 0f
        verticalBias = 0f
        pullOriginY = 0.5f
        diagonalPull = 0f
        pointerTravel = 0f
        edgeTravel = 0f
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
        edgeTravel: Float = progress,
        side: MaterialPageSide = MaterialPageSide.RIGHT,
        profile: MaterialPageProfile = this.profile,
        reducedMotion: Boolean = false,
        patina: Float = this.patina,
        tone: MaterialPageTone = this.tone
    ) {
        snapshot = bitmap
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
        this.edgeTravel =
            edgeTravel.takeIf { it.isFinite() }?.coerceIn(0f, 1f)
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
        if (active || snapshot != null) return
        preparedSnapshot = null
        preparedSnapshotBufferSlot = -1
        releaseBuffersWhenUploadsSettle = gpuUploadLeases.isNotEmpty()
        releaseUnleasedBuffers()
        if (gpuUploadLeases.isEmpty()) {
            snapshotBufferCursor = -1
            releaseBuffersWhenUploadsSettle = false
        }
    }

    private fun releaseUnleasedBuffers() {
        snapshotBuffers.indices.forEach { slot ->
            val bitmap = snapshotBuffers[slot]
            if (!snapshotHasPendingGpuUpload(bitmap)) {
                snapshotBuffers[slot] = null
            }
        }
    }

    fun dispose() {
        clearImmediately()
        releaseBufferIfIdle()
        // The GL SubmittedFrame keeps its own strong bitmap reference if an upload
        // is still executing. This engine is disposed and will never reuse storage.
        gpuUploadLeases.clear()
    }

    private fun captureIntoSourceBuffer(
        view: View
    ): MaterialPageSnapshotCapture {
        val revision = snapshotSourceRevision
        val totalStarted = SystemClock.elapsedRealtimeNanos()
        Trace.beginSection("paper.capture.total")
        val capture = try {
            val bitmap = obtainSnapshotBuffer(view)
            if (bitmap == null) {
                MaterialPageSnapshotCapture.Failed(
                    sourceRevision = revision,
                    reason = MaterialPageSnapshotFailureReason.DRAW_FAILED,
                    errorType = "BitmapAllocation"
                )
            } else {
                snapshotProvider.capture(
                    view = view,
                    target = bitmap,
                    sourceRevision = revision
                )
            }
        } finally {
            Trace.endSection()
        }
        val totalNanos =
            (SystemClock.elapsedRealtimeNanos() - totalStarted)
                .coerceAtLeast(0L)

        when (capture) {
            is MaterialPageSnapshotCapture.Ready ->
                ReaderTrace.event(
                    name = "paper_snapshot_ready",
                    details =
                        "provider=${capture.provider} " +
                            "revision=${capture.sourceRevision} " +
                            "width=${capture.bitmap.width} " +
                            "height=${capture.bitmap.height} " +
                            "captureUs=${capture.elapsedNanos / 1_000L} " +
                            "totalUs=${totalNanos / 1_000L}"
                )

            is MaterialPageSnapshotCapture.NotReady ->
                ReaderTrace.event(
                    name = "paper_snapshot_not_ready",
                    details =
                        "revision=${capture.sourceRevision} " +
                            "reason=${capture.reason} " +
                            "totalUs=${totalNanos / 1_000L}"
                )

            is MaterialPageSnapshotCapture.Failed ->
                ReaderTrace.event(
                    name = "paper_snapshot_failed",
                    details =
                        "revision=${capture.sourceRevision} " +
                            "reason=${capture.reason} " +
                            "errorType=${capture.errorType.orEmpty()} " +
                            "totalUs=${totalNanos / 1_000L}"
                )
        }
        return capture
    }

    private fun nextWritableSnapshotBufferSlot(): Int? {
        val preferred = nextMaterialPageBufferSlot(snapshotBufferCursor)
        val alternate = nextMaterialPageBufferSlot(preferred)
        return sequenceOf(preferred, alternate)
            .distinct()
            .firstOrNull { slot ->
                !snapshotHasPendingGpuUpload(snapshotBuffers[slot])
            }
    }

    private fun obtainSnapshotBuffer(view: View): Bitmap? {
        val slot = nextWritableSnapshotBufferSlot() ?: return null
        snapshotBufferCursor = slot
        return obtainReusableBuffer(
            current = snapshotBuffers[slot],
            view = view
        ).also { resolved ->
            if (resolved != null) {
                snapshotBuffers[slot] = resolved
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
                !snapshotHasPendingGpuUpload(it) &&
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

internal fun <T : Any> materialPageIdentityLeaseContains(
    leases: List<T>,
    candidate: T?
): Boolean =
    candidate != null &&
        leases.any { it === candidate }

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
