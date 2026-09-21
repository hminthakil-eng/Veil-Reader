package com.veilreader.app.manga.reader

import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.MangaChapterAnchor
import org.junit.Assert.assertEquals
import org.junit.Test

class MangaReaderRestoreTest {

    private val controller = MangaReaderController(minZoom = 1.0, maxZoom = 5.0)

    @Test
    fun processDeathRestoreClampsToNewPageCountAndKeepsStableSettings() {
        val chapter = chapter("work", 10.0)
        var state = controller.initial(
            chapter = chapter,
            mode = MangaReaderMode.PAGED,
            direction = MangaPageDirection.RIGHT_TO_LEFT,
            orientationPolicy = MangaOrientationPolicy.PORTRAIT,
            pageCount = 20,
            initialItemIndex = 18
        )
        state = controller.updateZoom(state, 3.5, 0.25, 0.75)
        val snapshot = controller.snapshot(state)

        val restored = controller.restore(
            snapshot = snapshot,
            currentChapter = chapter,
            currentPageCount = 12
        )

        assertEquals(11, restored.position.itemIndex)
        assertEquals(MangaPageDirection.RIGHT_TO_LEFT, restored.direction)
        assertEquals(MangaOrientationPolicy.PORTRAIT, restored.orientationPolicy)
        assertEquals(3.5, restored.zoom.scale, 0.0001)
        assertEquals(0.25, restored.zoom.centerXFraction, 0.0001)
    }

    @Test
    fun webtoonRestorePreservesItemOffsetAndZoom() {
        val chapter = chapter("work", 4.0)
        var state = controller.initial(
            chapter = chapter,
            mode = MangaReaderMode.WEBTOON,
            pageCount = 30,
            initialItemIndex = 12
        )
        state = controller.updateWebtoonPosition(state, 12, 0.73)
        state = controller.updateZoom(state, 2.2, 0.4, 0.6)

        val restored = controller.restore(
            controller.snapshot(state),
            currentChapter = chapter,
            currentPageCount = 30
        )

        val position = restored.position as MangaReaderPosition.Webtoon
        assertEquals(12, position.itemIndex)
        assertEquals(0.73, position.offsetFraction, 0.0001)
        assertEquals(2.2, restored.zoom.scale, 0.0001)
    }

    @Test
    fun snapshotFromDifferentChapterNeverRestoresForeignPosition() {
        val oldChapter = chapter("work", 4.0)
        val newChapter = chapter("work", 5.0)
        val oldState = controller.initial(
            chapter = oldChapter,
            pageCount = 40,
            initialItemIndex = 30
        )

        val restored = controller.restore(
            controller.snapshot(oldState),
            currentChapter = newChapter,
            currentPageCount = 8
        )

        assertEquals(0, restored.position.itemIndex)
        assertEquals(newChapter, restored.chapter)
    }

    @Test
    fun pageCountArrivalAfterDecodeClampsExistingUnknownPosition() {
        val chapter = chapter("work", 7.0)
        val unknownCount = controller.initial(
            chapter = chapter,
            mode = MangaReaderMode.PAGED,
            pageCount = null,
            initialItemIndex = 50
        )

        val decoded = controller.withPageCount(unknownCount, 6)

        assertEquals(5, decoded.position.itemIndex)
        assertEquals(6, decoded.pageCount)
    }

    private fun chapter(work: String, number: Double) = MangaReaderChapterRef(
        mangaId = CanonicalMangaId(work),
        anchor = MangaChapterAnchor(number = number, languageTag = "en")
    )
}
