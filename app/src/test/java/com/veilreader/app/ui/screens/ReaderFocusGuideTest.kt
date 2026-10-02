package com.veilreader.app.ui.screens

import com.veilreader.app.domain.FocusGuideStyle
import com.veilreader.app.domain.ReaderAppearance
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderFocusGuideTest {
    @Test
    fun focusGuideClampsUnsafeValues() {
        val appearance = ReaderAppearance().withFocusGuide(
            style = FocusGuideStyle.RULER,
            strength = 9.0,
            height = -1.0
        )

        assertEquals(FocusGuideStyle.RULER, appearance.focusGuideStyle)
        assertEquals(0.80, appearance.focusGuideStrength, 0.0001)
        assertEquals(0.08, appearance.focusGuideHeight, 0.0001)
    }

    @Test
    fun focusGuideFallsBackFromNonFiniteValues() {
        val appearance = ReaderAppearance().withFocusGuide(
            style = FocusGuideStyle.WINDOW,
            strength = Double.NaN,
            height = Double.POSITIVE_INFINITY
        )

        assertEquals(0.42, appearance.focusGuideStrength, 0.0001)
        assertEquals(0.18, appearance.focusGuideHeight, 0.0001)
    }
}
