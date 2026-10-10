package com.veilreader.app.ui.reader.material

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SoftwareMaterialPageCurlModelTest {
    private val width = 1000f
    private val height = 1600f

    @Test
    fun `flat frame preserves the page grid`() {
        val frame = GpuPageCurlFrame(
            cylinderX = 1f,
            cylinderY = 0.5f,
            cylinderTilt = 0f,
            radius = 0.08f,
            sideSign = 1f,
            shadowStrength = 0f,
            edgeStrength = 0.5f
        )
        val mesh = softwareMaterialPageMesh(
            widthPx = width,
            heightPx = height,
            frame = frame,
            side = MaterialPageSide.RIGHT,
            columns = 2,
            rows = 2
        )

        assertEquals(18, mesh.size)
        val expected = floatArrayOf(
            0f, 0f, 500f, 0f, 1000f, 0f,
            0f, 800f, 500f, 800f, 1000f, 800f,
            0f, 1600f, 500f, 1600f, 1000f, 1600f
        )
        expected.indices.forEach { index ->
            assertEquals(expected[index], mesh[index], 0.01f)
        }
    }

    @Test
    fun `right curl carries the free edge inward`() {
        val frame = curledFrame(sideSign = 1f)
        val mesh = softwareMaterialPageMesh(
            widthPx = width,
            heightPx = height,
            frame = frame,
            side = MaterialPageSide.RIGHT,
            columns = 4,
            rows = 2
        )

        val topRightX = mesh[4 * 2]
        assertTrue(topRightX < width * 0.90f)
        assertTrue(mesh.all { it.isFinite() })
    }

    @Test
    fun `left curl mirrors right curl around page center`() {
        val frame = curledFrame(sideSign = 1f)
        val right = softwareMaterialPageMesh(
            widthPx = width,
            heightPx = height,
            frame = frame,
            side = MaterialPageSide.RIGHT,
            columns = 4,
            rows = 2
        )
        val left = softwareMaterialPageMesh(
            widthPx = width,
            heightPx = height,
            frame = frame.copy(sideSign = -1f),
            side = MaterialPageSide.LEFT,
            columns = 4,
            rows = 2
        )

        val rightFreeEdgeX = right[4 * 2]
        val leftFreeEdgeX = left[0]
        assertTrue(leftFreeEdgeX > width * 0.10f)
        assertTrue(abs((leftFreeEdgeX + rightFreeEdgeX) - width) < 0.5f)
        assertTrue(left.all { it.isFinite() })
    }

    private fun curledFrame(sideSign: Float) = GpuPageCurlFrame(
        cylinderX = 0.72f,
        cylinderY = 0.46f,
        cylinderTilt = 0.08f,
        radius = 0.075f,
        sideSign = sideSign,
        shadowStrength = 0.18f,
        edgeStrength = 0.66f
    )
}
