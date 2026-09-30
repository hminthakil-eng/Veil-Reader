package com.veilreader.app.manga.library

data class MangaChapterAnchor(
    val volume: Double? = null,
    val number: Double? = null,
    val languageTag: String? = null,
    val normalizedTitle: String? = null,
    val providerChapterKeyHint: String? = null
) {
    init {
        require(volume == null || volume.isFinite()) { "Volume must be finite" }
        require(number == null || number.isFinite()) { "Chapter number must be finite" }
        require(
            volume != null || number != null || !normalizedTitle.isNullOrBlank() ||
                !providerChapterKeyHint.isNullOrBlank()
        ) { "A chapter anchor needs at least one matching signal" }
    }
}

data class MangaReadingProgress(
    val mangaId: CanonicalMangaId,
    val chapter: MangaChapterAnchor,
    val pageIndex: Int,
    val pageCount: Int? = null,
    val chapterProgression: Double,
    val updatedAtEpochMs: Long
) {
    init {
        require(pageIndex >= 0) { "Page index cannot be negative" }
        require(pageCount == null || pageCount > 0) { "Page count must be positive" }
        require(chapterProgression.isFinite() && chapterProgression in 0.0..1.0) {
            "Chapter progression must be between 0 and 1"
        }
    }

    fun pageIndexFor(pageCount: Int): Int {
        require(pageCount > 0) { "Page count must be positive" }
        if (pageCount == 1) return 0
        return (chapterProgression * (pageCount - 1))
            .toInt()
            .coerceIn(0, pageCount - 1)
    }
}

interface MangaProgressStore {
    suspend fun load(mangaId: CanonicalMangaId): MangaReadingProgress?
    suspend fun save(progress: MangaReadingProgress)
    suspend fun delete(mangaId: CanonicalMangaId)
}

class InMemoryMangaProgressStore : MangaProgressStore {
    private val values = linkedMapOf<CanonicalMangaId, MangaReadingProgress>()

    override suspend fun load(mangaId: CanonicalMangaId): MangaReadingProgress? = values[mangaId]

    override suspend fun save(progress: MangaReadingProgress) {
        values[progress.mangaId] = progress
    }

    override suspend fun delete(mangaId: CanonicalMangaId) {
        values.remove(mangaId)
    }
}
