package com.veilreader.app.ui.screens

import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderColumnMode
import com.veilreader.app.domain.ReaderDarkImageTreatment
import com.veilreader.app.domain.ReaderFontFamily
import com.veilreader.app.domain.ReaderNavigationMode
import com.veilreader.app.domain.ReaderPreferenceToggle
import com.veilreader.app.domain.ReaderTextAlignment
import com.veilreader.app.domain.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.readium.r2.navigator.preferences.ImageFilter
import org.junit.Test

class ReaderPreferenceMappingTest {
    @Test
    fun `new reader sessions default to the paper sanctuary`() {
        val appearance = ReaderAppearance()

        assertEquals(ReaderTheme.PAPER, appearance.theme)
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
    fun `navigation modes keep paper curl slide and scroll mutually exclusive`() {
        val original = ReaderAppearance(scroll = false, pageTurnStyle = PageTurnStyle.PAPER)

        val curl = original.withNavigationMode(ReaderNavigationMode.PAPER_CURL)
        assertEquals(ReaderNavigationMode.PAPER_CURL, curl.navigationMode)
        assertFalse(curl.scroll)
        assertEquals(PageTurnStyle.PAPER, curl.pageTurnStyle)

        val slide = curl.withNavigationMode(ReaderNavigationMode.SLIDE)
        assertEquals(ReaderNavigationMode.SLIDE, slide.navigationMode)
        assertFalse(slide.scroll)
        assertEquals(PageTurnStyle.SLIDE, slide.pageTurnStyle)

        val paged = slide.withNavigationMode(ReaderNavigationMode.PAGED)
        assertEquals(ReaderNavigationMode.PAGED, paged.navigationMode)
        assertFalse(paged.scroll)
        assertEquals(PageTurnStyle.NONE, paged.pageTurnStyle)

        val scroll = paged.withNavigationMode(ReaderNavigationMode.SCROLL)
        assertEquals(ReaderNavigationMode.SCROLL, scroll.navigationMode)
        assertEquals(true, scroll.scroll)
        assertEquals(PageTurnStyle.NONE, scroll.pageTurnStyle)

        val backToCurl = scroll.withNavigationMode(ReaderNavigationMode.PAPER_CURL)
        assertEquals(ReaderNavigationMode.PAPER_CURL, backToCurl.navigationMode)
        assertFalse(backToCurl.scroll)
        assertEquals(PageTurnStyle.PAPER, backToCurl.pageTurnStyle)
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
    fun `advanced typography overrides are deterministic and disable publisher styles`() {
        val original = ReaderAppearance(publisherStyles = true)
            .withFontFamily(ReaderFontFamily.OPEN_DYSLEXIC)
            .withTextAlignment(ReaderTextAlignment.JUSTIFY)
            .withParagraphSpacing(9.0)
            .withParagraphIndent(-3.0)
            .withLetterSpacing(2.0)
            .withWordSpacing(Double.NaN)
            .withTypeScale(9.0)
            .copy(
                columnMode = ReaderColumnMode.TWO,
                hyphenation = ReaderPreferenceToggle.ON,
                ligatures = ReaderPreferenceToggle.OFF,
                textNormalization = ReaderPreferenceToggle.ON
            )

        assertFalse(original.publisherStyles)
        assertEquals(ReaderFontFamily.OPEN_DYSLEXIC, original.fontFamily)
        assertEquals(ReaderTextAlignment.JUSTIFY, original.textAlignment)
        assertEquals(2.0, original.paragraphSpacing!!, 0.0001)
        assertEquals(0.0, original.paragraphIndent!!, 0.0001)
        assertEquals(0.2, original.letterSpacing!!, 0.0001)
        assertEquals(null, original.wordSpacing)
        assertEquals(2.0, original.typeScale!!, 0.0001)
        assertEquals(ReaderColumnMode.TWO, original.columnMode)
    }

    @Test
    fun `paper patina is finite and clamped`() {
        assertEquals(1.0, ReaderAppearance().withPaperPatina(9.0).paperPatina, 0.0001)
        assertEquals(0.0, ReaderAppearance().withPaperPatina(-2.0).paperPatina, 0.0001)
        assertEquals(0.72, ReaderAppearance().withPaperPatina(Double.NaN).paperPatina, 0.0001)
    }

    @Test
    fun `font weight is optional finite and clamped to Readium 3_4 range`() {
        assertEquals(null, ReaderAppearance().withFontWeight(null).fontWeight)
        assertEquals(null, ReaderAppearance().withFontWeight(Double.NaN).fontWeight)
        assertEquals(0.0, ReaderAppearance().withFontWeight(-4.0).fontWeight!!, 0.0001)
        assertEquals(1.25, ReaderAppearance().withFontWeight(1.25).fontWeight!!, 0.0001)
        assertEquals(2.5, ReaderAppearance().withFontWeight(9.0).fontWeight!!, 0.0001)
    }

    @Test
    fun `EPUB preferences map dark image treatment and weight without fake state`() {
        val original = ReaderAppearance(
            theme = ReaderTheme.DUSK,
            fontWeight = 1.5,
            darkImageTreatment = ReaderDarkImageTreatment.DARKEN
        ).toEpubPreferences()
        assertEquals(1.5, original.fontWeight!!, 0.0001)
        assertEquals(ImageFilter.DARKEN, original.imageFilter)

        val inverted = ReaderAppearance(
            theme = ReaderTheme.OLED,
            darkImageTreatment = ReaderDarkImageTreatment.INVERT
        ).toEpubPreferences()
        assertEquals(ImageFilter.INVERT, inverted.imageFilter)

        val none = ReaderAppearance(
            theme = ReaderTheme.DUSK,
            darkImageTreatment = ReaderDarkImageTreatment.NONE
        ).toEpubPreferences()
        assertEquals(null, none.imageFilter)
    }

    @Test
    fun `scroll mode dominates retained page-turn style in effective navigation mode`() {
        PageTurnStyle.entries.forEach { retainedStyle ->
            val appearance = ReaderAppearance(
                scroll = true,
                pageTurnStyle = retainedStyle
            )
            assertEquals(ReaderNavigationMode.SCROLL, appearance.navigationMode)
        }
    }

    @Test
    fun `normalization rejects non finite core typography before renderer submission`() {
        val unsafe = ReaderAppearance(
            fontScale = Double.NaN,
            lineHeight = Double.POSITIVE_INFINITY,
            pageMargins = Double.NEGATIVE_INFINITY,
            fontWeight = Double.NaN,
            paragraphSpacing = Double.POSITIVE_INFINITY,
            paragraphIndent = Double.NaN,
            letterSpacing = Double.NaN,
            wordSpacing = Double.NEGATIVE_INFINITY,
            typeScale = Double.POSITIVE_INFINITY,
            paperPatina = Double.NaN
        )

        val normalized = unsafe.normalized()
        assertEquals(1.0, normalized.fontScale, 0.0001)
        assertEquals(1.45, normalized.lineHeight, 0.0001)
        assertEquals(1.0, normalized.pageMargins, 0.0001)
        assertEquals(null, normalized.fontWeight)
        assertEquals(null, normalized.paragraphSpacing)
        assertEquals(null, normalized.paragraphIndent)
        assertEquals(null, normalized.letterSpacing)
        assertEquals(null, normalized.wordSpacing)
        assertEquals(null, normalized.typeScale)
        assertEquals(0.72, normalized.paperPatina, 0.0001)

        val prefs = unsafe.toEpubPreferences()
        assertEquals(1.0, requireNotNull(prefs.fontSize), 0.0001)
        assertEquals(1.45, requireNotNull(prefs.lineHeight), 0.0001)
        assertEquals(1.0, requireNotNull(prefs.pageMargins), 0.0001)
        assertEquals(null, prefs.fontWeight)
    }

    @Test
    fun `core typography mutators clamp and fail calm`() {
        val original = ReaderAppearance(publisherStyles = true)

        assertEquals(1.0, original.withFontScale(Double.NaN).fontScale, 0.0001)
        assertEquals(1.8, original.withFontScale(9.0).fontScale, 0.0001)
        assertEquals(1.45, original.withLineHeight(Double.NaN).lineHeight, 0.0001)
        assertEquals(2.0, original.withLineHeight(9.0).lineHeight, 0.0001)
        assertEquals(1.0, original.withPageMargins(Double.NaN).pageMargins, 0.0001)
        assertEquals(0.5, original.withPageMargins(-9.0).pageMargins, 0.0001)
        assertFalse(original.withFontScale(1.1).publisherStyles)
    }

    @Test
    fun `fixed layout disables renderer-owned typography and continuous scroll`() {
        val capabilities = readerAppearanceCapabilities(
            fixedLayout = true,
            languageTag = "en",
            continuousScroll = false
        )

        assertFalse(capabilities.typographyEditable)
        assertFalse(capabilities.continuousScrollEditable)
        assertFalse(capabilities.columnsEditable)
        assertFalse(capabilities.hyphenationEditable)
        assertFalse(capabilities.letterSpacingEditable)
        assertFalse(capabilities.wordSpacingEditable)
    }

    @Test
    fun `RTL publications keep script-safe spacing and hyphenation defaults`() {
        listOf("fa", "fa-IR", "ar", "ur-PK", "he").forEach { language ->
            val capabilities = readerAppearanceCapabilities(
                fixedLayout = false,
                languageTag = language,
                continuousScroll = false
            )
            assertTrue(capabilities.typographyEditable)
            assertFalse(capabilities.hyphenationEditable)
            assertFalse(capabilities.letterSpacingEditable)
            assertFalse(capabilities.wordSpacingEditable)
        }

        val ltr = readerAppearanceCapabilities(
            fixedLayout = false,
            languageTag = "en-US",
            continuousScroll = false
        )
        assertTrue(ltr.hyphenationEditable)
        assertTrue(ltr.letterSpacingEditable)
        assertTrue(ltr.wordSpacingEditable)
    }

    @Test
    fun `continuous scroll alone disables column count`() {
        val paged = readerAppearanceCapabilities(
            fixedLayout = false,
            languageTag = "en",
            continuousScroll = false
        )
        val scrolling = readerAppearanceCapabilities(
            fixedLayout = false,
            languageTag = "en",
            continuousScroll = true
        )

        assertTrue(paged.columnsEditable)
        assertFalse(scrolling.columnsEditable)
        assertTrue(scrolling.typographyEditable)
    }

    @Test
    fun `RTL language detection uses primary BCP 47 subtag`() {
        assertTrue(usesRtlReaderTypography("fa-IR"))
        assertTrue(usesRtlReaderTypography("CKB_IQ"))
        assertFalse(usesRtlReaderTypography("en-GB"))
        assertFalse(usesRtlReaderTypography(null))
    }

    @Test
    fun `fixed-layout runtime disables continuous scroll without destroying retained paged style`() {
        PageTurnStyle.entries.forEach { retainedStyle ->
            val requested = ReaderAppearance(
                scroll = true,
                pageTurnStyle = retainedStyle,
                fontScale = 1.25
            )
            val effective = effectiveReaderAppearanceForPublication(
                appearance = requested,
                fixedLayout = true
            )

            assertFalse(effective.scroll)
            assertEquals(retainedStyle, effective.pageTurnStyle)
            assertEquals(1.25, effective.fontScale, 0.0001)
        }
    }

    @Test
    fun `reflowable runtime preserves requested continuous scroll`() {
        val requested = ReaderAppearance(
            scroll = true,
            pageTurnStyle = PageTurnStyle.SLIDE
        )
        assertEquals(
            requested,
            effectiveReaderAppearanceForPublication(
                appearance = requested,
                fixedLayout = false
            )
        )
    }

    @Test
    fun `highlight palette stays distinct across sanctuary themes`() {
        val tints = ReaderTheme.entries.map(::readerHighlightTint)
        assertEquals(ReaderTheme.entries.size, tints.distinct().size)
        tints.forEach { tint ->
            assertEquals(0xFF, tint ushr 24)
        }
    }

}
