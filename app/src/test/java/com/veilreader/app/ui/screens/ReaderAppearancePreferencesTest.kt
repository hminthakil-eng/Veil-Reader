package com.veilreader.app.ui.screens

import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class ReaderAppearancePreferencesTest {
    @Test
    fun `changing page colors preserves customized reading behavior at the engine boundary`() {
        for (style in PageTurnStyle.entries) {
            for (scroll in listOf(false, true)) {
                for (publisherStyles in listOf(false, true)) {
                    var appearance = ReaderAppearance(
                        fontScale = 1.65,
                        lineHeight = 1.85,
                        pageMargins = 1.7,
                        scroll = scroll,
                        publisherStyles = publisherStyles,
                        pageTurnStyle = style
                    )
                    for (theme in ReaderTheme.entries) {
                        appearance = appearance.withPageTheme(theme)
                        val prefs = appearance.toEpubPreferenceSpec()

                        assertEquals(theme, prefs.theme)
                        assertEquals(1.65, prefs.fontSize, 0.0001)
                        assertEquals(1.85, prefs.lineHeight, 0.0001)
                        assertEquals(1.7, prefs.pageMargins, 0.0001)
                        assertEquals(scroll, prefs.scroll)
                        assertEquals(style, appearance.pageTurnStyle)
                        assertFalse(prefs.publisherStyles)
                        assertNotNull(prefs.backgroundColorArgb)
                        assertNotNull(prefs.textColorArgb)
                    }
                }
            }
        }
    }

    @Test
    fun `font size is passed to Readium as a ratio not a percent number`() {
        val prefs = ReaderAppearance(
            theme = ReaderTheme.PAPER,
            fontScale = 1.40,
            publisherStyles = true
        ).toEpubPreferenceSpec()

        assertEquals(1.40, prefs.fontSize, 0.0001)
        assertEquals(ReaderTheme.PAPER, prefs.theme)
    }

    @Test
    fun `custom theme with publisher styles disabled supplies effective page colors`() {
        val prefs = ReaderAppearance(
            theme = ReaderTheme.SEPIA,
            fontScale = 1.0,
            publisherStyles = false
        ).toEpubPreferenceSpec()

        assertEquals(ReaderTheme.SEPIA, prefs.theme)
        assertFalse(prefs.publisherStyles)
        assertNotNull(prefs.backgroundColorArgb)
        assertNotNull(prefs.textColorArgb)
    }

    @Test
    fun `font size is clamped to Veil supported range before Readium submission`() {
        val tiny = ReaderAppearance(fontScale = 0.1).toEpubPreferenceSpec()
        val huge = ReaderAppearance(fontScale = 20.0).toEpubPreferenceSpec()

        assertEquals(0.75, tiny.fontSize, 0.0001)
        assertEquals(1.80, huge.fontSize, 0.0001)
    }
}
