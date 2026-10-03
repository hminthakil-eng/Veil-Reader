package com.veilreader.app.ui.reader.material

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.util.Log
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.veilreader.app.ui.theme.LocalVeilHighContrast
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
    private val onRendererFailure: () -> Unit = {}
) : GLSurfaceView(context), GLSurfaceView.Renderer {

    private data class SubmittedFrame(
        val bitmap: Bitmap?,
        val backBitmap: Bitmap?,
        val active: Boolean,
        val curl: GpuPageCurlFrame,
        val profile: MaterialPageProfile,
        val patina: Float,
        val tone: MaterialPageTone,
        val visualAlpha: Float,
        val highContrast: Boolean
    )

    private val frameLock = Any()
    private var submittedFrame: SubmittedFrame? = null
    private var lastSubmittedActive = false
    private var frontTextureDirty = true
    private var backTextureDirty = true
    private var rendererFailed = false

    private var program = 0
    private var vertexBufferId = 0
    private var indexBufferId = 0
    private var frontTextureId = 0
    private var backTextureId = 0
    private var frontTextureWidth = 1
    private var frontTextureHeight = 1
    private var backTextureWidth = 1
    private var backTextureHeight = 1
    private var indexCount = 0
    private var viewportWidth = 0
    private var viewportHeight = 0
    private var maxTextureSize = 0
    private var failureReported = false
    private var rendererPaused = false

    private var aPosition = -1
    private var aTexCoord = -1
    private var uFrontTexture = -1
    private var uBackTexture = -1
    private var uHasBackTexture = -1
    private var uCylinderPosition = -1
    private var uCylinderTilt = -1
    private var uCylinderRadius = -1
    private var uPageAspect = -1
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
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderOnTop(true)
        isClickable = false
        isFocusable = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        preserveEGLContextOnPause = true
        setRenderer(this)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun submitFrame(
        bitmap: Bitmap?,
        backBitmap: Bitmap?,
        active: Boolean,
        curl: GpuPageCurlFrame,
        profile: MaterialPageProfile,
        patina: Float,
        tone: MaterialPageTone,
        visualAlpha: Float,
        highContrast: Boolean
    ) {
        val usableBitmap =
            bitmap?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
        val usableBackBitmap =
            backBitmap?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
        synchronized(frameLock) {
            if (
                active &&
                (
                    !lastSubmittedActive ||
                        submittedFrame?.bitmap !== usableBitmap
                    )
            ) {
                frontTextureDirty = true
            }
            if (submittedFrame?.backBitmap !== usableBackBitmap) {
                backTextureDirty = true
            }
            submittedFrame = SubmittedFrame(
                bitmap = usableBitmap,
                backBitmap = usableBackBitmap,
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
        }
        requestRender()
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        rendererFailed = false
        runCatching {
            program = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER)
            resolveLocations()
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
            failureReported = false
            synchronized(frameLock) {
                frontTextureDirty = true
                backTextureDirty = true
            }
        }.onFailure { error ->
            failRenderer("GPU page renderer initialization failed", error)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width.coerceAtLeast(1)
        viewportHeight = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(
            GLES20.GL_COLOR_BUFFER_BIT or
                GLES20.GL_DEPTH_BUFFER_BIT
        )
        if (rendererFailed || program == 0) return

        val frame = synchronized(frameLock) { submittedFrame } ?: return
        val bitmap = frame.bitmap ?: return
        if (!frame.active || bitmap.isRecycled) return
        if (!textureFits(bitmap) || frame.backBitmap?.let { !textureFits(it) } == true) {
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
        uploadTexturesIfNeeded(
            frontBitmap = bitmap,
            backBitmap = frame.backBitmap
        )
        val uploadError = GLES20.glGetError()
        if (uploadError != GLES20.GL_NO_ERROR) {
            failRenderer("GPU page texture upload failed: glError=$uploadError")
            return
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
        for (shadowLayer in 1..3) {
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
            post { onRendererFailure() }
        }
    }

    fun pauseRenderer() {
        if (rendererPaused) return
        runCatching { onPause() }
            .onSuccess { rendererPaused = true }
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
        val bitmap = frame.bitmap
        val pageAspect =
            if (bitmap != null && bitmap.width > 0) {
                bitmap.height.toFloat() / bitmap.width.toFloat()
            } else {
                1f
            }
        GLES20.glUniform1f(
            uPageAspect,
            pageAspect.takeIf { it.isFinite() }?.coerceIn(0.5f, 3f) ?: 1f
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

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, backTextureId)
        GLES20.glUniform1i(uBackTexture, 1)
        GLES20.glUniform1f(
            uHasBackTexture,
            if (frame.backBitmap != null && !frame.backBitmap.isRecycled) 1f else 0f
        )
    }

    private fun setColorUniform(location: Int, argb: Long) {
        val r = ((argb ushr 16) and 0xFF).toFloat() / 255f
        val g = ((argb ushr 8) and 0xFF).toFloat() / 255f
        val b = (argb and 0xFF).toFloat() / 255f
        GLES20.glUniform3f(location, r, g, b)
    }

    private fun uploadTexturesIfNeeded(
        frontBitmap: Bitmap,
        backBitmap: Bitmap?
    ) {
        val uploadFront: Boolean
        val uploadBack: Boolean
        synchronized(frameLock) {
            uploadFront = frontTextureDirty
            uploadBack = backTextureDirty && backBitmap != null
            frontTextureDirty = false
            if (uploadBack) backTextureDirty = false
        }

        if (uploadFront) {
            uploadBitmapToTexture(
                textureId = frontTextureId,
                bitmap = frontBitmap,
                front = true
            )
        }

        if (uploadBack && backBitmap != null && !backBitmap.isRecycled) {
            uploadBitmapToTexture(
                textureId = backTextureId,
                bitmap = backBitmap,
                front = false
            )
        }
    }

    private fun uploadBitmapToTexture(
        textureId: Int,
        bitmap: Bitmap,
        front: Boolean
    ) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        val currentWidth =
            if (front) frontTextureWidth else backTextureWidth
        val currentHeight =
            if (front) frontTextureHeight else backTextureHeight

        if (
            currentWidth == bitmap.width &&
            currentHeight == bitmap.height
        ) {
            GLUtils.texSubImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                0,
                0,
                bitmap
            )
        } else {
            GLUtils.texImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                bitmap,
                0
            )
            if (front) {
                frontTextureWidth = bitmap.width
                frontTextureHeight = bitmap.height
            } else {
                backTextureWidth = bitmap.width
                backTextureHeight = bitmap.height
            }
        }
    }

    private fun createTextures() {
        val ids = IntArray(2)
        GLES20.glGenTextures(2, ids, 0)
        frontTextureId = ids[0]
        backTextureId = ids[1]
        frontTextureWidth = 1
        frontTextureHeight = 1
        backTextureWidth = 1
        backTextureHeight = 1
        configureTexture(frontTextureId)
        configureTexture(backTextureId)
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
        val vertexCount = (MESH_X + 1) * (MESH_Y + 1)
        val vertexData = FloatArray(vertexCount * FLOATS_PER_VERTEX)
        var vertexOffset = 0
        for (y in 0..MESH_Y) {
            val fy = y.toFloat() / MESH_Y.toFloat()
            for (x in 0..MESH_X) {
                val fx = x.toFloat() / MESH_X.toFloat()
                vertexData[vertexOffset++] = fx
                vertexData[vertexOffset++] = fy
                vertexData[vertexOffset++] = fx
                vertexData[vertexOffset++] = fy
            }
        }

        val indices = ShortArray(MESH_X * MESH_Y * 6)
        var indexOffset = 0
        val row = MESH_X + 1
        for (y in 0 until MESH_Y) {
            for (x in 0 until MESH_X) {
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
        uBackTexture = GLES20.glGetUniformLocation(program, "uBackTexture")
        uHasBackTexture = GLES20.glGetUniformLocation(program, "uHasBackTexture")
        uCylinderPosition = GLES20.glGetUniformLocation(program, "uCylinderPosition")
        uCylinderTilt = GLES20.glGetUniformLocation(program, "uCylinderTilt")
        uCylinderRadius = GLES20.glGetUniformLocation(program, "uCylinderRadius")
        uPageAspect = GLES20.glGetUniformLocation(program, "uPageAspect")
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

    private fun buildProgram(vertexSource: String, fragmentSource: String): Int {
        val vertex = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragment = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        val result = GLES20.glCreateProgram()
        GLES20.glAttachShader(result, vertex)
        GLES20.glAttachShader(result, fragment)
        GLES20.glLinkProgram(result)

        val status = IntArray(1)
        GLES20.glGetProgramiv(result, GLES20.GL_LINK_STATUS, status, 0)
        GLES20.glDeleteShader(vertex)
        GLES20.glDeleteShader(fragment)
        if (status[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(result)
            GLES20.glDeleteProgram(result)
            error("GPU page program link failed: $log")
        }
        return result
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
        private const val MESH_X = 64
        private const val MESH_Y = 12

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

        private const val VERTEX_SHADER = """
            precision highp float;

            attribute vec2 aPosition;
            attribute vec2 aTexCoord;

            uniform vec2 uCylinderPosition;
            uniform float uCylinderTilt;
            uniform float uCylinderRadius;
            uniform float uPageAspect;
            uniform float uSideSign;
            uniform float uShadowPass;
            uniform float uSideSign;

            varying vec2 vTexCoord;
            varying vec3 vNormal;
            varying float vLift;

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

        private const val FRAGMENT_SHADER = """
            precision mediump float;

            uniform sampler2D uFrontTexture;
            uniform sampler2D uBackTexture;
            uniform float uHasBackTexture;
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
            uniform float uShadowPass;

            varying vec2 vTexCoord;
            varying vec3 vNormal;
            varying float vLift;

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
                vec4 destinationInk =
                    texture2D(uBackTexture, vec2(1.0 - vTexCoord.x, vTexCoord.y));
                vec3 n = normalize(vNormal);
                float facing = clamp(abs(n.z), 0.0, 1.0);
                float grazing = 1.0 - facing;

                vec3 faceNormal =
                    normalize(gl_FrontFacing ? n : -n);
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
                if (gl_FrontFacing) {
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
                    vec4 backInk =
                        mix(mirroredFrontInk, destinationInk, uHasBackTexture);
                    float backContent =
                        mix(
                            clamp(uGhostAlpha, 0.0, 0.34),
                            0.92 - uRoughness * 0.06,
                            uHasBackTexture
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

                gl_FragColor = vec4(
                    clamp(color, vec3(0.0), vec3(1.0)),
                    outputAlpha * uVisualAlpha
                );
            }
        """
    }
}

@Composable
internal fun GpuMaterialPageOverlay(
    state: MaterialPageEngineState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val supported = remember(context) {
        GpuMaterialPageCurlView.isSupported(context)
    }
    val rendererFailed = remember { mutableStateOf(false) }
    if (!supported || rendererFailed.value || state.reducedMotion) {
        // Reduced Motion intentionally bypasses geometric curl; the existing
        // Material fallback renders the restrained flat alpha/edge transition.
        MaterialPageOverlay(state = state, modifier = modifier)
        return
    }

    val highContrast = LocalVeilHighContrast.current
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
        }
    }

    val bitmap = state.snapshot
    val backBitmap = state.backSnapshot
    val active =
        state.active &&
            bitmap != null &&
            !bitmap.isRecycled
    val curl = gpuPageCurlFrame(
        progress = state.progress,
        verticalBias = state.verticalBias,
        pullOriginY = state.pullOriginY,
        diagonalPull = state.diagonalPull,
        profile = state.profile,
        side = state.side
    )

    AndroidView(
        factory = { viewContext ->
            GpuMaterialPageCurlView(
                context = viewContext,
                onRendererFailure = {
                    rendererFailed.value = true
                }
            ).also { created ->
                viewRef.value = created
            }
        },
        modifier = modifier,
        update = { view ->
            view.submitFrame(
                bitmap = bitmap,
                backBitmap = backBitmap,
                active = active,
                curl = curl,
                profile = state.profile,
                patina = state.patina,
                tone = state.tone,
                visualAlpha = state.visualAlpha,
                highContrast = highContrast
            )
        }
    )
}
