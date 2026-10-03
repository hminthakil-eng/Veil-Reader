package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderTapZonesTest {
    @Test
    fun `3x3 geometry resolves every zone deterministically`() {
        val width = 900f
        val height = 1200f
        val points = listOf(
            Triple(150f, 200f, ReaderTapZone.TOP_LEFT),
            Triple(450f, 200f, ReaderTapZone.TOP_CENTER),
            Triple(750f, 200f, ReaderTapZone.TOP_RIGHT),
            Triple(150f, 600f, ReaderTapZone.MIDDLE_LEFT),
            Triple(450f, 600f, ReaderTapZone.MIDDLE_CENTER),
            Triple(750f, 600f, ReaderTapZone.MIDDLE_RIGHT),
            Triple(150f, 1000f, ReaderTapZone.BOTTOM_LEFT),
            Triple(450f, 1000f, ReaderTapZone.BOTTOM_CENTER),
            Triple(750f, 1000f, ReaderTapZone.BOTTOM_RIGHT)
        )

        points.forEach { (x, y, expected) ->
            assertEquals(expected, readerTapZoneAt(x, y, width, height))
        }
    }

    @Test
    fun `invalid geometry fails calm`() {
        assertNull(readerTapZoneAt(10f, 10f, 0f, 100f))
        assertNull(readerTapZoneAt(-1f, 10f, 100f, 100f))
        assertNull(readerTapZoneAt(10f, Float.NaN, 100f, 100f))
    }

    @Test
    fun `tap grid codec preserves all nine actions`() {
        var grid = ReaderTapGrid()
        grid = grid
            .withAction(ReaderTapZone.TOP_LEFT, ReaderTapAction.RENDERER)
            .withAction(ReaderTapZone.MIDDLE_CENTER, ReaderTapAction.NEXT_PAGE)
            .withAction(ReaderTapZone.BOTTOM_RIGHT, ReaderTapAction.TOGGLE_CONTROLS)

        assertEquals(grid, decodeReaderTapGrid(encodeReaderTapGrid(grid)))
    }

    @Test
    fun `malformed codec input falls back safely`() {
        assertEquals(ReaderTapGrid(), decodeReaderTapGrid(null))
        assertEquals(ReaderTapGrid(), decodeReaderTapGrid("v1:PREVIOUS_PAGE"))

        val mixed = decodeReaderTapGrid(
            "v1:BOGUS,TOGGLE_CONTROLS,NEXT_PAGE,PREVIOUS_PAGE,TOGGLE_CONTROLS,NEXT_PAGE,PREVIOUS_PAGE,TOGGLE_CONTROLS,NEXT_PAGE"
        )
        assertEquals(
            ReaderTapAction.PREVIOUS_PAGE,
            mixed[ReaderTapZone.TOP_LEFT]
        )
        assertEquals(
            ReaderTapAction.TOGGLE_CONTROLS,
            mixed[ReaderTapZone.TOP_CENTER]
        )
    }
}
