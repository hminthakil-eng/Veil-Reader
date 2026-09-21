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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaSourceCompositionTest {
    @Test
    fun registrations_assignOriginsAndFailClosedRemoteDefaults() {
        val local = FakeSource("local.one")
        val direct = FakeSource("direct.one")
        val gateway = FakeSource("gateway.one")

        val registrations = MangaSourceComposition.registrations(
            MangaSourceGroups(
                local = listOf(local),
                direct = listOf(direct),
                gateway = listOf(gateway)
            )
        )

        assertEquals(
            listOf(
                MangaSourceOrigin.LOCAL,
                MangaSourceOrigin.DIRECT,
                MangaSourceOrigin.GATEWAY
            ),
            registrations.map { it.origin }
        )
        assertEquals(listOf(true, false, false), registrations.map { it.enabledByDefault })
    }

    @Test
    fun catalog_enablesLocalButKeepsRemoteSourcesClosedByDefault() {
        val catalog = MangaSourceComposition.catalog(
            groups = MangaSourceGroups(
                local = listOf(FakeSource("local.one")),
                direct = listOf(FakeSource("direct.one")),
                gateway = listOf(FakeSource("gateway.one"))
            ),
            policy = MangaFeaturePolicy(
                hubEnabled = true,
                localSourcesEnabled = true,
                directSourcesEnabled = true,
                gatewaySourcesEnabled = true
            )
        )

        val enabled = catalog.snapshot().entries.filter { it.enabled }.map { it.descriptor.id.value }

        assertEquals(listOf("local.one"), enabled)
    }

    @Test
    fun explicitEnable_opensOneGatewayWithoutOpeningEveryGateway() {
        val first = FakeSource("gateway.first")
        val second = FakeSource("gateway.second")
        val catalog = MangaSourceComposition.catalog(
            groups = MangaSourceGroups(gateway = listOf(first, second)),
            policy = MangaFeaturePolicy(
                hubEnabled = true,
                gatewaySourcesEnabled = false,
                explicitlyEnabledSources = setOf(first.descriptor.id)
            )
        )

        assertEquals(
            listOf("gateway.first"),
            catalog.enabledProviders().map { it.descriptor.id.value }
        )
    }

    @Test
    fun duplicateIds_acrossGroupsAreRejectedByCanonicalCatalog() {
        val duplicate = MangaSourceId("same")
        val error = runCatching {
            MangaSourceComposition.catalog(
                MangaSourceGroups(
                    local = listOf(FakeSource(duplicate.value)),
                    gateway = listOf(FakeSource(duplicate.value))
                )
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
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
