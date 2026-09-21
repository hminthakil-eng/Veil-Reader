package com.veilreader.app.manga

import com.veilreader.app.manga.core.MangaChapter
import com.veilreader.app.manga.core.MangaChapterRef
import com.veilreader.app.manga.core.MangaFeaturePolicy
import com.veilreader.app.manga.core.MangaPage
import com.veilreader.app.manga.core.MangaProviderId
import com.veilreader.app.manga.core.MangaRef
import com.veilreader.app.manga.core.MangaResultPage
import com.veilreader.app.manga.core.MangaSearchRequest
import com.veilreader.app.manga.core.MangaSourceCapability
import com.veilreader.app.manga.core.MangaSourceDescriptor
import com.veilreader.app.manga.core.MangaSourceId
import com.veilreader.app.manga.core.MangaSourceOrigin
import com.veilreader.app.manga.core.MangaSourceProvider
import com.veilreader.app.manga.core.MangaSummary
import com.veilreader.app.manga.core.MangaUpdate
import com.veilreader.app.manga.core.MangaUpdateOptions
import com.veilreader.app.manga.net.MangaHttpClient
import com.veilreader.app.manga.net.MangaHttpResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaRuntimeSourceAssemblyTest {
    @Test
    fun directSource_isRegisteredFailClosedUntilExplicitlyEnabled() {
        val http = MangaHttpClient { _, _ -> MangaHttpResponse(code = 200, body = "{}") }

        val closedCatalog = MangaRuntimeSourceAssembly.catalog(
            config = MangaRuntimeSourceConfig(
                directFlags = MangaDirectSourceFlags(mangaDexEnabled = true),
                translatedLanguage = "en",
                policy = MangaFeaturePolicy(
                    hubEnabled = true,
                    directSourcesEnabled = true
                )
            ),
            directHttpClient = http
        )

        val closedEntry = closedCatalog.snapshot().entries.single()
        assertEquals("mangadex.en", closedEntry.descriptor.id.value)
        assertEquals(MangaSourceOrigin.DIRECT, closedEntry.origin)
        assertFalse(closedEntry.enabled)

        val openCatalog = MangaRuntimeSourceAssembly.catalog(
            config = MangaRuntimeSourceConfig(
                directFlags = MangaDirectSourceFlags(mangaDexEnabled = true),
                translatedLanguage = "en",
                policy = MangaFeaturePolicy(
                    hubEnabled = true,
                    directSourcesEnabled = true,
                    explicitlyEnabledSources = setOf(MangaSourceId("mangadex.en"))
                )
            ),
            directHttpClient = http
        )

        assertTrue(openCatalog.snapshot().entries.single().enabled)
    }

    @Test
    fun runtimeAssembly_preservesLocalAndGatewayOrigins() {
        val local = FakeSource("local.one")
        val gateway = FakeSource("gateway.one")

        val catalog = MangaRuntimeSourceAssembly.catalog(
            config = MangaRuntimeSourceConfig(
                policy = MangaFeaturePolicy(
                    hubEnabled = true,
                    localSourcesEnabled = true,
                    gatewaySourcesEnabled = true
                )
            ),
            localProviders = listOf(local),
            gatewayProviders = listOf(gateway),
            directHttpClient = MangaHttpClient { _, _ ->
                MangaHttpResponse(code = 200, body = "{}")
            }
        )

        val entries = catalog.snapshot().entries.associateBy { it.descriptor.id.value }
        assertEquals(MangaSourceOrigin.LOCAL, entries.getValue("local.one").origin)
        assertEquals(MangaSourceOrigin.GATEWAY, entries.getValue("gateway.one").origin)
        assertTrue(entries.getValue("local.one").enabled)
        assertFalse(entries.getValue("gateway.one").enabled)
    }

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
