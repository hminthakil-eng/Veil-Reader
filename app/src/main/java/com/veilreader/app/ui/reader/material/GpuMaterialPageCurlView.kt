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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
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
    private var textureDirty = true
    private var rendererFailed = false

    private var program = 0
    private var vertexBufferId = 0
    private var indexBufferId = 0
    private var textureId = 0
    private var indexCount = 0

    private var aPosition = -1
    private var aTexCoord = -1
    private var uTexture = -1
    private var uCylinderPosition = -1
    private var uCylinderTilt = -1
    private var uCylinderRadius = -1
    private var uPageAspect = -1
    private var uSideSign = -1
    private var uFrontTint = -1
    private var uBackTint = -1
    private var uFrontTintAlpha = -1
    private var uGhostAlpha = -1
    private var uRoughness = -1
    private var uSpecular = -1
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
        synchronized(frameLock) {
            if (
                active &&
                (
                    !lastSubmittedActive ||
                        submittedFrame?.bitmap !== usableBitmap
                    )
            ) {
                textureDirty = true
            }
            submittedFrame = SubmittedFrame(
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
        }
        requestRender()
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        rendererFailed = false
        runCatching {
            program = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER)
            resolveLocations()
            createMesh()
            createTexture()
            GLES20.glDisable(GLES20.GL_CULL_FACE)
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(
                GLES20.GL_SRC_ALPHA,
                GLES20.GL_ONE_MINUS_SRC_ALPHA
            )
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            GLES20.glDepthFunc(GLES20.GL_LEQUAL)
            GLES20.glClearColor(0f, 0f, 0f, 0f)
            synchronized(frameLock) {
                textureDirty = true
            }
        }.onFailure { error ->
            rendererFailed = true
            Log.e(TAG, "GPU page renderer initialization failed", error)
            post { onRendererFailure() }
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width.coerceAtLeast(1), height.coerceAtLeast(1))
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

        GLES20.glUseProgram(program)
        uploadTextureIfNeeded(bitmap)

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

        // First project a restrained translucent shadow onto the destination page,
        // then draw the physical sheet. Both use the same deformed mesh so the
        // shadow follows lift and diagonal corner pulls without per-frame CPU meshes.
        GLES20.glUniform1f(uShadowPass, 1f)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            indexCount,
            GLES20.GL_UNSIGNED_SHORT,
            0
        )

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
        setColorUniform(uFrontTint, front)
        setColorUniform(uBackTint, back)

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
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uTexture, 0)
    }

    private fun setColorUniform(location: Int, argb: Long) {
        val r = ((argb ushr 16) and 0xFF).toFloat() / 255f
        val g = ((argb ushr 8) and 0xFF).toFloat() / 255f
        val b = (argb and 0xFF).toFloat() / 255f
        GLES20.glUniform3f(location, r, g, b)
    }

    private fun uploadTextureIfNeeded(bitmap: Bitmap) {
        val shouldUpload = synchronized(frameLock) {
            val value = textureDirty
            textureDirty = false
            value
        }
        if (!shouldUpload) return

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLUtils.texImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            bitmap,
            0
        )
    }

    private fun createTexture() {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        textureId = ids[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
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
        uTexture = GLES20.glGetUniformLocation(program, "uTexture")
        uCylinderPosition = GLES20.glGetUniformLocation(program, "uCylinderPosition")
        uCylinderTilt = GLES20.glGetUniformLocation(program, "uCylinderTilt")
        uCylinderRadius = GLES20.glGetUniformLocation(program, "uCylinderRadius")
        uPageAspect = GLES20.glGetUniformLocation(program, "uPageAspect")
        uSideSign = GLES20.glGetUniformLocation(program, "uSideSign")
        uFrontTint = GLES20.glGetUniformLocation(program, "uFrontTint")
        uBackTint = GLES20.glGetUniformLocation(program, "uBackTint")
        uFrontTintAlpha = GLES20.glGetUniformLocation(program, "uFrontTintAlpha")
        uGhostAlpha = GLES20.glGetUniformLocation(program, "uGhostAlpha")
        uRoughness = GLES20.glGetUniformLocation(program, "uRoughness")
        uSpecular = GLES20.glGetUniformLocation(program, "uSpecular")
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
        private const val MESH_X = 48
        private const val MESH_Y = 8

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
                    deformed.xy += normal2 * (0.008 + deformed.z * 0.13);
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

            uniform sampler2D uTexture;
            uniform vec3 uFrontTint;
            uniform vec3 uBackTint;
            uniform float uFrontTintAlpha;
            uniform float uGhostAlpha;
            uniform float uRoughness;
            uniform float uSpecular;
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
                    float shadowEnvelope =
                        smoothstep(0.02, 0.34, vLift) *
                        (1.0 - smoothstep(0.82, 1.0, vLift) * 0.35);
                    gl_FragColor = vec4(
                        0.0,
                        0.0,
                        0.0,
                        uShadowStrength * shadowEnvelope
                    );
                    return;
                }

                vec4 ink = texture2D(uTexture, vTexCoord);
                vec3 n = normalize(vNormal);
                float facing = clamp(abs(n.z), 0.0, 1.0);
                float grazing = 1.0 - facing;

                vec3 color;
                if (gl_FrontFacing) {
                    color = mix(ink.rgb, uFrontTint, uFrontTintAlpha);
                    float diffuse = 0.82 + facing * 0.18;
                    float highlight =
                        grazing * grazing *
                        uSpecular *
                        (1.0 - uRoughness) *
                        0.34;
                    color = color * diffuse + vec3(highlight);
                } else {
                    color = mix(
                        uBackTint,
                        ink.rgb,
                        clamp(uGhostAlpha, 0.0, 0.34)
                    );
                    float diffuse = 0.72 + facing * 0.22;
                    color *= diffuse;
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

                float edgeDistance = min(
                    min(vTexCoord.x, 1.0 - vTexCoord.x),
                    min(vTexCoord.y, 1.0 - vTexCoord.y)
                );
                float edge = 1.0 - smoothstep(0.0, 0.012, edgeDistance);
                color *= 1.0 - edge * uEdgeStrength * 0.16;

                gl_FragColor = vec4(
                    clamp(color, vec3(0.0), vec3(1.0)),
                    ink.a * uVisualAlpha
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
    if (!supported || rendererFailed.value) {
        MaterialPageOverlay(state = state, modifier = modifier)
        return
    }

    val highContrast = LocalVeilHighContrast.current
    val bitmap = state.snapshot
    val active =
        state.active &&
            bitmap != null &&
            !bitmap.isRecycled
    val curl = gpuPageCurlFrame(
        progress = state.progress,
        verticalBias = state.verticalBias,
        pullOriginY = state.pullOriginY,
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
            )
        },
        modifier = modifier,
        update = { view ->
            view.submitFrame(
                bitmap = bitmap,
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
