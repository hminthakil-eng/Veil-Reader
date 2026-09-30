package com.veilreader.app.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderImageTransformTest {
    private val viewport = IntSize(1000, 800)

    private fun assertPan(expected: Offset, actual: Offset) {
        assertEquals(expected.x, actual.x, 0.001f)
        assertEquals(expected.y, actual.y, 0.001f)
    }

    @Test fun fitSizeCannotPan() {
        assertPan(Offset.Zero, clampReaderImagePan(Offset(500f, -500f), 1f, viewport))
    }

    @Test fun wideIllustrationDoesNotPanVerticallyInsideLetterbox() {
        assertPan(
            Offset(500f, 0f),
            clampReaderImagePan(Offset(999f, 999f), 2f, viewport, IntSize(2000, 400))
        )
    }

    @Test fun tallIllustrationDoesNotPanHorizontallyInsideLetterbox() {
        assertPan(
            Offset(0f, -400f),
            clampReaderImagePan(Offset(999f, -999f), 2f, viewport, IntSize(400, 2000))
        )
    }

    @Test fun zoomKeepsOffCenterImagePointUnderFinger() {
        assertPan(
            Offset(-200f, -100f),
            readerImagePanAfterZoom(
                pan = Offset.Zero, currentScale = 1f, nextScale = 2f,
                centroid = Offset(700f, 500f), gesturePan = Offset.Zero,
                viewport = viewport, imageSize = viewport
            )
        )
    }

    @Test fun zoomPreservesExistingTranslationAndIncludesGesturePan() {
        assertPan(
            Offset(-45f, 75f),
            readerImagePanAfterZoom(
                pan = Offset(100f, 50f), currentScale = 2f, nextScale = 4f,
                centroid = Offset(750f, 450f), gesturePan = Offset(5f, 25f),
                viewport = viewport, imageSize = viewport
            )
        )
    }

    @Test fun returningToFitClearsPan() {
        assertPan(
            Offset.Zero,
            readerImagePanAfterZoom(
                pan = Offset(200f, -100f), currentScale = 4f, nextScale = 1f,
                centroid = Offset(800f, 500f), gesturePan = Offset(20f, 20f),
                viewport = viewport, imageSize = viewport
            )
        )
    }

    @Test fun zoomIsClampedToFiveTimes() {
        assertPan(
            Offset(2000f, -1600f),
            clampReaderImagePan(Offset(10000f, -10000f), 100f, viewport)
        )
    }

    @Test fun malformedGeometryDoesNotProduceNonFiniteTranslation() {
        assertPan(Offset.Zero, clampReaderImagePan(Offset.Zero, Float.NaN, viewport))
        assertPan(Offset.Zero, clampReaderImagePan(Offset.Zero, 2f, IntSize.Zero))
        assertPan(
            Offset.Zero,
            clampReaderImagePan(Offset(Float.NaN, Float.POSITIVE_INFINITY), 2f, viewport)
        )
        assertPan(
            Offset.Zero,
            readerImagePanAfterZoom(
                pan = Offset.Zero, currentScale = 0f, nextScale = 2f,
                centroid = Offset.Zero, gesturePan = Offset.Zero,
                viewport = viewport, imageSize = viewport
            )
        )
    }

    @Test fun hugeImagesKeepDecodeSampleBounded() {
        assertEquals(4, readerImageSampleSize(12000, 8000))
        assertEquals(4194304, readerImageSampleSize(Int.MAX_VALUE, Int.MAX_VALUE, 512))
    }
}

