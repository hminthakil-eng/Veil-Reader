package com.veilreader.app.manga.core

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaHubTest {
    private val sourceId = MangaSourceId("test.en")

    @Test
    fun verticalSlice_searchDetailsChaptersPages_routesThroughOneSource() = runBlocking {
        val hub = MangaHub(MangaSourceRegistry(listOf(FakeSource())))

        val manga = hub.search(sourceId, " veil ").items.single()
        val details = hub.details(manga.ref)
        val chapter = hub.chapters(manga.ref).single()
        val pages = hub.pages(chapter.ref)

        assertEquals("Veil Knight", details.title)
        assertEquals(1.0, chapter.chapterNumber ?: 0.0, 0.0)
        assertEquals(listOf(0, 1), pages.map { it.index })
        assertEquals("https://cdn.example.test/1.jpg", pages.first().image.url)
        assertEquals("https://example.test/", pages.first().image.headers["Referer"])
    }

    @Test
    fun combinedUpdate_fetchesDetailsAndChaptersInOneProviderCall() = runBlocking {
        val source = FakeSource()
        val hub = MangaHub(MangaSourceRegistry(listOf(source)))
        val ref = MangaRef(sourceId, "veil-knight")

        val update = hub.update(ref)

        assertEquals(1, source.updateCalls)
        assertEquals("Veil Knight", update.details?.title)
        assertEquals(1, update.chapters?.size)
    }

    @Test
    fun providerIdentity_isStableAcrossLanguageScopedSourceInstances() {
        val descriptor = FakeSource().descriptor

        assertEquals("test", descriptor.providerId.value)
        assertEquals("test.en", descriptor.id.value)
        assertEquals(1, descriptor.version)
    }

    @Test
    fun registry_rejectsDuplicateSourceIds() {
        val error = runCatching {
            MangaSourceRegistry(listOf(FakeSource(), FakeSource()))
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun hub_rejectsCrossSourceResults() = runBlocking {
        val wrong = object : MangaSourceProvider {
            override val descriptor = FakeSource().descriptor

            override suspend fun search(request: MangaSearchRequest) = MangaResultPage(
                items = listOf(
                    MangaSummary(
                        MangaRef(MangaSourceId("other.en"), "m1"),
                        "Wrong source"
                    )
                )
            )

            override suspend fun fetchUpdate(
                ref: MangaRef,
                existingChapters: List<MangaChapter>,
                options: MangaUpdateOptions
            ) = MangaUpdate(ref = ref)

            override suspend fun pages(ref: MangaChapterRef) = emptyList<MangaPage>()
        }

        val error = runCatching {
            MangaHub(MangaSourceRegistry(listOf(wrong))).search(sourceId, "x")
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    private inner class FakeSource : MangaSourceProvider {
        override val descriptor = MangaSourceDescriptor(
            id = sourceId,
            providerId = MangaProviderId("test"),
            version = 1,
            name = "Test Source",
            language = "en",
            capabilities = setOf(
                MangaSourceCapability.SEARCH,
                MangaSourceCapability.POPULAR
            )
        )

        private val mangaRef = MangaRef(sourceId, "veil-knight")
        private val chapterRef = MangaChapterRef(mangaRef, "chapter-1")
        var updateCalls: Int = 0
            private set

        override suspend fun search(request: MangaSearchRequest) =
            MangaResultPage(listOf(MangaSummary(mangaRef, "Veil Knight")))

        override suspend fun popular(request: MangaBrowseRequest) =
            MangaResultPage(listOf(MangaSummary(mangaRef, "Veil Knight")))

        override suspend fun fetchUpdate(
            ref: MangaRef,
            existingChapters: List<MangaChapter>,
            options: MangaUpdateOptions
        ): MangaUpdate {
            updateCalls += 1
            return MangaUpdate(
                ref = ref,
                details = if (options.fetchDetails) {
                    MangaDetails(ref = ref, title = "Veil Knight")
                } else {
                    null
                },
                chapters = if (options.fetchChapters) {
                    listOf(MangaChapter(ref = chapterRef, chapterNumber = 1.0))
                } else {
                    null
                }
            )
        }

        override suspend fun pages(ref: MangaChapterRef) = listOf(
            MangaPage(1, MangaResourceRequest("https://cdn.example.test/2.jpg")),
            MangaPage(
                0,
                MangaResourceRequest(
                    "https://cdn.example.test/1.jpg",
                    headers = mapOf("Referer" to "https://example.test/")
                )
            )
        )
    }
}