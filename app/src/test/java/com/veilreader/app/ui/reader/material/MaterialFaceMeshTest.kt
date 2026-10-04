package com.veilreader.app.ui.reader.material

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class MaterialFaceMeshTest {
    @Test fun `horizon partition preserves every source texel in both directions`() {
        val geometry = MaterialPageGeometry()
        val front = MaterialFaceMesh(geometry.columns, geometry.rows)
        val back = MaterialFaceMesh(geometry.columns, geometry.rows)
        val colors = IntArray(geometry.normals.size) { -1 }
        for (material in listOf(PageMaterials.glossy, PageMaterials.matte, PageMaterials.parchment, PageMaterials.papyrus)) {
            for (mirror in listOf(false, true)) for (origin in listOf(0f, .5f, 1f)) for (step in 0..40) {
                geometry.update(400f, 900f, step / 40f, origin, .22f, material, mirror)
                front.update(geometry, 400f, 900f, colors, true)
                back.update(geometry, 400f, 900f, colors, false)
                assertEquals("Source area lost at $material / $mirror / $origin / $step",
                    360_000.0, sourceArea(front) + sourceArea(back), .5)
                for (face in listOf(front, back)) {
                    assertEquals(0, face.count % 3)
                    for (i in 0 until face.count * 2) assertTrue(face.vertices[i].isFinite())
                }
            }
        }
    }

    @Test fun `idle is front only and buffers survive repeated topology changes`() {
        val geometry = MaterialPageGeometry()
        val face = MaterialFaceMesh(geometry.columns, geometry.rows)
        val vertices = face.vertices
        val texture = face.texture
        val colors = face.colors
        val shades = IntArray(geometry.normals.size) { -1 }
        geometry.update(400f, 900f, 0f, .5f, 0f, PageMaterials.matte, false)
        face.update(geometry, 400f, 900f, shades, false)
        assertEquals(0, face.count)
        face.update(geometry, 400f, 900f, shades, true)
        assertEquals(geometry.columns * geometry.rows * 6, face.count)
        for (step in 0..20) {
            geometry.update(400f, 900f, step / 20f, .1f, -.22f, PageMaterials.glossy, true)
            face.update(geometry, 200f, 450f, shades, true)
            assertSame(vertices, face.vertices)
            assertSame(texture, face.texture)
            assertSame(colors, face.colors)
        }
    }

    private fun sourceArea(face: MaterialFaceMesh): Double {
        var area = 0.0
        for (i in 0 until face.count step 3) {
            val j = i * 2
            val u = face.texture
            area += abs((u[j + 2] - u[j]).toDouble() * (u[j + 5] - u[j + 1]) -
                (u[j + 4] - u[j]).toDouble() * (u[j + 3] - u[j + 1])) * .5
        }
        return area
    }
}
