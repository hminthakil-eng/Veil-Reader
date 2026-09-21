package com.veilreader.app.manga.core

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException

enum class MangaSourceOrigin {
    LOCAL,
    DIRECT,
    GATEWAY
}

data class MangaFeaturePolicy(
    val hubEnabled: Boolean = false,
    val localSourcesEnabled: Boolean = true,
    val directSourcesEnabled: Boolean = false,
    val gatewaySourcesEnabled: Boolean = false,
    val explicitlyEnabledSources: Set<MangaSourceId> = emptySet(),
    val explicitlyDisabledSources: Set<MangaSourceId> = emptySet()
) {
    init {
        require(explicitlyEnabledSources.intersect(explicitlyDisabledSources).isEmpty()) {
            "A manga source cannot be explicitly enabled and disabled at the same time."
        }
    }

    fun allows(registration: MangaSourceRegistration): Boolean {
        if (!hubEnabled) return false
        if (registration.provider.descriptor.id in explicitlyDisabledSources) return false
        if (registration.provider.descriptor.id in explicitlyEnabledSources) return true

        val originAllowed = when (registration.origin) {
            MangaSourceOrigin.LOCAL -> localSourcesEnabled
            MangaSourceOrigin.DIRECT -> directSourcesEnabled
            MangaSourceOrigin.GATEWAY -> gatewaySourcesEnabled
        }
        return originAllowed && registration.enabledByDefault
    }
}

data class MangaSourceRegistration(
    val provider: MangaSourceProvider,
    val origin: MangaSourceOrigin,
    val enabledByDefault: Boolean = false
)

enum class MangaSourceHealthKind {
    UNKNOWN,
    HEALTHY,
    AUTH_REQUIRED,
    RATE_LIMITED,
    BLOCKED,
    NOT_FOUND,
    TEMPORARILY_UNAVAILABLE,
    NETWORK_ERROR,
    SOURCE_CHANGED,
    PARSE_ERROR,
    UNSUPPORTED
}

data class MangaSourceHealth(
    val kind: MangaSourceHealthKind = MangaSourceHealthKind.UNKNOWN,
    val message: String? = null,
    val retryAfterMillis: Long? = null,
    val updatedAtEpochMs: Long = 0L
)

data class MangaSourceCatalogEntry(
    val descriptor: MangaSourceDescriptor,
    val origin: MangaSourceOrigin,
    val enabled: Boolean,
    val health: MangaSourceHealth
)

data class MangaSourceCatalogSnapshot(
    val hubEnabled: Boolean,
    val entries: List<MangaSourceCatalogEntry>
) {
    val enabledSources: List<MangaSourceDescriptor>
        get() = entries.filter(MangaSourceCatalogEntry::enabled).map(MangaSourceCatalogEntry::descriptor)
}

class MangaSourceHealthTracker {
    private val health = ConcurrentHashMap<MangaSourceId, MangaSourceHealth>()

    fun health(sourceId: MangaSourceId): MangaSourceHealth =
        health[sourceId] ?: MangaSourceHealth()

    fun recordSuccess(
        sourceId: MangaSourceId,
        updatedAtEpochMs: Long = System.currentTimeMillis()
    ) {
        health[sourceId] = MangaSourceHealth(
            kind = MangaSourceHealthKind.HEALTHY,
            updatedAtEpochMs = updatedAtEpochMs
        )
    }

    fun recordFailure(
        sourceId: MangaSourceId,
        error: Throwable,
        updatedAtEpochMs: Long = System.currentTimeMillis()
    ) {
        health[sourceId] = error.toMangaSourceHealth(updatedAtEpochMs)
    }

    fun clear(sourceId: MangaSourceId) {
        health.remove(sourceId)
    }
}

class MangaSourceCatalog(
    registrations: Iterable<MangaSourceRegistration>,
    private var policy: MangaFeaturePolicy = MangaFeaturePolicy(),
    private val healthTracker: MangaSourceHealthTracker = MangaSourceHealthTracker()
) {
    private val registrationsById: Map<MangaSourceId, MangaSourceRegistration>

    init {
        val items = registrations.toList()
        val duplicates = items
            .groupBy { it.provider.descriptor.id }
            .filterValues { it.size > 1 }
            .keys
        require(duplicates.isEmpty()) {
            "Duplicate manga source registrations are not allowed: " +
                duplicates.joinToString { it.value }
        }
        registrationsById = items.associateBy { it.provider.descriptor.id }
    }

    fun updatePolicy(value: MangaFeaturePolicy) {
        policy = value
    }

    fun snapshot(): MangaSourceCatalogSnapshot =
        MangaSourceCatalogSnapshot(
            hubEnabled = policy.hubEnabled,
            entries = registrationsById.values
                .map { registration ->
                    MangaSourceCatalogEntry(
                        descriptor = registration.provider.descriptor,
                        origin = registration.origin,
                        enabled = policy.allows(registration),
                        health = healthTracker.health(registration.provider.descriptor.id)
                    )
                }
                .sortedWith(
                    compareBy<MangaSourceCatalogEntry>(
                        { it.descriptor.language },
                        { it.descriptor.name },
                        { it.descriptor.id.value }
                    )
                )
        )

    fun enabledProviders(): List<MangaSourceProvider> =
        registrationsById.values
            .filter(policy::allows)
            .map { registration ->
                HealthTrackedMangaSourceProvider(
                    delegate = registration.provider,
                    healthTracker = healthTracker
                )
            }

    fun buildHubOrNull(): MangaHub? {
        if (!policy.hubEnabled) return null
        return MangaHub(MangaSourceRegistry(enabledProviders()))
    }

    fun healthTracker(): MangaSourceHealthTracker = healthTracker
}

private class HealthTrackedMangaSourceProvider(
    private val delegate: MangaSourceProvider,
    private val healthTracker: MangaSourceHealthTracker
) : MangaSourceProvider {
    override val descriptor: MangaSourceDescriptor
        get() = delegate.descriptor

    override suspend fun search(request: MangaSearchRequest): MangaResultPage<MangaSummary> =
        track { delegate.search(request) }

    override suspend fun popular(request: MangaBrowseRequest): MangaResultPage<MangaSummary> =
        track { delegate.popular(request) }

    override suspend fun latest(request: MangaBrowseRequest): MangaResultPage<MangaSummary> =
        track { delegate.latest(request) }

    override suspend fun filters(): List<MangaFilterDefinition> =
        track { delegate.filters() }

    override suspend fun resolveUrl(url: String): MangaRef? =
        track { delegate.resolveUrl(url) }

    override suspend fun fetchUpdate(
        ref: MangaRef,
        existingChapters: List<MangaChapter>,
        options: MangaUpdateOptions
    ): MangaUpdate =
        track { delegate.fetchUpdate(ref, existingChapters, options) }

    override suspend fun pages(ref: MangaChapterRef): List<MangaPage> =
        track { delegate.pages(ref) }

    private suspend fun <T> track(block: suspend () -> T): T =
        try {
            block().also {
                healthTracker.recordSuccess(descriptor.id)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            healthTracker.recordFailure(descriptor.id, error)
            throw error
        }
}

internal fun Throwable.toMangaSourceHealth(
    updatedAtEpochMs: Long
): MangaSourceHealth = when (this) {
    is MangaSourceException.AuthRequired -> MangaSourceHealth(
        kind = MangaSourceHealthKind.AUTH_REQUIRED,
        message = message,
        updatedAtEpochMs = updatedAtEpochMs
    )
    is MangaSourceException.RateLimited -> MangaSourceHealth(
        kind = MangaSourceHealthKind.RATE_LIMITED,
        message = message,
        retryAfterMillis = retryAfterMillis,
        updatedAtEpochMs = updatedAtEpochMs
    )
    is MangaSourceException.Blocked -> MangaSourceHealth(
        kind = MangaSourceHealthKind.BLOCKED,
        message = message,
        updatedAtEpochMs = updatedAtEpochMs
    )
    is MangaSourceException.NotFound -> MangaSourceHealth(
        kind = MangaSourceHealthKind.NOT_FOUND,
        message = message,
        updatedAtEpochMs = updatedAtEpochMs
    )
    is MangaSourceException.TemporarilyUnavailable -> MangaSourceHealth(
        kind = MangaSourceHealthKind.TEMPORARILY_UNAVAILABLE,
        message = message,
        updatedAtEpochMs = updatedAtEpochMs
    )
    is MangaSourceException.NetworkFailure -> MangaSourceHealth(
        kind = MangaSourceHealthKind.NETWORK_ERROR,
        message = message,
        updatedAtEpochMs = updatedAtEpochMs
    )
    is MangaSourceException.SourceChanged -> MangaSourceHealth(
        kind = MangaSourceHealthKind.SOURCE_CHANGED,
        message = message,
        updatedAtEpochMs = updatedAtEpochMs
    )
    is MangaSourceException.ParseFailure -> MangaSourceHealth(
        kind = MangaSourceHealthKind.PARSE_ERROR,
        message = message,
        updatedAtEpochMs = updatedAtEpochMs
    )
    is MangaSourceException.Unsupported -> MangaSourceHealth(
        kind = MangaSourceHealthKind.UNSUPPORTED,
        message = message,
        updatedAtEpochMs = updatedAtEpochMs
    )
    else -> MangaSourceHealth(
        kind = MangaSourceHealthKind.NETWORK_ERROR,
        message = message,
        updatedAtEpochMs = updatedAtEpochMs
    )
}
