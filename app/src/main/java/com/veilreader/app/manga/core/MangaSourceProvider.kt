package com.veilreader.app.manga.core

interface MangaSourceProvider {
    val descriptor: MangaSourceDescriptor

    suspend fun search(request: MangaSearchRequest): MangaResultPage<MangaSummary>

    suspend fun popular(
        request: MangaBrowseRequest
    ): MangaResultPage<MangaSummary> =
        throw MangaSourceException.Unsupported("Popular browse is not supported by this source.")

    suspend fun latest(
        request: MangaBrowseRequest
    ): MangaResultPage<MangaSummary> =
        throw MangaSourceException.Unsupported("Latest browse is not supported by this source.")

    suspend fun filters(): List<MangaFilterDefinition> = emptyList()

    suspend fun resolveUrl(url: String): MangaRef? = null

    suspend fun fetchUpdate(
        ref: MangaRef,
        existingChapters: List<MangaChapter> = emptyList(),
        options: MangaUpdateOptions = MangaUpdateOptions()
    ): MangaUpdate

    suspend fun pages(ref: MangaChapterRef): List<MangaPage>

    suspend fun search(
        query: String,
        cursor: String? = null
    ): MangaResultPage<MangaSummary> =
        search(MangaSearchRequest(query = query, cursor = cursor))

    suspend fun popular(
        cursor: String? = null
    ): MangaResultPage<MangaSummary> =
        popular(MangaBrowseRequest(cursor = cursor))

    suspend fun latest(
        cursor: String? = null
    ): MangaResultPage<MangaSummary> =
        latest(MangaBrowseRequest(cursor = cursor))

    suspend fun details(ref: MangaRef): MangaDetails =
        requireNotNull(
            fetchUpdate(
                ref = ref,
                options = MangaUpdateOptions(fetchDetails = true, fetchChapters = false)
            ).details
        ) { "Source update did not return requested manga details." }

    suspend fun chapters(ref: MangaRef): List<MangaChapter> =
        requireNotNull(
            fetchUpdate(
                ref = ref,
                options = MangaUpdateOptions(fetchDetails = false, fetchChapters = true)
            ).chapters
        ) { "Source update did not return requested manga chapters." }
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
