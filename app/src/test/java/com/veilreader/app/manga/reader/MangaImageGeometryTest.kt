package com.veilreader.app.manga.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaImageGeometryTest {

    @Test
    fun decodeNotReadyDimensionsReturnNullInsteadOfInvalidScale() {
        assertNull(
            MangaImageGeometryCalculator.calculate(
                sourceWidthPx = 0,
                sourceHeightPx = 0,
                viewportWidthPx = 1080,
                viewportHeightPx = 1920,
                fitMode = MangaImageFitMode.FIT_WIDTH
            )
        )
        assertNull(
            MangaImageGeometryCalculator.calculate(
                sourceWidthPx = 1200,
                sourceHeightPx = 1800,
                viewportWidthPx = 0,
                viewportHeightPx = 1920,
                fitMode = MangaImageFitMode.FIT_INSIDE
            )
        )
    }

    @Test
    fun veryTallWebtoonImageHasFinitePositiveHeightAtFitWidth() {
        val geometry = requireNotNull(
            MangaImageGeometryCalculator.calculate(
                sourceWidthPx = 1440,
                sourceHeightPx = 50_000,
                viewportWidthPx = 1080,
                viewportHeightPx = 2200,
                fitMode = MangaImageFitMode.FIT_WIDTH
            )
        )

        assertTrue(geometry.fitScale.isFinite())
        assertTrue(geometry.fitScale > 0.0)
        assertTrue(geometry.renderedHeightPx.isFinite())
        assertTrue(geometry.renderedHeightPx > geometry.viewportHeightPx)
        assertEquals(1080.0, geometry.renderedWidthPx, 0.0001)
    }

    @Test
    fun pagedFitInsideNeverOverflowsEitherViewportAxis() {
        val geometry = requireNotNull(
            MangaImageGeometryCalculator.calculate(
                sourceWidthPx = 2400,
                sourceHeightPx = 3600,
                viewportWidthPx = 1080,
                viewportHeightPx = 1920,
                fitMode = MangaImageFitMode.FIT_INSIDE
            )
        )

        assertTrue(geometry.renderedWidthPx <= 1080.0 + 0.001)
        assertTrue(geometry.renderedHeightPx <= 1920.0 + 0.001)
    }

    @Test
    fun extremeValidDimensionsStillProduceFiniteGeometry() {
        val geometry = requireNotNull(
            MangaImageGeometryCalculator.calculate(
                sourceWidthPx = 1,
                sourceHeightPx = Int.MAX_VALUE,
                viewportWidthPx = 1080,
                viewportHeightPx = 1920,
                fitMode = MangaImageFitMode.FIT_WIDTH
            )
        )

        assertTrue(geometry.renderedHeightPx.isFinite())
        assertTrue(geometry.renderedHeightPx > 0.0)
    }
}
