package com.veilreader.app.ui.screens

import com.veilreader.app.domain.ReaderBrightness
import com.veilreader.app.domain.ReaderBrightnessMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderBrightnessPolicyTest {

    @Test
    fun `system mode has no window override`() {
        assertNull(
            ReaderBrightness(
                mode = ReaderBrightnessMode.SYSTEM,
                level = 0.2
            ).toWindowBrightnessOverride()
        )
    }

    @Test
    fun `override mode clamps and normalizes brightness`() {
        assertEquals(
            0.42f,
            ReaderBrightness(
                mode = ReaderBrightnessMode.OVERRIDE,
                level = 0.42
            ).toWindowBrightnessOverride(),
            0.0001f
        )
        assertEquals(
            ReaderBrightness.MIN_LEVEL.toFloat(),
            ReaderBrightness(
                mode = ReaderBrightnessMode.OVERRIDE,
                level = -5.0
            ).toWindowBrightnessOverride(),
            0.0001f
        )
        assertEquals(
            ReaderBrightness.DEFAULT_LEVEL.toFloat(),
            ReaderBrightness(
                mode = ReaderBrightnessMode.OVERRIDE,
                level = Double.NaN
            ).toWindowBrightnessOverride(),
            0.0001f
        )
    }
}
