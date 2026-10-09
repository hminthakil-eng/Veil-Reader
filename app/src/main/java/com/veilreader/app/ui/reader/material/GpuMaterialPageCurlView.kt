package com.veilreader.app.ui.reader.material

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLExt
import android.view.TextureView
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.os.SystemClock
import android.os.Trace
import com.veilreader.app.diagnostics.ReaderTrace
import android.util.Log
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.irurueta.android.glutils.GLTextureView
import com.veilreader.app.ui.theme.LocalVeilHighContrast
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import java.util.concurrent.atomic.AtomicLong
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * GPU renderer for Material Page Engine v2.
 *
 * Architecture references:
 * - virtual-cylinder deformation as used by XBPageCurl;
 * - triangle-mesh page topology and front/back rendering as used by android-pagecurl;
 * - an Android GL view kept as an overlay so Readium remains the source of reader truth.
 *
 * The implementation is Veil-owned and clean-room: benchmark repositories informed
 * the model and architecture, while the shaders/state bridge below are written for
 * Veil's material profiles, lifecycle and accessibility contracts.
 */
internal class GpuMaterialPageCurlView(
    context: Context,
    private val onRendererReady: (Boolean) -> Unit = {},
    private val onRendererFailure: () -> Unit = {},
    private val onRendererFramePresented: () -> Unit = {},
    private val onSheetPresented: (Long) -> Unit = {},
    private val onTextureUploadLeaseRequired: (Bitmap, Long, Long) -> Unit = { _, _, _ -> },
    private val onTextureUploaded: (Bitmap, Long, Long) -> Unit = { _, _, _ -> },
    private val onTextureUploadsInvalidated: (Long) -> Unit = {}
) : GLTextureView(context), GLSurfaceView.Renderer {
    constructor(context: Context) : this(context, onRendererReady = {})

    private data class SubmittedFrame(
        val generation: Long,
        val viewportGeneration: Long,
        val sequence: Long,
        val textureRevision: Long,
        val sheetEpoch: Long,
        val submittedAtElapsedNanos: Long,
        val bitmap: Bitmap?,
        val active: Boolean,
        val curl: GpuPageCurlFrame,
        val profile: MaterialPageProfile,
        val patina: Float,
        val tone: MaterialPageTone,
        val visualAlpha: Float,
        val highContrast: Boolean
    )

    private data class PresentedSheet(val timestamp: Long, val generation: Long, val viewport: Long, val epoch: Long)
    private val completedSheetDraws = ArrayDeque<PresentedSheet>(8)

    private val frameLock = Any()
    private val lowMemoryDevice =
        (
            context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            )?.isLowRamDevice == true
    private val meshQuality = gpuPageMeshQuality(lowMemoryDevice)
    private val meshColumns = meshQuality.columns
    private val meshRows = meshQuality.rows
    private val shadowLayerCount = gpuPageShadowLayerCount(lowMemoryDevice)
    private var submittedFrame: SubmittedFrame? = null
    private val pendingUploadLeases = mutableListOf<MaterialPageGpuUploadLease>()
    private var lastSubmittedActive = false
    private var submittedSequence = 0L
    private var textureRevision = 0L
    private var textureSubmissionStartedAtNanos = 0L
    private var uploadedTextureRevision = 0L
    private var rendererFailed = false

    @Volatile
    private var rendererGeneration = 0L

    @Volatile
    private var viewportGeneration = 0L

    private var program = 0
    private var vertexBufferId = 0
    private var indexBufferId = 0
    private var frontTextureId = 0
    private var frontTextureWidth = 1
    private var frontTextureHeight = 1
    private var indexCount = 0
    private var viewportWidth = 0
    private var viewportHeight = 0
    private var maxTextureSize = 0
    private var failureReported = false
    private var rendererPaused = false
    private var surfaceReadyReported = false
    private var frameSuccessReportedForGeneration = false

    private var aPosition = -1
    private var aTexCoord = -1
    private var uFrontTexture = -1
    private var uCylinderPosition = -1
    private var uCylinderTilt = -1
    private var uCylinderRadius = -1
    private var uPageAspect = -1
    private var uTexelSize = -1
    private var uSideSign = -1
    private var uFrontTint = -1
    private var uBackTint = -1
    private var uEdgeTint = -1
    private var uFrontTintAlpha = -1
    private var uGhostAlpha = -1
    private var uRoughness = -1
    private var uSpecular = -1
    private var uTranslucency = -1
    private var uGrain = -1
    private var uFiber = -1
    private var uMaterialPhase = -1
    private var uEdgeStrength = -1
    private var uShadowStrength = -1
    private var uVisualAlpha = -1
    private var uShadowPass = -1

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        isOpaque = false
        isClickable = false
        isFocusable = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        preserveEGLContextOnPause =
            shouldPreserveGpuPageContextOnPause(lowMemoryDevice)
        setRenderer(this)
        val surfaceDelegate = surfaceTextureListener
        // Preserve the GL host's lifecycle listener while acknowledging the actual
        // acquired buffer, rather than a draw callback or a fixed-delay guess.
        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                surfaceDelegate?.onSurfaceTextureAvailable(surface, width, height)
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                surfaceDelegate?.onSurfaceTextureSizeChanged(surface, width, height)
            }
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                synchronized(frameLock) { completedSheetDraws.clear() }
                return surfaceDelegate?.onSurfaceTextureDestroyed(surface) ?: true
            }
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                surfaceDelegate?.onSurfaceTextureUpdated(surface)
                val acquiredTimestamp = surface.timestamp
                val presented = synchronized(frameLock) {
                    completedSheetDraws.firstOrNull { it.timestamp == acquiredTimestamp }.also {
                        while (completedSheetDraws.isNotEmpty() && completedSheetDraws.first().timestamp <= acquiredTimestamp) {
                            completedSheetDraws.removeFirst()
                        }
                    }
                } ?: return
                if (gpuMaterialSheetPresentationMatches(
                        presented.timestamp, acquiredTimestamp,
                        presented.generation, rendererGeneration,
                        presented.viewport, viewportGeneration
                    )) {
                    onSheetPresented(presented.epoch)
                }
            }
        }
        renderMode = GLTextureView.RENDER_MODE_WHEN_DIRTY
    }

    fun submitFrame(
        bitmap: Bitmap?,
        active: Boolean,
        curl: GpuPageCurlFrame,
        profile: MaterialPageProfile,
        patina: Float,
        tone: MaterialPageTone,
        visualAlpha: Float,
        highContrast: Boolean,
        sheetEpoch: Long = 0L
    ) {
        val usableBitmap =
            bitmap?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
        var requiresTextureUpload = false
        var uploadGeneration = 0L
        var uploadRevision = 0L
        synchronized(frameLock) {
            submittedSequence =
                nextGpuMaterialFrameSequence(submittedSequence)
            requiresTextureUpload =
                active &&
                    usableBitmap != null &&
                    (
                        !lastSubmittedActive ||
                            submittedFrame?.bitmap !== usableBitmap
                        )
            if (requiresTextureUpload) {
                textureRevision =
                    nextGpuMaterialTextureRevision(textureRevision)
                textureSubmissionStartedAtNanos = SystemClock.elapsedRealtimeNanos()
            }
            submittedFrame = SubmittedFrame(
                generation = rendererGeneration,
                viewportGeneration = viewportGeneration,
                sequence = submittedSequence,
                textureRevision = textureRevision,
                sheetEpoch = sheetEpoch,
                submittedAtElapsedNanos = textureSubmissionStartedAtNanos,
                bitmap = usableBitmap,
                active = active && usableBitmap != null,
                curl = curl,
                profile = profile,
                patina = patina.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.35f,
                tone = tone,
                visualAlpha =
                    visualAlpha.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 1f,
                highContrast = highContrast
            )
            lastSubmittedActive = active && usableBitmap != null
            uploadGeneration = rendererGeneration
            uploadRevision = textureRevision
            if (requiresTextureUpload && usableBitmap != null) {
                pendingUploadLeases += MaterialPageGpuUploadLease(usableBitmap, uploadGeneration, uploadRevision)
            }
        }
        if (requiresTextureUpload && usableBitmap != null) {
            // Lease synchronously before the GL thread can observe requestRender().
            onTextureUploadLeaseRequired(
                usableBitmap,
                uploadGeneration,
                uploadRevision
            )
        }
        requestRender()
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // A new GL context cannot retain a client-memory read from the previous one.
        // Invalidate only the previous generation: UI delivery can race with a
        // newly submitted frame for the fresh context.
        synchronized(frameLock) { completedSheetDraws.clear() }
        val abandonedGeneration = rendererGeneration
        if (abandonedGeneration > 0L) {
            post { onTextureUploadsInvalidated(abandonedGeneration) }
        }
        rendererFailed = false
        // A recreated EGL context is a fresh failure-reporting attempt. If the previous
        // context failed and this initialization fails too, the UI must receive another
        // failure callback instead of remaining stuck in INITIALIZING forever.
        failureReported = false
        surfaceReadyReported = false
        frameSuccessReportedForGeneration = false
        resetGlHandlesForNewGeneration()
        synchronized(frameLock) {
            rendererGeneration = allocateGpuMaterialRendererGeneration()
            pendingUploadLeases.clear()
            submittedFrame = null
            lastSubmittedActive = false
            submittedSequence = 0L
            textureRevision = 0L
            uploadedTextureRevision = 0L
        }
        post { onRendererReady(false) }
        runCatching {
            program = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER)
            resolveLocations()
            validateLocations()
            createMesh()
            createTextures()
            GLES20.glDisable(GLES20.GL_CULL_FACE)
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(
                GLES20.GL_SRC_ALPHA,
                GLES20.GL_ONE_MINUS_SRC_ALPHA
            )
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            GLES20.glDepthFunc(GLES20.GL_LEQUAL)
            GLES20.glClearColor(0f, 0f, 0f, 0f)
            val textureLimit = IntArray(1)
            GLES20.glGetIntegerv(
                GLES20.GL_MAX_TEXTURE_SIZE,
                textureLimit,
                0
            )
            maxTextureSize = textureLimit[0].coerceAtLeast(1)
            val initError = consumeGpuPageGlErrors()
            check(initError == GLES20.GL_NO_ERROR) {
                "GPU page renderer initialization glError=$initError"
            }
            failureReported = false
        }.onFailure { error ->
            failRenderer("GPU page renderer initialization failed", error)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        if (width <= 0 || height <= 0) {
            viewportWidth = 0
            viewportHeight = 0
            surfaceReadyReported = false
            post { onRendererReady(false) }
            return
        }
        val resolvedWidth = width
        val resolvedHeight = height
        if (
            viewportWidth != resolvedWidth ||
            viewportHeight != resolvedHeight
        ) {
            viewportGeneration =
                nextGpuMaterialViewportGeneration(viewportGeneration)
        }
        viewportWidth = resolvedWidth
        viewportHeight = resolvedHeight
        GLES20.glViewport(0, 0, viewportWidth, viewportHeight)

        // Reserve texture storage while the Reader is idle so the first deliberate
        // Paper gesture pays only the bitmap upload, not a simultaneous GPU storage
        // allocation. If the publication capture size differs, the normal upload
        // path still reallocates safely on demand.
        if (
            shouldPreallocateGpuPageTexture(
                viewportWidth = viewportWidth,
                viewportHeight = viewportHeight,
                maxTextureSize = maxTextureSize
            )
        ) {
            preallocateFrontTextureStorage(viewportWidth, viewportHeight)
        }

        // A compiled GL program without a viewport is not render-ready. Waiting
        // until onSurfaceChanged also ensures the initial viewport generation is
        // non-zero before Compose is allowed to start a Paper transaction.
        if (
            !rendererFailed &&
            program != 0 &&
            viewportGeneration > 0L &&
            !surfaceReadyReported
        ) {
            surfaceReadyReported = true
            post { onRendererReady(true) }
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        Trace.beginSection("paper.gpu.draw")
        try {
        GLES20.glClear(
            GLES20.GL_COLOR_BUFFER_BIT or
                GLES20.GL_DEPTH_BUFFER_BIT
        )
        if (rendererFailed || program == 0) return

        val frame = synchronized(frameLock) { submittedFrame } ?: return
        if (
            !gpuMaterialFrameIsCurrent(
                frameGeneration = frame.generation,
                rendererGeneration = rendererGeneration,
                frameViewportGeneration = frame.viewportGeneration,
                rendererViewportGeneration = viewportGeneration,
                frameSequence = frame.sequence,
                latestSequence = synchronized(frameLock) { submittedSequence }
            )
        ) {
            return
        }
        val bitmap = frame.bitmap ?: return
        if (!frame.active || bitmap.isRecycled) return
        if (!textureFits(bitmap)) {
            failRenderer(
                "GPU page texture exceeds GL_MAX_TEXTURE_SIZE=$maxTextureSize"
            )
            return
        }
        if (
            !materialPageSnapshotScaleIsSafe(
                snapshotWidth = bitmap.width.toFloat(),
                snapshotHeight = bitmap.height.toFloat(),
                canvasWidth = viewportWidth.toFloat(),
                canvasHeight = viewportHeight.toFloat()
            )
        ) {
            return
        }

        GLES20.glUseProgram(program)
        val didUpload = uploadFrontTextureIfNeeded(
            frontBitmap = bitmap,
            requiredTextureRevision = frame.textureRevision
        )
        val uploadError = consumeGpuPageGlErrors()
        if (uploadError != GLES20.GL_NO_ERROR) {
            failRenderer("GPU page texture upload failed: glError=$uploadError")
            return
        }
        if (didUpload) {
            synchronized(frameLock) {
                uploadedTextureRevision = frame.textureRevision
                pendingUploadLeases.removeAll {
                    materialPageGpuUploadLeaseMatches(it, bitmap, frame.generation, frame.textureRevision)
                }
            }
            // GL has consumed the client bitmap bytes for this texture update.
            // Acknowledge on the UI thread so the CPU snapshot pool may reuse it.
            post {
                onTextureUploaded(
                    bitmap,
                    frame.generation,
                    frame.textureRevision
                )
            }
        }
        if (
            !gpuMaterialFrameIsCurrent(
                frameGeneration = frame.generation,
                rendererGeneration = rendererGeneration,
                frameViewportGeneration = frame.viewportGeneration,
                rendererViewportGeneration = viewportGeneration,
                frameSequence = frame.sequence,
                latestSequence = synchronized(frameLock) { submittedSequence }
            )
        ) {
            return
        }

        val staleDrawError = consumeGpuPageGlErrors()
        if (staleDrawError != GLES20.GL_NO_ERROR) {
            Log.w(TAG, "Cleared stale GL error before Paper draw: $staleDrawError")
        }

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferId)
        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(
            aPosition,
            2,
            GLES20.GL_FLOAT,
            false,
            VERTEX_STRIDE_BYTES,
            0
        )
        GLES20.glEnableVertexAttribArray(aTexCoord)
        GLES20.glVertexAttribPointer(
            aTexCoord,
            2,
            GLES20.GL_FLOAT,
            false,
            VERTEX_STRIDE_BYTES,
            2 * FLOAT_BYTES
        )
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBufferId)

        bindFrameUniforms(frame)

        // Layered mesh-projected shadow approximates the drop-shadow penumbra
        // used by mature GL curl engines without allocating a moving blur texture.
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        for (shadowLayer in 1..shadowLayerCount) {
            GLES20.glUniform1f(uShadowPass, shadowLayer.toFloat())
            GLES20.glDrawElements(
                GLES20.GL_TRIANGLES,
                indexCount,
                GLES20.GL_UNSIGNED_SHORT,
                0
            )
        }

        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glUniform1f(uShadowPass, 0f)
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            indexCount,
            GLES20.GL_UNSIGNED_SHORT,
            0
        )

        GLES20.glDisableVertexAttribArray(aPosition)
        GLES20.glDisableVertexAttribArray(aTexCoord)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)

        val drawError = consumeGpuPageGlErrors()
        if (drawError != GLES20.GL_NO_ERROR) {
            failRenderer("GPU page draw failed: glError=$drawError")
            return
        }
        // eglSwapBuffers belongs to GLTextureView. Tag the buffer before that swap;
        // TextureView reports this exact timestamp only after acquiring it.
        val presentationTimestamp = System.nanoTime()
        check(EGLExt.eglPresentationTimeANDROID(
            EGL14.eglGetCurrentDisplay(), EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW), presentationTimestamp
        )) { "GPU Paper presentation timestamp is unavailable" }
        synchronized(frameLock) {
            if (completedSheetDraws.size == 8) completedSheetDraws.removeFirst()
            completedSheetDraws.addLast(PresentedSheet(presentationTimestamp, frame.generation, frame.viewportGeneration, frame.sheetEpoch))
        }
        if (didUpload && ReaderTrace.isEnabled()) {
            ReaderTrace.event(
                name = "paper_gpu_first_draw",
                details = "generation=${frame.generation} revision=${frame.textureRevision} " +
                    "submitToDrawUs=${(SystemClock.elapsedRealtimeNanos() - frame.submittedAtElapsedNanos).coerceAtLeast(0L) / 1_000L} " +
                    "textureBytesEstimate=${frontTextureWidth.toLong() * frontTextureHeight * 4L}"
            )
        }
        if (!frameSuccessReportedForGeneration) {
            frameSuccessReportedForGeneration = true
            post { onRendererFramePresented() }
        }
        } catch (error: Exception) {
            failRenderer("GPU page draw threw ${error::class.java.simpleName}", error)
        } finally {
            Trace.endSection()
            releaseObsoleteUploadLeases()
        }
    }

    /** GL has finished its client-memory reads before retiring skipped submissions. */
    private fun releaseObsoleteUploadLeases(keepCurrent: Boolean = true) {
        val retired = synchronized(frameLock) {
            if (pendingUploadLeases.isEmpty()) return@synchronized emptyList<MaterialPageGpuUploadLease>()
            val current = submittedFrame
            pendingUploadLeases.filter { lease ->
                !keepCurrent || current == null || !current.active ||
                    !materialPageGpuUploadLeaseMatches(lease, current.bitmap, current.generation, current.textureRevision)
            }.also { pendingUploadLeases.removeAll(it.toSet()) }
        }
        if (retired.isNotEmpty()) post {
            retired.forEach { onTextureUploaded(it.bitmap, it.rendererGeneration, it.textureRevision) }
        }
    }

    private fun resetGlHandlesForNewGeneration() {
        program = 0
        vertexBufferId = 0
        indexBufferId = 0
        frontTextureId = 0
        frontTextureWidth = 1
        frontTextureHeight = 1
        indexCount = 0
        maxTextureSize = 0
        aPosition = -1
        aTexCoord = -1
        uFrontTexture = -1
        uCylinderPosition = -1
        uCylinderTilt = -1
        uCylinderRadius = -1
        uPageAspect = -1
        uTexelSize = -1
        uSideSign = -1
        uFrontTint = -1
        uBackTint = -1
        uEdgeTint = -1
        uFrontTintAlpha = -1
        uGhostAlpha = -1
        uRoughness = -1
        uSpecular = -1
        uTranslucency = -1
        uGrain = -1
        uFiber = -1
        uMaterialPhase = -1
        uEdgeStrength = -1
        uShadowStrength = -1
        uVisualAlpha = -1
        uShadowPass = -1
    }

    private fun textureFits(bitmap: Bitmap): Boolean =
        maxTextureSize > 0 &&
            bitmap.width <= maxTextureSize &&
            bitmap.height <= maxTextureSize

    private fun failRenderer(
        message: String,
        error: Throwable? = null
    ) {
        rendererFailed = true
        if (error != null) {
            Log.e(TAG, message, error)
        } else {
            Log.e(TAG, message)
        }
        if (!failureReported) {
            failureReported = true
            val failedGeneration = rendererGeneration
            post {
                onTextureUploadsInvalidated(failedGeneration)
                onRendererFailure()
            }
        }
    }

    fun pauseRenderer() {
        if (rendererPaused) return
        runCatching { onPause() }
            .onSuccess {
                rendererPaused = true
                // onPause joins the GL pause barrier: no upload can still read CPU storage.
                synchronized(frameLock) {
                    submittedFrame = null
                    lastSubmittedActive = false
                    uploadedTextureRevision = 0L
                }
                releaseObsoleteUploadLeases(keepCurrent = false)
            }
    }

    fun resumeRenderer() {
        if (!rendererPaused) return
        runCatching { onResume() }
            .onSuccess { rendererPaused = false }
    }

    private fun bindFrameUniforms(frame: SubmittedFrame) {
        val curl = frame.curl
        val optics = frame.profile.optics

        GLES20.glUniform2f(
            uCylinderPosition,
            curl.cylinderX,
            curl.cylinderY
        )
        GLES20.glUniform1f(uCylinderTilt, curl.cylinderTilt)
        GLES20.glUniform1f(uCylinderRadius, curl.radius)
        val bitmap = frame.bitmap ?: return
        val pageAspect =
            if (bitmap.width > 0) {
                bitmap.height.toFloat() / bitmap.width.toFloat()
            } else {
                1f
            }
        GLES20.glUniform1f(
            uPageAspect,
            pageAspect
                .takeIf { it.isFinite() }
                ?.coerceIn(GPU_PAGE_MIN_ASPECT, GPU_PAGE_MAX_ASPECT)
                ?: 1f
        )
        GLES20.glUniform2f(
            uTexelSize,
            1f / bitmap.width.toFloat().coerceAtLeast(1f),
            1f / bitmap.height.toFloat().coerceAtLeast(1f)
        )
        GLES20.glUniform1f(uSideSign, curl.sideSign)

        val front = materialPageToneAdjustedArgb(optics.frontArgb, frame.tone)
        val back = materialPageToneAdjustedArgb(optics.backArgb, frame.tone)
        val edge = materialPageToneAdjustedArgb(optics.edgeArgb, frame.tone)
        setColorUniform(uFrontTint, front)
        setColorUniform(uBackTint, back)
        setColorUniform(uEdgeTint, edge)

        val frontTintAlpha =
            materialPageFrontSurfaceTintAlpha(frame.profile, frame.patina) *
                if (frame.highContrast) 0.50f else 1f
        GLES20.glUniform1f(uFrontTintAlpha, frontTintAlpha.coerceIn(0f, 0.10f))
        GLES20.glUniform1f(
            uGhostAlpha,
            materialPageBacksideContentAlpha(frame.profile, frame.patina)
                .coerceIn(0.06f, 0.34f)
        )
        GLES20.glUniform1f(uRoughness, optics.roughness.coerceIn(0f, 1f))
        GLES20.glUniform1f(
            uSpecular,
            optics.specularResponse.coerceIn(0f, 1f)
        )
        GLES20.glUniform1f(
            uTranslucency,
            optics.translucency.coerceIn(0f, 1f)
        )
        GLES20.glUniform1f(
            uGrain,
            (
                optics.grain *
                    (0.32f + frame.patina * optics.patinaResponse * 0.68f) *
                    if (frame.highContrast) 0.42f else 1f
                ).coerceIn(0f, 1f)
        )
        GLES20.glUniform1f(
            uFiber,
            (
                optics.directionalFiber *
                    if (frame.highContrast) 0.55f else 1f
                ).coerceIn(0f, 1f)
        )
        GLES20.glUniform1f(
            uMaterialPhase,
            frame.profile.preset.ordinal.toFloat() * 1.6180339f
        )
        GLES20.glUniform1f(
            uEdgeStrength,
            curl.edgeStrength * if (frame.highContrast) 1.10f else 1f
        )
        GLES20.glUniform1f(
            uShadowStrength,
            curl.shadowStrength * if (frame.highContrast) 1.12f else 1f
        )
        GLES20.glUniform1f(uVisualAlpha, frame.visualAlpha)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frontTextureId)
        GLES20.glUniform1i(uFrontTexture, 0)

    }

    private fun setColorUniform(location: Int, argb: Long) {
        val r = ((argb ushr 16) and 0xFF).toFloat() / 255f
        val g = ((argb ushr 8) and 0xFF).toFloat() / 255f
        val b = (argb and 0xFF).toFloat() / 255f
        GLES20.glUniform3f(location, r, g, b)
    }

    private fun preallocateFrontTextureStorage(width: Int, height: Int) {
        if (
            frontTextureId == 0 ||
            (frontTextureWidth == width && frontTextureHeight == height)
        ) {
            return
        }

        val staleError = consumeGpuPageGlErrors()
        if (staleError != GLES20.GL_NO_ERROR) {
            Log.w(TAG, "Cleared stale GL error before texture preallocation: $staleError")
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frontTextureId)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            width,
            height,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            null
        )
        val error = consumeGpuPageGlErrors()
        if (error == GLES20.GL_NO_ERROR) {
            frontTextureWidth = width
            frontTextureHeight = height
            synchronized(frameLock) {
                uploadedTextureRevision = 0L
            }
        } else {
            // Prewarm is an optimization, never a correctness gate. Keep tracked
            // dimensions mismatched so the first real frame retries with texImage2D.
            frontTextureWidth = 1
            frontTextureHeight = 1
            Log.w(TAG, "GPU page texture preallocation skipped: glError=$error")
        }
    }

    private fun uploadFrontTextureIfNeeded(
        frontBitmap: Bitmap,
        requiredTextureRevision: Long
    ): Boolean {
        val alreadyUploaded = synchronized(frameLock) {
            requiredTextureRevision > 0L &&
                uploadedTextureRevision == requiredTextureRevision
        }
        if (alreadyUploaded) return false

        Trace.beginSection("paper.gpu.texture_upload")
        try {
            val staleError = consumeGpuPageGlErrors()
            if (staleError != GLES20.GL_NO_ERROR) {
                Log.w(TAG, "Cleared stale GL error before texture upload: $staleError")
            }
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frontTextureId)
            if (
                frontTextureWidth == frontBitmap.width &&
                frontTextureHeight == frontBitmap.height
            ) {
                GLUtils.texSubImage2D(
                    GLES20.GL_TEXTURE_2D,
                    0,
                    0,
                    0,
                    frontBitmap
                )
            } else {
                GLUtils.texImage2D(
                    GLES20.GL_TEXTURE_2D,
                    0,
                    frontBitmap,
                    0
                )
                frontTextureWidth = frontBitmap.width
                frontTextureHeight = frontBitmap.height
            }
            return true
        } finally {
            Trace.endSection()
        }
    }

    private fun createTextures() {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        frontTextureId = ids[0]
        frontTextureWidth = 1
        frontTextureHeight = 1
        configureTexture(frontTextureId)
    }

    private fun configureTexture(textureId: Int) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        val transparentPixel =
            ByteBuffer.allocateDirect(4)
                .order(ByteOrder.nativeOrder())
                .apply {
                    put(byteArrayOf(0, 0, 0, 0))
                    position(0)
                }
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            1,
            1,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            transparentPixel
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
    }

    private fun createMesh() {
        val vertexCount = (meshColumns + 1) * (meshRows + 1)
        val vertexData = FloatArray(vertexCount * FLOATS_PER_VERTEX)
        var vertexOffset = 0
        for (y in 0..meshRows) {
            val fy = y.toFloat() / meshRows.toFloat()
            for (x in 0..meshColumns) {
                val fx = x.toFloat() / meshColumns.toFloat()
                vertexData[vertexOffset++] = fx
                vertexData[vertexOffset++] = fy
                vertexData[vertexOffset++] = fx
                vertexData[vertexOffset++] = fy
            }
        }

        val indices = ShortArray(meshColumns * meshRows * 6)
        var indexOffset = 0
        val row = meshColumns + 1
        for (y in 0 until meshRows) {
            for (x in 0 until meshColumns) {
                val topLeft = y * row + x
                val topRight = topLeft + 1
                val bottomLeft = topLeft + row
                val bottomRight = bottomLeft + 1

                indices[indexOffset++] = topLeft.toShort()
                indices[indexOffset++] = bottomLeft.toShort()
                indices[indexOffset++] = topRight.toShort()

                indices[indexOffset++] = topRight.toShort()
                indices[indexOffset++] = bottomLeft.toShort()
                indices[indexOffset++] = bottomRight.toShort()
            }
        }
        indexCount = indices.size

        val vertexBuffer = directFloatBuffer(vertexData)
        val indexBuffer = directShortBuffer(indices)

        val ids = IntArray(2)
        GLES20.glGenBuffers(2, ids, 0)
        vertexBufferId = ids[0]
        indexBufferId = ids[1]

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferId)
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            vertexData.size * FLOAT_BYTES,
            vertexBuffer,
            GLES20.GL_STATIC_DRAW
        )

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBufferId)
        GLES20.glBufferData(
            GLES20.GL_ELEMENT_ARRAY_BUFFER,
            indices.size * SHORT_BYTES,
            indexBuffer,
            GLES20.GL_STATIC_DRAW
        )

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    private fun resolveLocations() {
        aPosition = GLES20.glGetAttribLocation(program, "aPosition")
        aTexCoord = GLES20.glGetAttribLocation(program, "aTexCoord")
        uFrontTexture = GLES20.glGetUniformLocation(program, "uFrontTexture")
        uCylinderPosition = GLES20.glGetUniformLocation(program, "uCylinderPosition")
        uCylinderTilt = GLES20.glGetUniformLocation(program, "uCylinderTilt")
        uCylinderRadius = GLES20.glGetUniformLocation(program, "uCylinderRadius")
        uPageAspect = GLES20.glGetUniformLocation(program, "uPageAspect")
        uTexelSize = GLES20.glGetUniformLocation(program, "uTexelSize")
        uSideSign = GLES20.glGetUniformLocation(program, "uSideSign")
        uFrontTint = GLES20.glGetUniformLocation(program, "uFrontTint")
        uBackTint = GLES20.glGetUniformLocation(program, "uBackTint")
        uEdgeTint = GLES20.glGetUniformLocation(program, "uEdgeTint")
        uFrontTintAlpha = GLES20.glGetUniformLocation(program, "uFrontTintAlpha")
        uGhostAlpha = GLES20.glGetUniformLocation(program, "uGhostAlpha")
        uRoughness = GLES20.glGetUniformLocation(program, "uRoughness")
        uSpecular = GLES20.glGetUniformLocation(program, "uSpecular")
        uTranslucency = GLES20.glGetUniformLocation(program, "uTranslucency")
        uGrain = GLES20.glGetUniformLocation(program, "uGrain")
        uFiber = GLES20.glGetUniformLocation(program, "uFiber")
        uMaterialPhase = GLES20.glGetUniformLocation(program, "uMaterialPhase")
        uEdgeStrength = GLES20.glGetUniformLocation(program, "uEdgeStrength")
        uShadowStrength = GLES20.glGetUniformLocation(program, "uShadowStrength")
        uVisualAlpha = GLES20.glGetUniformLocation(program, "uVisualAlpha")
        uShadowPass = GLES20.glGetUniformLocation(program, "uShadowPass")
    }

    private fun validateLocations() {
        val required = mapOf(
            "aPosition" to aPosition,
            "aTexCoord" to aTexCoord,
            "uFrontTexture" to uFrontTexture,
            "uCylinderPosition" to uCylinderPosition,
            "uCylinderTilt" to uCylinderTilt,
            "uCylinderRadius" to uCylinderRadius,
            "uPageAspect" to uPageAspect,
            "uTexelSize" to uTexelSize,
            "uSideSign" to uSideSign,
            "uFrontTint" to uFrontTint,
            "uBackTint" to uBackTint,
            "uEdgeTint" to uEdgeTint,
            "uFrontTintAlpha" to uFrontTintAlpha,
            "uGhostAlpha" to uGhostAlpha,
            "uRoughness" to uRoughness,
            "uSpecular" to uSpecular,
            "uTranslucency" to uTranslucency,
            "uGrain" to uGrain,
            "uFiber" to uFiber,
            "uMaterialPhase" to uMaterialPhase,
            "uEdgeStrength" to uEdgeStrength,
            "uShadowStrength" to uShadowStrength,
            "uVisualAlpha" to uVisualAlpha,
            "uShadowPass" to uShadowPass
        )
        val missing = required.filterValues { it < 0 }.keys
        check(missing.isEmpty()) {
            "GPU page shader locations missing: " + missing.joinToString()
        }
    }

    private fun buildProgram(vertexSource: String, fragmentSource: String): Int {
        val vertex = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        var fragment = 0
        var result = 0
        var linked = false
        try {
            fragment = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
            result = GLES20.glCreateProgram()
            check(result != 0) { "GPU page program allocation failed" }
            GLES20.glAttachShader(result, vertex)
            GLES20.glAttachShader(result, fragment)
            GLES20.glLinkProgram(result)
            val status = IntArray(1)
            GLES20.glGetProgramiv(result, GLES20.GL_LINK_STATUS, status, 0)
            check(status[0] != 0) {
                "GPU page program link failed: ${GLES20.glGetProgramInfoLog(result)}"
            }
            linked = true
            return result
        } finally {
            GLES20.glDeleteShader(vertex)
            if (fragment != 0) GLES20.glDeleteShader(fragment)
            if (!linked && result != 0) GLES20.glDeleteProgram(result)
        }
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            error("GPU page shader compile failed: $log")
        }
        return shader
    }

    companion object {
        private const val TAG = "GpuMaterialPageCurl"
        private const val FLOAT_BYTES = 4
        private const val SHORT_BYTES = 2
        private const val FLOATS_PER_VERTEX = 4
        private const val VERTEX_STRIDE_BYTES =
            FLOATS_PER_VERTEX * FLOAT_BYTES
        fun isSupported(context: Context): Boolean {
            val manager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                    ?: return false
            return manager.deviceConfigurationInfo.reqGlEsVersion >= 0x20000
        }

        private fun directFloatBuffer(values: FloatArray): FloatBuffer =
            ByteBuffer.allocateDirect(values.size * FLOAT_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply {
                    put(values)
                    position(0)
                }

        private fun directShortBuffer(values: ShortArray): ShortBuffer =
            ByteBuffer.allocateDirect(values.size * SHORT_BYTES)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .apply {
                    put(values)
                    position(0)
                }

        internal const val VERTEX_SHADER = """
            precision highp float;

            attribute vec2 aPosition;
            attribute vec2 aTexCoord;

            uniform vec2 uCylinderPosition;
            uniform float uCylinderTilt;
            uniform float uCylinderRadius;
            uniform float uPageAspect;
            uniform mediump float uSideSign;
            uniform mediump float uShadowPass;

            varying mediump vec2 vTexCoord;
            varying mediump vec3 vNormal;
            varying mediump float vLift;

            const float PI = 3.14159265358979323846;

            void main() {
                bool turnFromRight = uSideSign > 0.0;
                vec2 p = vec2(
                    turnFromRight ? aPosition.x : 1.0 - aPosition.x,
                    aPosition.y * uPageAspect
                );
                vec2 cylinderPosition = vec2(
                    uCylinderPosition.x,
                    uCylinderPosition.y * uPageAspect
                );

                vec2 direction = normalize(vec2(uCylinderTilt, 1.0));
                vec2 normal2 = vec2(direction.y, -direction.x);
                vec2 relative = p - cylinderPosition;
                float distanceToAxis = dot(relative, normal2);

                vec3 deformed = vec3(p, 0.0);
                vec3 normal3 = vec3(0.0, 0.0, 1.0);

                if (distanceToAxis > 0.0) {
                    vec2 projection = p - normal2 * distanceToAxis;
                    float halfTurn = PI * uCylinderRadius;

                    if (distanceToAxis <= halfTurn) {
                        float angle = distanceToAxis / uCylinderRadius;
                        float s = sin(angle);
                        float c = cos(angle);
                        deformed = vec3(
                            projection + normal2 * (s * uCylinderRadius),
                            (1.0 - c) * uCylinderRadius
                        );
                        vec3 center = vec3(projection, uCylinderRadius);
                        normal3 = normalize(center - deformed);
                    } else {
                        vec2 displacement =
                            normal2 * (2.0 * distanceToAxis - halfTurn);
                        deformed = vec3(
                            p - displacement,
                            2.0 * uCylinderRadius
                        );
                        normal3 = vec3(0.0, 0.0, -1.0);
                    }
                }

                vLift = clamp(
                    deformed.z / max(2.0 * uCylinderRadius, 0.0001),
                    0.0,
                    1.0
                );

                if (uShadowPass > 0.5) {
                    float shadowLayer = uShadowPass - 1.0;
                    float layerOffset = 0.004 + shadowLayer * 0.006;
                    float liftOffset = deformed.z * (0.10 + shadowLayer * 0.035);
                    deformed.xy += normal2 * (layerOffset + liftOffset);
                    deformed.z = 0.0;
                }

                deformed.y /= max(uPageAspect, 0.0001);

                if (!turnFromRight) {
                    deformed.x = 1.0 - deformed.x;
                    normal3.x = -normal3.x;
                }

                float clipZ =
                    uShadowPass > 0.5
                        ? 0.55
                        : -clamp(deformed.z * 2.2, 0.0, 0.82);

                gl_Position = vec4(
                    deformed.x * 2.0 - 1.0,
                    1.0 - deformed.y * 2.0,
                    clipZ,
                    1.0
                );
                vTexCoord = aTexCoord;
                vNormal = normal3;
            }
        """

        internal const val FRAGMENT_SHADER = """
            precision mediump float;

            uniform sampler2D uFrontTexture;
            uniform vec3 uFrontTint;
            uniform vec3 uBackTint;
            uniform vec3 uEdgeTint;
            uniform float uFrontTintAlpha;
            uniform float uGhostAlpha;
            uniform float uRoughness;
            uniform float uSpecular;
            uniform float uTranslucency;
            uniform float uGrain;
            uniform float uFiber;
            uniform float uMaterialPhase;
            uniform float uEdgeStrength;
            uniform float uShadowStrength;
            uniform float uVisualAlpha;
            uniform mediump float uShadowPass;
            uniform vec2 uTexelSize;
            uniform mediump float uSideSign;

            varying mediump vec2 vTexCoord;
            varying mediump vec3 vNormal;
            varying mediump float vLift;

            float pageNoise(vec2 uv) {
                float a = sin(uv.x * 173.0 + uMaterialPhase * 7.0);
                float b = sin(uv.y * 131.0 - uMaterialPhase * 11.0);
                return a * b;
            }

            void main() {
                if (uShadowPass > 0.5) {
                    float layer = uShadowPass - 1.0;
                    float shadowEnvelope =
                        smoothstep(0.015, 0.30, vLift) *
                        (1.0 - smoothstep(0.86, 1.0, vLift) * 0.28);
                    float penumbra = 1.0 / (1.0 + layer * layer * 0.90);
                    gl_FragColor = vec4(
                        0.0,
                        0.0,
                        0.0,
                        uShadowStrength * shadowEnvelope * penumbra
                    );
                    return;
                }

                vec4 frontInk = texture2D(uFrontTexture, vTexCoord);
                vec4 mirroredFrontInk =
                    texture2D(uFrontTexture, vec2(1.0 - vTexCoord.x, vTexCoord.y));
                vec3 n = normalize(vNormal);
                float facing = clamp(abs(n.z), 0.0, 1.0);
                float grazing = 1.0 - facing;

                bool physicalFront = n.z >= 0.0;
                vec3 faceNormal =
                    normalize(physicalFront ? n : -n);
                vec3 lightDir = normalize(
                    vec3(-0.32 * uSideSign, -0.18, 0.93)
                );
                vec3 viewDir = vec3(0.0, 0.0, 1.0);
                vec3 halfDir = normalize(lightDir + viewDir);
                float ndotl = clamp(dot(faceNormal, lightDir), 0.0, 1.0);
                float ndoth = clamp(dot(faceNormal, halfDir), 0.0, 1.0);
                float rough = clamp(uRoughness, 0.04, 1.0);
                float shininess = mix(54.0, 7.0, rough);
                float materialSpecular =
                    pow(ndoth, shininess) *
                    uSpecular *
                    mix(0.34, 0.08, rough);
                float selfOcclusion =
                    grazing *
                    smoothstep(0.06, 0.72, vLift) *
                    mix(0.15, 0.06, rough);

                vec3 color;
                float outputAlpha;
                if (physicalFront) {
                    color = mix(frontInk.rgb, uFrontTint, uFrontTintAlpha);
                    outputAlpha = frontInk.a;
                    float diffuse = 0.78 + ndotl * 0.22;
                    float fresnel =
                        grazing * grazing * grazing *
                        uSpecular *
                        mix(0.16, 0.035, rough);
                    color =
                        color * max(0.58, diffuse - selfOcclusion) +
                        vec3(materialSpecular + fresnel);
                } else {
                    // The reverse of the lifted leaf is not the Readium destination page.
                    // Until a true opposite-leaf provider exists, derive only restrained
                    // ink-through from the source sheet and let the live destination remain
                    // physically underneath the mesh.
                    vec4 backInk = mirroredFrontInk;
                    float backContent =
                        clamp(
                            uGhostAlpha * (0.72 + uTranslucency * 0.28),
                            0.06,
                            0.34
                        );
                    color = mix(
                        uBackTint,
                        backInk.rgb,
                        clamp(backContent, 0.08, 0.94)
                    );
                    outputAlpha = backInk.a;
                    float diffuse = 0.70 + ndotl * 0.24;
                    float transmitted =
                        grazing * uTranslucency * 0.12;
                    color =
                        color * max(0.54, diffuse - selfOcclusion * 0.72) +
                        vec3(transmitted + materialSpecular * 0.35);
                }

                float grain = pageNoise(vTexCoord) * uGrain * 0.022;
                float fiber =
                    sin(
                        vTexCoord.y * 260.0 +
                            vTexCoord.x * 19.0 +
                            uMaterialPhase * 3.0
                    ) *
                    uFiber *
                    0.010;
                color += vec3(grain - fiber);

                float freeEdgeDistance =
                    uSideSign > 0.0
                        ? 1.0 - vTexCoord.x
                        : vTexCoord.x;
                float freeEdge =
                    1.0 - smoothstep(0.0, 0.010, freeEdgeDistance);
                float outerEdgeDistance = min(
                    min(vTexCoord.x, 1.0 - vTexCoord.x),
                    min(vTexCoord.y, 1.0 - vTexCoord.y)
                );
                float outerEdge =
                    1.0 - smoothstep(0.0, 0.008, outerEdgeDistance);
                color = mix(
                    color,
                    uEdgeTint,
                    freeEdge * uEdgeStrength * 0.46
                );
                color *=
                    1.0 - outerEdge * uEdgeStrength * 0.055;

                float edgeFeather =
                    max(uTexelSize.x, uTexelSize.y) * 1.35;
                float edgeCoverage =
                    smoothstep(0.0, edgeFeather, outerEdgeDistance);
                outputAlpha *= edgeCoverage;

                gl_FragColor = vec4(
                    clamp(color, vec3(0.0), vec3(1.0)),
                    outputAlpha * uVisualAlpha
                );
            }
        """
    }
}

internal enum class GpuMaterialPageRendererStatus {
    INITIALIZING,
    READY,
    SOFTWARE_READY,
    UNSUPPORTED,
    FAILED,
    REDUCED_MOTION
}

internal fun materialPageRendererCanPresent(
    status: GpuMaterialPageRendererStatus
): Boolean =
    status == GpuMaterialPageRendererStatus.READY ||
        status == GpuMaterialPageRendererStatus.SOFTWARE_READY

internal fun shouldMountGpuMaterialPageRenderer(
    status: GpuMaterialPageRendererStatus
): Boolean =
    status == GpuMaterialPageRendererStatus.INITIALIZING ||
        status == GpuMaterialPageRendererStatus.READY

internal const val GPU_MATERIAL_PAGE_MAX_AUTO_RETRIES = 2

internal fun shouldRetryGpuMaterialPageRenderer(
    failureCount: Int,
    supported: Boolean,
    reducedMotion: Boolean
): Boolean =
    supported &&
        !reducedMotion &&
        failureCount in 1..GPU_MATERIAL_PAGE_MAX_AUTO_RETRIES

internal fun gpuMaterialPageRendererRetryDelayMillis(failureCount: Int): Long =
    when (failureCount.coerceAtLeast(1)) {
        1 -> 180L
        else -> 420L
    }

// Replacement hosts share the allocator, so a late old-host callback cannot retire a new lease.
private val gpuMaterialRendererGenerations = AtomicLong(0L)
internal fun allocateGpuMaterialRendererGeneration(): Long =
    gpuMaterialRendererGenerations.updateAndGet(::nextGpuMaterialRendererGeneration)

internal fun nextGpuMaterialRendererGeneration(current: Long): Long =
    if (current == Long.MAX_VALUE) 1L else current + 1L

internal fun gpuMaterialFrameMatchesRendererGeneration(
    frameGeneration: Long,
    rendererGeneration: Long
): Boolean =
    frameGeneration > 0L &&
        frameGeneration == rendererGeneration

internal fun nextGpuMaterialViewportGeneration(current: Long): Long =
    if (current == Long.MAX_VALUE) 1L else current + 1L

internal fun nextGpuMaterialFrameSequence(current: Long): Long =
    if (current == Long.MAX_VALUE) 1L else current + 1L

internal fun nextGpuMaterialTextureRevision(current: Long): Long =
    if (current == Long.MAX_VALUE) 1L else current + 1L

internal fun gpuMaterialFrameIsCurrent(
    frameGeneration: Long,
    rendererGeneration: Long,
    frameViewportGeneration: Long,
    rendererViewportGeneration: Long,
    frameSequence: Long,
    latestSequence: Long
): Boolean =
    gpuMaterialFrameMatchesRendererGeneration(
        frameGeneration = frameGeneration,
        rendererGeneration = rendererGeneration
    ) &&
        frameViewportGeneration > 0L &&
        frameViewportGeneration == rendererViewportGeneration &&
        frameSequence > 0L &&
        frameSequence == latestSequence

internal fun consumeGpuPageGlErrors(
    getError: () -> Int = { GLES20.glGetError() }
): Int {
    var first = GLES20.GL_NO_ERROR
    repeat(16) {
        val error = getError()
        if (error == GLES20.GL_NO_ERROR) return first
        if (first == GLES20.GL_NO_ERROR) first = error
    }
    return first
}

internal fun shouldPreserveGpuPageContextOnPause(
    lowMemoryDevice: Boolean
): Boolean = !lowMemoryDevice

internal fun shouldPreallocateGpuPageTexture(
    viewportWidth: Int,
    viewportHeight: Int,
    maxTextureSize: Int
): Boolean =
    viewportWidth > 0 &&
        viewportHeight > 0 &&
        maxTextureSize > 0 &&
        viewportWidth <= maxTextureSize &&
        viewportHeight <= maxTextureSize

private data class GpuOverlaySnapshot(
    val bitmap: Bitmap?,
    val active: Boolean,
    val progress: Float,
    val verticalBias: Float,
    val pullOriginY: Float,
    val diagonalPull: Float,
    val pointerTravel: Float,
    val edgeTravel: Float,
    val profile: MaterialPageProfile,
    val patina: Float,
    val tone: MaterialPageTone,
    val visualAlpha: Float,
    val side: MaterialPageSide,
    val sheetEpoch: Long
)

@Composable
internal fun GpuMaterialPageOverlay(
    state: MaterialPageEngineState,
    modifier: Modifier = Modifier,
    onRendererStatus: (GpuMaterialPageRendererStatus) -> Unit = {}
) {
    val context = LocalContext.current
    val supported = remember(context) {
        GpuMaterialPageCurlView.isSupported(context)
    }
    val rendererFailed = remember { mutableStateOf(false) }
    val rendererReady = remember { mutableStateOf(false) }
    val rendererFailureCount = remember { mutableIntStateOf(0) }

    val gpuStatus = when {
        state.reducedMotion -> GpuMaterialPageRendererStatus.REDUCED_MOTION
        !supported -> GpuMaterialPageRendererStatus.UNSUPPORTED
        rendererFailed.value -> GpuMaterialPageRendererStatus.FAILED
        !rendererReady.value -> GpuMaterialPageRendererStatus.INITIALIZING
        else -> GpuMaterialPageRendererStatus.READY
    }
    val effectiveStatus = when {
        state.reducedMotion -> GpuMaterialPageRendererStatus.REDUCED_MOTION
        gpuStatus == GpuMaterialPageRendererStatus.READY ->
            GpuMaterialPageRendererStatus.READY
        else -> GpuMaterialPageRendererStatus.SOFTWARE_READY
    }

    LaunchedEffect(effectiveStatus) {
        onRendererStatus(effectiveStatus)
    }
    LaunchedEffect(
        rendererFailed.value,
        rendererFailureCount.intValue,
        supported,
        state.reducedMotion
    ) {
        if (
            rendererFailed.value &&
            shouldRetryGpuMaterialPageRenderer(
                failureCount = rendererFailureCount.intValue,
                supported = supported,
                reducedMotion = state.reducedMotion
            )
        ) {
            delay(
                gpuMaterialPageRendererRetryDelayMillis(
                    rendererFailureCount.intValue
                )
            )
            if (!state.reducedMotion) {
                rendererFailed.value = false
            }
        }
    }

    val highContrast = LocalVeilHighContrast.current

    if (shouldMountGpuMaterialPageRenderer(gpuStatus)) {
        val lifecycleOwner = LocalLifecycleOwner.current
        val viewRef = remember { mutableStateOf<GpuMaterialPageCurlView?>(null) }
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> viewRef.value?.resumeRenderer()
                    Lifecycle.Event.ON_PAUSE -> viewRef.value?.pauseRenderer()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                viewRef.value?.pauseRenderer()
                viewRef.value = null
                rendererReady.value = false
            }
        }

        val gpuView = viewRef.value
        LaunchedEffect(gpuView, state, highContrast, rendererReady.value) {
            if (gpuView == null) return@LaunchedEffect
            snapshotFlow {
                GpuOverlaySnapshot(
                    bitmap = state.snapshot,
                    active = state.active,
                    progress = state.progress,
                    verticalBias = state.verticalBias,
                    pullOriginY = state.pullOriginY,
                    diagonalPull = state.diagonalPull,
                    pointerTravel = state.pointerTravel,
                    edgeTravel = state.edgeTravel,
                    profile = state.profile,
                    patina = state.patina,
                    tone = state.tone,
                    visualAlpha = state.visualAlpha,
                    side = state.side,
                    sheetEpoch = state.sheetEpoch
                )
            }.collect { frame ->
                val bitmap = frame.bitmap
                val active =
                    rendererReady.value &&
                        frame.active &&
                        bitmap != null &&
                        !bitmap.isRecycled
                gpuView.submitFrame(
                    bitmap = bitmap,
                    active = active,
                    curl = gpuPageCurlFrame(
                        progress = frame.progress,
                        verticalBias = frame.verticalBias,
                        pullOriginY = frame.pullOriginY,
                        diagonalPull = frame.diagonalPull,
                        pointerTravel = frame.pointerTravel,
                        edgeTravel = frame.edgeTravel,
                        pageAspect =
                            if (bitmap != null && bitmap.width > 0) {
                                bitmap.height.toFloat() / bitmap.width.toFloat()
                            } else {
                                1f
                            },
                        profile = frame.profile,
                        side = frame.side
                    ),
                    profile = frame.profile,
                    patina = frame.patina,
                    tone = frame.tone,
                    visualAlpha = frame.visualAlpha,
                    highContrast = highContrast,
                    sheetEpoch = frame.sheetEpoch
                )
            }
        }

        AndroidView(
            factory = { viewContext ->
                GpuMaterialPageCurlView(
                    context = viewContext,
                    onRendererReady = { ready ->
                        rendererReady.value = ready
                        if (ready) rendererFailed.value = false
                    },
                    onRendererFailure = {
                        rendererReady.value = false
                        rendererFailureCount.intValue += 1
                        rendererFailed.value = true
                    },
                    onRendererFramePresented = {
                        rendererFailureCount.intValue = 0
                    },
                    onSheetPresented = state::acknowledgeSheetPresented,
                    onTextureUploadLeaseRequired = state::markSnapshotSubmittedForGpu,
                    onTextureUploaded = state::acknowledgeSnapshotUploaded,
                    onTextureUploadsInvalidated = state::abandonGpuUploadLeases
                ).also { created ->
                    viewRef.value = created
                    if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        created.pauseRenderer()
                    }
                }
            },
            modifier = modifier,
            update = { }
        )
    }

    if (effectiveStatus == GpuMaterialPageRendererStatus.SOFTWARE_READY) {
        SoftwareMaterialPageCurlOverlay(
            state = state,
            modifier = modifier,
            highContrast = highContrast
        )
    }
}

/** Exact acquired-buffer acknowledgement, fenced against old contexts and resized viewports. */
internal fun gpuMaterialSheetPresentationMatches(
    drawTimestamp: Long,
    acquiredTimestamp: Long,
    drawGeneration: Long,
    rendererGeneration: Long,
    drawViewport: Long,
    viewportGeneration: Long
): Boolean = drawTimestamp > 0L && drawTimestamp == acquiredTimestamp &&
    gpuMaterialFrameMatchesRendererGeneration(drawGeneration, rendererGeneration) &&
    drawViewport > 0L && drawViewport == viewportGeneration
