package com.veilreader.app.data.manga

import com.veilreader.app.data.db.MangaChapterSourceEntity
import com.veilreader.app.manga.source.SourceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomMangaSessionSourceSelectionTest {

    @Test
    fun `invalid first link cannot starve a later valid offline route`() {
        val selected = selectMangaReaderSourceLink(
            sourceLinks = listOf(
                link("BAD SOURCE", "broken"),
                link("valid.source", "good")
            ),
            canLoadPages = { false }
        )

        assertEquals("valid.source", selected?.sourceId?.value)
        assertEquals("good", selected?.link?.chapterKey)
    }

    @Test
    fun `live page provider wins over an earlier valid offline route`() {
        val selected = selectMangaReaderSourceLink(
            sourceLinks = listOf(
                link("offline.source", "offline"),
                link("live.source", "live")
            ),
            canLoadPages = { it == SourceId("live.source") }
        )

        assertEquals("live.source", selected?.sourceId?.value)
        assertEquals("live", selected?.link?.chapterKey)
    }

    @Test
    fun `first valid route remains offline fallback when no provider can load pages`() {
        val selected = selectMangaReaderSourceLink(
            sourceLinks = listOf(
                link("first.source", "first"),
                link("second.source", "second")
            ),
            canLoadPages = { false }
        )

        assertEquals("first.source", selected?.sourceId?.value)
    }

    @Test
    fun `all malformed source ids remain unavailable`() {
        val selected = selectMangaReaderSourceLink(
            sourceLinks = listOf(
                link("!", "one"),
                link("UPPERCASE", "two")
            ),
            canLoadPages = { true }
        )

        assertNull(selected)
    }

    private fun link(sourceId: String, chapterKey: String) =
        MangaChapterSourceEntity(
            chapterId = "chapter-1",
            bookId = "book-1",
            sourceId = sourceId,
            mangaKey = "work",
            chapterKey = chapterKey
        )
}
