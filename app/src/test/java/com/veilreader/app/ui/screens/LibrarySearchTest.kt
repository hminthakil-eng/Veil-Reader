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

    @Test
    fun `half space and ordinary space do not hide Persian titles`() {
        val stored = normalizeLibrarySearchText("می‌روم")
        assertEquals(stored, normalizeLibrarySearchText("میروم"))
        assertEquals(stored, normalizeLibrarySearchText("می روم"))
        assertTrue(normalizeLibrarySearchText("کتاب‌های کهن").contains(normalizeLibrarySearchText("کتابهای")))
    }

    @Test
    fun `Persian Arabic and Latin digits match in titles and queries`() {
        val stored = normalizeLibrarySearchText("جلد ۱۲")
        assertEquals(stored, normalizeLibrarySearchText("جلد ١٢"))
        assertEquals(stored, normalizeLibrarySearchText("جلد12"))
    }

    @Test
    fun `localized decimal metadata accepts Persian Arabic and comma keyboards`() {
        assertEquals(12.5, parseLocalizedDecimalInput("۱۲٫۵")!!, 0.0001)
        assertEquals(12.5, parseLocalizedDecimalInput("١٢٫٥")!!, 0.0001)
        assertEquals(12.5, parseLocalizedDecimalInput("12,5")!!, 0.0001)
        assertEquals(-2.0, parseLocalizedDecimalInput("−۲")!!, 0.0001)
    }

    @Test
    fun `localized decimal metadata rejects malformed and non finite values`() {
        assertEquals(null, parseLocalizedDecimalInput("۱۲٫۵٫۲"))
        assertEquals(null, parseLocalizedDecimalInput("NaN"))
        assertEquals(null, parseLocalizedDecimalInput("Infinity"))
    }

}
