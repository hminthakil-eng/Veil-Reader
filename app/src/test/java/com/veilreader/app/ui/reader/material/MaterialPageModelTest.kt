package com.veilreader.app.ui.reader.material

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialPageModelTest {

    @Test
    fun `prepared snapshot requires exact revision viewport and live bitmap`() {
        val bitmap = android.graphics.Bitmap.createBitmap(
            32,
            48,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val prepared = MaterialPagePreparedSnapshot(
            bitmap = bitmap,
            sourceRevision = 9L,
            width = 32,
            height = 48,
            capturedAtElapsedNanos = 100L,
            provider = "test"
        )
        assertTrue(materialPagePreparedSnapshotIsCurrent(prepared, 9L, 32, 48))
        assertFalse(materialPagePreparedSnapshotIsCurrent(prepared, 8L, 32, 48))
        assertFalse(materialPagePreparedSnapshotIsCurrent(prepared, 9L, 48, 32))
        bitmap.recycle()
        assertFalse(materialPagePreparedSnapshotIsCurrent(prepared, 9L, 32, 48))
    }

    @Test
    fun `prepared snapshot expires instead of lifting old WebView pixels`() {
        val bitmap = android.graphics.Bitmap.createBitmap(
            24,
            36,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val prepared = MaterialPagePreparedSnapshot(
            bitmap = bitmap,
            sourceRevision = 5L,
            width = 24,
            height = 36,
            capturedAtElapsedNanos = 10_000L,
            provider = "test"
        )

        assertTrue(
            materialPagePreparedSnapshotIsCurrent(
                prepared = prepared,
                expectedRevision = 5L,
                expectedWidth = 24,
                expectedHeight = 36,
                nowElapsedNanos = 10_000L + 100L,
                maxAgeNanos = 200L
            )
        )
        assertFalse(
            materialPagePreparedSnapshotIsCurrent(
                prepared = prepared,
                expectedRevision = 5L,
                expectedWidth = 24,
                expectedHeight = 36,
                nowElapsedNanos = 10_000L + 201L,
                maxAgeNanos = 200L
            )
        )
        assertFalse(
            materialPagePreparedSnapshotIsCurrent(
                prepared = prepared,
                expectedRevision = 5L,
                expectedWidth = 24,
                expectedHeight = 36,
                nowElapsedNanos = 9_999L,
                maxAgeNanos = 200L
            )
        )
        bitmap.recycle()
    }

    @Test
    fun `snapshot revisions reject stale captures and survive counter rollover`() {
        assertTrue(materialPageSnapshotCaptureIsCurrent(7L, 7L))
        assertFalse(materialPageSnapshotCaptureIsCurrent(6L, 7L))
        assertFalse(materialPageSnapshotCaptureIsCurrent(0L, 0L))
        assertEquals(8L, nextMaterialPageSnapshotRevision(7L))
        assertEquals(1L, nextMaterialPageSnapshotRevision(Long.MAX_VALUE))
    }

    @Test
    fun `tap grip begins from lower corner with material variation`() {
        val glossyOrigin = materialPageTapPullOrigin(MaterialPageProfiles.Glossy)
        val manuscriptOrigin = materialPageTapPullOrigin(MaterialPageProfiles.Manuscript)
        val glossyDiagonal = materialPageTapDiagonalPull(MaterialPageProfiles.Glossy)
        val manuscriptDiagonal = materialPageTapDiagonalPull(MaterialPageProfiles.Manuscript)

        assertTrue(glossyOrigin > 0.75f)
        assertTrue(manuscriptOrigin >= glossyOrigin)
        assertTrue(glossyDiagonal < 0f)
        assertTrue(manuscriptDiagonal <= glossyDiagonal)
    }

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
    fun `extreme corner origin is softened but remains directionally faithful`() {
        val top = materialPageStablePullOrigin(
            startY = 0f,
            heightPx = 1_600f
        )
        val bottom = materialPageStablePullOrigin(
            startY = 1_600f,
            heightPx = 1_600f
        )
        val center = materialPageStablePullOrigin(
            startY = 800f,
            heightPx = 1_600f
        )

        assertTrue(top in 0.04f..0.20f)
        assertTrue(bottom in 0.80f..0.96f)
        assertEquals(0.5f, center, 0.0001f)
        assertEquals(
            0.5f,
            materialPageStablePullOrigin(Float.NaN, 1_600f),
            0.0001f
        )
    }

    @Test
    fun `backside ink transmission stays restrained and material specific`() {
        val glossy = materialPageBacksideContentAlpha(
            MaterialPageProfiles.Glossy,
            patina = 0.65f
        )
        val matte = materialPageBacksideContentAlpha(
            MaterialPageProfiles.MatteBook,
            patina = 0.65f
        )
        val manuscript = materialPageBacksideContentAlpha(
            MaterialPageProfiles.Manuscript,
            patina = 0.65f
        )

        assertTrue(glossy in 0.08f..0.32f)
        assertTrue(matte in 0.08f..0.32f)
        assertTrue(manuscript in 0.08f..0.32f)
        assertNotEquals(glossy, matte)
        assertNotEquals(matte, manuscript)
        assertTrue(manuscript < glossy)
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
    fun `tap lift choreography changes with material body`() {
        val glossyLift = materialPageTapLiftFraction(MaterialPageProfiles.Glossy)
        val manuscriptLift = materialPageTapLiftFraction(MaterialPageProfiles.Manuscript)
        val glossyDuration = materialPageTapLiftDurationMillis(MaterialPageProfiles.Glossy)
        val manuscriptDuration = materialPageTapLiftDurationMillis(MaterialPageProfiles.Manuscript)

        assertTrue(glossyLift in 0.10f..0.17f)
        assertTrue(manuscriptLift in 0.10f..0.17f)
        assertTrue(manuscriptDuration > glossyDuration)
        assertNotEquals(glossyLift, manuscriptLift)
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
    fun `non finite physics input collapses safely instead of poisoning renderer state`() {
        val drag = materialPageDragSample(
            inwardDistancePx = Float.NaN,
            verticalDistancePx = Float.POSITIVE_INFINITY,
            widthPx = 1_000f,
            heightPx = 1_600f,
            profile = MaterialPageProfiles.MatteBook
        )

        assertEquals(0f, drag.progress, 0.0001f)
        assertEquals(0f, drag.rawProgress, 0.0001f)
        assertEquals(0f, drag.verticalBias, 0.0001f)
        assertEquals(
            MaterialPageReleaseDecision.CANCEL,
            materialPageReleaseDecision(
                progress = Float.NaN,
                inwardVelocityDpPerSec = Float.NaN,
                profile = MaterialPageProfiles.MatteBook
            )
        )
    }

    @Test
    fun `lifted front tint remains subtle while preserving material identity`() {
        val glossy = materialPageFrontSurfaceTintAlpha(
            MaterialPageProfiles.Glossy,
            patina = 0.7f
        )
        val parchment = materialPageFrontSurfaceTintAlpha(
            MaterialPageProfiles.Parchment,
            patina = 0.7f
        )

        assertTrue(glossy in 0.012f..0.072f)
        assertTrue(parchment in 0.012f..0.072f)
        assertTrue(parchment > glossy)
    }

    @Test
    fun `snapshot scaling rejects rotation warp but permits small layout drift`() {
        assertTrue(
            materialPageSnapshotScaleIsSafe(
                snapshotWidth = 1_080f,
                snapshotHeight = 1_920f,
                canvasWidth = 1_070f,
                canvasHeight = 1_900f
            )
        )
        assertFalse(
            materialPageSnapshotScaleIsSafe(
                snapshotWidth = 1_080f,
                snapshotHeight = 1_920f,
                canvasWidth = 1_920f,
                canvasHeight = 1_080f
            )
        )
        assertFalse(
            materialPageSnapshotScaleIsSafe(
                snapshotWidth = Float.NaN,
                snapshotHeight = 1_920f,
                canvasWidth = 1_080f,
                canvasHeight = 1_920f
            )
        )
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
