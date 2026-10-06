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
    fun `archive records reserve readable measure at accessibility sizes`() {
        assertEquals(112f, galleryCellMeasureDp(112f, 1f), 0.001f)
        assertEquals(224f, galleryCellMeasureDp(112f, 2f), 0.001f)
        assertEquals(112f, galleryCellMeasureDp(112f, Float.NaN), 0.001f)
        assertEquals(112f, galleryCellMeasureDp(112f, 0f), 0.001f)
    }

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
    fun `navigation modes expose distinct sanctuary material language`() {
        val curl = sanctuaryPageMaterialFor(ReaderNavigationMode.PAPER_CURL)
        val slide = sanctuaryPageMaterialFor(ReaderNavigationMode.SLIDE)
        val paged = sanctuaryPageMaterialFor(ReaderNavigationMode.PAGED)
        val scroll = sanctuaryPageMaterialFor(ReaderNavigationMode.SCROLL)

        assertTrue(curl.showPhysicalPageStack)
        assertFalse(slide.showPhysicalPageStack)
        assertTrue(paged.showPhysicalPageStack)
        assertFalse(scroll.showPhysicalPageStack)

        ReaderNavigationMode.entries.forEach { mode ->
            val material = sanctuaryPageMaterialFor(mode)
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

    @Test
    fun `architectural pairing uses usable width and yields to large text`() {
        assertFalse(useArchitecturalPair(599f, 1f))
        assertTrue(useArchitecturalPair(720f, 1f))
        assertFalse(useArchitecturalPair(720f, 1.3f))
        assertFalse(useArchitecturalPair(639f, 1f))
        assertTrue(useArchitecturalPair(640f, 1f))
        assertTrue(useArchitecturalPair(839f, 1f))
        assertTrue(useArchitecturalPair(840f, 1f))
        assertTrue(useArchitecturalPair(840f, 1.3f))
        assertFalse(useArchitecturalPair(840f, 1.5f))
        assertFalse(useArchitecturalPair(1120f, 2f))
        assertTrue(useArchitecturalPair(1400f, 2f))
        assertFalse(useArchitecturalPair(Float.NaN, 1f))
        assertFalse(useArchitecturalPair(900f, Float.POSITIVE_INFINITY))
        assertFalse(useArchitecturalPair(900f, 0f))
    }
    @Test
    fun `aged paper keeps broad stain and noise below the reading contrast budget`() {
        listOf(ReaderTheme.PAPER, ReaderTheme.SEPIA).forEach { theme ->
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { age ->
                val material = sanctuarySurfaceProfileFor(theme, age)
                assertTrue(material.mottleAlpha in 0f..0.015f)
                assertTrue(material.fibreAlpha in 0f..0.017f)
                assertTrue(material.speckAlpha in 0f..0.014f)
                assertTrue(material.stackEdgeAlpha > material.mottleAlpha)
            }
        }
    }

    @Test
    fun `current threshold artifact remains larger than recent shelf objects`() {
        VeilAdaptiveClass.entries.forEach { window ->
            val layout = thresholdLayoutPolicyFor(window)
            assertTrue(layout.heroCoverWidthDp > layout.recentCoverWidthDp)
            assertTrue(layout.heroCoverHeightDp > layout.recentCoverHeightDp)
        }
    }

    @Test
    fun `approach prose yields to short windows and large text without depending on script`() {
        assertFalse(condenseRealmApproach(1f, 800))
        assertFalse(condenseRealmApproach(1.29f, 500))
        assertTrue(condenseRealmApproach(1.3f, 800))
        assertTrue(condenseRealmApproach(1f, 499))
        assertTrue(condenseRealmApproach(2f, 1400))
        assertFalse(condenseRealmApproach(Float.NaN, 0))
    }
    @Test
    fun `missing cover captions need readable width and height together`() {
        assertTrue(artifactCaptionFits(120f, 180f, 1f))
        assertFalse(artifactCaptionFits(119f, 300f, 1f))
        assertFalse(artifactCaptionFits(200f, 179f, 1f))
        assertTrue(artifactCaptionFits(240f, 360f, 2f))
        assertFalse(artifactCaptionFits(240f, 359f, 2f))
        assertFalse(artifactCaptionFits(Float.NaN, 300f, 1f))
        assertTrue(artifactCaptionFits(120f, 180f, Float.NaN))
    }

    @Test
    fun `returning phone threshold abbreviates recurring cinematic copy`() {
        assertTrue(
            shouldAbbreviateThresholdEntry(
                hasCurrentBook = true,
                widthDp = 412f,
                heightDp = 915,
                fontScale = 1f
            )
        )
    }

    @Test
    fun `wide returning threshold may keep full approach when accessibility permits`() {
        assertFalse(
            shouldAbbreviateThresholdEntry(
                hasCurrentBook = true,
                widthDp = 900f,
                heightDp = 900,
                fontScale = 1f
            )
        )
    }

    @Test
    fun `large text still abbreviates wide returning threshold without shrinking text`() {
        assertTrue(
            shouldAbbreviateThresholdEntry(
                hasCurrentBook = true,
                widthDp = 900f,
                heightDp = 900,
                fontScale = 1.5f
            )
        )
    }

    @Test
    fun `empty threshold preserves cinematic entrance`() {
        assertFalse(
            shouldAbbreviateThresholdEntry(
                hasCurrentBook = false,
                widthDp = 412f,
                heightDp = 915,
                fontScale = 2f
            )
        )
    }


    @Test
    fun `populated archive condenses header`() {
        assertTrue(
            shouldCondenseArchiveHeader(
                bookCount = 1,
                retrievalActive = false,
                fontScale = 1f,
                heightDp = 900
            )
        )
    }

    @Test
    fun `empty archive keeps authored entrance when space permits`() {
        assertFalse(
            shouldCondenseArchiveHeader(
                bookCount = 0,
                retrievalActive = false,
                fontScale = 1f,
                heightDp = 900
            )
        )
    }

    @Test
    fun `compact normal text may keep archive actions inline`() {
        assertTrue(
            shouldInlineCondensedArchiveActions(
                adjacentLayout = false,
                condensed = true,
                compactLayout = true,
                fontScale = 1f
            )
        )
    }

    @Test
    fun `compact large text moves archive actions below instead of shrinking labels`() {
        assertFalse(
            shouldInlineCondensedArchiveActions(
                adjacentLayout = false,
                condensed = true,
                compactLayout = true,
                fontScale = 2f
            )
        )
    }

    @Test
    fun `gallery cover remains bounded while record widens`() {
        assertEquals(
            VeilComposition.GalleryCoverMaxWidthDp,
            galleryBookObjectWidthDp(220f, 1f),
            0.001f
        )
    }

    @Test
    fun `large text uses tighter physical cover cap`() {
        assertEquals(
            VeilComposition.GalleryLargeTextCoverMaxWidthDp,
            galleryBookObjectWidthDp(260f, 2f),
            0.001f
        )
    }

}
