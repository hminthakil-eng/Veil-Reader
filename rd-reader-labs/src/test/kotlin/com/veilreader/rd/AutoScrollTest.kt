package com.veilreader.rd

import kotlin.test.Test
import kotlin.test.assertEquals

class AutoScrollTest {
    @Test fun pausedEngineNeverMoves() {
        val config = AutoScrollConfig(AutoScrollMode.PIXEL, 100.0, paused = true)
        assertEquals(0.0, AutoScrollEngine.delta(config, 1000, 1000.0))
    }

    @Test fun pageModeUsesViewportExtent() {
        val config = AutoScrollConfig(AutoScrollMode.PAGE, 1.0)
        assertEquals(500.0, AutoScrollEngine.delta(config, 500, 1000.0), 0.001)
    }
}
