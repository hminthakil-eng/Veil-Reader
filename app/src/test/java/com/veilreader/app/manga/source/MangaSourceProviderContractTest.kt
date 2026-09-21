package com.veilreader.app.manga.source

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract proof for the first complete Source SDK vertical slice.
 *
 * This provider is intentionally deterministic and local: it proves search -> details -> chapters ->
 * pages behavior without coupling CI to a live manga website.
 */
class MangaSourceProviderContractTest {

    @Test
    fun fixtureProviderCompletesSearchDetailsChaptersPagesFlow() = runBlocking {
        val provider = FixtureProvider()
        val coordinator = SourceExecutionCoordinator(sleeper = {})

        val searchResult = coordinator.search(provider, SourceSearchRequest("veil"))
        val search = (
            searchResult.outcome as SourceOutcome.Success<PagedSourceResult<SourceMangaSummary>>
        ).value
        assertEquals(1, search.items.size)

        val manga = search.items.single()
        assertEquals("The Veiled Library", manga.title)
        assertEquals(SourceId("fixture.reference"), manga.ref.sourceId)

        val detailsResult = coordinator.details(provider, manga.ref)
        val details = (
            detailsResult.outcome as SourceOutcome.Success<SourceMangaDetails>
        ).value
        assertEquals(manga.ref, details.summary.ref)
        assertEquals(setOf("Archive Team"), details.authors)

        val chaptersResult = coordinator.chapters(provider, manga.ref)
        val chapters = (
            chaptersResult.outcome as SourceOutcome.Success<List<SourceChapter>>
        ).value
        assertEquals(2, chapters.size)

        val pagesResult = coordinator.pages(provider, chapters.first())
        val pages = (
            pagesResult.outcome as SourceOutcome.Success<List<MangaPageImage>>
        ).value
        assertEquals(listOf(0, 1, 2), pages.map { it.index })
        assertTrue(pages.all { it.imageUrl.startsWith("https://fixture.example/") })

        val resolvedResult = coordinator.resolvePublicUrl(
            provider,
            "https://fixture.example/title/veiled-library"
        )
        val resolved = (
            resolvedResult.outcome as SourceOutcome.Success<SourceMangaRef?>
        ).value
        assertNotNull(resolved)
        assertEquals(manga.ref, resolved)

        assertTrue(provider.seenContexts.isNotEmpty())
        assertTrue(provider.seenContexts.all { it.domain == "fixture.example" })
    }

    @Test
    fun fixtureProviderRejectsUnknownMangaWithoutThrowing() = runBlocking {
        val provider = FixtureProvider()
        val result = SourceExecutionCoordinator(sleeper = {}).details(
            provider,
            SourceMangaRef(
                sourceId = SourceId("fixture.reference"),
                key = "missing"
            )
        )

        val failure = result.outcome as SourceOutcome.Failure
        assertEquals(SourceFailureKind.NOT_FOUND, failure.error.kind)
    }

    private class FixtureProvider : MangaSourceProvider {

        override val descriptor = MangaSourceDescriptor(
            id = SourceId("fixture.reference"),
            displayName = "Fixture Reference",
            domains = listOf("fixture.example", "fixture-mirror.example"),
            localeTags = setOf("en"),
            contentTypes = setOf(MangaContentType.MANGA, MangaContentType.WEBTOON)
        )

        override val capabilities = setOf(
            MangaSourceCapability.SEARCH,
            MangaSourceCapability.DETAILS,
            MangaSourceCapability.CHAPTERS,
            MangaSourceCapability.PAGES,
            MangaSourceCapability.DOMAIN_FAILOVER
        )

        val seenContexts = mutableListOf<SourceRequestContext>()

        private val ref = SourceMangaRef(
            sourceId = descriptor.id,
            key = "veiled-library",
            publicUrl = "https://fixture.example/title/veiled-library"
        )

        private val summary = SourceMangaSummary(
            ref = ref,
            title = "The Veiled Library",
            alternativeTitles = setOf("Veiled Library"),
            coverUrl = "https://fixture.example/assets/cover.jpg",
            languageTag = "en",
            contentType = MangaContentType.MANGA
        )

        override suspend fun search(
            request: SourceSearchRequest,
            context: SourceRequestContext
        ): SourceOutcome<PagedSourceResult<SourceMangaSummary>> {
            seenContexts += context
            return SourceOutcome.Success(
                PagedSourceResult(
                    items = if ("veil" in request.query.lowercase()) listOf(summary) else emptyList()
                )
            )
        }

        override suspend fun details(
            manga: SourceMangaRef,
            context: SourceRequestContext
        ): SourceOutcome<SourceMangaDetails> {
            seenContexts += context
            if (manga.key != ref.key) {
                return SourceOutcome.Failure(
                    SourceFailure(SourceFailureKind.NOT_FOUND, "Fixture manga not found")
                )
            }
            return SourceOutcome.Success(
                SourceMangaDetails(
                    summary = summary,
                    description = "A deterministic fixture used to prove the Manga Source SDK.",
                    authors = setOf("Archive Team"),
                    status = MangaPublicationStatus.ONGOING,
                    tags = setOf("mystery", "fantasy")
                )
            )
        }

        override suspend fun chapters(
            manga: SourceMangaRef,
            context: SourceRequestContext
        ): SourceOutcome<List<SourceChapter>> {
            seenContexts += context
            if (manga.key != ref.key) {
                return SourceOutcome.Failure(
                    SourceFailure(SourceFailureKind.NOT_FOUND, "Fixture manga not found")
                )
            }
            return SourceOutcome.Success(
                listOf(
                    chapter("chapter-1", 1.0),
                    chapter("chapter-2", 2.0)
                )
            )
        }

        override suspend fun pages(
            chapter: SourceChapter,
            context: SourceRequestContext
        ): SourceOutcome<List<MangaPageImage>> {
            seenContexts += context
            if (chapter.mangaKey != ref.key) {
                return SourceOutcome.Failure(
                    SourceFailure(SourceFailureKind.NOT_FOUND, "Fixture chapter not found")
                )
            }
            return SourceOutcome.Success(
                List(3) { index ->
                    MangaPageImage(
                        index = index,
                        imageUrl = "https://fixture.example/pages/${chapter.chapterKey}/$index.jpg",
                        requestHeaders = mapOf("Referer" to "https://fixture.example/")
                    )
                }
            )
        }

        override suspend fun resolvePublicUrl(
            url: String,
            context: SourceRequestContext
        ): SourceOutcome<SourceMangaRef?> {
            seenContexts += context
            return SourceOutcome.Success(ref.takeIf { url.endsWith("/title/veiled-library") })
        }

        private fun chapter(key: String, number: Double) = SourceChapter(
            sourceId = descriptor.id,
            mangaKey = ref.key,
            chapterKey = key,
            title = "Chapter ${number.toInt()}",
            number = number,
            languageTag = "en"
        )
    }
}
