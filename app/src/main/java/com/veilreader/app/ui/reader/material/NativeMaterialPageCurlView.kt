package com.veilreader.app.ui.reader.material

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLExt
import android.opengl.GLSurfaceView
import android.view.TextureView
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.irurueta.android.glutils.GLTextureView
import com.veilreader.app.diagnostics.ReaderTrace
import fi.harism.curl.CurlMesh
import fi.harism.curl.CurlPage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * The licensed Harism cylinder mesh attached to REAL Readium page snapshots.
 * The host is visual-only: Readium owns input, locators and release decisions.
 * No demo Activity, second navigator, synthetic book or native slide fallback.
 * Original engine retained for explicit rollback; debug/benchmark select this backend.
 */
internal class NativeMaterialPageCurlView(
    context: Context,
    private val state: MaterialPageEngineState,
    private val onReady: (Boolean) -> Unit,
    private val onFailure: () -> Unit,
    private val onSuccessfulFrame: () -> Unit
) : GLTextureView(context), GLSurfaceView.Renderer {
    private data class Frame(
        val bitmap: Bitmap?, val epoch: Long, val generation: Long, val viewport: Long,
        val revision: Long, val active: Boolean, val travel: Float, val gripY: Float,
        val vertical: Float, val fromRight: Boolean, val tone: MaterialPageTone
    )
    private data class Acquired(val timestamp: Long, val epoch: Long, val generation: Long, val viewport: Long)

    private val lock = Any()
    private val draws = ArrayDeque<Acquired>()
    private val leases = mutableListOf<MaterialPageGpuUploadLease>()
    private var frame: Frame? = null
    private var revision = 0L
    @Volatile private var generation = 0L
    @Volatile private var viewport = 0L
    @Volatile private var paused = false
    @Volatile private var failureReported = false
    @Volatile private var ready = false
    internal val renderingReady: Boolean get() = ready && !failureReported && !paused
    private var mesh = CurlMesh(32)
    private var uploadedEpoch = 0L
    private var maxTextureSize = 0
    private var aspect = 1f
    private var probeScheduled = false
    internal var lastAcquiredSheetEpoch = 0L
        private set
    private val probe = Runnable {
        probeScheduled = false
        if (!paused && isAttachedToWindow && isAvailable) {
            surfaceTexture?.let(::acknowledgeAcquisition)
            scheduleProbe()
        }
    }

    init {
        // CurlMesh is a real ES1 triangle-strip renderer, not an ES2 shader imitation.
        setEGLContextClientVersion(1)
        setEGLConfigChooser(8, 8, 8, 8, 0, 0)
        preserveEGLContextOnPause = false
        isOpaque = false
        isClickable = false
        isFocusable = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        setRenderer(this)
        renderMode = RENDER_MODE_WHEN_DIRTY
        val delegate = surfaceTextureListener
        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                delegate?.onSurfaceTextureAvailable(surface, width, height)
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                delegate?.onSurfaceTextureSizeChanged(surface, width, height)
            }
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                cancelProbe()
                synchronized(lock) { draws.clear() }
                return delegate?.onSurfaceTextureDestroyed(surface) ?: true
            }
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                delegate?.onSurfaceTextureUpdated(surface)
                acknowledgeAcquisition(surface)
                scheduleProbe()
            }
        }
    }

    /** Called on UI thread; a lease prevents the snapshot pool reusing pixels during copy. */
    fun submit() {
        val bitmap = state.snapshot
        val active = state.active && bitmap != null && !bitmap.isRecycled
        synchronized(lock) {
            val old = frame
            if (active && (old?.bitmap !== bitmap || old.epoch != state.sheetEpoch || old.generation != generation)) {
                revision = nextGpuMaterialTextureRevision(revision)
                state.markSnapshotSubmittedForGpu(bitmap!!, generation, revision)
                leases += MaterialPageGpuUploadLease(bitmap, generation, revision)
            }
            frame = Frame(bitmap, state.sheetEpoch, generation, viewport, revision, active,
                state.nativeLeafTravel, state.pullOriginY, state.verticalBias,
                state.side == MaterialPageSide.RIGHT, state.tone)
            if (!active) draws.clear()
        }
        if (!active) cancelProbe()
        requestRender()
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig?) {
        generation = allocateGpuMaterialRendererGeneration()
        mesh = CurlMesh(32)
        uploadedEpoch = 0L
        failureReported = false
        ready = false
        gl.glClearColor(0f, 0f, 0f, 0f)
        gl.glShadeModel(GL10.GL_SMOOTH)
        gl.glDisable(GL10.GL_DEPTH_TEST)
        gl.glDisable(GL10.GL_CULL_FACE)
        val limit = IntArray(1)
        gl.glGetIntegerv(GL10.GL_MAX_TEXTURE_SIZE, limit, 0)
        maxTextureSize = limit[0]
        synchronized(lock) { draws.clear() }
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        viewport = nextGpuMaterialViewportGeneration(viewport)
        aspect = height.toFloat() / width
        gl.glViewport(0, 0, width, height)
        gl.glMatrixMode(GL10.GL_PROJECTION)
        gl.glLoadIdentity()
        gl.glOrthof(0f, 1f, 0f, aspect, -2f, 2f)
        gl.glMatrixMode(GL10.GL_MODELVIEW)
        gl.glLoadIdentity()
        mesh.setRect(RectF(0f, aspect, 1f, 0f))
        mesh.reset()
        uploadedEpoch = 0L
        synchronized(lock) { draws.clear() }
        // Do not use compile/attach as readiness; the first clear draw must succeed.
        ready = false
        post { onReady(false); submit() }
        requestRender()
    }

    override fun onDrawFrame(gl: GL10) {
        // Snapshot these before drawing. A UI submission arriving DURING this
        // draw still needs its lease until a later GL read completes.
        val retired = synchronized(lock) { leases.toList().also { leases.clear() } }
        try {
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT)
            if (paused || failureReported) return
            val current = synchronized(lock) { frame }
            if (current?.active == true && current.generation == generation && current.viewport == viewport) {
                val source = current.bitmap ?: return
                if (source.isRecycled || source.width != width || source.height != height ||
                    Integer.highestOneBit(source.width - 1) * 2 > maxTextureSize ||
                    Integer.highestOneBit(source.height - 1) * 2 > maxTextureSize) {
                    error("Native paper snapshot does not match the live viewport/texture limit")
                }
                if (uploadedEpoch != current.epoch) {
                    // CurlPage recycles its inputs after upload. Transfer an owned copy,
                    // NEVER Readium's borrowed pooled snapshot. Back uses the SAME leaf.
                    val owned = source.copy(Bitmap.Config.ARGB_8888, false)
                        ?: error("Native paper copy failed")
                    mesh.texturePage.setColor(Color.WHITE, CurlPage.SIDE_FRONT)
                    mesh.texturePage.setColor(when (current.tone) {
                        MaterialPageTone.LIGHT -> Color.rgb(232, 230, 220)
                        MaterialPageTone.SEPIA -> Color.rgb(232, 221, 196)
                        MaterialPageTone.DARK -> Color.rgb(150, 155, 165)
                    }, CurlPage.SIDE_BACK)
                    mesh.texturePage.setTexture(owned, CurlPage.SIDE_BOTH)
                    mesh.prepareTexture(gl)
                    uploadedEpoch = current.epoch
                }
                val fold = nativePaperFold(current.travel, current.gripY,
                    current.vertical * aspect, aspect, current.fromRight)
                if (fold.flat) mesh.reset() else mesh.curl(PointF(fold.x, fold.y),
                    PointF(fold.directionX, fold.directionY), fold.radius)
                mesh.onDrawFrame(gl)
                check(gl.glGetError() == GL10.GL_NO_ERROR) { "Native paper GL draw failed" }
                val timestamp = System.nanoTime()
                check(EGLExt.eglPresentationTimeANDROID(EGL14.eglGetCurrentDisplay(),
                    EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW), timestamp)) { "Paper timestamp unsupported" }
                synchronized(lock) {
                    if (draws.size == 8) draws.removeFirst()
                    draws.addLast(Acquired(timestamp, current.epoch, generation, viewport))
                }
                post { onSuccessfulFrame(); scheduleProbe() }
            }
            if (!ready) {
                check(gl.glGetError() == GL10.GL_NO_ERROR) { "Native paper initial draw failed" }
                ready = true
                post { if (!paused) onReady(true) }
            }
        } catch (error: Exception) {
            if (!failureReported) {
                failureReported = true
                ReaderTrace.event("paper_native_render_failed", details = "type=${error::class.java.simpleName}")
                post { onReady(false); onFailure() }
            }
        } finally {
            // Copy/upload have finished. Retire skipped submissions too, bounded by input.
            if (retired.isNotEmpty()) post {
                retired.forEach { state.acknowledgeSnapshotUploaded(it.bitmap, it.rendererGeneration, it.textureRevision) }
            }
        }
    }

    private fun acknowledgeAcquisition(surface: SurfaceTexture) {
        val timestamp = surface.timestamp
        val match = synchronized(lock) {
            draws.firstOrNull { it.timestamp == timestamp }.also {
                while (draws.isNotEmpty() && draws.first().timestamp <= timestamp) draws.removeFirst()
            }
        } ?: return
        val live = synchronized(lock) { frame }
        if (!paused && live?.active == true && live.epoch == match.epoch &&
            gpuMaterialSheetPresentationMatches(match.timestamp, timestamp, match.generation, generation, match.viewport, viewport)) {
            lastAcquiredSheetEpoch = match.epoch
            state.acknowledgeSheetPresented(match.epoch)
        }
    }

    private fun scheduleProbe() {
        if (!probeScheduled && !paused && isAvailable && isAttachedToWindow && synchronized(lock) { draws.isNotEmpty() }) {
            probeScheduled = true
            postOnAnimation(probe)
        }
    }

    private fun cancelProbe() { removeCallbacks(probe); probeScheduled = false }

    fun pauseRenderer() {
        if (paused) return
        paused = true
        cancelProbe()
        state.invalidateSheetPresentation()
        onReady(false)
        onPause() // Join GL before abandoning borrowed pixels.
        val oldGeneration = generation
        synchronized(lock) { draws.clear(); leases.clear() }
        state.abandonGpuUploadLeases(oldGeneration)
    }

    fun resumeRenderer() {
        if (paused) { paused = false; onResume() }
        requestRender()
    }

    override fun onDetachedFromWindow() {
        cancelProbe()
        super.onDetachedFromWindow()
        val oldGeneration = generation
        synchronized(lock) { draws.clear(); leases.clear() }
        state.abandonGpuUploadLeases(oldGeneration)
    }
}

@Composable
internal fun NativeMaterialPageOverlay(
    state: MaterialPageEngineState,
    modifier: Modifier,
    onRendererStatus: (GpuMaterialPageRendererStatus) -> Unit
) {
    var ready by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var failures by remember { mutableIntStateOf(0) }
    var view by remember { mutableStateOf<NativeMaterialPageCurlView?>(null) }
    val owner = LocalLifecycleOwner.current
    val status = when {
        state.reducedMotion -> GpuMaterialPageRendererStatus.REDUCED_MOTION
        failed -> GpuMaterialPageRendererStatus.FAILED
        !ready -> GpuMaterialPageRendererStatus.INITIALIZING
        else -> GpuMaterialPageRendererStatus.READY
    }
    LaunchedEffect(status) { onRendererStatus(status) }
    LaunchedEffect(failed, failures, state.reducedMotion) {
        if (failed && shouldRetryGpuMaterialPageRenderer(failures, true, state.reducedMotion)) {
            delay(gpuMaterialPageRendererRetryDelayMillis(failures))
            failed = false
        }
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> view?.pauseRenderer()
                Lifecycle.Event.ON_RESUME -> view?.resumeRenderer()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            view?.pauseRenderer()
            view = null
            ready = false
        }
    }
    val nativeView = view
    LaunchedEffect(nativeView, state, ready) {
        if (nativeView == null) return@LaunchedEffect
        snapshotFlow { listOf(state.snapshot, state.active, state.sheetEpoch, state.nativeLeafTravel,
            state.pullOriginY, state.verticalBias, state.side, state.tone) }.collect { nativeView.submit() }
    }
    if (!shouldMountGpuMaterialPageRenderer(status)) return
    AndroidView(factory = { context ->
        NativeMaterialPageCurlView(context, state, onReady = { ready = it },
            onFailure = { ready = false; failed = true; failures++ },
            onSuccessfulFrame = { failures = 0 }).also {
            view = it
            if (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) it.pauseRenderer()
        }
    }, modifier = modifier)
}
