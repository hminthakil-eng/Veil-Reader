package com.veilreader.rd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReadingRulerTest {
    @Test fun focusWindowIsClampedInsideViewport() {
        val window = ReadingRulerGeometry.window(
            1000f,
            ReadingRulerConfig(ReadingRulerStyle.BAND, centerFraction = 1.5, heightFraction = 0.2)
        )
        assertTrue(window.topPx >= 0f)
        assertTrue(window.bottomPx <= 1000f)
        assertTrue(window.bottomPx >= window.topPx)
    }

    @Test fun zeroViewportProducesEmptyWindow() {
        assertEquals(FocusWindow(0f, 0f), ReadingRulerGeometry.window(0f, ReadingRulerConfig(ReadingRulerStyle.SINGLE_LINE)))
    }
}
