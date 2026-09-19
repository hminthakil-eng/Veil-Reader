package com.veilreader.app.ui.screens

import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.readium.r2.navigator.preferences.Theme

class ReaderAppearancePreferencesTest {
    @Test
    fun `font size is passed to Readium as a ratio not a percent number`() {
        val prefs = ReaderAppearance(
            theme = ReaderTheme.PAPER,
            fontScale = 1.40,
            publisherStyles = true
        ).toEpubPreferences()

        assertEquals(1.40, prefs.fontSize ?: 0.0, 0.0001)
        assertEquals(Theme.LIGHT, prefs.theme)
    }

    @Test
    fun `custom theme with publisher styles disabled supplies effective page colors`() {
        val prefs = ReaderAppearance(
            theme = ReaderTheme.SEPIA,
            fontScale = 1.0,
            publisherStyles = false
        ).toEpubPreferences()

        assertEquals(Theme.SEPIA, prefs.theme)
        assertFalse(prefs.publisherStyles ?: true)
        assertNotNull(prefs.backgroundColor)
        assertNotNull(prefs.textColor)
    }

    @Test
    fun `font size is clamped to Veil supported range before Readium submission`() {
        val tiny = ReaderAppearance(fontScale = 0.1).toEpubPreferences()
        val huge = ReaderAppearance(fontScale = 20.0).toEpubPreferences()

        assertEquals(0.75, tiny.fontSize ?: 0.0, 0.0001)
        assertEquals(1.80, huge.fontSize ?: 0.0, 0.0001)
    }
}
