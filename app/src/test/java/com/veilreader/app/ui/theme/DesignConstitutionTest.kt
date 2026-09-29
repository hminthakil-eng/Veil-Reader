package com.veilreader.app.ui.theme

import com.veilreader.app.domain.ReaderNavigationMode
import com.veilreader.app.domain.ReaderTheme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignConstitutionTest {
    @Test
    fun `quality tiers degrade atmosphere before capability`() {
        val full = qualityPolicyFor(VeilQualityTier.FULL)
        val balanced = qualityPolicyFor(VeilQualityTier.BALANCED)
        val essential = qualityPolicyFor(VeilQualityTier.ESSENTIAL)

        assertTrue(full.atmosphereMultiplier > balanced.atmosphereMultiplier)
        assertTrue(balanced.atmosphereMultiplier > essential.atmosphereMultiplier)
        assertTrue(full.expensiveBlurAllowed)
        assertFalse(balanced.expensiveBlurAllowed)
        assertFalse(essential.ambientMotionEnabled)
    }

    @Test
    fun `adaptive classes are compositional breakpoints`() {
        assertEquals(VeilAdaptiveClass.COMPACT, adaptiveClassFor(412f))
        assertEquals(VeilAdaptiveClass.WIDE, adaptiveClassFor(700f))
        assertEquals(VeilAdaptiveClass.LARGE, adaptiveClassFor(840f))
        assertEquals(VeilAdaptiveClass.COMPACT, adaptiveClassFor(Float.NaN))
    }

    @Test
    fun `archive composition becomes roomier and richer with window class`() {
        val compact = archiveLayoutPolicyFor(VeilAdaptiveClass.COMPACT)
        val wide = archiveLayoutPolicyFor(VeilAdaptiveClass.WIDE)
        val large = archiveLayoutPolicyFor(VeilAdaptiveClass.LARGE)

        assertTrue(compact.galleryMinCellDp < wide.galleryMinCellDp)
        assertTrue(wide.galleryMinCellDp < large.galleryMinCellDp)
        assertTrue(compact.horizontalPaddingDp < large.horizontalPaddingDp)
        assertTrue(compact.shelfCoverWidthDp < large.shelfCoverWidthDp)
        assertFalse(compact.showIndexMemorySummary)
        assertTrue(wide.showIndexMemorySummary)
        assertTrue(large.showIndexMemorySummary)
    }

    @Test
    fun `threshold grows with window class without becoming a dashboard`() {
        val compact = thresholdLayoutPolicyFor(VeilAdaptiveClass.COMPACT)
        val wide = thresholdLayoutPolicyFor(VeilAdaptiveClass.WIDE)
        val large = thresholdLayoutPolicyFor(VeilAdaptiveClass.LARGE)

        assertTrue(compact.heroCoverWidthDp < wide.heroCoverWidthDp)
        assertTrue(wide.heroCoverWidthDp < large.heroCoverWidthDp)
        assertTrue(compact.horizontalPaddingDp < large.horizontalPaddingDp)
        assertTrue(compact.contentMaxWidthDp <= large.contentMaxWidthDp)
    }

    @Test
    fun `threshold atmosphere wakes with first volume and saturates`() {
        assertTrue(thresholdAtmosphereIntensityFor(0) < thresholdAtmosphereIntensityFor(1))
        assertTrue(thresholdAtmosphereIntensityFor(1) < thresholdAtmosphereIntensityFor(12))
        assertEquals(1f, thresholdAtmosphereIntensityFor(100))
        assertEquals(thresholdAtmosphereIntensityFor(0), thresholdAtmosphereIntensityFor(-4))
    }

    @Test
    fun `castle composition grows spatially with window class`() {
        val compact = castleLayoutPolicyFor(VeilAdaptiveClass.COMPACT)
        val wide = castleLayoutPolicyFor(VeilAdaptiveClass.WIDE)
        val large = castleLayoutPolicyFor(VeilAdaptiveClass.LARGE)

        assertTrue(compact.keepMinHeightDp < wide.keepMinHeightDp)
        assertTrue(wide.keepMinHeightDp < large.keepMinHeightDp)
        assertTrue(compact.chamberMinHeightDp < large.chamberMinHeightDp)
        assertTrue(compact.mapHorizontalPaddingDp < large.mapHorizontalPaddingDp)
        assertTrue(compact.observatoryHeightDp < wide.observatoryHeightDp)
        assertTrue(wide.observatoryHeightDp < large.observatoryHeightDp)
        assertTrue(compact.contentMaxWidthDp <= large.contentMaxWidthDp)
    }

    @Test
    fun `reduced motion removes translation and ambient loops`() {
        VeilMotionClass.entries.forEach { motionClass ->
            val policy = motionPolicyFor(motionClass, reducedMotion = true)
            assertFalse(policy.translationAllowed)
            assertFalse(policy.ambientLoopAllowed)
            assertEquals(VeilMotion.REDUCED_MOTION_FADE_MS, policy.fixedDurationMillis)
        }
    }

    @Test
    fun `physical motion remains physics owned at full motion`() {
        val policy = motionPolicyFor(VeilMotionClass.PHYSICAL, reducedMotion = false)
        assertNull(policy.fixedDurationMillis)
        assertTrue(policy.translationAllowed)
        assertFalse(policy.ambientLoopAllowed)
    }

    @Test
    fun `scroll keeps paper material without physical page stack`() {
        ReaderNavigationMode.entries.forEach { mode ->
            val material = sanctuaryPageMaterialFor(mode)
            assertEquals(
                mode != ReaderNavigationMode.SCROLL,
                material.showPhysicalPageStack
            )
            assertTrue(material.showEdgeFalloff)
            assertTrue(material.showMicroFibres)
        }
    }

    @Test
    fun `paper patina monotonically deepens light material`() {
        val clean = sanctuarySurfaceProfileFor(ReaderTheme.PAPER, 0f)
        val aged = sanctuarySurfaceProfileFor(ReaderTheme.PAPER, 1f)

        assertTrue(aged.pageShadeAlpha > clean.pageShadeAlpha)
        assertTrue(aged.stackEdgeAlpha > clean.stackEdgeAlpha)
        assertTrue(aged.edgeOxidationAlpha > clean.edgeOxidationAlpha)
        assertTrue(aged.fibreAlpha > clean.fibreAlpha)
        assertTrue(aged.fibreCount > clean.fibreCount)
        assertTrue(aged.speckCount > clean.speckCount)
    }

    @Test
    fun `dark sanctuary suppresses paper aging texture`() {
        listOf(ReaderTheme.DUSK, ReaderTheme.OLED).forEach { theme ->
            val surface = sanctuarySurfaceProfileFor(theme, 1f)
            assertEquals(0f, surface.patina)
            assertEquals(0f, surface.mottleAlpha)
            assertEquals(0f, surface.edgeOxidationAlpha)
            assertEquals(0, surface.fibreCount)
            assertEquals(0, surface.speckCount)
        }
    }

    @Test
    fun `invalid paper patina fails calm to clean paper`() {
        val invalid = sanctuarySurfaceProfileFor(ReaderTheme.PAPER, Float.NaN)
        val clean = sanctuarySurfaceProfileFor(ReaderTheme.PAPER, 0f)
        assertEquals(clean, invalid)
    }

    @Test
    fun `sanctuary remains visually quiet and bounded`() {
        assertFalse(VeilSanctuary.chromeOrnamentAllowed)
        assertFalse(VeilSanctuary.persistentDecorativeControlsAllowed)
        assertTrue(VeilSanctuary.atmosphereIntensity <= 0.05f)
        assertEquals(2f, VeilSanctuary.minimumPageStackDp)
        assertEquals(8f, VeilSanctuary.maximumPageStackDp)
        assertEquals(VeilMotion.READER_AUTO_HIDE_MS, VeilSanctuary.chromeAutoHideMillis)
    }
}
