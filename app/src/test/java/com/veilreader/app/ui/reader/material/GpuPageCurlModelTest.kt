package com.veilreader.app.ui.reader.material

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GpuPageCurlModelTest {

    @Test
    fun `mesh quality keeps low memory conservative and normal devices dense`() {
        val low = gpuPageMeshQuality(lowMemoryDevice = true)
        val normal = gpuPageMeshQuality(lowMemoryDevice = false)

        assertEquals(56, low.columns)
        assertEquals(22, low.rows)
        assertEquals(80, normal.columns)
        assertEquals(32, normal.rows)
        assertTrue(normal.columns * normal.rows > low.columns * low.rows)
        assertTrue(low.rows >= 20)
        assertTrue(normal.rows >= 30)
    }

    @Test
    fun `mesh topology stays smooth while remaining GLES2 ushort safe`() {
        listOf(
            gpuPageMeshQuality(lowMemoryDevice = true),
            gpuPageMeshQuality(lowMemoryDevice = false)
        ).forEach { quality ->
            val vertices = gpuPageMeshVertexCount(quality)
            val indices = gpuPageMeshIndexCount(quality)

            assertTrue(vertices in 1..65_535)
            assertTrue(indices > vertices)
            assertEquals(0, indices % 6)
        }
    }

    @Test
    fun `low memory degrades shadow atmosphere before geometry ownership`() {
        assertEquals(2, gpuPageShadowLayerCount(lowMemoryDevice = true))
        assertEquals(3, gpuPageShadowLayerCount(lowMemoryDevice = false))
        assertTrue(gpuPageMeshQuality(lowMemoryDevice = true).rows >= 20)
    }

    @Test
    fun `ping pong bitmap slots alternate without prewarm reset`() {
        var cursor = -1
        cursor = nextMaterialPageBufferSlot(cursor)
        assertEquals(0, cursor)
        cursor = nextMaterialPageBufferSlot(cursor)
        assertEquals(1, cursor)
        cursor = nextMaterialPageBufferSlot(cursor)
        assertEquals(0, cursor)
    }

    @Test
    fun `virtual cylinder starts at free edge and clears viewport at completion`() {
        val start = gpuPageCurlFrame(
            progress = 0f,
            verticalBias = 0f,
            pullOriginY = 0.5f,
            profile = MaterialPageProfiles.MatteBook,
            side = MaterialPageSide.RIGHT
        )
        val end = gpuPageCurlFrame(
            progress = 1f,
            verticalBias = 0f,
            pullOriginY = 0.5f,
            profile = MaterialPageProfiles.MatteBook,
            side = MaterialPageSide.RIGHT
        )

        assertEquals(1f, start.cylinderX, 0.0001f)
        assertTrue(end.cylinderX < 0f)
        assertTrue(end.radius > 0f)
    }

    @Test
    fun `early physical pointer travel keeps curl radius tight before opening`() {
        val early = gpuPageCurlFrame(
            progress = 0.08f,
            verticalBias = 0f,
            pullOriginY = 0.5f,
            pointerTravel = 0.08f,
            profile = MaterialPageProfiles.MatteBook,
            side = MaterialPageSide.RIGHT
        )
        val opened = gpuPageCurlFrame(
            progress = 0.08f,
            verticalBias = 0f,
            pullOriginY = 0.5f,
            pointerTravel = 0.34f,
            profile = MaterialPageProfiles.MatteBook,
            side = MaterialPageSide.RIGHT
        )

        assertTrue(early.radius < opened.radius)
        assertTrue(early.radius > 0f)
    }

    @Test
    fun `stiffer glossy stock uses broader cylinder than papyrus`() {
        val glossy = gpuPageCurlFrame(
            progress = 0.5f,
            verticalBias = 0f,
            pullOriginY = 0.5f,
            profile = MaterialPageProfiles.Glossy,
            side = MaterialPageSide.RIGHT
        )
        val papyrus = gpuPageCurlFrame(
            progress = 0.5f,
            verticalBias = 0f,
            pullOriginY = 0.5f,
            profile = MaterialPageProfiles.Papyrus,
            side = MaterialPageSide.RIGHT
        )

        assertTrue(glossy.radius > papyrus.radius)
    }

    @Test
    fun `corner pull tilts cylinder while center pull stays vertical`() {
        val center = gpuPageCurlFrame(
            progress = 0.45f,
            verticalBias = 0f,
            pullOriginY = 0.5f,
            profile = MaterialPageProfiles.MatteBook,
            side = MaterialPageSide.RIGHT
        )
        val corner = gpuPageCurlFrame(
            progress = 0.45f,
            verticalBias = 0.08f,
            pullOriginY = 0.08f,
            profile = MaterialPageProfiles.MatteBook,
            side = MaterialPageSide.RIGHT
        )

        assertTrue(kotlin.math.abs(center.cylinderTilt) < 0.001f)
        assertTrue(kotlin.math.abs(corner.cylinderTilt) > 0.05f)
    }

    @Test
    fun `diagonal finger vector directly steers cylinder tilt`() {
        val neutral = gpuPageCurlFrame(
            progress = 0.42f,
            verticalBias = 0f,
            pullOriginY = 0.5f,
            diagonalPull = 0f,
            profile = MaterialPageProfiles.MatteBook,
            side = MaterialPageSide.RIGHT
        )
        val diagonal = gpuPageCurlFrame(
            progress = 0.42f,
            verticalBias = 0f,
            pullOriginY = 0.5f,
            diagonalPull = 0.75f,
            profile = MaterialPageProfiles.MatteBook,
            side = MaterialPageSide.RIGHT
        )

        assertTrue(kotlin.math.abs(neutral.cylinderTilt) < 0.001f)
        assertTrue(diagonal.cylinderTilt > 0.05f)
    }

    @Test
    fun `left and right turns share geometry with opposite side sign`() {
        val right = gpuPageCurlFrame(
            progress = 0.5f,
            verticalBias = 0.03f,
            pullOriginY = 0.25f,
            profile = MaterialPageProfiles.Parchment,
            side = MaterialPageSide.RIGHT
        )
        val left = gpuPageCurlFrame(
            progress = 0.5f,
            verticalBias = 0.03f,
            pullOriginY = 0.25f,
            profile = MaterialPageProfiles.Parchment,
            side = MaterialPageSide.LEFT
        )

        assertEquals(right.cylinderX, left.cylinderX, 0.0001f)
        assertEquals(right.radius, left.radius, 0.0001f)
        assertEquals(1f, right.sideSign, 0f)
        assertEquals(-1f, left.sideSign, 0f)
    }


    @Test
    fun `terminal travel tightens radius while mid turn stays broad`() {
        val mid = gpuPageCurlFrame(
            progress = 0.5f,
            verticalBias = 0f,
            pullOriginY = 0.5f,
            profile = MaterialPageProfiles.MatteBook,
            side = MaterialPageSide.RIGHT
        )
        val terminal = gpuPageCurlFrame(
            progress = 0.98f,
            verticalBias = 0f,
            pullOriginY = 0.5f,
            profile = MaterialPageProfiles.MatteBook,
            side = MaterialPageSide.RIGHT
        )

        assertTrue(terminal.radius < mid.radius)
    }

    @Test
    fun `vertical drag moves cylinder grip without escaping page bounds`() {
        val up = gpuPageCurlFrame(
            progress = 0.45f,
            verticalBias = -0.18f,
            pullOriginY = 0.5f,
            profile = MaterialPageProfiles.Parchment,
            side = MaterialPageSide.RIGHT
        )
        val down = gpuPageCurlFrame(
            progress = 0.45f,
            verticalBias = 0.18f,
            pullOriginY = 0.5f,
            profile = MaterialPageProfiles.Parchment,
            side = MaterialPageSide.RIGHT
        )

        assertTrue(up.cylinderY < 0.5f)
        assertTrue(down.cylinderY > 0.5f)
        assertTrue(up.cylinderY >= 0.03f)
        assertTrue(down.cylinderY <= 0.97f)
    }

    @Test
    fun `tall page binding invariant clamps diagonal cylinder before spine release`() {
        val square = gpuPageCurlFrame(
            progress = 0.78f,
            verticalBias = 0.10f,
            pullOriginY = 0.90f,
            diagonalPull = 1f,
            pointerTravel = 0.78f,
            pageAspect = 1f,
            profile = MaterialPageProfiles.MatteBook,
            side = MaterialPageSide.RIGHT
        )
        val tall = gpuPageCurlFrame(
            progress = 0.78f,
            verticalBias = 0.10f,
            pullOriginY = 0.90f,
            diagonalPull = 1f,
            pointerTravel = 0.78f,
            pageAspect = 3f,
            profile = MaterialPageProfiles.MatteBook,
            side = MaterialPageSide.RIGHT
        )
        assertTrue(kotlin.math.abs(tall.cylinderTilt) <= kotlin.math.abs(square.cylinderTilt))
        if (tall.cylinderX > 0f) {
            val cy = tall.cylinderY * 3f
            val reach = if (tall.cylinderTilt >= 0f) cy else 3f - cy
            assertTrue(
                kotlin.math.abs(tall.cylinderTilt) * reach <=
                    tall.cylinderX + 0.0002f
            )
        }
    }

    @Test
    fun `extreme material geometry matrix stays finite bound and terminal safe`() {
        val aspects = listOf(0.5f, 1f, 2f, 4f)
        val origins = listOf(0.04f, 0.5f, 0.96f)
        val diagonals = listOf(-1f, 0f, 1f)
        val verticalBiases = listOf(-0.18f, 0f, 0.18f)
        val progresses = listOf(0f, 0.03f, 0.12f, 0.35f, 0.60f, 0.72f, 0.90f, 1f)

        MaterialPageProfiles.all.forEach { profile ->
            MaterialPageSide.entries.forEach { side ->
                aspects.forEach { aspect ->
                    origins.forEach { origin ->
                        diagonals.forEach { diagonal ->
                            verticalBiases.forEach { vertical ->
                                progresses.forEach { progress ->
                                    val frame = gpuPageCurlFrame(
                                        progress = progress,
                                        verticalBias = vertical,
                                        pullOriginY = origin,
                                        diagonalPull = diagonal,
                                        pointerTravel = progress.coerceAtLeast(0.02f),
                                        pageAspect = aspect,
                                        profile = profile,
                                        side = side
                                    )

                                    assertTrue(
                                        "non-finite frame preset=${profile.preset} side=$side " +
                                            "aspect=$aspect origin=$origin diagonal=$diagonal " +
                                            "vertical=$vertical progress=$progress",
                                        isFiniteGpuPageCurlFrame(frame)
                                    )
                                    assertTrue(frame.cylinderY in 0f..1f)
                                    assertTrue(frame.radius in 0.020f..0.132f)
                                    assertEquals(
                                        if (side == MaterialPageSide.RIGHT) 1f else -1f,
                                        frame.sideSign,
                                        0f
                                    )

                                    if (frame.cylinderX > 0f) {
                                        val yInAspect = frame.cylinderY * aspect
                                        val bindingReach =
                                            if (frame.cylinderTilt >= 0f) {
                                                yInAspect
                                            } else {
                                                aspect - yInAspect
                                            }
                                        assertTrue(
                                            "binding release preset=${profile.preset} side=$side " +
                                                "aspect=$aspect progress=$progress",
                                            kotlin.math.abs(frame.cylinderTilt) *
                                                bindingReach.coerceAtLeast(0f) <=
                                                frame.cylinderX + 0.00025f
                                        )
                                    }

                                    if (progress == 1f) {
                                        assertTrue(
                                            "terminal cylinder must clear free edge",
                                            frame.cylinderX < 0f
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `canonical storage estimate covers source ping pong and one GPU texture`() {
        val onePage = 1080L * 2400L * 4L
        assertEquals(
            onePage * 3L,
            estimatedMaterialPageStorageBytes(
                pageWidthPx = 1080,
                pageHeightPx = 2400,
                cpuBitmapCount = 2,
                gpuTextureCount = 1
            )
        )
    }

    @Test
    fun `non finite inspection input collapses to finite safe frame`() {
        val frame = gpuPageCurlFrame(
            progress = Float.NaN,
            verticalBias = Float.POSITIVE_INFINITY,
            pullOriginY = Float.NaN,
            profile = MaterialPageProfiles.Manuscript,
            side = MaterialPageSide.RIGHT
        )

        assertTrue(isFiniteGpuPageCurlFrame(frame))
        assertEquals(0.5f, frame.cylinderY, 0.0001f)
    }
}
