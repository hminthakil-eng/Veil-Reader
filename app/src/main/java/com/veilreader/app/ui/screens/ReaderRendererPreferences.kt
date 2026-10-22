package com.veilreader.app.ui.screens

import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderColumnMode
import com.veilreader.app.domain.ReaderDarkImageTreatment
import com.veilreader.app.domain.ReaderFontFamily
import com.veilreader.app.domain.ReaderFixedLayoutSpread
import com.veilreader.app.domain.ReaderPreferenceToggle
import com.veilreader.app.domain.ReaderTextAlignment
import com.veilreader.app.domain.ReaderTheme
import org.readium.adapter.pdfium.navigator.PdfiumPreferences
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.ColumnCount
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.ImageFilter
import org.readium.r2.navigator.preferences.Color as ReadiumColor
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.Spread
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.navigator.preferences.TextAlign as ReadiumTextAlign
import org.readium.r2.shared.ExperimentalReadiumApi

internal fun shouldAwaitReaderAppearanceClose(
    rendererPreferencesSettling: Boolean,
    presented: ReaderAppearance,
    expected: ReaderAppearance?
): Boolean =
    rendererPreferencesSettling ||
        (expected != null && presented != expected)

internal fun readerPreferencesNeedSubmission(
    previousPresented: ReaderAppearance,
    previousAccepted: ReaderAppearance,
    previousSpread: ReaderFixedLayoutSpread,
    requestedPresented: ReaderAppearance,
    requestedSource: ReaderAppearance,
    requestedSpread: ReaderFixedLayoutSpread
): Boolean =
    previousPresented != requestedPresented ||
        previousAccepted != requestedSource ||
        previousSpread != requestedSpread

// Compare what Readium receives, rather than Veil-only animation/material options.
@OptIn(ExperimentalReadiumApi::class)
internal fun readerRendererPreferencesChanged(
    format: BookFormat,
    previous: ReaderAppearance,
    requested: ReaderAppearance,
    previousSpread: ReaderFixedLayoutSpread,
    requestedSpread: ReaderFixedLayoutSpread
): Boolean = when (format) {
    BookFormat.EPUB -> previous.toEpubPreferences(previousSpread) !=
        requested.toEpubPreferences(requestedSpread)
    BookFormat.PDF -> previous.toPdfiumPreferences() != requested.toPdfiumPreferences()
    else -> false
}

@OptIn(ExperimentalReadiumApi::class)
internal fun ReaderAppearance.toEpubPreferences(
    fixedLayoutSpread: ReaderFixedLayoutSpread = ReaderFixedLayoutSpread.AUTO
): EpubPreferences {
    val safe = normalized()
    val colors = if (safe.publisherStyles) null else readiumThemeColors(safe.theme)
    return EpubPreferences(
        spread = fixedLayoutSpread.toReadiumSpread(),
        theme = when (safe.theme) {
            ReaderTheme.PAPER -> Theme.LIGHT
            ReaderTheme.SEPIA -> Theme.SEPIA
            ReaderTheme.DUSK, ReaderTheme.OLED -> Theme.DARK
        },
        imageFilter = when (safe.darkImageTreatment) {
            ReaderDarkImageTreatment.NONE -> null
            ReaderDarkImageTreatment.DARKEN -> ImageFilter.DARKEN
            ReaderDarkImageTreatment.INVERT -> ImageFilter.INVERT
        },
        backgroundColor = colors?.first?.let(::ReadiumColor),
        textColor = colors?.second?.let(::ReadiumColor),
        fontFamily = when (safe.fontFamily) {
            ReaderFontFamily.PUBLISHER -> null
            ReaderFontFamily.SERIF -> FontFamily.SERIF
            ReaderFontFamily.SANS_SERIF -> FontFamily.SANS_SERIF
            ReaderFontFamily.MONOSPACE -> FontFamily.MONOSPACE
            ReaderFontFamily.OPEN_DYSLEXIC -> FontFamily.OPEN_DYSLEXIC
            ReaderFontFamily.ACCESSIBLE_DFA -> FontFamily.ACCESSIBLE_DFA
            ReaderFontFamily.IA_WRITER_DUOSPACE -> FontFamily.IA_WRITER_DUOSPACE
        },
        fontSize = readiumFontSizeRatio(safe.fontScale),
        fontWeight = safe.fontWeight,
        lineHeight = safe.lineHeight,
        pageMargins = safe.pageMargins,
        paragraphSpacing = safe.paragraphSpacing,
        paragraphIndent = safe.paragraphIndent,
        letterSpacing = safe.letterSpacing,
        wordSpacing = safe.wordSpacing,
        typeScale = safe.typeScale,
        textAlign = when (safe.textAlignment) {
            ReaderTextAlignment.PUBLISHER -> null
            ReaderTextAlignment.START -> ReadiumTextAlign.START
            ReaderTextAlignment.JUSTIFY -> ReadiumTextAlign.JUSTIFY
            ReaderTextAlignment.CENTER -> ReadiumTextAlign.CENTER
        },
        columnCount = when (safe.columnMode) {
            ReaderColumnMode.AUTO -> ColumnCount.AUTO
            ReaderColumnMode.ONE -> ColumnCount.ONE
            ReaderColumnMode.TWO -> ColumnCount.TWO
        },
        hyphens = safe.hyphenation.toNullableBoolean(),
        ligatures = safe.ligatures.toNullableBoolean(),
        textNormalization = safe.textNormalization.toNullableBoolean(),
        scroll = safe.scroll,
        publisherStyles = safe.publisherStyles
    )
}

private fun ReaderPreferenceToggle.toNullableBoolean(): Boolean? =
    when (this) {
        ReaderPreferenceToggle.DEFAULT -> null
        ReaderPreferenceToggle.ON -> true
        ReaderPreferenceToggle.OFF -> false
    }

internal fun readiumFontSizeRatio(scale: Double): Double =
    (if (scale.isFinite()) scale else 1.0).coerceIn(0.75, 1.8)

internal fun ReaderFixedLayoutSpread.toReadiumSpread(): Spread? =
    when (this) {
        ReaderFixedLayoutSpread.AUTO -> null
        ReaderFixedLayoutSpread.SINGLE -> Spread.NEVER
        ReaderFixedLayoutSpread.DUAL -> Spread.ALWAYS
    }

internal fun readerHighlightTint(theme: ReaderTheme): Int =
    when (theme) {
        ReaderTheme.PAPER -> 0xFFB58A34.toInt()
        ReaderTheme.SEPIA -> 0xFFA8732E.toInt()
        ReaderTheme.DUSK -> 0xFFC7A253.toInt()
        ReaderTheme.OLED -> 0xFFD1B15B.toInt()
    }

internal fun readiumThemeColors(theme: ReaderTheme): Pair<Int, Int> = when (theme) {
    ReaderTheme.PAPER -> 0xFFE9DEC5.toInt() to 0xFF2A251F.toInt()
    ReaderTheme.SEPIA -> 0xFFE2D0AA.toInt() to 0xFF362E24.toInt()
    ReaderTheme.DUSK -> 0xFF18151D.toInt() to 0xFFF5F0F7.toInt()
    ReaderTheme.OLED -> 0xFF000000.toInt() to 0xFFF5F0F7.toInt()
}

@OptIn(ExperimentalReadiumApi::class)
internal fun ReaderAppearance.toPdfiumPreferences(): PdfiumPreferences = PdfiumPreferences(
    fit = if (scroll) Fit.WIDTH else Fit.CONTAIN,
    pageSpacing = if (scroll) 12.0 else 6.0,
    scroll = scroll,
    scrollAxis = if (scroll) Axis.VERTICAL else null
)
