package com.veilreader.app.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderImageViewerTest {
    @Test
    fun `pan resets when image is not zoomed`() {
        assertEquals(
            Offset.Zero,
            clampReaderImagePan(
                requested = Offset(120f, -90f),
                scale = 1f,
                viewport = IntSize(1080, 1920)
            )
        )
    }

    @Test
    fun `pan is clamped to the scaled viewport`() {
        assertEquals(
            Offset(540f, -960f),
            clampReaderImagePan(
                requested = Offset(900f, -1400f),
                scale = 2f,
                viewport = IntSize(1080, 1920)
            )
        )
    }

    @Test
    fun `invalid viewport cannot retain stale pan`() {
        assertEquals(
            Offset.Zero,
            clampReaderImagePan(
                requested = Offset(100f, 100f),
                scale = 3f,
                viewport = IntSize.Zero
            )
        )
    }

    @Test
    fun `image sampling stays power of two and respects maximum dimension`() {
        assertEquals(1, readerImageSampleSize(1200, 1800, 4096))
        assertEquals(2, readerImageSampleSize(6000, 3000, 4096))
        assertEquals(4, readerImageSampleSize(12000, 9000, 4096))
    }

    @Test
    fun `image sampling guards invalid dimensions and tiny limits`() {
        assertEquals(1, readerImageSampleSize(0, 1000, 4096))
        assertEquals(2, readerImageSampleSize(1024, 1024, 128))
    }
}
