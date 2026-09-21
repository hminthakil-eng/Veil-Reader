package com.veilreader.app.manga

import com.veilreader.app.manga.core.MangaFeaturePolicy
import com.veilreader.app.manga.core.MangaSourceCatalog
import com.veilreader.app.manga.core.MangaSourceProvider
import com.veilreader.app.manga.net.MangaHttpClient
import com.veilreader.app.manga.net.OkHttpMangaHttpClient

data class MangaRuntimeSourceConfig(
    val directFlags: MangaDirectSourceFlags = MangaDirectSourceFlags(),
    val translatedLanguage: String = "en",
    val policy: MangaFeaturePolicy = MangaFeaturePolicy()
)

/**
 * Runtime assembly boundary for the Manga source stack.
 *
 * This is intentionally UI-agnostic:
 * - local providers are supplied by the app/runtime;
 * - direct providers are built through Veil's canonical direct-source factory;
 * - gateway discovery happens outside this object and the discovered providers are supplied here;
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
}
