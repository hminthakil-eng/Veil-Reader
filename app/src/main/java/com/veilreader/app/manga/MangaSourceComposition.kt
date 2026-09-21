package com.veilreader.app.manga

import com.veilreader.app.manga.core.MangaFeaturePolicy
import com.veilreader.app.manga.core.MangaSourceCatalog
import com.veilreader.app.manga.core.MangaSourceOrigin
import com.veilreader.app.manga.core.MangaSourceProvider
import com.veilreader.app.manga.core.MangaSourceRegistration

data class MangaSourceGroups(
    val local: List<MangaSourceProvider> = emptyList(),
    val direct: List<MangaSourceProvider> = emptyList(),
    val gateway: List<MangaSourceProvider> = emptyList()
)

/**
 * App-layer composition boundary for Manga sources.
 *
 * Provider implementations remain independent of feature policy:
 * - local sources are eligible by default once the hub is enabled;
 * - direct and gateway sources are registered fail-closed and require an explicit source enable.
 *
 * Dynamic gateways such as Suwayomi perform discovery outside this class and pass the resulting
 * providers in [MangaSourceGroups.gateway]. This keeps gateway/network concerns out of core.
 */
object MangaSourceComposition {
    fun registrations(groups: MangaSourceGroups): List<MangaSourceRegistration> = buildList {
        addAll(
            groups.local.map { provider ->
                MangaSourceRegistration(
                    provider = provider,
                    origin = MangaSourceOrigin.LOCAL,
                    enabledByDefault = true
                )
            }
        )
        addAll(
            groups.direct.map { provider ->
                MangaSourceRegistration(
                    provider = provider,
                    origin = MangaSourceOrigin.DIRECT,
                    enabledByDefault = false
                )
            }
        )
        addAll(
            groups.gateway.map { provider ->
                MangaSourceRegistration(
                    provider = provider,
                    origin = MangaSourceOrigin.GATEWAY,
                    enabledByDefault = false
                )
            }
        )
    }

    fun catalog(
        groups: MangaSourceGroups,
        policy: MangaFeaturePolicy = MangaFeaturePolicy()
    ): MangaSourceCatalog =
        MangaSourceCatalog(
            registrations = registrations(groups),
            policy = policy
        )
}
