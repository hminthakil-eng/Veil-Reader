package com.veilreader.app.ui.reader.material

import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.PageMaterial
import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderNavigationMode
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class MaterialEngineTest {
    @Test fun `all materials have independent mechanics optics and acoustics`() {
        val profiles = PageMaterial.entries.map { PageMaterials.forId(it) }
        assertEquals(4, profiles.map { it.mass }.distinct().size)
        assertEquals(4, profiles.map { it.radiusFraction }.distinct().size)
        assertEquals(4, profiles.map { it.soundFilter }.distinct().size)
        assertTrue(PageMaterials.glossy.specular > PageMaterials.matte.specular)
        assertTrue(PageMaterials.parchment.thicknessDp > PageMaterials.matte.thicknessDp)
        assertTrue(PageMaterials.papyrus.directionalFibre > PageMaterials.parchment.directionalFibre)
    }

    @Test fun `material switch never collapses the four reader modes`() {
        assertFalse(ReaderAppearance().materialEngineEnabled)
        ReaderNavigationMode.entries.forEach { mode ->
            val appearance = ReaderAppearance(materialEngineEnabled = true, pageMaterial = PageMaterial.PAPYRUS)
                .withNavigationMode(mode)
            assertEquals(mode, appearance.navigationMode)
            assertEquals(PageMaterial.PAPYRUS, appearance.pageMaterial)
        }
    }

    @Test fun `deformation is gated away from pdf fixed layout slide paged and scroll`() {
        assertEquals(MaterialMotionPath.DEFORMED_SHEET, materialMotionPath(true, BookFormat.EPUB, false, false, PageTurnStyle.PAPER, false))
        assertEquals(MaterialMotionPath.LIVE_EDGE, materialMotionPath(true, BookFormat.EPUB, false, false, PageTurnStyle.PAPER, true))
        assertEquals(MaterialMotionPath.LEGACY, materialMotionPath(false, BookFormat.EPUB, false, false, PageTurnStyle.PAPER, false))
        assertEquals(MaterialMotionPath.LEGACY, materialMotionPath(true, BookFormat.PDF, false, false, PageTurnStyle.PAPER, false))
        assertEquals(MaterialMotionPath.LEGACY, materialMotionPath(true, BookFormat.EPUB, true, false, PageTurnStyle.PAPER, false))
        assertEquals(MaterialMotionPath.LEGACY, materialMotionPath(true, BookFormat.EPUB, false, true, PageTurnStyle.PAPER, false))
        for (style in listOf(PageTurnStyle.SLIDE, PageTurnStyle.NONE)) assertEquals(MaterialMotionPath.LEGACY,
            materialMotionPath(true, BookFormat.EPUB, false, false, style, false))
    }

    @Test fun `pull is monotonic bounded and never outruns finger`() {
        PageMaterial.entries.map { PageMaterials.forId(it) }.forEach { m ->
            var last = 0f
            for (i in 0..1000) {
                val p = i / 1000f
                val actual = materialDragProgress(p, m)
                assertTrue(actual >= last && actual <= p)
                last = actual
            }
            assertEquals(0f, materialDragProgress(-1f, m), 0f)
            assertEquals(0f, materialDragProgress(Float.NaN, m), 0f)
        }
        assertTrue(materialDragProgress(.3f, PageMaterials.parchment) < materialDragProgress(.3f, PageMaterials.glossy))
    }

    @Test fun `deliberate progress completes short manipulation cancels and outward release cancels`() {
        PageMaterial.entries.map { PageMaterials.forId(it) }.forEach { m ->
            assertFalse(materialShouldComplete(100f, 1000f, 1f, .09f, 0f, m))
            assertTrue(materialShouldComplete(500f, 1000f, 1f, .48f, 0f, m))
            assertFalse(materialShouldComplete(500f, 1000f, 1f, .48f, -1200f, m))
            assertTrue(materialShouldComplete(50f, 1000f, 1f, .04f, 2000f, m))
            assertFalse(materialShouldComplete(20f, 1000f, 1f, .02f, 2000f, m))
            assertFalse(materialShouldComplete(-50f, 1000f, 1f, .5f, 2000f, m))
        }
    }

    @Test fun `material threshold has an exact completion and cancellation boundary`() {
        for (m in PageMaterial.entries.map { PageMaterials.forId(it) }) {
            val threshold = materialCompletionThreshold(m)
            assertFalse(materialShouldComplete(100f, 1000f, 1f, threshold - .0001f, 0f, m))
            assertTrue(materialShouldComplete(100f, 1000f, 1f, threshold, 0f, m))
        }
    }

    @Test fun `invalid and zero metrics cannot commit`() {
        listOf(Float.NaN, Float.POSITIVE_INFINITY, 0f, -1f).forEach { width ->
            assertFalse(materialShouldComplete(500f, width, 1f, .9f, 3000f, PageMaterials.matte))
        }
        assertFalse(materialShouldComplete(500f, 1000f, 1f, Float.NaN, 3000f, PageMaterials.matte))
    }

    @Test fun `complete and cancel releases settle once without oscillation for every material`() {
        PageMaterial.entries.map { PageMaterials.forId(it) }.forEach { material ->
            for (target in listOf(0f, 1f)) for (velocity in listOf(-20f, 0f, 4f, 20f)) {
                val release = MaterialRelease(.4f, target, velocity, material)
                var last = .4f
                for (i in 0..200) {
                    val p = release.position(i * .005f)
                    assertTrue(p.isFinite() && p in 0f..1f)
                    assertTrue(if (target == 1f) p >= last - .000001f else p <= last + .000001f)
                    last = p
                }
                assertEquals(target, release.position(10f), 0f)
                assertEquals(target, release.position(release.durationSeconds), 0f)
            }
        }
    }

    @Test fun `idle mesh is an exact readable identity in both directions`() {
        val mesh = MaterialPageGeometry()
        for (mirror in listOf(false, true)) {
            mesh.update(400f, 800f, 0f, .8f, .2f, PageMaterials.matte, mirror)
            for (row in 0..mesh.rows) for (col in 0..mesh.columns) {
                val i = row * (mesh.columns + 1) + col
                assertEquals(400f * col / mesh.columns, mesh.vertices[i * 2], .0001f)
                assertEquals(800f * row / mesh.rows, mesh.vertices[i * 2 + 1], .0001f)
                assertEquals(1f, mesh.normals[i], 0f)
                assertEquals(0f, mesh.heights[i], 0f)
            }
        }
    }

    @Test fun `rtl mirrors geometry without reversing publication texture order`() {
        val left = MaterialPageGeometry()
        val right = MaterialPageGeometry()
        for (m in PageMaterial.entries.map { PageMaterials.forId(it) }) for (step in 0..100) {
            val p = step / 100f
            left.update(412f, 900f, p, .7f, .1f, m, true)
            right.update(412f, 900f, p, .7f, .1f, m, false)
            for (row in 0..left.rows) for (col in 0..left.columns) {
                val a = row * (left.columns + 1) + col
                val b = row * (right.columns + 1) + right.columns - col
                assertTrue(left.vertices[a * 2].isFinite())
                assertEquals(412f - right.vertices[b * 2], left.vertices[a * 2], .001f)
                assertEquals(right.vertices[b * 2 + 1], left.vertices[a * 2 + 1], .001f)
                assertEquals(right.normals[b], left.normals[a], .0001f)
            }
        }
    }

    @Test fun `binding stays fixed while on page and completed sheet clears viewport`() {
        val mesh = MaterialPageGeometry()
        mesh.update(400f, 800f, .3f, .8f, .1f, PageMaterials.matte, false)
        for (row in 0..mesh.rows) {
            val i = row * (mesh.columns + 1)
            assertEquals(0f, mesh.vertices[i * 2], .001f)
            assertEquals(0f, mesh.heights[i], .001f)
        }
        mesh.update(400f, 800f, 1f, .8f, .1f, PageMaterials.matte, false)
        assertTrue(mesh.vertices.filterIndexed { i, _ -> i % 2 == 0 }.all { it < 0f })
    }

    @Test fun `almost completed drag still has a visible sheet for every material`() {
        val mesh = MaterialPageGeometry()
        for (m in PageMaterial.entries.map { PageMaterials.forId(it) }) {
            mesh.update(400f, 800f, .99f, .5f, 0f, m, false)
            assertTrue(mesh.vertices.filterIndexed { i, _ -> i % 2 == 0 }.any { it > 0f })
            mesh.update(400f, 800f, 1f, .5f, 0f, m, false)
            assertTrue(mesh.vertices.filterIndexed { i, _ -> i % 2 == 0 }.all { it < 0f })
        }
    }

    @Test fun `cylinder preserves surface length and limits normal and height`() {
        val mesh = MaterialPageGeometry(200, 2)
        mesh.update(1000f, 1200f, .3f, .5f, 0f, PageMaterials.parchment, false)
        val stride = mesh.columns + 1
        for (col in 0 until mesh.columns) {
            val a = stride + col
            val b = a + 1
            val dx = mesh.vertices[b * 2] - mesh.vertices[a * 2]
            val dz = mesh.heights[b] - mesh.heights[a]
            val length = kotlin.math.sqrt(dx * dx + dz * dz)
            assertEquals(5f, length, .08f)
            assertTrue(mesh.normals[a] in -1f..1f)
            assertTrue(mesh.heights[a] in 0f..mesh.radius * 2.001f)
        }
    }

    @Test fun `material cues are quiet bounded distinct and cancel softer than completion`() {
        val outputs = PageMaterial.entries.map { m ->
            val cue = MaterialSensoryCue(m, MaterialSensoryMoment.COMPLETE, 1400f)
            val pcm = materialCuePcm(cue, .55f)
            assertTrue(pcm.any { it.toInt() != 0 })
            assertTrue(pcm.maxOf { abs(it.toInt()) } < 7000)
            assertEquals(0, pcm.first().toInt())
            assertEquals(0, pcm.last().toInt())
            assertTrue(materialAcousticProfile(cue.copy(moment = MaterialSensoryMoment.CANCEL)).gain < materialAcousticProfile(cue).gain)
            for (moment in listOf(MaterialSensoryMoment.COMPLETE, MaterialSensoryMoment.CANCEL)) {
                val samples = materialCuePcm(cue.copy(moment = moment), .18f)
                val header = java.nio.ByteBuffer.allocate(44 + samples.size * 2)
                    .order(java.nio.ByteOrder.LITTLE_ENDIAN)
                header.put("RIFF".toByteArray()).putInt(36 + samples.size * 2).put("WAVEfmt ".toByteArray())
                    .putInt(16).putShort(1).putShort(1).putInt(22050).putInt(44100)
                    .putShort(2).putShort(16).put("data".toByteArray()).putInt(samples.size * 2)
                samples.forEach { header.putShort(it) }
                val file = java.io.File("build/outputs/material-page-review/audio/${m.name.lowercase()}-${moment.name.lowercase()}.wav")
                file.parentFile!!.mkdirs()
                file.writeBytes(header.array())
            }
            pcm.contentHashCode()
        }
        assertEquals(4, outputs.distinct().size)
        for (moment in listOf(MaterialSensoryMoment.LIFT, MaterialSensoryMoment.THRESHOLD)) {
            assertTrue(materialCuePcm(MaterialSensoryCue(PageMaterial.MATTE, moment), .5f).all { it.toInt() == 0 })
        }
    }

    @Test fun `review matrix covers all materials rtl persian and accessibility`() {
        assertEquals(21, MaterialReviewStates.paper.size)
        assertEquals(6, MaterialReviewStates.slide.size)
        assertEquals(PageMaterial.entries.toSet(), MaterialReviewStates.paper.map { it.material }.toSet())
        assertTrue(MaterialReviewStates.paper.any { it.rtl && it.persian })
        assertTrue(MaterialReviewStates.paper.any { it.reducedMotion })
    }
}
