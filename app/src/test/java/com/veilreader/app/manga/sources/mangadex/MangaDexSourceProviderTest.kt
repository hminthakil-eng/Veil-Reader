package com.veilreader.app.manga.sources.mangadex

import com.veilreader.app.manga.MangaSourceCatalog
import com.veilreader.app.manga.MangaSourceFeatureFlags
import com.veilreader.app.manga.core.MangaRef
import com.veilreader.app.manga.core.MangaSourceId
import com.veilreader.app.manga.core.MangaStatus
import com.veilreader.app.manga.net.MangaHttpClient
import com.veilreader.app.manga.net.MangaHttpResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaDexSourceProviderTest {
    @Test
    fun search_mapsTitleCoverAndCursor() = runBlocking {
        val http = RecordingHttpClient { url ->
            assertTrue(url.contains("title=Veil+Knight"))
            assertTrue(url.contains("includes%5B%5D=cover_art"))
            SEARCH_JSON
        }
        val source = MangaDexSourceProvider(http)

        val result = source.search("Veil Knight", null)

        assertEquals(1, result.items.size)
        assertEquals("1", result.nextCursor)
        assertEquals("Veil Knight", result.items.single().title)
        assertEquals(
            "https://uploads.mangadex.org/covers/manga-1/cover.jpg.256.jpg",
            result.items.single().cover?.url
        )
    }

    @Test
    fun details_mapsMetadataWithoutLeakingTransportShape() = runBlocking {
        val source = MangaDexSourceProvider(RecordingHttpClient { DETAILS_JSON })
        val ref = MangaRef(MangaSourceId("mangadex.en"), "manga-1")

        val details = source.details(ref)

        assertEquals("Veil Knight", details.title)
        assertEquals("A hidden city.", details.description)
        assertEquals(MangaStatus.ONGOING, details.status)
        assertEquals(listOf("Author One", "Artist Two"), details.authors)
        assertEquals(listOf("Action"), details.tags)
    }

    @Test
    fun pages_buildsAtHomeImageRequestsInReadingOrder() = runBlocking {
        val source = MangaDexSourceProvider(RecordingHttpClient { AT_HOME_JSON })
        val manga = MangaRef(MangaSourceId("mangadex.en"), "manga-1")

        val pages = source.pages(com.veilreader.app.manga.core.MangaChapterRef(manga, "chapter-1"))

        assertEquals(listOf(0, 1), pages.map { it.index })
        assertEquals("https://uploads.example/data/hash/a.jpg", pages[0].image.url)
        assertEquals("https://uploads.example/data/hash/b.jpg", pages[1].image.url)
    }

    @Test
    fun catalog_keepsMangaDexDisabledByDefault() {
        val fake = RecordingHttpClient { error("Network must not be used while disabled") }

        val disabled = MangaSourceCatalog.providers(MangaSourceFeatureFlags(), httpClient = fake)
        val enabled = MangaSourceCatalog.providers(
            MangaSourceFeatureFlags(mangaDexEnabled = true),
            httpClient = fake
        )

        assertTrue(disabled.isEmpty())
        assertEquals(1, enabled.size)
        assertEquals("mangadex.en", enabled.single().descriptor.id.value)
    }

    @Test
    fun foreignSourceReference_isRejectedBeforeNetwork() = runBlocking {
        var called = false
        val source = MangaDexSourceProvider(RecordingHttpClient {
            called = true
            DETAILS_JSON
        })

        val error = runCatching {
            source.details(MangaRef(MangaSourceId("other.en"), "manga-1"))
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertFalse(called)
    }

    private class RecordingHttpClient(
        private val response: (String) -> String
    ) : MangaHttpClient {
        val urls = mutableListOf<String>()

        override suspend fun get(
            url: String,
            headers: Map<String, String>
        ): MangaHttpResponse {
            urls += url
            return MangaHttpResponse(200, response(url))
        }
    }

    private companion object {
        val SEARCH_JSON = """
            {
              "data": [
                {
                  "id": "manga-1",
                  "attributes": {"title": {"en": "Veil Knight"}},
                  "relationships": [
                    {
                      "type": "cover_art",
                      "attributes": {"fileName": "cover.jpg"}
                    }
                  ]
                }
              ],
              "total": 2
            }
        """.trimIndent()

        val DETAILS_JSON = """
            {
              "data": {
                "id": "manga-1",
                "attributes": {
                  "title": {"en": "Veil Knight"},
                  "description": {"en": "A hidden city."},
                  "status": "ongoing",
                  "tags": [
                    {"attributes": {"name": {"en": "Action"}}}
                  ]
                },
                "relationships": [
                  {"type": "author", "attributes": {"name": "Author One"}},
                  {"type": "artist", "attributes": {"name": "Artist Two"}},
                  {"type": "cover_art", "attributes": {"fileName": "cover.jpg"}}
                ]
              }
            }
        """.trimIndent()

        val AT_HOME_JSON = """
            {
              "baseUrl": "https://uploads.example",
              "chapter": {
                "hash": "hash",
                "data": ["a.jpg", "b.jpg"],
                "dataSaver": ["a-small.jpg", "b-small.jpg"]
              }
            }
        """.trimIndent()
    }
}