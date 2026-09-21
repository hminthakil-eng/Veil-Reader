package com.veilreader.app.manga

import com.veilreader.app.manga.core.MangaFeaturePolicy
import com.veilreader.app.manga.core.MangaSourceCatalog
import com.veilreader.app.manga.core.MangaSourceProvider
import com.veilreader.app.manga.gateway.suwayomi.SuwayomiRuntimeDiscovery
import com.veilreader.app.manga.gateway.suwayomi.SuwayomiRuntimeDiscoveryFailure
import com.veilreader.app.manga.gateway.suwayomi.SuwayomiRuntimeServer
import com.veilreader.app.manga.net.MangaHttpClient
import com.veilreader.app.manga.net.OkHttpMangaHttpClient

data class MangaRuntimeSourceConfig(
    val directFlags: MangaDirectSourceFlags = MangaDirectSourceFlags(),
    val translatedLanguage: String = "en",
    val policy: MangaFeaturePolicy = MangaFeaturePolicy()
)

data class MangaRuntimeCatalogResult(
    val catalog: MangaSourceCatalog,
    val gatewayFailures: List<SuwayomiRuntimeDiscoveryFailure>
)

/**
 * Runtime assembly boundary for the Manga source stack.
 *
 * This is intentionally UI-agnostic:
 * - local providers are supplied by the app/runtime;
 * - direct providers are built through Veil's canonical direct-source factory;
 * - gateway discovery is isolated before providers enter the canonical catalog;
 * - policy remains the single authority for which registered sources are actually enabled.
 */
object MangaRuntimeSourceAssembly {
    fun catalog(
        config: MangaRuntimeSourceConfig = MangaRuntimeSourceConfig(),
        localProviders: List<MangaSourceProvider> = emptyList(),
        gatewayProviders: List<MangaSourceProvider> = emptyList(),
        directHttpClient: MangaHttpClient = OkHttpMangaHttpClient()
    ): MangaSourceCatalog {
        val directProviders = MangaDirectSourceFactory.providers(
            flags = config.directFlags,
            translatedLanguage = config.translatedLanguage,
            httpClient = directHttpClient
        )

        return MangaSourceComposition.catalog(
            groups = MangaSourceGroups(
                local = localProviders,
                direct = directProviders,
                gateway = gatewayProviders
            ),
            policy = config.policy
        )
    }

    suspend fun catalogWithSuwayomi(
        config: MangaRuntimeSourceConfig = MangaRuntimeSourceConfig(),
        localProviders: List<MangaSourceProvider> = emptyList(),
        suwayomiServers: List<SuwayomiRuntimeServer> = emptyList(),
        suwayomiDiscovery: SuwayomiRuntimeDiscovery = SuwayomiRuntimeDiscovery(),
        directHttpClient: MangaHttpClient = OkHttpMangaHttpClient()
    ): MangaRuntimeCatalogResult {
        val discovery = suwayomiDiscovery.discover(suwayomiServers)

        return MangaRuntimeCatalogResult(
            catalog = catalog(
                config = config,
                localProviders = localProviders,
                gatewayProviders = discovery.providers,
                directHttpClient = directHttpClient
            ),
            gatewayFailures = discovery.failures
        )
    }
}
