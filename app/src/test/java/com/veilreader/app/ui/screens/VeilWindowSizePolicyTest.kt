package com.veilreader.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class VeilWindowSizePolicyTest {

    @Test
    fun `live app window wins over physical screen fallback`() {
        val size = resolveVeilWindowSizeDp(
            windowWidthDp = 684f,
            windowHeightDp = 912f,
            fallbackWidthDp = 1280,
            fallbackHeightDp = 1600
        )

        assertEquals(684f, size.width, 0f)
        assertEquals(912, size.height)
    }

    @Test
    fun `zero or non finite bootstrap window falls back deterministically`() {
        val zero = resolveVeilWindowSizeDp(
            windowWidthDp = 0f,
            windowHeightDp = 0f,
            fallbackWidthDp = 412,
            fallbackHeightDp = 915
        )
        assertEquals(412f, zero.width, 0f)
        assertEquals(915, zero.height)

        val invalid = resolveVeilWindowSizeDp(
            windowWidthDp = Float.NaN,
            windowHeightDp = Float.POSITIVE_INFINITY,
            fallbackWidthDp = 600,
            fallbackHeightDp = 900
        )
        assertEquals(600f, invalid.width, 0f)
        assertEquals(900, invalid.height)
    }

    @Test
    fun `negative fallback dimensions fail calm to zero`() {
        val size = resolveVeilWindowSizeDp(
            windowWidthDp = -1f,
            windowHeightDp = -1f,
            fallbackWidthDp = -400,
            fallbackHeightDp = -800
        )
        assertEquals(0f, size.width, 0f)
        assertEquals(0, size.height)
    }
}
