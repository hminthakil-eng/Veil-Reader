package com.veilreader.app.ui.screens

import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderFontFamily
import com.veilreader.app.domain.ReaderHyphenation
import com.veilreader.app.domain.ReaderTextAlignment
import com.veilreader.app.domain.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.TextAlign

class ReaderPreferenceMappingTest {
    @Test
    fun `font size uses Readium ratio and clamps supported range`() {
        assertEquals(1.0, readiumFontSizeRatio(1.0), 0.0001)
        assertEquals(1.4, readiumFontSizeRatio(1.4), 0.0001)
        assertEquals(0.75, readiumFontSizeRatio(0.1), 0.0001)
        assertEquals(1.8, readiumFontSizeRatio(20.0), 0.0001)
        assertEquals(1.0, readiumFontSizeRatio(Double.NaN), 0.0001)
    }

    @Test
    fun `reader themes expose deterministic background and text colors`() {
        assertEquals(0xFFF1E5C9.toInt() to 0xFF3D3325.toInt(), readiumThemeColors(ReaderTheme.SEPIA))
        assertEquals(0xFF18151D.toInt() to 0xFFF5F0F7.toInt(), readiumThemeColors(ReaderTheme.DUSK))
        assertEquals(0xFF000000.toInt() to 0xFFF5F0F7.toInt(), readiumThemeColors(ReaderTheme.OLED))
    }

    @Test
    fun `explicit theme selection disables publisher style override`() {
        ReaderTheme.entries.forEach { theme ->
            val selected = ReaderAppearance(publisherStyles = true).withTheme(theme)

            assertEquals(theme, selected.theme)
            assertFalse(selected.publisherStyles)
        }
    }


    @Test
    fun `reader brightness clamps custom values and preserves system mode`() {
        assertEquals(null, ReaderAppearance().withScreenBrightness(null).screenBrightness)
        assertEquals(null, ReaderAppearance().withScreenBrightness(Double.NaN).screenBrightness)
        assertEquals(0.05, ReaderAppearance().withScreenBrightness(0.01).screenBrightness!!, 0.0001)
        assertEquals(0.42, ReaderAppearance().withScreenBrightness(0.42).screenBrightness!!, 0.0001)
        assertEquals(1.0, ReaderAppearance().withScreenBrightness(2.0).screenBrightness!!, 0.0001)
    }

    @Test
    fun `explicit typography overrides disable publisher styles`() {
        val original = ReaderAppearance(publisherStyles = true)

        val font = original.withFontScale(1.25)
        assertEquals(1.25, font.fontScale, 0.0001)
        assertFalse(font.publisherStyles)

        val line = original.withLineHeight(1.7)
        assertEquals(1.7, line.lineHeight, 0.0001)
        assertFalse(line.publisherStyles)

        val margins = original.withPageMargins(1.3)
        assertEquals(1.3, margins.pageMargins, 0.0001)
        assertFalse(margins.publisherStyles)
    }

    @Test
    fun `advanced typography maps to Readium without custom rendering`() {
        val prefs = ReaderAppearance(
            publisherStyles = false,
            fontFamily = ReaderFontFamily.OPEN_DYSLEXIC,
            textAlignment = ReaderTextAlignment.JUSTIFY,
            hyphenation = ReaderHyphenation.ON
        ).toEpubPreferences()

        assertEquals(FontFamily.OPEN_DYSLEXIC, prefs.fontFamily)
        assertEquals(TextAlign.JUSTIFY, prefs.textAlign)
        assertEquals(true, prefs.hyphens)
    }

    @Test
    fun `book typography leaves advanced Readium preferences unset`() {
        val prefs = ReaderAppearance().toEpubPreferences()

        assertEquals(null, prefs.fontFamily)
        assertEquals(null, prefs.textAlign)
        assertEquals(null, prefs.hyphens)
    }

}
