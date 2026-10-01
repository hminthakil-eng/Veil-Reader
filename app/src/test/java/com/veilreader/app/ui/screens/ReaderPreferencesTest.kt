package com.veilreader.app.ui.screens

import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.PageTurnStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Fit

class ReaderPreferencesTest {
    @Test
    fun pdfLayoutRoundTrip_preservesEveryEpubTurnStyleAndUsesOnlyPdfNavigation() {
        PageTurnStyle.entries.forEach { style ->
            val original = ReaderAppearance(scroll = false, pageTurnStyle = style)
            // PDF controls change layout only; decoration remains an EPUB preference.
            val continuous = original.copy(scroll = true)
            val continuousPreferences = continuous.toPdfiumPreferences()
            assertEquals(style, continuous.pageTurnStyle)
            assertEquals(true, continuousPreferences.scroll)
            assertEquals(Fit.WIDTH, continuousPreferences.fit)
            assertEquals(Axis.VERTICAL, continuousPreferences.scrollAxis)

            val paged = continuous.copy(scroll = false)
            val pagedPreferences = paged.toPdfiumPreferences()
            assertEquals(original, paged)
            assertEquals(false, pagedPreferences.scroll)
            assertEquals(Fit.CONTAIN, pagedPreferences.fit)
            assertNull(pagedPreferences.scrollAxis)
        }
    }

    @Test
    fun pdfPaginatedMode_usesContainFitAndHorizontalPageSnappingPolicy() {
        val prefs = ReaderAppearance(scroll = false).toPdfiumPreferences()

        assertEquals(false, prefs.scroll)
        assertEquals(Fit.CONTAIN, prefs.fit)
        assertNull(prefs.scrollAxis)
        assertTrue((prefs.pageSpacing ?: 0.0) > 0.0)
    }

    @Test
    fun pdfScrollMode_usesWidthFitAndVerticalAxis() {
        val prefs = ReaderAppearance(scroll = true).toPdfiumPreferences()

        assertEquals(true, prefs.scroll)
        assertEquals(Fit.WIDTH, prefs.fit)
        assertEquals(Axis.VERTICAL, prefs.scrollAxis)
    }

}
