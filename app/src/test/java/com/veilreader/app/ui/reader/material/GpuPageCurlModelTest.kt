package com.veilreader.app.ui.reader.material

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GpuPageCurlModelTest {

    @Test
    fun `mesh quality keeps low memory conservative and normal devices dense`() {
        val low = gpuPageMeshQuality(lowMemoryDevice = true)
        val normal = gpuPageMeshQuality(lowMemoryDevice = false)

        assertEquals(48, low.columns)
        assertEquals(8, low.rows)
        assertEquals(72, normal.columns)
        assertEquals(14, normal.rows)
        assertTrue(normal.columns * normal.rows > low.columns * low.rows)
    }

    @Test
    fun `dual back texture is gated by low ram and memory class`() {
        assertTrue(
            shouldCaptureMaterialBackSnapshot(
                lowMemoryDevice = false,
                memoryClassMb = 256
            )
        )
        assertTrue(
            !shouldCaptureMaterialBackSnapshot(
                lowMemoryDevice = true,
                memoryClassMb = 512
            )
        )
        assertTrue(
            !shouldCaptureMaterialBackSnapshot(
                lowMemoryDevice = false,
                memoryClassMb = 192
            )
        )
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
