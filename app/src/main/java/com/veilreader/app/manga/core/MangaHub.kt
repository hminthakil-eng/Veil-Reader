package com.veilreader.app.manga.core

class MangaHub(private val registry: MangaSourceRegistry) {
    fun sources(): List<MangaSourceDescriptor> = registry.sources()

    suspend fun search(
        sourceId: MangaSourceId,
        query: String,
        cursor: String? = null
    ): MangaResultPage<MangaSummary> {
        require(query.isNotBlank()) { "Search query cannot be blank." }
        val provider = registry.provider(sourceId)
        require(MangaSourceCapability.SEARCH in provider.descriptor.capabilities) {
            "Source does not advertise search support."
        }
        return provider.search(query.trim(), cursor).also { page ->
            require(page.items.all { it.ref.sourceId == sourceId }) {
                "Source returned manga owned by another source."
            }
        }
    }

    suspend fun popular(
        sourceId: MangaSourceId,
        cursor: String? = null
    ): MangaResultPage<MangaSummary> {
        val provider = registry.provider(sourceId)
        require(MangaSourceCapability.POPULAR in provider.descriptor.capabilities) {
            "Source does not advertise popular browse support."
        }
        return provider.popular(cursor).also { page ->
            require(page.items.all { it.ref.sourceId == sourceId }) {
                "Source returned manga owned by another source."
            }
        }
    }

    suspend fun details(ref: MangaRef): MangaDetails {
        val result = registry.provider(ref.sourceId).details(ref)
        require(result.ref == ref) { "Source returned details for a different manga." }
        return result
    }

    suspend fun chapters(ref: MangaRef): List<MangaChapter> =
        registry.provider(ref.sourceId).chapters(ref).also { chapters ->
            require(chapters.all { it.ref.manga == ref }) {
                "Source returned chapters for a different manga."
            }
        }

    suspend fun pages(ref: MangaChapterRef): List<MangaPage> =
        registry.provider(ref.manga.sourceId).pages(ref)
            .sortedBy(MangaPage::index)
            .also { pages ->
                require(pages.map(MangaPage::index).distinct().size == pages.size) {
                    "Source returned duplicate manga page indices."
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
    suspend fun saveChapter(ref: MangaChapterRef, pages: List<MangaPage>)
    suspend fun removeChapter(ref: MangaChapterRef)
}