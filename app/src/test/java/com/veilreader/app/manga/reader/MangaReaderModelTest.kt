package com.veilreader.app.manga.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaReaderModelTest {
    @Test
    fun preloadWindow_staysSmallAndClampedAtEdges() {
        assertEquals(listOf(0, 1, 2), MangaPrefetchWindow.indices(center = 0, total = 10))
        assertEquals(listOf(2, 3, 4, 5, 6), MangaPrefetchWindow.indices(center = 4, total = 10))
        assertEquals(listOf(7, 8, 9), MangaPrefetchWindow.indices(center = 9, total = 10))
    }

    @Test
    fun preloadWindow_handlesEmptyAndOutOfRangeCenters() {
        assertTrue(MangaPrefetchWindow.indices(center = 4, total = 0).isEmpty())
        assertEquals(listOf(0, 1, 2), MangaPrefetchWindow.indices(center = -20, total = 3))
        assertEquals(listOf(0, 1, 2), MangaPrefetchWindow.indices(center = 99, total = 3))
    }
}