package com.veilreader.app.manga.reader.presentation

import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.InMemoryMangaOfflineCacheIndex
import com.veilreader.app.manga.library.MangaChapterAnchor
import com.veilreader.app.manga.library.OfflineChapterManifest
import com.veilreader.app.manga.library.OfflinePageEntry
import com.veilreader.app.manga.reader.MangaReaderChapterRef
import com.veilreader.app.manga.source.MangaContentType
import com.veilreader.app.manga.source.MangaPageImage
import com.veilreader.app.manga.source.MangaSourceCapability
import com.veilreader.app.manga.source.MangaSourceDescriptor
import com.veilreader.app.manga.source.MangaSourceProvider
import com.veilreader.app.manga.source.SourceChapter
import com.veilreader.app.manga.source.SourceExecutionCoordinator
import com.veilreader.app.manga.source.SourceFailure
import com.veilreader.app.manga.source.SourceFailureKind
import com.veilreader.app.manga.source.SourceId
import com.veilreader.app.manga.source.SourceOutcome
import com.veilreader.app.manga.source.SourceRequestContext
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaReaderChapterLoaderTest {

    @Test
    fun completeOfflineChapterReopensWithoutProvider() = runBlocking {
        val cache = InMemoryMangaOfflineCacheIndex()
        val request = requestWithoutNetwork()
        cache.put(
            manifest(
                request = request,
                completed = true,
                pages = listOf(localPage(0), localPage(1))
            )
        )

        val loader = loader(cache)
        val state = loader.load(request)

        val ready = (state as MangaReaderPresentationState.Ready).value
        assertEquals(MangaChapterAvailability.COMPLETE_OFFLINE, ready.availability)
        assertTrue(ready.complete)
        assertEquals(2, ready.pageCount)
        assertTrue(ready.pages.all { it is MangaPageAsset.Local })
    }

    @Test
    fun partialCachePlusOnlinePagesBecomesHybrid() = runBlocking {
        val cache = InMemoryMangaOfflineCacheIndex()
        val request = requestWithNetwork(
            pageOutcome = SourceOutcome.Success(
                listOf(
                    MangaPageImage(0, "https://example.test/0.jpg"),
                    MangaPageImage(1, "https://example.test/1.jpg")
                )
            )
        )
        cache.put(
            manifest(
                request = request,
                completed = false,
                pages = listOf(localPage(0))
            )
        )

        val ready = (loader(cache).load(request) as MangaReaderPresentationState.Ready).value

        assertEquals(MangaChapterAvailability.HYBRID, ready.availability)
        assertTrue(ready.complete)
        assertTrue(ready.page(0) is MangaPageAsset.Local)
        assertTrue(ready.page(1) is MangaPageAsset.Remote)
    }

    @Test
    fun networkFailureFallsBackToContiguousPartialOfflinePrefix() = runBlocking {
        val cache = InMemoryMangaOfflineCacheIndex()
        val request = requestWithNetwork(
            pageOutcome = SourceOutcome.Failure(
                SourceFailure(SourceFailureKind.NETWORK, "offline")
            )
        )
        cache.put(
            manifest(
                request = request,
                completed = false,
                pages = listOf(localPage(0), localPage(1))
            )
        )

        val ready = (loader(cache).load(request) as MangaReaderPresentationState.Ready).value

        assertEquals(MangaChapterAvailability.PARTIAL_OFFLINE, ready.availability)
        assertEquals(false, ready.complete)
        assertEquals(2, ready.pageCount)
    }

    @Test
    fun partialCacheThatDoesNotStartAtZeroIsNeverRenumbered() = runBlocking {
        val cache = InMemoryMangaOfflineCacheIndex()
        val request = requestWithNetwork(
            pageOutcome = SourceOutcome.Failure(
                SourceFailure(SourceFailureKind.NETWORK, "offline")
            )
        )
        cache.put(
            manifest(
                request = request,
                completed = false,
                pages = listOf(localPage(7), localPage(8))
            )
        )

        val state = loader(cache).load(request)

        assertTrue(state is MangaReaderPresentationState.Error)
        val error = (state as MangaReaderPresentationState.Error).error
        assertEquals(MangaPresentationErrorKind.SOURCE_FAILURE, error.kind)
    }

    @Test
    fun completedOfflineManifestWithGapFailsClosedWhenOfflineOnly() = runBlocking {
        val cache = InMemoryMangaOfflineCacheIndex()
        val request = requestWithoutNetwork()
        cache.put(
            manifest(
                request = request,
                completed = true,
                pages = listOf(localPage(0), localPage(2))
            )
        )

        val state = loader(cache).load(request)

        val error = (state as MangaReaderPresentationState.Error).error
        assertEquals(MangaPresentationErrorKind.INVALID_PAGE_SET, error.kind)
        assertEquals(false, error.retryable)
    }

    @Test
    fun invalidRemotePageSetFailsClosedWithoutPartialCache() = runBlocking {
        val cache = InMemoryMangaOfflineCacheIndex()
        val request = requestWithNetwork(
            pageOutcome = SourceOutcome.Success(
                listOf(
                    MangaPageImage(0, "https://example.test/0.jpg"),
                    MangaPageImage(2, "https://example.test/2.jpg")
                )
            )
        )

        val state = loader(cache).load(request)

        val error = (state as MangaReaderPresentationState.Error).error
        assertEquals(MangaPresentationErrorKind.INVALID_PAGE_SET, error.kind)
        assertEquals(true, error.retryable)
    }

    private fun loader(cache: InMemoryMangaOfflineCacheIndex) =
        MangaReaderChapterLoader(
            offlineIndex = cache,
            sourceExecution = SourceExecutionCoordinator(
                maxAttempts = 1,
                sleeper = {}
            )
        )

    private fun requestWithoutNetwork() = MangaChapterPresentationRequest(
        chapter = readerChapter()
    )

    private fun requestWithNetwork(
        pageOutcome: SourceOutcome<List<MangaPageImage>>
    ): MangaChapterPresentationRequest {
        val sourceId = SourceId("test.source")
        val chapter = SourceChapter(
            sourceId = sourceId,
            mangaKey = "work",
            chapterKey = "chapter-1",
            title = "Chapter 1",
            number = 1.0,
            languageTag = "en"
        )
        val provider = object : MangaSourceProvider {
            override val descriptor = MangaSourceDescriptor(
                id = sourceId,
                displayName = "Test",
                domains = listOf("example.test"),
                contentTypes = setOf(MangaContentType.MANGA)
            )
            override val capabilities = setOf(MangaSourceCapability.PAGES)

            override suspend fun pages(
                chapter: SourceChapter,
                context: SourceRequestContext
            ): SourceOutcome<List<MangaPageImage>> = pageOutcome
        }

        return MangaChapterPresentationRequest(
            chapter = readerChapter(),
            provider = provider,
            sourceChapter = chapter
        )
    }

    private fun readerChapter() = MangaReaderChapterRef(
        mangaId = CanonicalMangaId("canonical-work"),
        anchor = MangaChapterAnchor(
            number = 1.0,
            languageTag = "en",
            providerChapterKeyHint = "chapter-1"
        )
    )

    private fun manifest(
        request: MangaChapterPresentationRequest,
        completed: Boolean,
        pages: List<OfflinePageEntry>
    ) = OfflineChapterManifest(
        chapterId = request.offlineChapterId(),
        anchor = request.chapter.anchor,
        pages = pages,
        originSourceId = SourceId("test.source"),
        originChapterKey = "chapter-1",
        completed = completed,
        updatedAtEpochMs = 1L
    )

    private fun localPage(index: Int) = OfflinePageEntry(
        index = index,
        relativePath = "manga/canonical-work/en/vna/c1_0-main/page-" +
            index.toString().padStart(5, '0') + ".jpg",
        byteSize = 100L
    )
}
