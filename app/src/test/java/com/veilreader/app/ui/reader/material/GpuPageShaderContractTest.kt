package com.veilreader.app.ui.reader.material

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GpuPageShaderContractTest {

    @Test
    fun `presentation acknowledges only the acquired buffer from the live context and viewport`() {
        assertTrue(gpuMaterialSheetPresentationMatches(100L, 100L, 2L, 2L, 3L, 3L))
        assertTrue(!gpuMaterialSheetPresentationMatches(100L, 99L, 2L, 2L, 3L, 3L))
        assertTrue(!gpuMaterialSheetPresentationMatches(100L, 100L, 1L, 2L, 3L, 3L))
        assertTrue(!gpuMaterialSheetPresentationMatches(100L, 100L, 2L, 2L, 2L, 3L))
        assertTrue(!gpuMaterialSheetPresentationMatches(0L, 0L, 2L, 2L, 3L, 3L))
    }

    @Test
    fun `replacement renderer hosts never share a generation under concurrent initialization`() {
        val executor = java.util.concurrent.Executors.newFixedThreadPool(4)
        try {
            val generations = executor.invokeAll((1..100).map {
                java.util.concurrent.Callable { allocateGpuMaterialRendererGeneration() }
            }).map { it.get() }
            assertEquals(100, generations.toSet().size)
            assertTrue(generations.all { it > 0L })
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `vertex shader has unique and declared uniforms`() {
        assertShaderUniformContract(
            shader = GpuMaterialPageCurlView.VERTEX_SHADER
        )
    }

    @Test
    fun `fragment shader has unique and declared uniforms`() {
        assertShaderUniformContract(
            shader = GpuMaterialPageCurlView.FRAGMENT_SHADER
        )
    }

    @Test
    fun `vertex and fragment varyings match exactly`() {
        val varyingRegex =
            Regex("""\bvarying\s+(?:(?:lowp|mediump|highp)\s+)?(\w+)\s+(v[A-Za-z0-9_]+)\s*;""")
        fun varyings(shader: String): Map<String, String> =
            varyingRegex.findAll(shader)
                .associate { match ->
                    match.groupValues[2] to match.groupValues[1]
                }

        assertEquals(
            varyings(GpuMaterialPageCurlView.VERTEX_SHADER),
            varyings(GpuMaterialPageCurlView.FRAGMENT_SHADER)
        )
    }

    @Test
    fun `shared shader symbols use identical effective precision on both stages`() {
        val vertex = GpuMaterialPageCurlView.VERTEX_SHADER
        val fragment = GpuMaterialPageCurlView.FRAGMENT_SHADER

        fun precision(shader: String, qualifier: String, symbol: String): String? {
            val pattern = Regex(
                """\b${qualifier}\s+(?:(lowp|mediump|highp)\s+)?(?:float|vec2|vec3|vec4)\s+${symbol}\s*;"""
            )
            val match = pattern.find(shader) ?: return null
            val explicitlyDeclared = match.groupValues[1]
            if (explicitlyDeclared.isNotEmpty()) return explicitlyDeclared

            // GLSL ES 1.00: vertex float defaults to highp while fragment
            // float defaults to mediump. A matching type alone is insufficient
            // and failed to link the actual Material Page shader in CI.
            return if (shader === vertex) "highp" else "mediump"
        }

        val shared = mapOf(
            "uSideSign" to "uniform",
            "uShadowPass" to "uniform",
            "vTexCoord" to "varying",
            "vNormal" to "varying",
            "vLift" to "varying"
        )
        shared.forEach { (symbol, qualifier) ->
            val v = precision(vertex, qualifier, symbol)
            val f = precision(fragment, qualifier, symbol)
            assertTrue("No vertex precision for $symbol", v != null)
            assertTrue("No fragment precision for $symbol", f != null)
            assertEquals("GLSL ES program link precision mismatch: $symbol", v, f)
        }
    }

    @Test
    fun `renderer critical uniforms stay present`() {
        val source =
            GpuMaterialPageCurlView.VERTEX_SHADER +
                "\n" +
                GpuMaterialPageCurlView.FRAGMENT_SHADER
        val required = setOf(
            "uCylinderPosition",
            "uCylinderTilt",
            "uCylinderRadius",
            "uPageAspect",
            "uSideSign",
            "uShadowPass",
            "uFrontTexture",
            "uTexelSize",
            "uFrontTint",
            "uBackTint",
            "uEdgeTint",
            "uRoughness",
            "uSpecular",
            "uTranslucency",
            "uVisualAlpha"
        )

        required.forEach { name ->
            assertTrue(
                "Missing critical GPU page uniform: $name",
                source.contains(name)
            )
        }
    }

    @Test
    fun `initializing renderer stays mounted so GL can become ready`() {
        assertTrue(shouldMountGpuMaterialPageRenderer(GpuMaterialPageRendererStatus.INITIALIZING))
        assertTrue(shouldMountGpuMaterialPageRenderer(GpuMaterialPageRendererStatus.READY))
        assertTrue(!shouldMountGpuMaterialPageRenderer(GpuMaterialPageRendererStatus.FAILED))
        assertTrue(!shouldMountGpuMaterialPageRenderer(GpuMaterialPageRendererStatus.UNSUPPORTED))
        assertTrue(!shouldMountGpuMaterialPageRenderer(GpuMaterialPageRendererStatus.REDUCED_MOTION))
    }

    @Test
    fun `gpu context generation rejects stale frames and survives counter rollover`() {
        assertEquals(8L, nextGpuMaterialRendererGeneration(7L))
        assertEquals(1L, nextGpuMaterialRendererGeneration(Long.MAX_VALUE))
        assertTrue(gpuMaterialFrameMatchesRendererGeneration(4L, 4L))
        assertTrue(!gpuMaterialFrameMatchesRendererGeneration(3L, 4L))
        assertTrue(!gpuMaterialFrameMatchesRendererGeneration(0L, 0L))
    }

    @Test
    fun `gpu frame sequence rejects superseded work inside same context`() {
        assertEquals(8L, nextGpuMaterialFrameSequence(7L))
        assertEquals(1L, nextGpuMaterialFrameSequence(Long.MAX_VALUE))
        assertEquals(8L, nextGpuMaterialTextureRevision(7L))
        assertEquals(1L, nextGpuMaterialTextureRevision(Long.MAX_VALUE))

        assertTrue(
            gpuMaterialFrameIsCurrent(
                frameGeneration = 4L,
                rendererGeneration = 4L,
                frameViewportGeneration = 3L,
                rendererViewportGeneration = 3L,
                frameSequence = 12L,
                latestSequence = 12L
            )
        )
        assertTrue(
            !gpuMaterialFrameIsCurrent(
                frameGeneration = 4L,
                rendererGeneration = 4L,
                frameViewportGeneration = 3L,
                rendererViewportGeneration = 3L,
                frameSequence = 11L,
                latestSequence = 12L
            )
        )
        assertTrue(
            !gpuMaterialFrameIsCurrent(
                frameGeneration = 3L,
                rendererGeneration = 4L,
                frameViewportGeneration = 3L,
                rendererViewportGeneration = 3L,
                frameSequence = 12L,
                latestSequence = 12L
            )
        )
        assertTrue(
            !gpuMaterialFrameIsCurrent(
                frameGeneration = 4L,
                rendererGeneration = 4L,
                frameViewportGeneration = 3L,
                rendererViewportGeneration = 3L,
                frameSequence = 0L,
                latestSequence = 0L
            )
        )

        assertEquals(4L, nextGpuMaterialViewportGeneration(3L))
        assertEquals(1L, nextGpuMaterialViewportGeneration(Long.MAX_VALUE))
        assertTrue(
            !gpuMaterialFrameIsCurrent(
                frameGeneration = 4L,
                rendererGeneration = 4L,
                frameViewportGeneration = 2L,
                rendererViewportGeneration = 3L,
                frameSequence = 12L,
                latestSequence = 12L
            )
        )
    }

    @Test
    fun `gpu renderer retries are bounded and accessibility aware`() {
        assertTrue(
            shouldRetryGpuMaterialPageRenderer(
                failureCount = 1,
                supported = true,
                reducedMotion = false
            )
        )
        assertTrue(
            shouldRetryGpuMaterialPageRenderer(
                failureCount = 2,
                supported = true,
                reducedMotion = false
            )
        )
        assertTrue(
            !shouldRetryGpuMaterialPageRenderer(
                failureCount = 3,
                supported = true,
                reducedMotion = false
            )
        )
        assertTrue(
            !shouldRetryGpuMaterialPageRenderer(
                failureCount = 1,
                supported = false,
                reducedMotion = false
            )
        )
        assertTrue(
            !shouldRetryGpuMaterialPageRenderer(
                failureCount = 1,
                supported = true,
                reducedMotion = true
            )
        )
        assertTrue(
            gpuMaterialPageRendererRetryDelayMillis(2) >
                gpuMaterialPageRendererRetryDelayMillis(1)
        )
    }

    @Test
    fun `gl error drain reports first failure and clears remaining queue`() {
        val queue = ArrayDeque(
            listOf(
                android.opengl.GLES20.GL_INVALID_VALUE,
                android.opengl.GLES20.GL_INVALID_OPERATION,
                android.opengl.GLES20.GL_NO_ERROR
            )
        )
        val first = consumeGpuPageGlErrors {
            if (queue.isEmpty()) {
                android.opengl.GLES20.GL_NO_ERROR
            } else {
                queue.removeFirst()
            }
        }

        assertEquals(android.opengl.GLES20.GL_INVALID_VALUE, first)
        assertTrue(queue.isEmpty())
        assertEquals(
            android.opengl.GLES20.GL_NO_ERROR,
            consumeGpuPageGlErrors { android.opengl.GLES20.GL_NO_ERROR }
        )
    }

    @Test
    fun `low memory devices release EGL context across background pause`() {
        assertTrue(shouldPreserveGpuPageContextOnPause(lowMemoryDevice = false))
        assertTrue(!shouldPreserveGpuPageContextOnPause(lowMemoryDevice = true))
    }

    @Test
    fun `gpu texture prewarm accepts only valid viewport within hardware limit`() {
        assertTrue(shouldPreallocateGpuPageTexture(1080, 2400, 4096))
        assertTrue(!shouldPreallocateGpuPageTexture(0, 2400, 4096))
        assertTrue(!shouldPreallocateGpuPageTexture(1080, 0, 4096))
        assertTrue(!shouldPreallocateGpuPageTexture(1080, 2400, 0))
        assertTrue(!shouldPreallocateGpuPageTexture(5000, 2400, 4096))
        assertTrue(!shouldPreallocateGpuPageTexture(1080, 5000, 4096))
    }

    @Test
    fun `reverse face stays source derived and cannot duplicate destination content`() {
        val source = GpuMaterialPageCurlView.FRAGMENT_SHADER
        assertTrue(source.contains("mirroredFrontInk"))
        assertTrue(!source.contains("uBackTexture"))
        assertTrue(!source.contains("uHasBackTexture"))
        assertTrue(!source.contains("destinationInk"))
    }

    @Test
    fun `fragment face ownership follows interpolated physical normal`() {
        val source = GpuMaterialPageCurlView.FRAGMENT_SHADER
        assertTrue(source.contains("bool physicalFront = n.z >= 0.0"))
        assertTrue(source.contains("if (physicalFront)"))
        assertTrue(!source.contains("gl_FrontFacing"))
    }

    private fun assertShaderUniformContract(shader: String) {
        val declarationRegex =
            Regex("""\buniform\s+(?:(?:lowp|mediump|highp)\s+)?\w+\s+(u[A-Za-z0-9_]+)\s*;""")
        val tokenRegex =
            Regex("""\b(u[A-Z][A-Za-z0-9_]*)\b""")

        val declarations =
            declarationRegex.findAll(shader)
                .map { it.groupValues[1] }
                .toList()
        assertEquals(
            "Duplicate uniform declaration in shader",
            declarations.size,
            declarations.toSet().size
        )

        val declared = declarations.toSet()
        val used =
            tokenRegex.findAll(shader)
                .map { it.groupValues[1] }
                .toSet()

        val undeclared = used - declared
        assertTrue(
            "Undeclared GPU shader uniforms: $undeclared",
            undeclared.isEmpty()
        )
    }
}
