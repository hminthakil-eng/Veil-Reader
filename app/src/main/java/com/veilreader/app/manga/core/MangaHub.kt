package com.veilreader.app.manga.core

class MangaHub(private val registry: MangaSourceRegistry) {
    fun sources(): List<MangaSourceDescriptor> = registry.sources()

    suspend fun search(
        sourceId: MangaSourceId,
        query: String,
        cursor: String? = null
    ): MangaResultPage<MangaSummary> =
        search(
            sourceId = sourceId,
            request = MangaSearchRequest(query = query, cursor = cursor)
        )

    suspend fun search(
        sourceId: MangaSourceId,
        request: MangaSearchRequest
    ): MangaResultPage<MangaSummary> {
        val provider = registry.provider(sourceId)
        requireCapability(provider, MangaSourceCapability.SEARCH)
        return provider.search(request.copy(query = request.query.trim()))
            .validateOwnership(sourceId)
    }

    suspend fun popular(
        sourceId: MangaSourceId,
        cursor: String? = null
    ): MangaResultPage<MangaSummary> =
        popular(sourceId, MangaBrowseRequest(cursor = cursor))

    suspend fun popular(
        sourceId: MangaSourceId,
        request: MangaBrowseRequest
    ): MangaResultPage<MangaSummary> {
        val provider = registry.provider(sourceId)
        requireCapability(provider, MangaSourceCapability.POPULAR)
        return provider.popular(request).validateOwnership(sourceId)
    }

    suspend fun latest(
        sourceId: MangaSourceId,
        cursor: String? = null
    ): MangaResultPage<MangaSummary> =
        latest(sourceId, MangaBrowseRequest(cursor = cursor))

    suspend fun latest(
        sourceId: MangaSourceId,
        request: MangaBrowseRequest
    ): MangaResultPage<MangaSummary> {
        val provider = registry.provider(sourceId)
        requireCapability(provider, MangaSourceCapability.LATEST)
        return provider.latest(request).validateOwnership(sourceId)
    }

    suspend fun filters(sourceId: MangaSourceId): List<MangaFilterDefinition> {
        val provider = registry.provider(sourceId)
        requireCapability(provider, MangaSourceCapability.FILTERS)
        return provider.filters()
    }

    suspend fun resolveUrl(sourceId: MangaSourceId, url: String): MangaRef? {
        val provider = registry.provider(sourceId)
        requireCapability(provider, MangaSourceCapability.URL_RESOLUTION)
        return provider.resolveUrl(url)?.also { ref ->
            require(ref.sourceId == sourceId) {
                "Source resolved a manga owned by another source."
            }
        }
    }

    suspend fun update(
        ref: MangaRef,
        existingChapters: List<MangaChapter> = emptyList(),
        options: MangaUpdateOptions = MangaUpdateOptions()
    ): MangaUpdate {
        val provider = registry.provider(ref.sourceId)
        val update = provider.fetchUpdate(
            ref = ref,
            existingChapters = existingChapters,
            options = options
        )

        require(update.ref == ref) {
            "Source returned an update for a different manga."
        }
        update.details?.let { details ->
            require(details.ref == ref) {
                "Source returned details for a different manga."
            }
        }
        update.chapters?.let { chapters ->
            require(chapters.all { it.ref.manga == ref }) {
                "Source returned chapters for a different manga."
            }
        }
        return update
    }

    suspend fun details(ref: MangaRef): MangaDetails =
        requireNotNull(
            update(
                ref = ref,
                options = MangaUpdateOptions(fetchDetails = true, fetchChapters = false)
            ).details
        )

    suspend fun chapters(ref: MangaRef): List<MangaChapter> =
        requireNotNull(
            update(
                ref = ref,
                options = MangaUpdateOptions(fetchDetails = false, fetchChapters = true)
            ).chapters
        )

    suspend fun pages(ref: MangaChapterRef): List<MangaPage> =
        registry.provider(ref.manga.sourceId).pages(ref)
            .sortedBy(MangaPage::index)
            .also { pages ->
                require(pages.map(MangaPage::index).distinct().size == pages.size) {
                    "Source returned duplicate manga page indices."
                }
            }

    private fun requireCapability(
        provider: MangaSourceProvider,
        capability: MangaSourceCapability
    ) {
        require(capability in provider.descriptor.capabilities) {
            "Source " + provider.descriptor.id.value + " does not advertise " + capability + "."
        }
    }

    private fun MangaResultPage<MangaSummary>.validateOwnership(
        sourceId: MangaSourceId
    ): MangaResultPage<MangaSummary> = also { page ->
        require(page.items.all { it.ref.sourceId == sourceId }) {
            "Source returned manga owned by another source."
        }
    }
}

data class MangaReadingProgress(
    val manga: MangaRef,
    val chapter: MangaChapterRef,
    val pageIndex: Int,
    val updatedAtEpochMs: Long
) {
    init {
        require(chapter.manga == manga) { "Progress chapter must belong to the same manga." }
        require(pageIndex >= 0) { "Progress page index cannot be negative." }
    }
}

interface MangaProgressStore {
    suspend fun load(manga: MangaRef): MangaReadingProgress?
    suspend fun save(progress: MangaReadingProgress)
}

interface MangaOfflineStore {
    suspend fun isChapterAvailable(ref: MangaChapterRef): Boolean
    suspend fun loadChapter(ref: MangaChapterRef): List<MangaPage>?
    suspend fun saveChapter(ref: MangaChapterRef, pages: List<MangaPage>)
    suspend fun removeChapter(ref: MangaChapterRef)
}