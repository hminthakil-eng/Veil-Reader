package com.veilreader.app.data

import com.veilreader.app.domain.BookFormat
import com.veilreader.app.manga.core.MangaDetails
import com.veilreader.app.manga.core.MangaProviderId
import com.veilreader.app.manga.core.MangaRef
import com.veilreader.app.manga.core.MangaSourceDescriptor
import com.veilreader.app.manga.core.MangaSourceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MangaLibraryBookModelTest {
    private val source = MangaSourceDescriptor(
        id = MangaSourceId("mangadex.en"),
        name = "MangaDex",
        language = "en",
        providerId = MangaProviderId("mangadex")
    )

    @Test
    fun sourceBackedBook_hasStableIdAndComicFormatAcrossMetadataRefresh() {
        val ref = MangaRef(source.id, "series-123")
        val first = mangaSourceBackedBook(
            MangaDetails(
                ref = ref,
                title = "First title",
                authors = listOf("Author A")
            ),
            source
        )
        val refreshed = mangaSourceBackedBook(
            MangaDetails(
                ref = ref,
                title = "Updated title",
                authors = listOf("Author A", "Author B")
            ),
            source
        )

        assertEquals(first.id, refreshed.id)
        assertEquals(BookFormat.COMIC, first.format)
        assertEquals(null, first.sourceUri)
        assertEquals("Manga", first.collection)
        assertEquals("Updated title", refreshed.title)
    }

    @Test
    fun sourceBackedBook_separatesDifferentRemoteKeys() {
        val first = mangaSourceBackedBook(
            MangaDetails(MangaRef(source.id, "series-a"), "A"),
            source
        )
        val second = mangaSourceBackedBook(
            MangaDetails(MangaRef(source.id, "series-b"), "B"),
            source
        )

        assertNotEquals(first.id, second.id)
    }
}
