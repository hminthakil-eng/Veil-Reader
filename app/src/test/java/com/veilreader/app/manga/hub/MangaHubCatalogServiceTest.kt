package com.veilreader.app.manga.hub

import com.veilreader.app.manga.library.CanonicalManga
import com.veilreader.app.manga.library.CanonicalMangaFactory
import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.InMemoryMangaCanonicalStore
import com.veilreader.app.manga.source.MangaContentType
import com.veilreader.app.manga.source.MangaPageImage
import com.veilreader.app.manga.source.MangaSourceCapability
import com.veilreader.app.manga.source.MangaSourceDescriptor
import com.veilreader.app.manga.source.MangaSourceProvider
import com.veilreader.app.manga.source.PagedSourceResult
import com.veilreader.app.manga.source.SourceChapter
import com.veilreader.app.manga.source.SourceExecutionCoordinator
import com.veilreader.app.manga.source.SourceFailure
import com.veilreader.app.manga.source.SourceFailureKind
import com.veilreader.app.manga.source.SourceId
import com.veilreader.app.manga.source.SourceMangaDetails
import com.veilreader.app.manga.source.SourceMangaRef
import com.veilreader.app.manga.source.SourceMangaSummary
import com.veilreader.app.manga.source.SourceOutcome
import com.veilreader.app.manga.source.SourceRegistry
import com.veilreader.app.manga.source.SourceRequestContext
import com.veilreader.app.manga.source.SourceSearchRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class MangaHubCatalogServiceTest {

    @Test
    fun addToLibraryIsIdempotentForStableSourceIdentity() = runBlocking {
        val provider = FakeProvider(
            id = "fixture.one",
            key = "same-work",
            title = "Same Work"
        )
        val store = InMemoryMangaCanonicalStore()
        var ids = 0
        val service = MangaHubCatalogService(
            sources = SourceRegistry(listOf(provider)),
            execution = SourceExecutionCoordinator(),
            library = store,
            canonicalFactory = CanonicalMangaFactory(
                idGenerator = { "canonical-" + (++ids) },
                clock = { 10L }
            )
        )
        val details = provider.detailsValue

        val first = service.addToLibrary(details)
        val second = service.addToLibrary(details)

        assertEquals(first.id, second.id)
        assertEquals(1, store.listWorks().size)
        assertEquals("canonical-1", first.id.value)
    }

    @Test
    fun searchNeverTitleMergesDifferentSourceIdentities() = runBlocking {
        val first = FakeProvider(
            id = "fixture.one",
            key = "work-a",
            title = "Shared Title"
        )
        val second = FakeProvider(
            id = "fixture.two",
            key = "work-b",
            title = "Shared Title"
        )
        val service = MangaHubCatalogService(
            sources = SourceRegistry(listOf(first, second)),
            execution = SourceExecutionCoordinator(),
            library = InMemoryMangaCanonicalStore()
        )

        val result = service.search("shared")

        assertEquals(2, result.items.size)
        assertEquals(
            setOf(SourceId("fixture.one"), SourceId("fixture.two")),
            result.items.map { it.summary.ref.sourceId }.toSet()
        )
    }

    @Test
    fun libraryOpenFallsBackToNextLinkedSourceAndPreservesProviderOrder() = runBlocking {
        val failing = FakeProvider(
            id = "fixture.one",
            key = "work",
            title = "Work",
            chapterOutcome = SourceOutcome.Failure(
                SourceFailure(
                    kind = SourceFailureKind.NOT_FOUND,
                    message = "gone"
                )
            )
        )
        val fallbackChapters = listOf(
            SourceChapter(
                sourceId = SourceId("fixture.two"),
                mangaKey = "work",
                chapterKey = "chapter-special",
                title = "Special",
                number = 2.0
            ),
            SourceChapter(
                sourceId = SourceId("fixture.two"),
                mangaKey = "work",
                chapterKey = "chapter-one",
                title = "One",
                number = 1.0
            )
        )
        val fallback = FakeProvider(
            id = "fixture.two",
            key = "work",
            title = "Work",
            chapterOutcome = SourceOutcome.Success(fallbackChapters)
        )
        val store = InMemoryMangaCanonicalStore()
        val firstRef = SourceMangaRef(SourceId("fixture.one"), "work")
        val secondRef = SourceMangaRef(SourceId("fixture.two"), "work")
        val canonical = CanonicalManga(
            id = CanonicalMangaId("canonical-work"),
            title = "Work",
            sourceRefs = mapOf(
                firstRef.sourceId to firstRef,
                secondRef.sourceId to secondRef
            ),
            createdAtEpochMs = 1L
        )
        store.saveWork(canonical)

        val service = MangaHubCatalogService(
            sources = SourceRegistry(listOf(failing, fallback)),
            execution = SourceExecutionCoordinator(maxAttempts = 1),
            library = store
        )

        val session = service.openLibraryWork(
            id = canonical.id,
            preferredSourceId = firstRef.sourceId
        )

        assertEquals(
            listOf("chapter-special", "chapter-one"),
            session.entriesInReadingOrder.map { it.route.sourceChapter.chapterKey }
        )
        assertEquals(
            SourceId("fixture.two"),
            session.entriesInReadingOrder.first().provider?.descriptor?.id
        )
    }

    @Test
    fun discoverMarksKnownProviderIdentityAsAlreadyInLibrary() = runBlocking {
        val provider = FakeProvider(
            id = "fixture.one",
            key = "known",
            title = "Known"
        )
        val store = InMemoryMangaCanonicalStore()
        val canonical = CanonicalManga(
            id = CanonicalMangaId("known-canonical"),
            title = "Known",
            sourceRefs = mapOf(
                provider.ref.sourceId to provider.ref
            ),
            createdAtEpochMs = 1L
        )
        store.saveWork(canonical)

        val service = MangaHubCatalogService(
            sources = SourceRegistry(listOf(provider)),
            execution = SourceExecutionCoordinator(),
            library = store,
            discoverySeed = MangaHubDiscoverySeed { listOf(provider.ref) }
        )

        val result = service.discover()

        assertEquals(1, result.items.size)
        assertEquals(canonical.id, result.items.single().canonicalId)
    }

    @Test
    fun cancellationAfterDurableAddIsNotReclassifiedAsUniqueIdentityRecovery() = runBlocking {
        val provider = FakeProvider("fixture.one", "work", "Work")
        val delegate = InMemoryMangaCanonicalStore()
        val cancellation = CancellationException("caller stopped after commit")
        var lookups = 0
        val store = object : com.veilreader.app.manga.library.MangaCanonicalStore by delegate {
            override suspend fun findWorkBySource(ref: SourceMangaRef): CanonicalManga? {
                lookups += 1
                return delegate.findWorkBySource(ref)
            }

            override suspend fun saveWork(manga: CanonicalManga) {
                delegate.saveWork(manga)
                throw cancellation
            }
        }
        val service = MangaHubCatalogService(
            sources = SourceRegistry(listOf(provider)),
            execution = SourceExecutionCoordinator(),
            library = store
        )
        var caught: CancellationException? = null
        try {
            service.addToLibrary(provider.detailsValue)
        } catch (error: CancellationException) {
            caught = error
        }
        assertSame(cancellation, caught)
        assertEquals(1, lookups)
        assertEquals(1, delegate.listWorks().size)
    }

    private class FakeProvider(
        id: String,
        key: String,
        title: String,
        private val chapterOutcome: SourceOutcome<List<SourceChapter>>? = null
    ) : MangaSourceProvider {

        private val sourceId = SourceId(id)
        val ref = SourceMangaRef(sourceId, key)
        private val summary = SourceMangaSummary(
            ref = ref,
            title = title,
            contentType = MangaContentType.MANGA
        )
        val detailsValue = SourceMangaDetails(summary = summary)

        override val descriptor = MangaSourceDescriptor(
            id = sourceId,
            displayName = id,
            domains = listOf(id + ".example"),
            contentTypes = setOf(MangaContentType.MANGA)
        )

        override val capabilities = setOf(
            MangaSourceCapability.SEARCH,
            MangaSourceCapability.DETAILS,
            MangaSourceCapability.CHAPTERS,
            MangaSourceCapability.PAGES
        )

        override suspend fun search(
            request: SourceSearchRequest,
            context: SourceRequestContext
        ): SourceOutcome<PagedSourceResult<SourceMangaSummary>> =
            SourceOutcome.Success(PagedSourceResult(listOf(summary)))

        override suspend fun details(
            manga: SourceMangaRef,
            context: SourceRequestContext
        ): SourceOutcome<SourceMangaDetails> =
            if (manga == ref) {
                SourceOutcome.Success(detailsValue)
            } else {
                SourceOutcome.Failure(
                    SourceFailure(SourceFailureKind.NOT_FOUND, "missing")
                )
            }

        override suspend fun chapters(
            manga: SourceMangaRef,
            context: SourceRequestContext
        ): SourceOutcome<List<SourceChapter>> =
            chapterOutcome ?: SourceOutcome.Success(
                listOf(
                    SourceChapter(
                        sourceId = sourceId,
                        mangaKey = ref.key,
                        chapterKey = "chapter-1",
                        number = 1.0
                    )
                )
            )

        override suspend fun pages(
            chapter: SourceChapter,
            context: SourceRequestContext
        ): SourceOutcome<List<MangaPageImage>> =
            SourceOutcome.Success(
                listOf(
                    MangaPageImage(
                        index = 0,
                        imageUrl = "https://" + context.domain + "/page.png"
                    )
                )
            )
    }
}
