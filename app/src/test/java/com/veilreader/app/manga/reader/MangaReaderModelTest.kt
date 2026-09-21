package com.veilreader.app.manga.reader

import com.veilreader.app.manga.core.MangaChapterRef
import com.veilreader.app.manga.core.MangaOfflineStore
import com.veilreader.app.manga.core.MangaPage
import com.veilreader.app.manga.core.MangaRef
import com.veilreader.app.manga.core.MangaResourceRequest
import com.veilreader.app.manga.core.MangaSourceId
import kotlinx.coroutines.runBlocking
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
    @Test
    fun preloadNextChapter_startsOnlyNearChapterEnd() {
        assertTrue(!shouldPreloadNextChapter(pageIndex = 0, totalPages = 20))
        assertTrue(!shouldPreloadNextChapter(pageIndex = 16, totalPages = 20))
        assertTrue(shouldPreloadNextChapter(pageIndex = 17, totalPages = 20))
        assertTrue(shouldPreloadNextChapter(pageIndex = 19, totalPages = 20))
    }

    @Test
    fun readerPreferences_keepLayoutAndDirectionIndependent() {
        val preferences = MangaReaderPreferences(
            layout = MangaReaderLayout.WEBTOON,
            direction = MangaReadingDirection.RIGHT_TO_LEFT,
            imageFit = MangaImageFit.WIDTH,
            pageLayout = MangaPageLayout.AUTO_DUAL,
            cropBorders = true,
            pageGapDp = 8,
            dataSaver = true
        )

        assertEquals(MangaReaderLayout.WEBTOON, preferences.layout)
        assertEquals(MangaReadingDirection.RIGHT_TO_LEFT, preferences.direction)
        assertEquals(MangaImageFit.WIDTH, preferences.imageFit)
        assertEquals(MangaPageLayout.AUTO_DUAL, preferences.pageLayout)
    }

    @Test
    fun loadingState_clearsPreviousChapterPagesButRetainsSameChapterOnRetry() {
        val page = MangaPage(
            index = 0,
            image = MangaResourceRequest("https://example.invalid/page.jpg")
        )
        val current = MangaReaderUiState(
            bookId = "book",
            chapterId = "chapter-old",
            pages = listOf(page),
            pageIndex = 0,
            previousChapterId = "previous",
            nextChapterId = "next"
        )

        val switched = loadingMangaReaderState(
            current = current,
            bookId = "book",
            chapterId = "chapter-new",
            preferences = current.preferences
        )
        assertTrue(switched.pages.isEmpty())
        assertEquals(0, switched.pageIndex)
        assertEquals(null, switched.previousChapterId)
        assertEquals(null, switched.nextChapterId)

        val retry = loadingMangaReaderState(
            current = current,
            bookId = "book",
            chapterId = "chapter-old",
            preferences = current.preferences
        )
        assertEquals(current.pages, retry.pages)
        assertEquals(current.nextChapterId, retry.nextChapterId)
    }

    @Test
    fun pageResolver_prefersOfflineChapterBeforeRemote() = runBlocking {
        val manga = MangaRef(MangaSourceId("test"), "series")
        val ref = MangaChapterRef(manga, "chapter")
        val offlinePages = listOf(
            MangaPage(0, MangaResourceRequest("file:///offline/00000.img"))
        )
        var remoteCalls = 0
        val store = object : MangaOfflineStore {
            override suspend fun isChapterAvailable(ref: MangaChapterRef): Boolean = true
            override suspend fun loadChapter(ref: MangaChapterRef): List<MangaPage>? = offlinePages
            override suspend fun saveChapter(ref: MangaChapterRef, pages: List<MangaPage>) = Unit
            override suspend fun removeChapter(ref: MangaChapterRef) = Unit
        }

        val pages = resolveMangaChapterPages(
            ref = ref,
            cached = null,
            offlineStore = store
        ) {
            remoteCalls += 1
            listOf(MangaPage(0, MangaResourceRequest("https://example.invalid/remote.jpg")))
        }

        assertEquals(offlinePages, pages)
        assertEquals(0, remoteCalls)
    }

    @Test
    fun pageResolver_fallsBackToRemoteWhenOfflineChapterIsMissing() = runBlocking {
        val manga = MangaRef(MangaSourceId("test"), "series")
        val ref = MangaChapterRef(manga, "chapter")
        val remotePages = listOf(
            MangaPage(0, MangaResourceRequest("https://example.invalid/remote.jpg"))
        )
        var remoteCalls = 0
        val store = object : MangaOfflineStore {
            override suspend fun isChapterAvailable(ref: MangaChapterRef): Boolean = false
            override suspend fun loadChapter(ref: MangaChapterRef): List<MangaPage>? = null
            override suspend fun saveChapter(ref: MangaChapterRef, pages: List<MangaPage>) = Unit
            override suspend fun removeChapter(ref: MangaChapterRef) = Unit
        }

        val pages = resolveMangaChapterPages(
            ref = ref,
            cached = null,
            offlineStore = store
        ) {
            remoteCalls += 1
            remotePages
        }

        assertEquals(remotePages, pages)
        assertEquals(1, remoteCalls)
    }

    @Test
    fun layoutModeMapping_keepsVerticalLayoutsOutOfHorizontalLegacyMode() {
        assertEquals(MangaReaderMode.PAGED, MangaReaderLayout.PAGED.toScreenMode())
        assertEquals(MangaReaderMode.PAGED, MangaReaderLayout.VERTICAL_PAGER.toScreenMode())
        assertEquals(MangaReaderMode.WEBTOON, MangaReaderLayout.WEBTOON.toScreenMode())
        assertEquals(MangaReaderMode.WEBTOON, MangaReaderLayout.CONTINUOUS_VERTICAL.toScreenMode())
    }

    @Test
    fun enumPreferenceDecode_fallsBackForMissingOrUnknownValues() {
        assertEquals(
            MangaReaderLayout.PAGED,
            enumValueOrDefault<MangaReaderLayout>(null, MangaReaderLayout.PAGED)
        )
        assertEquals(
            MangaReadingDirection.RIGHT_TO_LEFT,
            enumValueOrDefault("NOT_A_DIRECTION", MangaReadingDirection.RIGHT_TO_LEFT)
        )
        assertEquals(
            MangaImageFit.WIDTH,
            enumValueOrDefault("WIDTH", MangaImageFit.SCREEN)
        )
    }

    @Test
    fun loadingState_marksAdjacentTransitionBeforeAsyncLoad() {
        val current = MangaReaderUiState(
            bookId = "book",
            chapterId = "chapter-a",
            pages = listOf(
                MangaPage(
                    index = 0,
                    image = MangaResourceRequest("https://example.invalid/a.jpg")
                )
            ),
            previousChapterId = "chapter-prev",
            nextChapterId = "chapter-b"
        )

        val transition = loadingMangaReaderState(
            current = current,
            bookId = "book",
            chapterId = "chapter-b",
            preferences = current.preferences
        )

        assertTrue(transition.loading)
        assertEquals("chapter-b", transition.chapterId)
        assertTrue(transition.pages.isEmpty())
        assertEquals(null, transition.previousChapterId)
        assertEquals(null, transition.nextChapterId)
    }

    @Test(expected = com.veilreader.app.manga.core.MangaSourceException.NotFound::class)
    fun pageValidation_rejectsEmptyChapter() {
        validateMangaChapterPages(emptyList())
    }

    @Test(expected = com.veilreader.app.manga.core.MangaSourceException.ParseFailure::class)
    fun pageValidation_rejectsDuplicatePageIndices() {
        validateMangaChapterPages(
            listOf(
                MangaPage(0, MangaResourceRequest("https://example.invalid/a.jpg")),
                MangaPage(0, MangaResourceRequest("https://example.invalid/b.jpg"))
            )
        )
    }

    @Test
    fun offlineOnlyResume_doesNotTouchFailingRemoteSource() = runBlocking {
        val manga = MangaRef(MangaSourceId("test"), "series")
        val ref = MangaChapterRef(manga, "offline-chapter")
        val offlinePages = listOf(
            MangaPage(0, MangaResourceRequest("file:///offline/00000.img")),
            MangaPage(1, MangaResourceRequest("file:///offline/00001.img"))
        )
        var remoteCalls = 0
        val store = object : MangaOfflineStore {
            override suspend fun isChapterAvailable(ref: MangaChapterRef): Boolean = true
            override suspend fun loadChapter(ref: MangaChapterRef): List<MangaPage>? = offlinePages
            override suspend fun saveChapter(ref: MangaChapterRef, pages: List<MangaPage>) = Unit
            override suspend fun removeChapter(ref: MangaChapterRef) = Unit
        }

        val pages = validateMangaChapterPages(
            resolveMangaChapterPages(
                ref = ref,
                cached = null,
                offlineStore = store
            ) {
                remoteCalls += 1
                throw com.veilreader.app.manga.core.MangaSourceException.NetworkFailure()
            }
        )

        assertEquals(offlinePages, pages)
        assertEquals(0, remoteCalls)
    }

    @Test
    fun chapterWindow_usesCurrentOrderAfterReorder() {
        val first = mangaChapterWindow(
            orderedChapterIds = listOf("a", "b", "c"),
            currentChapterId = "b"
        )
        assertEquals("a", first.previousChapterId)
        assertEquals("c", first.nextChapterId)

        val reordered = mangaChapterWindow(
            orderedChapterIds = listOf("c", "b", "a"),
            currentChapterId = "b"
        )
        assertEquals("c", reordered.previousChapterId)
        assertEquals("a", reordered.nextChapterId)
    }

    @Test
    fun chapterWindow_handlesFirstAndLastBoundaries() {
        val first = mangaChapterWindow(
            orderedChapterIds = listOf("a", "b"),
            currentChapterId = "a"
        )
        assertEquals(null, first.previousChapterId)
        assertEquals("b", first.nextChapterId)

        val last = mangaChapterWindow(
            orderedChapterIds = listOf("a", "b"),
            currentChapterId = "b"
        )
        assertEquals("a", last.previousChapterId)
        assertEquals(null, last.nextChapterId)
    }

}