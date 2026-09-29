package com.veilreader.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class BookCoverSampleSizeTest {
    @Test
    fun `shelf cover avoids decoding publication sized bitmap`() {
        assertEquals(16, bookCoverSampleSize(4000, 6000, 200, 300))
    }

    @Test
    fun `large detail cover retains enough pixels for display`() {
        assertEquals(2, bookCoverSampleSize(4000, 6000, 1200, 1800))
    }

    @Test
    fun `extreme aspect ratio stays within a memory budget`() {
        assertEquals(4, bookCoverSampleSize(30000, 1000, 200, 300))
    }

    @Test
    fun `unmeasured cover keeps safe default sample`() {
        assertEquals(1, bookCoverSampleSize(4000, 6000, 0, 0))
    }
}
