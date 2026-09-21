package com.veilreader.app.manga.gateway.suwayomi

import com.veilreader.app.manga.core.MangaChapter
import com.veilreader.app.manga.core.MangaChapterRef
import com.veilreader.app.manga.core.MangaPage
import com.veilreader.app.manga.core.MangaProviderId
import com.veilreader.app.manga.core.MangaRef
import com.veilreader.app.manga.core.MangaResultPage
import com.veilreader.app.manga.core.MangaSearchRequest
import com.veilreader.app.manga.core.MangaSourceCapability
import com.veilreader.app.manga.core.MangaSourceDescriptor
import com.veilreader.app.manga.core.MangaSourceId
import com.veilreader.app.manga.core.MangaSourceProvider
import com.veilreader.app.manga.core.MangaSummary
import com.veilreader.app.manga.core.MangaUpdate
import com.veilreader.app.manga.core.MangaUpdateOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SuwayomiRuntimeDiscoveryTest {
    @Test
    fun discover_keepsHealthyServersWhenAnotherServerFails() = runBlocking {
        val first = server("first")
        val second = server("second")
        val expected = FakeSource("suwayomi.first.1")

        val discovery = SuwayomiRuntimeDiscovery(
            SuwayomiProviderDiscoverer { server ->
                when (server.config.serverId.value) {
                    "first" -> listOf(expected)
                    else -> error("offline")
                }
            }
        )

        val result = discovery.discover(listOf(first, second))

        assertEquals(listOf(expected), result.providers)
        assertEquals(1, result.failures.size)
        assertEquals(second.config.serverId, result.failures.single().serverId)
        assertEquals("offline", result.failures.single().error.message)
    }

    @Test
    fun discover_rejectsDuplicateServerIdsBeforeNetworkWork() = runBlocking {
        var calls = 0
        val discovery = SuwayomiRuntimeDiscovery(
            SuwayomiProviderDiscoverer {
                calls += 1
                emptyList()
            }
        )

        val error = runCatching {
            discovery.discover(listOf(server("same"), server("same")))
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals(0, calls)
    }

    @Test
    fun discover_doesNotSwallowCancellation() = runBlocking {
        val expected = CancellationException("stop")
        val discovery = SuwayomiRuntimeDiscovery(
            SuwayomiProviderDiscoverer { throw expected }
        )

        val actual = runCatching {
            discovery.discover(listOf(server("one")))
        }.exceptionOrNull()

        assertSame(expected, actual)
    }

    private fun server(id: String): SuwayomiRuntimeServer =
        SuwayomiRuntimeServer(
            config = SuwayomiServerConfig(
                serverId = SuwayomiServerId(id),
                origin = "https://$id.example"
            )
        )

    private class FakeSource(id: String) : MangaSourceProvider {
        override val descriptor = MangaSourceDescriptor(
            id = MangaSourceId(id),
            providerId = MangaProviderId("test"),
            name = id,
            language = "en",
            capabilities = setOf(MangaSourceCapability.SEARCH)
        )

        override suspend fun search(request: MangaSearchRequest): MangaResultPage<MangaSummary> =
            MangaResultPage(emptyList())

        override suspend fun fetchUpdate(
            ref: MangaRef,
            existingChapters: List<MangaChapter>,
            options: MangaUpdateOptions
        ): MangaUpdate = MangaUpdate(ref = ref)

        override suspend fun pages(ref: MangaChapterRef): List<MangaPage> = emptyList()
    }
}
