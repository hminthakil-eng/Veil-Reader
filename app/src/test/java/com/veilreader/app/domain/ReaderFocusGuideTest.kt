package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderFocusGuideTest {
    @Test
    fun `focus guide defaults to fully disabled`() {
        assertEquals(ReaderFocusGuideMode.OFF, ReaderFocusGuideSettings().mode)
        assertNull(readerFocusGuideBand(1000f, ReaderFocusGuideSettings()))
    }

    @Test
    fun `malformed focus guide values normalize into safe bounds`() {
        val normalized = ReaderFocusGuideSettings(
            mode = ReaderFocusGuideMode.WINDOW,
            verticalPosition = Double.NaN,
            bandFraction = 99.0,
            dimStrength = Double.POSITIVE_INFINITY
        ).normalized()

        assertEquals(0.50, normalized.verticalPosition, 0.0001)
        assertEquals(0.36, normalized.bandFraction, 0.0001)
        assertEquals(0.30, normalized.dimStrength, 0.0001)
    }

    @Test
    fun `window band remains inside viewport at extreme positions`() {
        val top = readerFocusGuideBand(
            viewportHeightPx = 1000f,
            settings = ReaderFocusGuideSettings(
                mode = ReaderFocusGuideMode.WINDOW,
                verticalPosition = 0.20,
                bandFraction = 0.36
            )
        )!!
        val bottom = readerFocusGuideBand(
            viewportHeightPx = 1000f,
            settings = ReaderFocusGuideSettings(
                mode = ReaderFocusGuideMode.WINDOW,
                verticalPosition = 0.80,
                bandFraction = 0.36
            )
        )!!

        assertEquals(20f, top.top, 0.001f)
        assertEquals(380f, top.bottom, 0.001f)
        assertEquals(620f, bottom.top, 0.001f)
        assertEquals(980f, bottom.bottom, 0.001f)
    }

    @Test
    fun `line mode caps the focus band without changing saved window height`() {
        val settings = ReaderFocusGuideSettings(
            mode = ReaderFocusGuideMode.LINE,
            verticalPosition = 0.50,
            bandFraction = 0.30
        )
        val band = readerFocusGuideBand(1000f, settings)!!

        assertEquals(440f, band.top, 0.001f)
        assertEquals(560f, band.bottom, 0.001f)
        assertEquals(0.30, settings.bandFraction, 0.0001)
    }

    @Test
    fun `quick toggle preserves tuning and restores prior active mode`() {
        val tuned = ReaderFocusGuideSettings(
            mode = ReaderFocusGuideMode.LINE,
            verticalPosition = 0.62,
            bandFraction = 0.22,
            dimStrength = 0.41
        ).normalized()
        val off = tuned.toggled()
        val on = off.toggled()

        assertEquals(ReaderFocusGuideMode.OFF, off.mode)
        assertEquals(ReaderFocusGuideMode.LINE, off.lastActiveMode)
        assertEquals(0.62, off.verticalPosition, 0.0001)
        assertEquals(ReaderFocusGuideMode.LINE, on.mode)
        assertEquals(0.22, on.bandFraction, 0.0001)
        assertEquals(0.41, on.dimStrength, 0.0001)
    }

    @Test
    fun `toggle repairs an unnormalized invalid resume mode on its first press`() {
        val toggled = ReaderFocusGuideSettings(
            lastActiveMode = ReaderFocusGuideMode.OFF,
            verticalPosition = Double.NaN
        ).toggled()
        assertEquals(ReaderFocusGuideMode.WINDOW, toggled.mode)
        assertEquals(0.50, toggled.verticalPosition, 0.0)
    }

    @Test
    fun `invalid resume mode fails calm to window`() {
        val normalized = ReaderFocusGuideSettings(
            mode = ReaderFocusGuideMode.OFF,
            lastActiveMode = ReaderFocusGuideMode.OFF
        ).normalized()

        assertEquals(ReaderFocusGuideMode.WINDOW, normalized.lastActiveMode)
        assertEquals(ReaderFocusGuideMode.WINDOW, normalized.toggled().mode)
    }
}
