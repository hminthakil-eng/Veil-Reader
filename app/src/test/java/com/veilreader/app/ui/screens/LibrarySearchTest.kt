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


    @Test
    fun `search relevance prefers exact prefix and contains within title`() {
        val exact = normalizedLibrarySearchDocument(
            title = "Dune",
            author = "Frank Herbert",
            series = null,
            language = "en",
            collections = emptyList()
        )
        val prefix = normalizedLibrarySearchDocument(
            title = "Dune Messiah",
            author = "Frank Herbert",
            series = null,
            language = "en",
            collections = emptyList()
        )
        val contains = normalizedLibrarySearchDocument(
            title = "The Dune Archive",
            author = "Frank Herbert",
            series = null,
            language = "en",
            collections = emptyList()
        )
        val query = normalizeLibrarySearchText("dune")

        val exactScore = requireNotNull(librarySearchRelevance(query, exact))
        val prefixScore = requireNotNull(librarySearchRelevance(query, prefix))
        val containsScore = requireNotNull(librarySearchRelevance(query, contains))

        assertTrue(exactScore > prefixScore)
        assertTrue(prefixScore > containsScore)
    }

    @Test
    fun `title match outranks author series collection and language matches`() {
        val query = normalizeLibrarySearchText("veil")

        fun score(
            title: String = "Other",
            author: String = "Other",
            series: String? = null,
            language: String? = null,
            collections: List<String> = emptyList()
        ) = requireNotNull(
            librarySearchRelevance(
                query,
                normalizedLibrarySearchDocument(
                    title = title,
                    author = author,
                    series = series,
                    language = language,
                    collections = collections
                )
            )
        )

        val title = score(title = "The Veil")
        val author = score(author = "Veil")
        val series = score(series = "Veil")
        val collection = score(collections = listOf("Veil"))
        val language = score(language = "veil")

        assertTrue(title > author)
        assertTrue(author > series)
        assertTrue(series > collection)
        assertTrue(collection > language)
    }

    @Test
    fun `incidental author exact match cannot outrank title contains match`() {
        val query = normalizeLibrarySearchText("ring")
        val titleContains = normalizedLibrarySearchDocument(
            title = "The Lord of the Rings",
            author = "Tolkien",
            series = null,
            language = "en",
            collections = emptyList()
        )
        val authorExact = normalizedLibrarySearchDocument(
            title = "Another Book",
            author = "Ring",
            series = null,
            language = "en",
            collections = emptyList()
        )

        assertTrue(
            requireNotNull(librarySearchRelevance(query, titleContains)) >
                requireNotNull(librarySearchRelevance(query, authorExact))
        )
    }

    @Test
    fun `search relevance returns null for a true non match`() {
        val document = normalizedLibrarySearchDocument(
            title = "Dune",
            author = "Frank Herbert",
            series = "Dune",
            language = "en",
            collections = listOf("Science Fiction")
        )

        assertEquals(
            null,
            librarySearchRelevance(
                normalizeLibrarySearchText("tolkien"),
                document
            )
        )
    }
}
