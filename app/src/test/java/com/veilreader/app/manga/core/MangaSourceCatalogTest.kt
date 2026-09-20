package com.veilreader.app.manga.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaSourceCatalogTest {
    @Test
    fun defaultPolicy_keepsHubAndRemoteSourcesDisabled() {
        val direct = registration("direct.en", MangaSourceOrigin.DIRECT, enabledByDefault = true)
        val gateway = registration("gateway.en", MangaSourceOrigin.GATEWAY, enabledByDefault = true)
        val catalog = MangaSourceCatalog(listOf(direct, gateway))

        val snapshot = catalog.snapshot()

        assertTrue(!snapshot.hubEnabled)
        assertTrue(snapshot.entries.none { it.enabled })
        assertNull(catalog.buildHubOrNull())
    }

    @Test
    fun originGates_applyWhenHubIsEnabled() {
        val local = registration("local", MangaSourceOrigin.LOCAL, enabledByDefault = true)
        val direct = registration("direct.en", MangaSourceOrigin.DIRECT, enabledByDefault = true)
        val gateway = registration("gateway.en", MangaSourceOrigin.GATEWAY, enabledByDefault = true)
        val catalog = MangaSourceCatalog(
            registrations = listOf(local, direct, gateway),
            policy = MangaFeaturePolicy(
                hubEnabled = true,
                localSourcesEnabled = true,
                directSourcesEnabled = false,
                gatewaySourcesEnabled = false
            )
        )

        val enabled = catalog.snapshot().enabledSources.map { it.id.value }

        assertEquals(listOf("local"), enabled)
    }

    @Test
    fun explicitEnable_canOpenOneSourceWithoutOpeningItsWholeOrigin() {
        val direct = registration("direct.en", MangaSourceOrigin.DIRECT)
        val catalog = MangaSourceCatalog(
            registrations = listOf(direct),
            policy = MangaFeaturePolicy(
                hubEnabled = true,
                directSourcesEnabled = false,
                explicitlyEnabledSources = setOf(MangaSourceId("direct.en"))
            )
        )

        assertEquals(
            listOf("direct.en"),
            catalog.enabledProviders().map { it.descriptor.id.value }
        )
    }

    @Test
    fun explicitDisable_overridesDefaultEnabledSource() {
        val local = registration("local", MangaSourceOrigin.LOCAL, enabledByDefault = true)
        val catalog = MangaSourceCatalog(
            registrations = listOf(local),
            policy = MangaFeaturePolicy(
                hubEnabled = true,
                localSourcesEnabled = true,
                explicitlyDisabledSources = setOf(MangaSourceId("local"))
            )
        )

        assertTrue(catalog.enabledProviders().isEmpty())
    }

    @Test
    fun duplicateSourceRegistrations_areRejectedAcrossOrigins() {
        val error = runCatching {
            MangaSourceCatalog(
                listOf(
                    registration("same.en", MangaSourceOrigin.DIRECT),
                    registration("same.en", MangaSourceOrigin.GATEWAY)
                )
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun healthTracker_mapsTypedSourceFailures() {
        val sourceId = MangaSourceId("direct.en")
        val tracker = MangaSourceHealthTracker()

        tracker.recordFailure(
            sourceId = sourceId,
            error = MangaSourceException.RateLimited(
                retryAfterMillis = 5_000L
            ),
            updatedAtEpochMs = 123L
        )

        val health = tracker.health(sourceId)
        assertEquals(MangaSourceHealthKind.RATE_LIMITED, health.kind)
        assertEquals(5_000L, health.retryAfterMillis)
        assertEquals(123L, health.updatedAtEpochMs)

        tracker.recordSuccess(sourceId, updatedAtEpochMs = 456L)
        assertEquals(MangaSourceHealthKind.HEALTHY, tracker.health(sourceId).kind)
        assertEquals(456L, tracker.health(sourceId).updatedAtEpochMs)
    }

    @Test
    fun hubOperations_updateSourceHealthAutomatically() {
        val sourceId = MangaSourceId("broken.en")
        val failing = object : MangaSourceProvider {
            override val descriptor = MangaSourceDescriptor(
                id = sourceId,
                providerId = MangaProviderId("test"),
                name = "Broken",
                language = "en",
                capabilities = setOf(MangaSourceCapability.SEARCH)
            )

            override suspend fun search(request: MangaSearchRequest): MangaResultPage<MangaSummary> {
                throw MangaSourceException.AuthRequired()
            }

            override suspend fun fetchUpdate(
                ref: MangaRef,
                existingChapters: List<MangaChapter>,
                options: MangaUpdateOptions
            ) = MangaUpdate(ref = ref)

            override suspend fun pages(ref: MangaChapterRef) = emptyList<MangaPage>()
        }

        val catalog = MangaSourceCatalog(
            registrations = listOf(
                MangaSourceRegistration(
                    provider = failing,
                    origin = MangaSourceOrigin.DIRECT,
                    enabledByDefault = true
                )
            ),
            policy = MangaFeaturePolicy(
                hubEnabled = true,
                directSourcesEnabled = true
            )
        )

        kotlinx.coroutines.runBlocking {
            runCatching {
                catalog.buildHubOrNull()!!.search(sourceId, "veil")
            }
        }

        val health = catalog.snapshot().entries.single().health
        assertEquals(MangaSourceHealthKind.AUTH_REQUIRED, health.kind)
    }

    private fun registration(
        id: String,
        origin: MangaSourceOrigin,
        enabledByDefault: Boolean = false
    ): MangaSourceRegistration =
        MangaSourceRegistration(
            provider = FakeSource(MangaSourceId(id)),
            origin = origin,
            enabledByDefault = enabledByDefault
        )

    private class FakeSource(
        private val sourceId: MangaSourceId
    ) : MangaSourceProvider {
        override val descriptor = MangaSourceDescriptor(
            id = sourceId,
            providerId = MangaProviderId("test"),
            name = sourceId.value,
            language = "en",
            capabilities = setOf(MangaSourceCapability.SEARCH)
        )

        override suspend fun search(request: MangaSearchRequest) =
            MangaResultPage<MangaSummary>(emptyList())

        override suspend fun fetchUpdate(
            ref: MangaRef,
            existingChapters: List<MangaChapter>,
            options: MangaUpdateOptions
        ) = MangaUpdate(ref = ref)

        override suspend fun pages(ref: MangaChapterRef) = emptyList<MangaPage>()
    }
}
