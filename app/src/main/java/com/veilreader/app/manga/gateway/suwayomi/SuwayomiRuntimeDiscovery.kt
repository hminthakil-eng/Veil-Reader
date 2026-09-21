package com.veilreader.app.manga.gateway.suwayomi

import com.veilreader.app.manga.core.MangaSourceProvider
import kotlinx.coroutines.CancellationException

/**
 * Runtime-only Suwayomi registration. Credentials stay behind an ephemeral token provider and are
 * never part of source identity or persistence.
 */
data class SuwayomiRuntimeServer(
    val config: SuwayomiServerConfig,
    val tokenProvider: SuwayomiAccessTokenProvider = SuwayomiAccessTokenProvider { null },
    val sourceLimit: Int = 500
) {
    init {
        require(sourceLimit in 1..2_000) {
            "Suwayomi source discovery limit is out of range."
        }
    }
}

data class SuwayomiRuntimeDiscoveryFailure(
    val serverId: SuwayomiServerId,
    val error: Throwable
)

data class SuwayomiRuntimeDiscoveryResult(
    val providers: List<MangaSourceProvider>,
    val failures: List<SuwayomiRuntimeDiscoveryFailure>
)

fun interface SuwayomiProviderDiscoverer {
    suspend fun discover(server: SuwayomiRuntimeServer): List<MangaSourceProvider>
}

/**
 * Discovers configured Suwayomi servers independently so one unavailable server cannot suppress
 * healthy gateway providers from every other configured server.
 */
class SuwayomiRuntimeDiscovery(
    private val discoverer: SuwayomiProviderDiscoverer = SuwayomiProviderDiscoverer { server ->
        val transport = OkHttpSuwayomiGraphQlTransport(
            config = server.config,
            tokenProvider = server.tokenProvider
        )
        SuwayomiGateway(
            config = server.config,
            transport = transport,
            tokenProvider = server.tokenProvider
        ).discoverProviders(server.sourceLimit)
    }
) {
    suspend fun discover(
        servers: List<SuwayomiRuntimeServer>
    ): SuwayomiRuntimeDiscoveryResult {
        val duplicates = servers
            .groupBy { it.config.serverId }
            .filterValues { it.size > 1 }
            .keys

        require(duplicates.isEmpty()) {
            "Duplicate Suwayomi runtime server ids are not allowed: " +
                duplicates.joinToString { it.value }
        }

        val providers = mutableListOf<MangaSourceProvider>()
        val failures = mutableListOf<SuwayomiRuntimeDiscoveryFailure>()

        for (server in servers) {
            try {
                providers += discoverer.discover(server)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                failures += SuwayomiRuntimeDiscoveryFailure(
                    serverId = server.config.serverId,
                    error = error
                )
            }
        }

        return SuwayomiRuntimeDiscoveryResult(
            providers = providers,
            failures = failures
        )
    }
}
