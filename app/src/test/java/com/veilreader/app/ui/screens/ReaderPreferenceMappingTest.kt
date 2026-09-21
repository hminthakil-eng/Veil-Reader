package com.veilreader.app.ui.screens

import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderTheme
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Fit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

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
    fun `pdf preferences map continuous and paginated layouts deterministically`() {
        val continuous = ReaderAppearance(scroll = true).toPdfiumPreferences()
        assertEquals(Fit.WIDTH, continuous.fit)
        assertEquals(12.0, continuous.pageSpacing!!, 0.0001)
        assertEquals(true, continuous.scroll)
        assertEquals(Axis.VERTICAL, continuous.scrollAxis)

        val paginated = ReaderAppearance(scroll = false).toPdfiumPreferences()
        assertEquals(Fit.CONTAIN, paginated.fit)
        assertEquals(6.0, paginated.pageSpacing!!, 0.0001)
        assertEquals(false, paginated.scroll)
        assertEquals(null, paginated.scrollAxis)
    }
}
