package com.veilreader.app.ui.reader.material

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialPageModelTest {

    @Test
    fun `canonical material catalog is complete and structurally distinct`() {
        val profiles = MaterialPageProfiles.all

        assertEquals(5, profiles.size)
        assertEquals(
            setOf(
                MaterialPagePreset.GLOSSY,
                MaterialPagePreset.MATTE_BOOK,
                MaterialPagePreset.PARCHMENT,
                MaterialPagePreset.PAPYRUS,
                MaterialPagePreset.MANUSCRIPT
            ),
            profiles.map { it.preset }.toSet()
        )

        assertTrue(MaterialPageProfiles.Glossy.optics.specularResponse >
            MaterialPageProfiles.MatteBook.optics.specularResponse)
        assertTrue(MaterialPageProfiles.Parchment.physics.apparentMass >
            MaterialPageProfiles.MatteBook.physics.apparentMass)
        assertTrue(MaterialPageProfiles.Papyrus.optics.directionalFiber >
            MaterialPageProfiles.Parchment.optics.directionalFiber)
        assertTrue(MaterialPageProfiles.Manuscript.physics.apparentMass >
            MaterialPageProfiles.Parchment.physics.apparentMass)
        assertNotEquals(
            MaterialPageProfiles.Glossy.sensory,
            MaterialPageProfiles.Papyrus.sensory
        )
    }

    @Test
    fun `drag model is monotonic and never outruns the finger`() {
        val profile = MaterialPageProfiles.MatteBook
        var previous = -1f

        for (distance in 0..1_000 step 50) {
            val sample = materialPageDragSample(
                inwardDistancePx = distance.toFloat(),
                verticalDistancePx = 90f,
                widthPx = 1_000f,
                heightPx = 1_600f,
                profile = profile
            )

            assertTrue(sample.progress >= previous)
            assertTrue(sample.progress <= sample.rawProgress + 0.0001f)
            assertTrue(sample.progress in 0f..1f)
            assertTrue(sample.lift in 0f..1f)
            previous = sample.progress
        }
    }

    @Test
    fun `heavier parchment resists early pull more than glossy sheet`() {
        val glossy = materialPageDragSample(
            inwardDistancePx = 240f,
            verticalDistancePx = 0f,
            widthPx = 1_000f,
            heightPx = 1_600f,
            profile = MaterialPageProfiles.Glossy
        )
        val parchment = materialPageDragSample(
            inwardDistancePx = 240f,
            verticalDistancePx = 0f,
            widthPx = 1_000f,
            heightPx = 1_600f,
            profile = MaterialPageProfiles.Parchment
        )

        assertTrue(parchment.progress < glossy.progress)
    }

    @Test
    fun `release policy distinguishes glossy flick from heavy parchment`() {
        assertEquals(
            MaterialPageReleaseDecision.COMPLETE,
            materialPageReleaseDecision(
                progress = 0.16f,
                inwardVelocityDpPerSec = 900f,
                profile = MaterialPageProfiles.Glossy
            )
        )
        assertEquals(
            MaterialPageReleaseDecision.CANCEL,
            materialPageReleaseDecision(
                progress = 0.16f,
                inwardVelocityDpPerSec = 900f,
                profile = MaterialPageProfiles.Parchment
            )
        )
        assertEquals(
            MaterialPageReleaseDecision.COMPLETE,
            materialPageReleaseDecision(
                progress = 0.40f,
                inwardVelocityDpPerSec = 0f,
                profile = MaterialPageProfiles.Parchment
            )
        )
    }

    @Test
    fun `reverse release cancels an ambiguous sheet while a deep turn stays committed`() {
        assertEquals(
            MaterialPageReleaseDecision.CANCEL,
            materialPageReleaseDecision(
                progress = 0.36f,
                inwardVelocityDpPerSec = -900f,
                profile = MaterialPageProfiles.MatteBook
            )
        )
        assertEquals(
            MaterialPageReleaseDecision.COMPLETE,
            materialPageReleaseDecision(
                progress = 0.62f,
                inwardVelocityDpPerSec = -900f,
                profile = MaterialPageProfiles.MatteBook
            )
        )
    }

    @Test
    fun `adaptive mesh spends detail where curvature and gloss need it`() {
        val shallow = materialPageAdaptiveSegmentCount(
            progress = 0.02f,
            profile = MaterialPageProfiles.MatteBook
        )
        val middle = materialPageAdaptiveSegmentCount(
            progress = 0.50f,
            profile = MaterialPageProfiles.MatteBook
        )
        val glossyMiddle = materialPageAdaptiveSegmentCount(
            progress = 0.50f,
            profile = MaterialPageProfiles.Glossy
        )

        assertTrue(shallow in 18..34)
        assertTrue(middle in 18..34)
        assertTrue(glossyMiddle in 18..34)
        assertTrue(middle > shallow)
        assertTrue(glossyMiddle >= middle)
    }

    @Test
    fun `vertical micro jitter is suppressed without deleting intentional diagonal pull`() {
        val tiny = materialPageDragSample(
            inwardDistancePx = 260f,
            verticalDistancePx = 2f,
            widthPx = 1_000f,
            heightPx = 1_600f,
            profile = MaterialPageProfiles.MatteBook
        )
        val deliberate = materialPageDragSample(
            inwardDistancePx = 260f,
            verticalDistancePx = 150f,
            widthPx = 1_000f,
            heightPx = 1_600f,
            profile = MaterialPageProfiles.MatteBook
        )

        assertEquals(0f, tiny.verticalBias, 0.0001f)
        assertTrue(deliberate.verticalBias > 0.02f)
    }

    @Test
    fun `glossy sheet develops stronger grazing highlight than matte stock`() {
        val glossy = materialPageGeometry(
            width = 1_000f,
            height = 1_600f,
            progress = 0.55f,
            verticalBias = 0f,
            profile = MaterialPageProfiles.Glossy
        )
        val matte = materialPageGeometry(
            width = 1_000f,
            height = 1_600f,
            progress = 0.55f,
            verticalBias = 0f,
            profile = MaterialPageProfiles.MatteBook
        )

        assertTrue(
            glossy.strips.maxOf { it.lightResponse } >
                matte.strips.maxOf { it.lightResponse }
        )
    }

    @Test
    fun `geometry stays finite through the full turn and reveals progressively`() {
        val progressValues = listOf(0.02f, 0.12f, 0.35f, 0.62f, 0.88f, 0.99f)
        var previousReveal = -1f

        progressValues.forEach { progress ->
            val frame = materialPageGeometry(
                width = 1_080f,
                height = 1_920f,
                progress = progress,
                verticalBias = 0.08f,
                profile = MaterialPageProfiles.MatteBook
            )

            assertTrue(isFiniteMaterialPageFrame(frame))
            assertEquals(
                materialPageAdaptiveSegmentCount(progress, MaterialPageProfiles.MatteBook),
                frame.strips.size
            )
            assertTrue(frame.revealFraction >= previousReveal)
            assertTrue(frame.foldX in 0f..1_080f)
            previousReveal = frame.revealFraction
        }
    }

    @Test
    fun `deep curl exposes a back face without making geometry unstable`() {
        val frame = materialPageGeometry(
            width = 1_080f,
            height = 1_920f,
            progress = 0.72f,
            verticalBias = -0.07f,
            profile = MaterialPageProfiles.Papyrus
        )

        assertTrue(frame.strips.any { it.backFacing })
        assertTrue(frame.strips.any { !it.backFacing })
        assertTrue(isFiniteMaterialPageFrame(frame))
    }

    @Test
    fun `left edge mirror preserves material state while reversing geometry`() {
        val width = 1_000f
        val right = materialPageGeometry(
            width = width,
            height = 1_600f,
            progress = 0.54f,
            verticalBias = 0.04f,
            profile = MaterialPageProfiles.Parchment
        )
        val left = mirrorMaterialPageFrame(right, width)

        assertEquals(right.revealFraction, left.revealFraction, 0.0001f)
        assertEquals(right.lift, left.lift, 0.0001f)
        assertEquals(width - right.foldX, left.foldX, 0.0001f)
        assertEquals(right.strips.size, left.strips.size)
        assertTrue(isFiniteMaterialPageFrame(left))
    }

    @Test
    fun `bend stiffness changes curl geometry at equal progress`() {
        val glossy = materialPageGeometry(
            width = 1_000f,
            height = 1_600f,
            progress = 0.55f,
            verticalBias = 0f,
            profile = MaterialPageProfiles.Glossy
        )
        val parchment = materialPageGeometry(
            width = 1_000f,
            height = 1_600f,
            progress = 0.55f,
            verticalBias = 0f,
            profile = MaterialPageProfiles.Parchment
        )

        assertTrue(parchment.foldAngleRadians > glossy.foldAngleRadians)
    }

    @Test
    fun `corner pull biases the near edge more than a centered pull`() {
        val topPull = materialPageGeometry(
            width = 1_000f,
            height = 1_600f,
            progress = 0.50f,
            verticalBias = 0.10f,
            profile = MaterialPageProfiles.MatteBook,
            pullOriginY = 0.08f
        )
        val bottomPull = materialPageGeometry(
            width = 1_000f,
            height = 1_600f,
            progress = 0.50f,
            verticalBias = 0.10f,
            profile = MaterialPageProfiles.MatteBook,
            pullOriginY = 0.92f
        )

        assertTrue(topPull.creaseTop.y == bottomPull.creaseTop.y)
        assertTrue(topPull.creaseTop.x < topPull.creaseBottom.x)
        assertTrue(bottomPull.creaseTop.x > bottomPull.creaseBottom.x)

        val topStrip = topPull.strips.last()
        val bottomStrip = bottomPull.strips.last()
        assertTrue(topStrip.topLeft.y > bottomStrip.topLeft.y)
        assertTrue(bottomStrip.bottomLeft.y > topStrip.bottomLeft.y)

        val mirrored = mirrorMaterialPageFrame(topPull, 1_000f)
        assertEquals(
            1_000f - topPull.creaseTop.x,
            mirrored.creaseTop.x,
            0.0001f
        )
        assertEquals(
            1_000f - topPull.creaseBottom.x,
            mirrored.creaseBottom.x,
            0.0001f
        )
    }

    @Test
    fun `dark tone preserves opacity while moving paper into sanctuary luminance`() {
        val source = MaterialPageProfiles.Parchment.optics.backArgb
        val dark = materialPageToneAdjustedArgb(source, MaterialPageTone.DARK)
        val light = materialPageToneAdjustedArgb(source, MaterialPageTone.LIGHT)

        assertEquals((source ushr 24) and 0xFF, (dark ushr 24) and 0xFF)
        assertTrue((dark and 0x00FFFFFF) < (light and 0x00FFFFFF))
    }

    @Test
    fun `material settling is never underdamped`() {
        MaterialPageProfiles.all.forEach { profile ->
            assertTrue(
                materialPageSpringDamping(
                    profile = profile,
                    cancelling = false
                ) >= 1f
            )
            assertTrue(
                materialPageSpringDamping(
                    profile = profile,
                    cancelling = true
                ) > 1f
            )
        }
    }

    @Test
    fun `vertical binding remains constrained under extreme diagonal drags`() {
        val sample = materialPageDragSample(
            inwardDistancePx = 520f,
            verticalDistancePx = 4_000f,
            widthPx = 1_000f,
            heightPx = 1_600f,
            profile = MaterialPageProfiles.Manuscript
        )

        assertTrue(sample.verticalBias <= 0.16f)
        assertTrue(sample.verticalBias >= -0.16f)
    }

    @Test
    fun `reduced motion alpha is stable and non flashing`() {
        val start = materialPageReducedMotionAlpha(
            progress = 0f,
            completing = true
        )
        val middle = materialPageReducedMotionAlpha(
            progress = 0.5f,
            completing = true
        )
        val end = materialPageReducedMotionAlpha(
            progress = 1f,
            completing = true
        )

        assertEquals(1f, start, 0.0001f)
        assertTrue(middle < start)
        assertTrue(end <= middle)
        assertTrue(end >= 0.08f)
    }

    @Test
    fun `sensory identity differs by material and completion speed`() {
        val glossy = materialPageSensoryCue(
            profile = MaterialPageProfiles.Glossy,
            action = MaterialPageSensoryAction.COMPLETE,
            velocityDpPerSec = 1_600f
        )
        val papyrus = materialPageSensoryCue(
            profile = MaterialPageProfiles.Papyrus,
            action = MaterialPageSensoryAction.COMPLETE,
            velocityDpPerSec = 1_600f
        )

        assertTrue(glossy.acoustic.brightness > papyrus.acoustic.brightness)
        assertTrue(papyrus.acoustic.fiber > glossy.acoustic.fiber)
        assertTrue(papyrus.haptic.weight > glossy.haptic.weight)
        assertFalse(glossy == papyrus)
    }
}
