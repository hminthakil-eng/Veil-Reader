package com.veilreader.app.ui.theme

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
