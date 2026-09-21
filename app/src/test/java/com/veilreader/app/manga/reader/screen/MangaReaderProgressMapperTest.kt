package com.veilreader.app.manga.reader.screen

import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.MangaChapterAnchor
import com.veilreader.app.manga.library.MangaReadingProgress
import com.veilreader.app.manga.reader.MangaReaderChapterRef
import com.veilreader.app.manga.reader.MangaReaderController
import com.veilreader.app.manga.reader.MangaReaderMode
import org.junit.Assert.assertEquals
import org.junit.Test

class MangaReaderProgressMapperTest {

    private val chapter = MangaReaderChapterRef(
        mangaId = CanonicalMangaId("work"),
        anchor = MangaChapterAnchor(number = 1.0, languageTag = "en")
    )

    @Test
    fun pagedProgressionMapsAcrossChangedPageCount() {
        val controller = MangaReaderController()
        val reader = controller.initial(
            chapter = chapter,
            pageCount = 10,
            initialItemIndex = 5
        )
        val progress = MangaReaderProgressMapper.toProgress(reader, 10L)

        assertEquals(5.0 / 9.0, progress.chapterProgression, 0.0001)
        assertEquals(10, MangaReaderProgressMapper.remappedItemIndex(progress, 20))
    }

    @Test
    fun samePageCountPreservesExactPageIndex() {
        val progress = MangaReadingProgress(
            mangaId = chapter.mangaId,
            chapter = chapter.anchor,
            pageIndex = 7,
            pageCount = 10,
            chapterProgression = 0.1,
            updatedAtEpochMs = 1L
        )

        assertEquals(7, MangaReaderProgressMapper.remappedItemIndex(progress, 10))
    }

    @Test
    fun webtoonProgressIncludesVisibleItemOffset() {
        val controller = MangaReaderController()
        var reader = controller.initial(
            chapter = chapter,
            mode = MangaReaderMode.WEBTOON,
            pageCount = 10,
            initialItemIndex = 4
        )
        reader = controller.updateWebtoonPosition(
            reader,
            itemIndex = 4,
            offsetFraction = 0.5
        )

        val progress = MangaReaderProgressMapper.toProgress(reader, 10L)

        assertEquals(0.45, progress.chapterProgression, 0.0001)
        assertEquals(4, progress.pageIndex)
    }

    @Test
    fun moveToItemClampsAndResetsStaleZoom() {
        val controller = MangaReaderController()
        var reader = controller.initial(
            chapter = chapter,
            pageCount = 5,
            initialItemIndex = 2
        )
        reader = controller.updateZoom(reader, 3.0, 0.2, 0.7)

        val moved = controller.moveToItem(reader, 99)

        assertEquals(4, moved.position.itemIndex)
        assertEquals(1.0, moved.zoom.scale, 0.0001)
    }
}
