package com.veilreader.app.manga.gateway.suwayomi

import com.veilreader.app.manga.core.MangaBrowseRequest
import com.veilreader.app.manga.core.MangaChapterRef
import com.veilreader.app.manga.core.MangaFilterSelection
import com.veilreader.app.manga.core.MangaRef
import com.veilreader.app.manga.core.MangaSearchRequest
import com.veilreader.app.manga.core.MangaSourceCapability
import com.veilreader.app.manga.core.MangaSourceException
import com.veilreader.app.manga.core.MangaStatus
import com.veilreader.app.manga.core.MangaUpdateOptions
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SuwayomiGatewayTest {
    private val config = SuwayomiServerConfig(
        serverId = SuwayomiServerId("home"),
        origin = "https://reader.example:4567"
    )

    @Test
    fun config_isHttpsFirstAndKeepsCredentialsOutOfOrigin() {
        assertEquals(
            "https://reader.example:4567/api/graphql",
            config.graphQlUrl
        )
        assertTrue(
            runCatching {
                SuwayomiServerConfig(SuwayomiServerId("bad"), "http://reader.example")
            }.isFailure
        )
        assertTrue(
            runCatching {
                SuwayomiServerConfig(
                    SuwayomiServerId("bad"),
                    "https://user:pass@reader.example"
                )
            }.isFailure
        )
        assertEquals(
            "https://reader.example:4567/api/v1/image/1",
            config.resolveServerResource("/api/v1/image/1")
        )
        assertNull(config.resolveServerResource("https://evil.example/image/1"))
    }

    @Test
    fun discovery_buildsStableProviderIdentityWithoutServerUrl() = runBlocking {
        val transport = RecordingTransport(
            mapOf(
                "VeilSuwayomiSources" to jsonObject(
                    """
                    {
                      "sources": {
                        "nodes": [
                          {
                            "id": 42,
                            "name": "SourceName",
                            "displayName": "Source Display",
                            "lang": "en",
                            "supportsLatest": true,
                            "contentWarning": "SAFE"
                          }
                        ]
                      }
                    }
                    """
                )
            )
        )
        val gateway = SuwayomiGateway(config, transport)

        val provider = gateway.discoverProviders().single()

        assertEquals("suwayomi.home.42", provider.descriptor.id.value)
        assertEquals("suwayomi.home", provider.descriptor.providerId.value)
        assertEquals("Source Display", provider.descriptor.name)
        assertEquals("en", provider.descriptor.language)
        assertTrue(MangaSourceCapability.LATEST in provider.descriptor.capabilities)
        assertFalse(provider.descriptor.id.value.contains("reader.example"))
    }

    @Test
    fun search_mapsPaginationAndResourceAuthorization() = runBlocking {
        val transport = RecordingTransport(
            mapOf(
                "VeilSuwayomiBrowse" to jsonObject(
                    """
                    {
                      "fetchSourceManga": {
                        "hasNextPage": true,
                        "mangas": [
                          {
                            "id": 7,
                            "sourceId": 42,
                            "title": "Veil Knight",
                            "thumbnailUrl": "/api/v1/manga/7/thumbnail",
                            "initialized": true
                          }
                        ]
                      }
                    }
                    """
                )
            )
        )
        val provider = provider(
            transport = transport,
            tokenProvider = SuwayomiAccessTokenProvider { "token-123" }
        )

        val result = provider.search(MangaSearchRequest("veil", cursor = "2"))

        assertEquals("m:7", result.items.single().ref.key)
        assertEquals("3", result.nextCursor)
        assertEquals(
            "https://reader.example:4567/api/v1/manga/7/thumbnail",
            result.items.single().cover?.url
        )
        assertEquals(
            "Bearer token-123",
            result.items.single().cover?.headers?.get("Authorization")
        )

        val request = transport.requests.single()
        val input = request.variables.getValue("input").jsonObject
        assertEquals("42", input.getValue("source").jsonPrimitive.content)
        assertEquals("SEARCH", input.getValue("type").jsonPrimitive.content)
        assertEquals("2", input.getValue("page").jsonPrimitive.content)
        assertEquals("veil", input.getValue("query").jsonPrimitive.content)
        assertTrue(request.query.contains("\$input"))
        assertFalse(request.query.contains("\\\$input"))
    }

    @Test
    fun update_respectsRequestedShapeAndMapsStatus() = runBlocking {
        val transport = RecordingTransport(
            mapOf("VeilSuwayomiUpdate" to updatePayload())
        )
        val provider = provider(transport)
        val ref = MangaRef(provider.descriptor.id, "m:7")

        val detailsOnly = provider.fetchUpdate(
            ref = ref,
            options = MangaUpdateOptions(fetchDetails = true, fetchChapters = false)
        )

        assertEquals("Veil Knight", detailsOnly.details?.title)
        assertEquals(MangaStatus.HIATUS, detailsOnly.details?.status)
        assertEquals(listOf("Author", "Artist"), detailsOnly.details?.authors)
        assertEquals(listOf("Action", "Fantasy"), detailsOnly.details?.tags)
        assertNull(detailsOnly.chapters)

        val input = transport.requests.single().variables.getValue("input").jsonObject
        assertEquals("true", input.getValue("fetchManga").jsonPrimitive.content)
        assertEquals("false", input.getValue("fetchChapters").jsonPrimitive.content)
    }

    @Test
    fun update_mapsStableRemoteChapterIdsWhenRequested() = runBlocking {
        val transport = RecordingTransport(
            mapOf("VeilSuwayomiUpdate" to updatePayload())
        )
        val provider = provider(transport)
        val ref = MangaRef(provider.descriptor.id, "m:7")

        val update = provider.fetchUpdate(ref = ref, options = MangaUpdateOptions())

        assertEquals(2, update.chapters?.size)
        assertEquals("c:101", update.chapters?.first()?.ref?.key)
        assertEquals(12.5, update.chapters?.first()?.chapterNumber ?: 0.0, 0.0)
        assertEquals(1_725_000_000_000L, update.chapters?.first()?.publishedAtEpochMs)
        assertEquals("Group A", update.chapters?.first()?.scanlator)
    }

    @Test
    fun pages_rejectForeignResourcesDeduplicateAndCarryAuthorization() = runBlocking {
        val transport = RecordingTransport(
            mapOf(
                "VeilSuwayomiPages" to jsonObject(
                    """
                    {
                      "fetchChapterPages": {
                        "chapter": {"id": 101, "pageCount": 3},
                        "pages": [
                          "/api/v1/manga/7/chapter/0/page/0",
                          "https://evil.example/page.jpg",
                          "/api/v1/manga/7/chapter/0/page/0",
                          "/api/v1/manga/7/chapter/0/page/1"
                        ]
                      }
                    }
                    """
                )
            )
        )
        val provider = provider(
            transport,
            SuwayomiAccessTokenProvider { "page-token" }
        )
        val manga = MangaRef(provider.descriptor.id, "m:7")

        val pages = provider.pages(MangaChapterRef(manga, "c:101"))

        assertEquals(2, pages.size)
        assertEquals(listOf(0, 1), pages.map { it.index })
        assertTrue(pages.all { it.image.url.startsWith("https://reader.example:4567/") })
        assertTrue(pages.all { it.image.headers["Authorization"] == "Bearer page-token" })
    }

    @Test
    fun protocolV1_failsClosedForUnmappedFiltersAndUnsupportedLatest() = runBlocking {
        val provider = provider(
            transport = RecordingTransport(emptyMap()),
            supportsLatest = false
        )

        val filterError = runCatching {
            provider.search(
                MangaSearchRequest(
                    query = "veil",
                    filters = MangaFilterSelection(mapOf("genre" to listOf("action")))
                )
            )
        }.exceptionOrNull()
        assertTrue(filterError is MangaSourceException.Unsupported)

        val latestError = runCatching {
            provider.latest(MangaBrowseRequest())
        }.exceptionOrNull()
        assertTrue(latestError is MangaSourceException.Unsupported)
    }

    private fun provider(
        transport: SuwayomiGraphQlTransport,
        tokenProvider: SuwayomiAccessTokenProvider = SuwayomiAccessTokenProvider { null },
        supportsLatest: Boolean = true
    ): SuwayomiSourceProvider = SuwayomiSourceProvider(
        config = config,
        remote = SuwayomiRemoteSource(
            remoteId = 42L,
            name = "SourceName",
            displayName = "Source Display",
            language = "en",
            supportsLatest = supportsLatest,
            contentWarning = "SAFE"
        ),
        transport = transport,
        tokenProvider = tokenProvider
    )

    private fun updatePayload(): JsonObject = jsonObject(
        """
        {
          "fetchMangaAndChapters": {
            "manga": {
              "id": 7,
              "sourceId": 42,
              "title": "Veil Knight",
              "thumbnailUrl": "/api/v1/manga/7/thumbnail",
              "initialized": true,
              "artist": "Artist",
              "author": "Author",
              "description": "Description",
              "genre": ["Action", "Fantasy"],
              "status": "ON_HIATUS",
              "realUrl": "https://source.example/veil"
            },
            "chapters": [
              {
                "id": 101,
                "name": "Chapter 12.5",
                "scanlator": "Group A",
                "realUrl": "https://source.example/chapter-12-5",
                "sourceOrder": 0,
                "chapterNumber": 12.5,
                "uploadDate": 1725000000000
              },
              {
                "id": 102,
                "name": "Chapter 13",
                "scanlator": null,
                "realUrl": null,
                "sourceOrder": 1,
                "chapterNumber": 13.0,
                "uploadDate": 1726000000000
              }
            ]
          }
        }
        """
    )

    private fun jsonObject(raw: String): JsonObject =
        Json.parseToJsonElement(raw).jsonObject

    private class RecordingTransport(
        private val responses: Map<String, JsonObject>
    ) : SuwayomiGraphQlTransport {
        val requests = mutableListOf<SuwayomiGraphQlRequest>()

        override suspend fun execute(request: SuwayomiGraphQlRequest): JsonObject {
            requests += request
            return responses[request.operationName]
                ?: error("Unexpected GraphQL operation: " + request.operationName)
        }
    }
}