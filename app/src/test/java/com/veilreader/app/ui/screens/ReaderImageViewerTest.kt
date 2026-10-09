package com.veilreader.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderImageViewerTest {
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
