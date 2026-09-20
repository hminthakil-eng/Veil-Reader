package com.veilreader.app.manga.gateway.suwayomi

import com.veilreader.app.manga.core.MangaSearchRequest
import com.veilreader.app.manga.core.MangaSourceCapability
import com.veilreader.app.manga.core.MangaSourceException
import com.veilreader.app.manga.net.MangaHttpClient
import com.veilreader.app.manga.net.MangaHttpResponse
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuwayomiGatewayTest {
    @Test
    fun discoverSources_createsStableInstanceScopedProviders() = runBlocking {
        val http = RecordingHttpClient(
            responses = mutableListOf(SOURCES_RESPONSE)
        )
        val client = client(http)
        val providers = SuwayomiGateway(client).discoverSources()

        assertEquals(2, providers.size)
        assertEquals("suwayomi.home.42", providers[0].descriptor.id.value)
        assertEquals("suwayomi.home", providers[0].descriptor.providerId.value)
        assertTrue(MangaSourceCapability.SEARCH in providers[0].descriptor.capabilities)
        assertTrue(MangaSourceCapability.POPULAR in providers[0].descriptor.capabilities)
        assertTrue(MangaSourceCapability.LATEST in providers[0].descriptor.capabilities)
        assertFalse(MangaSourceCapability.LATEST in providers[1].descriptor.capabilities)
        assertFalse(providers[0].descriptor.toString().contains("Bearer"))
    }

    @Test
    fun search_mapsMangaAndCursorThroughSourceApi() = runBlocking {
        val http = RecordingHttpClient(
            responses = mutableListOf(SOURCES_RESPONSE, BROWSE_RESPONSE)
        )
        val client = client(http)
        val provider = SuwayomiGateway(client).discoverSources().first()

        val result = provider.search(MangaSearchRequest("veil"))

        assertEquals(1, result.items.size)
        assertEquals("900", result.items.single().ref.key)
        assertEquals("Veil Knight", result.items.single().title)
        assertEquals("2", result.nextCursor)
        assertEquals(
            "https://suwayomi.example/api/v1/manga/900/thumbnail",
            result.items.single().cover?.url
        )
        assertEquals(
            "Bearer test-token",
            result.items.single().cover?.headers?.get("Authorization")
        )

        val browseRequest = Json.parseToJsonElement(http.requests[1].json).jsonObject
        val variables = browseRequest["variables"]!!.jsonObject
        val input = variables["input"]!!.jsonObject
        assertEquals("42", input["source"].toString().trim('"'))
        assertEquals("SEARCH", input["type"].toString().trim('"'))
        assertEquals("1", input["page"].toString())
    }

    @Test
    fun update_mapsDetailsAndChaptersWithoutLeakingRemoteRuntime() = runBlocking {
        val http = RecordingHttpClient(
            responses = mutableListOf(SOURCES_RESPONSE, UPDATE_RESPONSE)
        )
        val provider = SuwayomiGateway(client(http)).discoverSources().first()
        val ref = com.veilreader.app.manga.core.MangaRef(
            provider.descriptor.id,
            "900"
        )

        val update = provider.fetchUpdate(ref = ref)

        assertEquals("Veil Knight", update.details?.title)
        assertEquals(
            com.veilreader.app.manga.core.MangaStatus.ONGOING,
            update.details?.status
        )
        assertEquals(listOf("Writer", "Artist"), update.details?.authors)
        assertEquals(listOf("Action", "Fantasy"), update.details?.tags)
        assertEquals(2, update.chapters?.size)
        assertEquals("501", update.chapters?.first()?.ref?.key)
        assertEquals(12.5, update.chapters?.first()?.chapterNumber ?: 0.0, 0.0)
    }

    @Test
    fun pages_areSameOriginServerResourcesWithAuthHeaders() = runBlocking {
        val http = RecordingHttpClient(
            responses = mutableListOf(SOURCES_RESPONSE, PAGES_RESPONSE)
        )
        val provider = SuwayomiGateway(client(http)).discoverSources().first()
        val manga = com.veilreader.app.manga.core.MangaRef(
            provider.descriptor.id,
            "900"
        )

        val pages = provider.pages(
            com.veilreader.app.manga.core.MangaChapterRef(manga, "501")
        )

        assertEquals(2, pages.size)
        assertEquals(
            "https://suwayomi.example/api/v1/manga/900/chapter/0/page/0",
            pages[0].image.url
        )
        assertEquals(
            "Bearer test-token",
            pages[0].image.headers["Authorization"]
        )
    }

    @Test
    fun pages_rejectCrossOriginResources() = runBlocking {
        val http = RecordingHttpClient(
            responses = mutableListOf(SOURCES_RESPONSE, CROSS_ORIGIN_PAGES_RESPONSE)
        )
        val provider = SuwayomiGateway(client(http)).discoverSources().first()
        val manga = com.veilreader.app.manga.core.MangaRef(
            provider.descriptor.id,
            "900"
        )

        val error = runCatching {
            provider.pages(
                com.veilreader.app.manga.core.MangaChapterRef(manga, "501")
            )
        }.exceptionOrNull()

        assertTrue(error is MangaSourceException.Blocked)
    }

    @Test
    fun graphqlAuthError_mapsToTypedSourceFailure() = runBlocking {
        val http = RecordingHttpClient(
            responses = mutableListOf(AUTH_ERROR_RESPONSE)
        )
        val error = runCatching {
            client(http).sources()
        }.exceptionOrNull()

        assertTrue(error is MangaSourceException.AuthRequired)
    }

    @Test
    fun cleartextHttp_requiresExplicitOptIn() {
        val blocked = runCatching {
            SuwayomiGatewayConfig(
                instanceId = "home",
                baseUrl = "http://192.168.1.20:4567"
            )
        }.exceptionOrNull()
        assertTrue(blocked is IllegalArgumentException)

        val allowed = SuwayomiGatewayConfig(
            instanceId = "home",
            baseUrl = "http://192.168.1.20:4567",
            allowCleartextHttp = true
        )
        assertEquals(
            "http://192.168.1.20:4567/api/graphql",
            allowed.graphQlUrl
        )
    }

    private fun client(http: RecordingHttpClient) = SuwayomiGatewayClient(
        config = SuwayomiGatewayConfig(
            instanceId = "home",
            baseUrl = "https://suwayomi.example"
        ),
        http = http,
        authProvider = SuwayomiAuthProvider { "Bearer test-token" }
    )

    private data class RecordedRequest(
        val url: String,
        val headers: Map<String, String>,
        val json: String
    )

    private class RecordingHttpClient(
        private val responses: MutableList<String>
    ) : MangaHttpClient {
        val requests = mutableListOf<RecordedRequest>()

        override suspend fun get(
            url: String,
            headers: Map<String, String>
        ): MangaHttpResponse = error("Unexpected GET")

        override suspend fun postJson(
            url: String,
            headers: Map<String, String>,
            json: String
        ): MangaHttpResponse {
            requests += RecordedRequest(url, headers, json)
            return MangaHttpResponse(
                code = 200,
                body = responses.removeAt(0)
            )
        }
    }

    private companion object {
        val SOURCES_RESPONSE = """
            {
              "data": {
                "sources": {
                  "nodes": [
                    {
                      "id": "42",
                      "name": "alpha",
                      "displayName": "Alpha",
                      "lang": "en",
                      "supportsLatest": true,
                      "contentWarning": "SAFE",
                      "homeUrl": "https://source.example"
                    },
                    {
                      "id": "99",
                      "name": "beta",
                      "displayName": "Beta",
                      "lang": "fa",
                      "supportsLatest": false,
                      "contentWarning": "SAFE",
                      "homeUrl": null
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val BROWSE_RESPONSE = """
            {
              "data": {
                "fetchSourceManga": {
                  "hasNextPage": true,
                  "mangas": [
                    {
                      "id": 900,
                      "sourceId": "42",
                      "title": "Veil Knight",
                      "thumbnailUrl": "/api/v1/manga/900/thumbnail"
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val UPDATE_RESPONSE = """
            {
              "data": {
                "fetchMangaAndChapters": {
                  "manga": {
                    "id": 900,
                    "sourceId": "42",
                    "title": "Veil Knight",
                    "thumbnailUrl": "/api/v1/manga/900/thumbnail",
                    "author": "Writer",
                    "artist": "Artist",
                    "description": "A hidden city.",
                    "genre": ["Action", "Fantasy"],
                    "status": "ONGOING",
                    "realUrl": "https://source.example/manga/veil"
                  },
                  "chapters": [
                    {
                      "id": 501,
                      "mangaId": 900,
                      "name": "Chapter 12.5",
                      "chapterNumber": 12.5,
                      "scanlator": "Group A",
                      "uploadDate": "1700000000000",
                      "sourceOrder": 0,
                      "realUrl": null
                    },
                    {
                      "id": 502,
                      "mangaId": 900,
                      "name": "Chapter 13",
                      "chapterNumber": 13.0,
                      "scanlator": null,
                      "uploadDate": "1700100000000",
                      "sourceOrder": 1,
                      "realUrl": null
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val PAGES_RESPONSE = """
            {
              "data": {
                "fetchChapterPages": {
                  "chapter": { "id": 501, "mangaId": 900 },
                  "pages": [
                    "/api/v1/manga/900/chapter/0/page/0",
                    "/api/v1/manga/900/chapter/0/page/1"
                  ]
                }
              }
            }
        """.trimIndent()

        val CROSS_ORIGIN_PAGES_RESPONSE = """
            {
              "data": {
                "fetchChapterPages": {
                  "chapter": { "id": 501, "mangaId": 900 },
                  "pages": [
                    "https://evil.example/page/0"
                  ]
                }
              }
            }
        """.trimIndent()

        val AUTH_ERROR_RESPONSE = """
            {
              "errors": [
                {
                  "message": "suwayomi.tachidesk.server.user.UnauthorizedException"
                }
              ],
              "data": null
            }
        """.trimIndent()
    }
}
