package com.veilreader.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySearchTest {
    @Test
    fun `Persian keyboard finds titles typed with Arabic letter variants`() {
        val title = normalizeLibrarySearchText("تاريخ كهن")
        val query = normalizeLibrarySearchText("تاریخ کهن")

        assertEquals(query, title)
        assertTrue(title.contains(normalizeLibrarySearchText("كهن")))
    }

    @Test
    fun `optional marks do not hide a title`() {
        assertEquals(
            normalizeLibrarySearchText("کتاب"),
            normalizeLibrarySearchText("کِتاب")
        )
        assertEquals(
            normalizeLibrarySearchText("آرشیو"),
            normalizeLibrarySearchText("ارشیو")
        )
    }

    @Test
    fun `latin accents and casing do not hide an author`() {
        assertEquals(
            normalizeLibrarySearchText("cafe"),
            normalizeLibrarySearchText("CAFÉ")
        )
    }
}
