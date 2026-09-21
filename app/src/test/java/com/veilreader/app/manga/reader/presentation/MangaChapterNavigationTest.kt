package com.veilreader.app.manga.reader.presentation

import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.MangaChapterAnchor
import com.veilreader.app.manga.reader.MangaReaderBoundary
import com.veilreader.app.manga.reader.MangaReaderChapterRef
import com.veilreader.app.manga.reader.ui.MangaReaderUiEffect
import com.veilreader.app.manga.source.SourceChapter
import com.veilreader.app.manga.source.SourceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaChapterNavigationTest {

    @Test
    fun completeChapterNavigatesPreviousAndNextInExplicitReadingOrder() {
        val routes = routes()
        val resolver = MangaChapterNavigationResolver(routes)
        val current = ready(routes[1].readerChapter, complete = true)

        val previous = resolver.resolve(current, MangaReaderBoundary.PREVIOUS_CHAPTER)
        val next = resolver.resolve(current, MangaReaderBoundary.NEXT_CHAPTER)

        assertEquals(routes[0], (previous as MangaChapterNavigationResult.Target).route)
        assertEquals(routes[2], (next as MangaChapterNavigationResult.Target).route)
    }

    @Test
    fun partialOfflineNeverCrossesChapterBoundaryAutomatically() {
        val routes = routes()
        val resolver = MangaChapterNavigationResolver(routes)
        val current = ready(routes[1].readerChapter, complete = false)

        val result = resolver.resolve(current, MangaReaderBoundary.NEXT_CHAPTER)

        assertTrue(result is MangaChapterNavigationResult.BlockedByPartialOffline)
    }

    @Test
    fun providerKeyChangeStillFindsSameLogicalCurrentChapter() {
        val routes = routes()
        val replacementCurrent = MangaReaderChapterRef(
            mangaId = routes[1].readerChapter.mangaId,
            anchor = MangaChapterAnchor(
                number = 2.0,
                languageTag = "en",
                providerChapterKeyHint = "replacement-source-key"
            )
        )
        val resolver = MangaChapterNavigationResolver(routes)

        val result = resolver.resolve(
            ready(replacementCurrent, complete = true),
            MangaReaderBoundary.NEXT_CHAPTER
        )

        assertEquals(
            routes[2],
            (result as MangaChapterNavigationResult.Target).route
        )
    }

    @Test
    fun coordinatorTranslatesBoundaryIntoNavigationEffect() {
        val routes = routes()
        val coordinator = MangaReaderPresentationCoordinator(
            MangaChapterNavigationResolver(routes)
        )

        val effects = coordinator.onUiEffect(
            presentation = ready(routes[1].readerChapter, complete = true),
            effect = MangaReaderUiEffect.ChapterBoundaryRequested(
                MangaReaderBoundary.NEXT_CHAPTER
            )
        )

        val navigate = effects.single() as
            MangaReaderPresentationEffect.NavigateToChapter
        assertEquals(routes[2], navigate.route)
    }

    @Test
    fun coordinatorBlocksPartialOfflineBoundary() {
        val routes = routes()
        val coordinator = MangaReaderPresentationCoordinator(
            MangaChapterNavigationResolver(routes)
        )

        val effects = coordinator.onUiEffect(
            presentation = ready(routes[1].readerChapter, complete = false),
            effect = MangaReaderUiEffect.ChapterBoundaryRequested(
                MangaReaderBoundary.NEXT_CHAPTER
            )
        )

        assertEquals(
            listOf(MangaReaderPresentationEffect.PartialOfflineBoundaryBlocked),
            effects
        )
    }

    private fun routes(): List<MangaChapterRoute> =
        (1..3).map { number ->
            val sourceChapter = SourceChapter(
                sourceId = SourceId("nav.source"),
                mangaKey = "work",
                chapterKey = "ch-$number",
                title = "Chapter $number",
                number = number.toDouble(),
                languageTag = "en"
            )
            MangaChapterRoute.from(
                mangaId = CanonicalMangaId("work"),
                sourceChapter = sourceChapter
            )
        }

    private fun ready(
        chapter: MangaReaderChapterRef,
        complete: Boolean
    ) = MangaReadyPresentation(
        chapter = chapter,
        pages = listOf(
            MangaPageAsset.Local(
                index = 0,
                relativePath = "manga/work/en/vna/c1-main/page-00000.jpg",
                byteSize = 10
            )
        ),
        availability = if (complete) {
            MangaChapterAvailability.COMPLETE_OFFLINE
        } else {
            MangaChapterAvailability.PARTIAL_OFFLINE
        },
        complete = complete
    )
}
