package com.veilreader.app.manga.reader.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MangaReaderViewportMathTest {

    @Test
    fun zeroOrUnknownItemSizeNeverProducesInvalidOffset() {
        assertEquals(
            0.0,
            MangaReaderViewportMath.webtoonOffsetFraction(100, 0),
            0.0001
        )
    }

    @Test
    fun webtoonOffsetAndProgressionAreClamped() {
        assertEquals(
            0.5,
            MangaReaderViewportMath.webtoonOffsetFraction(500, 1000),
            0.0001
        )
        assertEquals(
            1.0,
            MangaReaderViewportMath.webtoonOffsetFraction(5000, 1000),
            0.0001
        )

        assertEquals(
            0.75,
            MangaReaderViewportMath.progression(
                itemIndex = 7,
                offsetFraction = 0.5,
                itemCount = 10
            ),
            0.0001
        )
        assertEquals(
            0.0,
            MangaReaderViewportMath.progression(3, 0.4, 0),
            0.0001
        )
    }
}
