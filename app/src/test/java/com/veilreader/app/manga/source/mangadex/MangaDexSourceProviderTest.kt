package com.veilreader.app.manga.source.mangadex

import com.veilreader.app.manga.source.MangaContentType
import com.veilreader.app.manga.source.SourceChapter
import com.veilreader.app.manga.source.SourceFailureKind
import com.veilreader.app.manga.source.SourceMangaRef
import com.veilreader.app.manga.source.SourceOutcome
import com.veilreader.app.manga.source.SourceRequestContext
import com.veilreader.app.manga.source.SourceSearchRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaDexSourceProviderTest {

    @Test
    fun searchMapsStableIdentityCoverAndContinuation() = runBlocking {
        val transport = FakeTransport { url, _ ->
            assertEquals("/manga", url.toHttpUrl().encodedPath)
            assertEquals("veil", url.toHttpUrl().queryParameter("title"))
            assertEquals("en", url.toHttpUrl().queryParameter("availableTranslatedLanguage[]"))
            ok(SEARCH_JSON)
        }
        val provider = provider(transport)

        val result = provider.search(
            SourceSearchRequest(query = "veil", limit = 1),
            context()
        )

        val page = (result as SourceOutcome.Success).value
        assertEquals(1, page.items.size)
        assertEquals("1", page.nextContinuationToken)
        val item = page.items.single()
        assertEquals(MANGA_ID, item.ref.key)
        assertEquals("Veil Reader", item.title)
        assertEquals("en", item.languageTag)
        assertEquals(MangaContentType.COMIC, item.contentType)
        assertTrue(item.alternativeTitles.contains("Veil"))
        assertEquals(
            "https://uploads.mangadex.org/covers/$MANGA_ID/cover.jpg.256.jpg",
            item.coverUrl
        )
        assertTrue(transport.lastHeaders["User-Agent"].orEmpty().contains("VeilReader"))
    }

    @Test
    fun detailsMapsAuthorsStatusTagsAndLocalizedDescription() = runBlocking {
        val provider = provider(FakeTransport { _, _ -> ok(DETAILS_JSON) })

        val result = provider.details(ref(), context())

        val details = (result as SourceOutcome.Success).value
        assertEquals("Veil Reader", details.summary.title)
        assertEquals("A source-neutral reader.", details.description)
        assertEquals(setOf("Author One", "Artist Two"), details.authors)
        assertEquals("ONGOING", details.status.name)
        assertEquals(setOf("Action", "Mystery"), details.tags)
    }

    @Test
    fun chaptersPreserveApiOrderAndScanlationCredit() = runBlocking {
        val provider = provider(FakeTransport { url, _ ->
            val parsed = url.toHttpUrl()
            assertEquals("/manga/$MANGA_ID/feed", parsed.encodedPath)
            assertEquals("0", parsed.queryParameter("includeExternalUrl"))
            assertEquals("asc", parsed.queryParameter("order[volume]"))
            assertEquals("asc", parsed.queryParameter("order[chapter]"))
            ok(CHAPTERS_JSON)
        })

        val result = provider.chapters(ref(), context())

        val chapters = (result as SourceOutcome.Success).value
        assertEquals(2, chapters.size)
        assertEquals(listOf(CHAPTER_TWO, CHAPTER_ONE), chapters.map(SourceChapter::chapterKey))
        assertEquals(listOf(2.0, 1.0), chapters.map(SourceChapter::number))
        assertFalse(chapters.any { it.chapterKey == EXTERNAL_CHAPTER })
        assertEquals("Group Two", chapters.first().scanlator)
        assertEquals("en", chapters.first().languageTag)
        assertNotNull(chapters.first().publishedAtEpochMs)
    }

    @Test
    fun pagesBuildHttpsAtHomeOriginalQualityUrls() = runBlocking {
        val provider = provider(FakeTransport { url, _ ->
            assertEquals("/at-home/server/$CHAPTER_ONE", url.toHttpUrl().encodedPath)
            ok(PAGES_JSON)
        })

        val result = provider.pages(
            SourceChapter(
                sourceId = MangaDexSourceProvider.ID,
                mangaKey = MANGA_ID,
                chapterKey = CHAPTER_ONE
            ),
            context()
        )

        val pages = (result as SourceOutcome.Success).value
        assertEquals(2, pages.size)
        assertEquals(
            "https://uploads.mangadex.org/data/hash-123/page-1.jpg",
            pages[0].imageUrl
        )
        assertEquals(
            "https://uploads.mangadex.org/data/hash-123/page-2.jpg",
            pages[1].imageUrl
        )
    }

    @Test
    fun rateLimitMapsRetryAfterWithoutLeakingPayload() = runBlocking {
        val headers = Headers.Builder()
            .add("Retry-After", "3")
            .build()
        val provider = provider(
            FakeTransport { _, _ ->
                MangaDexHttpResponse(
                    statusCode = 429,
                    body = """{"error":"too many"}""",
                    headers = headers
                )
            }
        )

        val result = provider.search(SourceSearchRequest("veil"), context())

        val failure = result as SourceOutcome.Failure
        assertEquals(SourceFailureKind.RATE_LIMITED, failure.error.kind)
        assertEquals(3_000L, failure.error.retryAfterMillis)
        assertFalse(failure.error.message.contains("too many"))
    }

    @Test
    fun reverseLinkAcceptsOnlyHttpsMangaDexTitleUrls() = runBlocking {
        val provider = provider(FakeTransport { _, _ -> error("No HTTP expected") })

        val owned = provider.resolvePublicUrl(
            "https://mangadex.org/title/$MANGA_ID/veil-reader",
            context()
        )
        val http = provider.resolvePublicUrl(
            "http://mangadex.org/title/$MANGA_ID",
            context()
        )
        val foreign = provider.resolvePublicUrl(
            "https://example.org/title/$MANGA_ID",
            context()
        )

        assertEquals(ref(), (owned as SourceOutcome.Success).value)
        assertNull((http as SourceOutcome.Success).value)
        assertNull((foreign as SourceOutcome.Success).value)
    }

    @Test
    fun policyGuardKeepsPilotOutOfMonetizedProductionEnablement() {
        assertEquals("MangaDex", MangaDexPilotPolicy.SOURCE_CREDIT)
        assertTrue(MangaDexPilotPolicy.REQUIRES_SCANLATION_GROUP_CREDIT)
        assertFalse(MangaDexPilotPolicy.MONETIZED_DISTRIBUTION_ALLOWED_BY_REVIEWED_POLICY)
        assertTrue(MangaDexPilotPolicy.PRODUCTION_ENABLE_REQUIRES_POLICY_REVIEW)
    }

    private fun provider(transport: MangaDexHttpTransport): MangaDexSourceProvider =
        MangaDexSourceProvider(
            transport = transport,
            preferredLanguageTags = listOf("en"),
            maxChapterCount = 2_000
        )

    private fun context() = SourceRequestContext(
        domain = MangaDexSourceProvider.API_DOMAIN,
        attempt = 1
    )

    private fun ref() = SourceMangaRef(
        sourceId = MangaDexSourceProvider.ID,
        key = MANGA_ID,
        publicUrl = "https://mangadex.org/title/$MANGA_ID"
    )

    private fun ok(body: String) = MangaDexHttpResponse(
        statusCode = 200,
        body = body,
        headers = Headers.Builder().build()
    )

    private class FakeTransport(
        private val handler: (String, Map<String, String>) -> MangaDexHttpResponse
    ) : MangaDexHttpTransport {
        var lastHeaders: Map<String, String> = emptyMap()
            private set

        override suspend fun get(
            url: String,
            headers: Map<String, String>
        ): MangaDexHttpResponse {
            lastHeaders = headers
            return handler(url, headers)
        }
    }

    private companion object {
        const val MANGA_ID = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
        const val CHAPTER_ONE = "11111111-1111-4111-8111-111111111111"
        const val CHAPTER_TWO = "22222222-2222-4222-8222-222222222222"
        const val EXTERNAL_CHAPTER = "44444444-4444-4444-8444-444444444444"

        val SEARCH_JSON = """
            {
              "result": "ok",
              "data": [{
                "id": "$MANGA_ID",
                "type": "manga",
                "attributes": {
                  "title": {"en": "Veil Reader"},
                  "altTitles": [{"en": "Veil"}],
                  "originalLanguage": "en"
                },
                "relationships": [{
                  "id": "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
                  "type": "cover_art",
                  "attributes": {"fileName": "cover.jpg"}
                }]
              }],
              "limit": 1,
              "offset": 0,
              "total": 2
            }
        """.trimIndent()

        val DETAILS_JSON = """
            {
              "result": "ok",
              "data": {
                "id": "$MANGA_ID",
                "type": "manga",
                "attributes": {
                  "title": {"en": "Veil Reader"},
                  "altTitles": [{"ja": "ヴェール"}],
                  "description": {"en": "A source-neutral reader."},
                  "originalLanguage": "ja",
                  "status": "ongoing",
                  "tags": [
                    {"attributes": {"name": {"en": "Action"}}},
                    {"attributes": {"name": {"en": "Mystery"}}}
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

        val CHAPTERS_JSON = """
            {
              "result": "ok",
              "data": [
                {
                  "id": "$EXTERNAL_CHAPTER",
                  "attributes": {
                    "title": "Publisher",
                    "volume": "1",
                    "chapter": "0",
                    "translatedLanguage": "en",
                    "externalUrl": "https://publisher.example/chapter/0",
                    "publishAt": "2025-12-31T00:00:00+00:00"
                  },
                  "relationships": []
                },
                {
                  "id": "$CHAPTER_TWO",
                  "attributes": {
                    "title": "Second",
                    "volume": "1",
                    "chapter": "2",
                    "translatedLanguage": "en",
                    "publishAt": "2026-01-02T00:00:00+00:00"
                  },
                  "relationships": [
                    {"type": "scanlation_group", "attributes": {"name": "Group Two"}}
                  ]
                },
                {
                  "id": "$CHAPTER_ONE",
                  "attributes": {
                    "title": "First",
                    "volume": "1",
                    "chapter": "1",
                    "translatedLanguage": "en",
                    "publishAt": "2026-01-01T00:00:00+00:00"
                  },
                  "relationships": [
                    {"type": "scanlation_group", "attributes": {"name": "Group One"}}
                  ]
                }
              ],
              "limit": 100,
              "offset": 0,
              "total": 3
            }
        """.trimIndent()

        val PAGES_JSON = """
            {
              "result": "ok",
              "baseUrl": "https://uploads.mangadex.org",
              "chapter": {
                "hash": "hash-123",
                "data": ["page-1.jpg", "page-2.jpg"],
                "dataSaver": ["page-1.webp", "page-2.webp"]
              }
            }
        """.trimIndent()
    }
}
