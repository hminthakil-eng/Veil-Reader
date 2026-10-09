package com.veilreader.app.ui.screens

import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderColumnMode
import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveTypesettingPolicyTest {
    private fun resolve(
        appearance: ReaderAppearance = ReaderAppearance(),
        width: Double = 412.0,
        systemScale: Double = 1.0,
        format: BookFormat = BookFormat.EPUB,
        fixed: Boolean = false
    ) = AdaptiveTypesettingPolicy.resolve(appearance, format, fixed, width, systemScale)

    @Test fun phoneAndSplitWindowUseOneColumnEvenWhenTwoWereRequested() {
        val requested = ReaderAppearance(columnMode = ReaderColumnMode.TWO)
        assertEquals(ReaderColumnMode.ONE, resolve(requested).columnMode)
        assertEquals(ReaderColumnMode.TWO, requested.columnMode)
        assertEquals(ReaderColumnMode.TWO, resolve(requested, width = 800.0).columnMode)
    }

    @Test fun autoAdaptsToWidthAndLargeReaderFonts() {
        assertEquals(ReaderColumnMode.TWO, resolve(width = 800.0).columnMode)
        assertEquals(ReaderColumnMode.ONE, resolve(ReaderAppearance(fontScale = 1.8), 800.0).columnMode)
    }

    @Test fun accessibilityLargeTextKeepsTabletInOneColumn() {
        assertEquals(ReaderColumnMode.ONE, resolve(width = 800.0, systemScale = 1.5).columnMode)
    }

    @Test fun explicitSingleColumnAndScrollNeverBecomeTwoColumns() {
        assertEquals(ReaderColumnMode.ONE, resolve(ReaderAppearance(columnMode = ReaderColumnMode.ONE), 1600.0).columnMode)
        assertEquals(ReaderColumnMode.ONE, resolve(ReaderAppearance(scroll = true), 1600.0).columnMode)
    }

    @Test fun fixedLayoutAndPdfRemainRendererOwned() {
        val requested = ReaderAppearance(columnMode = ReaderColumnMode.TWO)
        assertEquals(requested, resolve(requested, fixed = true))
        assertEquals(requested, resolve(requested, format = BookFormat.PDF))
    }

    @Test fun invalidViewportIsConservativeAndInvalidScaleIsSafe() {
        assertEquals(ReaderColumnMode.ONE, resolve(width = Double.NaN).columnMode)
        assertEquals(ReaderColumnMode.TWO, resolve(width = 800.0, systemScale = Double.NaN).columnMode)
    }
}
