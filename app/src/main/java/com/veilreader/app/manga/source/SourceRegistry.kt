package com.veilreader.app.manga.source

import java.util.Locale

/**
 * Process-local registry. Persistence stores SourceId only; provider instances remain replaceable.
 */
class SourceRegistry(providers: Iterable<MangaSourceProvider>) {

    private val byId: Map<SourceId, MangaSourceProvider>

    init {
        val providerList = providers.toList()
        val duplicates = providerList
            .groupBy { it.descriptor.id }
            .filterValues { it.size > 1 }
            .keys

        require(duplicates.isEmpty()) {
            "Duplicate manga source ids: ${duplicates.joinToString()}"
        }

        byId = providerList.associateBy { it.descriptor.id }
    }

    fun all(): List<MangaSourceProvider> =
        byId.values.sortedWith(
            compareBy<MangaSourceProvider> {
                it.descriptor.displayName.lowercase(Locale.ROOT)
            }.thenBy {
                it.descriptor.id.value
            }
        )

    fun find(id: SourceId): MangaSourceProvider? = byId[id]

    fun require(id: SourceId): MangaSourceProvider =
        checkNotNull(find(id)) { "Unknown manga source: $id" }

    fun supporting(capability: MangaSourceCapability): List<MangaSourceProvider> =
        all().filter { it.supports(capability) }

    fun candidates(
        capability: MangaSourceCapability,
        languageTag: String? = null,
        contentType: MangaContentType? = null
    ): List<MangaSourceProvider> = supporting(capability).filter { provider ->
        val descriptor = provider.descriptor
        val languageMatches = languageTag == null ||
            descriptor.localeTags.isEmpty() ||
            descriptor.localeTags.any { it.equals(languageTag, ignoreCase = true) }
        val typeMatches = contentType == null ||
            MangaContentType.UNKNOWN in descriptor.contentTypes ||
            contentType in descriptor.contentTypes

        languageMatches && typeMatches
    }
}
