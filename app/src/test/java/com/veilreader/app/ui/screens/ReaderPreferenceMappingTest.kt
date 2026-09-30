package com.veilreader.app.ui.screens

import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderLayoutMode
import com.veilreader.app.domain.ReaderNavigationMode
import com.veilreader.app.domain.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ReaderPreferenceMappingTest {
    @Test
    fun `new reader sessions default to the paper sanctuary`() {
        val appearance = ReaderAppearance()

        assertEquals(ReaderTheme.PAPER, appearance.theme)
        assertEquals(ReaderLayoutMode.PAGED, appearance.layoutMode)
        assertEquals(PageTurnStyle.PAPER, appearance.pageTurnStyle)
        assertEquals(ReaderNavigationMode.PAPER_CURL, appearance.navigationMode)
    }

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
        assertEquals(0xFFE9DEC5.toInt() to 0xFF2A251F.toInt(), readiumThemeColors(ReaderTheme.PAPER))
        assertEquals(0xFFE2D0AA.toInt() to 0xFF362E24.toInt(), readiumThemeColors(ReaderTheme.SEPIA))
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
    fun `layout mode and page turn style remain independent`() {
        val original = ReaderAppearance(
            scroll = false,
            pageTurnStyle = PageTurnStyle.PAPER
        )

        val scroll = original.withLayoutMode(ReaderLayoutMode.SCROLL)
        assertEquals(ReaderLayoutMode.SCROLL, scroll.layoutMode)
        assertEquals(true, scroll.scroll)
        assertEquals(PageTurnStyle.PAPER, scroll.pageTurnStyle)

        val slideWhileScrolling = scroll.withPageTurnStyle(PageTurnStyle.SLIDE)
        assertEquals(ReaderLayoutMode.SCROLL, slideWhileScrolling.layoutMode)
        assertEquals(PageTurnStyle.SLIDE, slideWhileScrolling.pageTurnStyle)

        val pagedAgain = slideWhileScrolling.withLayoutMode(ReaderLayoutMode.PAGED)
        assertFalse(pagedAgain.scroll)
        assertEquals(PageTurnStyle.SLIDE, pagedAgain.pageTurnStyle)
        assertEquals(ReaderNavigationMode.SLIDE, pagedAgain.navigationMode)

        val noEffect = pagedAgain.withPageTurnStyle(PageTurnStyle.NONE)
        assertEquals(ReaderLayoutMode.PAGED, noEffect.layoutMode)
        assertEquals(PageTurnStyle.NONE, noEffect.pageTurnStyle)
        assertEquals(ReaderNavigationMode.PAGED, noEffect.navigationMode)
    }

    @Test
    fun `scroll persistence preserves the selected paginated transition`() {
        val stored = ReaderAppearance(
            scroll = true,
            pageTurnStyle = PageTurnStyle.SLIDE
        )

        val canonical = stored.canonicalizedNavigation()

        assertEquals(ReaderLayoutMode.SCROLL, canonical.layoutMode)
        assertEquals(ReaderNavigationMode.SCROLL, canonical.navigationMode)
        assertEquals(PageTurnStyle.SLIDE, canonical.pageTurnStyle)

        val returnedToPages = canonical.withLayoutMode(ReaderLayoutMode.PAGED)
        assertFalse(returnedToPages.scroll)
        assertEquals(PageTurnStyle.SLIDE, returnedToPages.pageTurnStyle)
        assertEquals(ReaderNavigationMode.SLIDE, returnedToPages.navigationMode)
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

}
