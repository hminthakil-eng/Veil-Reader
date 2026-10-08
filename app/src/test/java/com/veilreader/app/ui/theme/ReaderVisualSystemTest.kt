package com.veilreader.app.ui.theme

import com.veilreader.app.domain.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderVisualSystemTest {
    @Test
    fun touchTargetsRemainAccessible() {
        assertTrue(ReaderVisualGeometry.TouchTarget.value >= 48f)
        assertTrue(ReaderVisualGeometry.ReaderControlMinHeight.value >= 48f)
    }

    @Test
    fun readerMarginsRemainInsideDesignedRange() {
        assertTrue(
            ReaderVisualGeometry.MobileHorizontalMargin.value >=
                ReaderVisualGeometry.MobileHorizontalMarginMin.value
        )
        assertTrue(
            ReaderVisualGeometry.MobileHorizontalMargin.value <=
                ReaderVisualGeometry.MobileHorizontalMarginMax.value
        )
    }

    @Test
    fun readerMotionPreservesFunctionalHierarchy() {
        assertTrue(ReaderVisualMotion.InstantMs < ReaderVisualMotion.HudFadeMs)
        assertTrue(ReaderVisualMotion.HudFadeMs < ReaderVisualMotion.SheetMs)
        assertTrue(ReaderVisualMotion.SheetMs < ReaderVisualMotion.SpatialMs)
        assertTrue(ReaderVisualMotion.SpatialMs <= ReaderVisualMotion.SignatureMs)
        assertTrue(ReaderVisualMotion.SignatureMs <= ReaderVisualMotion.NarrativeMaxMs)
    }

    @Test
    fun readingThemesRemainVisuallyDistinct() {
        val themes = listOf(
            ReaderVisualPalette.Paper,
            ReaderVisualPalette.Ivory,
            ReaderVisualPalette.Sepia,
            ReaderVisualPalette.Charcoal,
            ReaderVisualPalette.Night,
            ReaderVisualPalette.Oled
        )
        assertEquals(themes.size, themes.map { it.value }.distinct().size)
    }

    @Test
    fun readerThemeArgbMatchesApprovedSanctuaryPalette() {
        val paper = readerVisualThemeArgb(ReaderTheme.PAPER)
        val sepia = readerVisualThemeArgb(ReaderTheme.SEPIA)
        val dusk = readerVisualThemeArgb(ReaderTheme.DUSK)
        val oled = readerVisualThemeArgb(ReaderTheme.OLED)

        assertEquals(0xFFF4EFDF.toInt(), paper.background)
        assertEquals(0xFF29271F.toInt(), paper.text)
        assertEquals(0xFFE9DDC3.toInt(), sepia.background)
        assertEquals(0xFF372F24.toInt(), sepia.text)
        assertEquals(0xFF141517.toInt(), dusk.background)
        assertEquals(0xFFD8D6D0.toInt(), dusk.text)
        assertEquals(0xFF000000.toInt(), oled.background)
        assertEquals(0xFFD5D5D2.toInt(), oled.text)
    }


    @Test
    fun appearanceSheetGeometryStaysComfortableAndAccessible() {
        assertTrue(ReaderVisualGeometry.AppearanceSheetHorizontalPadding.value >= 16f)
        assertTrue(ReaderVisualGeometry.AppearanceSheetTopPadding.value >= 16f)
        assertTrue(ReaderVisualGeometry.AppearanceSectionGap.value >= 12f)
        assertTrue(
            ReaderVisualGeometry.AppearanceModeChoiceMinHeight.value >=
                ReaderVisualGeometry.TouchTarget.value
        )
        assertTrue(
            ReaderVisualGeometry.AppearancePreviewMinHeight.value >
                ReaderVisualGeometry.AppearanceModeChoiceMinHeight.value
        )
        assertTrue(
            ReaderVisualGeometry.AppearanceThemeCardMinHeight.value >
                ReaderVisualGeometry.TouchTarget.value
        )
    }


    @Test
    fun readerHudGeometryRemainsAccessibleAndRestrained() {
        assertTrue(ReaderVisualGeometry.HudTopRowMinHeight.value >= ReaderVisualGeometry.TouchTarget.value)
        assertTrue(ReaderVisualGeometry.HudHorizontalInset.value >= 6f)
        assertTrue(ReaderVisualGeometry.HudCornerRadius.value <= 16f)
        assertTrue(ReaderVisualGeometry.PreviousLocationTopOffset.value >= 56f)
        assertTrue(ReaderVisualOpacity.AccessDockSurface < ReaderVisualOpacity.ChromeSurface)
        assertTrue(ReaderVisualOpacity.HudProgressTrack < ReaderVisualOpacity.HudAccentHairline)
    }

    @Test
    fun annotationChannelsRemainDistinct() {
        val highlights = listOf(
            ReaderVisualPalette.GoldHighlight,
            ReaderVisualPalette.RoseHighlight,
            ReaderVisualPalette.MintHighlight,
            ReaderVisualPalette.BlueHighlight,
            ReaderVisualPalette.VioletHighlight
        )
        assertEquals(highlights.size, highlights.map { it.value }.distinct().size)
        assertNotEquals(ReaderVisualPalette.SearchWash.value, ReaderVisualPalette.TtsWash.value)
    }
}
