package com.veilreader.app.ui.screens

import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.readium.r2.shared.ExperimentalReadiumApi

@OptIn(ExperimentalReadiumApi::class)
class ReaderFontSizeContractTest {
    @Test
    fun submittedFontSizeUsesReadiumRatioInsteadOfPercent() {
        for (scale in listOf(0.75, 1.0, 1.4, 1.8)) {
            val appearance = ReaderAppearance(theme = ReaderTheme.PAPER, fontScale = scale)
            assertEquals(scale, requireNotNull(appearance.toEpubPreferences().fontSize), 0.0001)
        }
    }

    @Test
    fun explicitThemeSelectionProducesReadiumColors() {
        for (theme in ReaderTheme.entries) {
            val appearance = ReaderAppearance(publisherStyles = true).withTheme(theme)
            val preferences = appearance.toEpubPreferences()

            assertFalse(appearance.publisherStyles)
            assertNotNull(preferences.backgroundColor)
            assertNotNull(preferences.textColor)
        }
    }

    @Test
    fun invalidAndOutOfRangeScalesProduceUsablePreferences() {
        val cases = listOf(
            Double.NaN to 1.0,
            Double.POSITIVE_INFINITY to 1.0,
            Double.NEGATIVE_INFINITY to 1.0,
            0.1 to 0.75,
            20.0 to 1.8
        )
        for ((input, expected) in cases) {
            val appearance = ReaderAppearance(theme = ReaderTheme.PAPER, fontScale = input)
            assertEquals(expected, requireNotNull(appearance.toEpubPreferences().fontSize), 0.0001)
        }
    }
}
