package com.veilreader.app.manga.core

interface MangaSourceProvider {
    val descriptor: MangaSourceDescriptor

    suspend fun search(query: String, cursor: String? = null): MangaResultPage<MangaSummary>

    suspend fun popular(cursor: String? = null): MangaResultPage<MangaSummary> =
        throw UnsupportedOperationException("Popular browse is not supported by this source.")

    suspend fun latest(cursor: String? = null): MangaResultPage<MangaSummary> =
        throw UnsupportedOperationException("Latest browse is not supported by this source.")

    suspend fun details(ref: MangaRef): MangaDetails
    suspend fun chapters(ref: MangaRef): List<MangaChapter>
    suspend fun pages(ref: MangaChapterRef): List<MangaPage>
}

class MangaSourceRegistry(providers: Iterable<MangaSourceProvider>) {
    private val providersById: Map<MangaSourceId, MangaSourceProvider>

    init {
        val items = providers.toList()
        val duplicates = items.groupBy { it.descriptor.id }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "Duplicate manga source ids are not allowed." }
        providersById = items.associateBy { it.descriptor.id }
    }

    fun sources(): List<MangaSourceDescriptor> = providersById.values
        .map { it.descriptor }
        .sortedWith(compareBy(MangaSourceDescriptor::language, MangaSourceDescriptor::name))

    fun provider(id: MangaSourceId): MangaSourceProvider =
        requireNotNull(providersById[id]) { "Unknown manga source: " + id.value }
}