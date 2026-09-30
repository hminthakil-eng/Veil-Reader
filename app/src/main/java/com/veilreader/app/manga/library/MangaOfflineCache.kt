package com.veilreader.app.manga.library

import com.veilreader.app.manga.source.SourceId

data class OfflineChapterId(
    val mangaId: CanonicalMangaId,
    val languageTag: String?,
    val volume: Double?,
    val number: Double?,
    val discriminator: String = "main"
) {
    init {
        require(discriminator.isNotBlank()) { "Chapter discriminator cannot be blank" }
        require(volume == null || volume.isFinite()) { "Volume must be finite" }
        require(number == null || number.isFinite()) { "Chapter number must be finite" }
    }
}

data class OfflinePageEntry(
    val index: Int,
    val relativePath: String,
    val byteSize: Long,
    val contentSha256: String? = null
) {
    init {
        require(index >= 0) { "Offline page index cannot be negative" }
        require(byteSize >= 0) { "Offline page size cannot be negative" }
        require(isSafeRelativePath(relativePath)) { "Unsafe offline cache path: " + relativePath }
    }
}

data class OfflineChapterManifest(
    val chapterId: OfflineChapterId,
    val anchor: MangaChapterAnchor,
    val pages: List<OfflinePageEntry>,
    val originSourceId: SourceId,
    val originChapterKey: String,
    val completed: Boolean,
    val updatedAtEpochMs: Long
) {
    init {
        require(originChapterKey.isNotBlank()) { "Origin chapter key cannot be blank" }
        require(pages.map { it.index }.distinct().size == pages.size) {
            "Offline manifest page indices must be unique"
        }
    }

    val cachedPageCount: Int get() = pages.size

    fun page(index: Int): OfflinePageEntry? = pages.firstOrNull { it.index == index }
}

interface MangaOfflineCacheIndex {
    suspend fun load(chapterId: OfflineChapterId): OfflineChapterManifest?
    suspend fun put(manifest: OfflineChapterManifest)
    suspend fun remove(chapterId: OfflineChapterId)
    suspend fun listForManga(mangaId: CanonicalMangaId): List<OfflineChapterManifest>
}

class InMemoryMangaOfflineCacheIndex : MangaOfflineCacheIndex {
    private val values = linkedMapOf<OfflineChapterId, OfflineChapterManifest>()

    override suspend fun load(chapterId: OfflineChapterId): OfflineChapterManifest? = values[chapterId]

    override suspend fun put(manifest: OfflineChapterManifest) {
        values[manifest.chapterId] = manifest
    }

    override suspend fun remove(chapterId: OfflineChapterId) {
        values.remove(chapterId)
    }

    override suspend fun listForManga(mangaId: CanonicalMangaId): List<OfflineChapterManifest> =
        values.values.filter { it.chapterId.mangaId == mangaId }
}

object MangaCacheLayout {
    fun chapterDirectory(chapterId: OfflineChapterId): String {
        val language = safeSegment(chapterId.languageTag ?: "und")
        val volume = numberSegment(chapterId.volume)
        val chapter = numberSegment(chapterId.number)
        val discriminator = safeSegment(chapterId.discriminator)
        return "manga/" + safeSegment(chapterId.mangaId.value) + "/" + language +
            "/v" + volume + "/c" + chapter + "-" + discriminator
    }

    fun pagePath(chapterId: OfflineChapterId, pageIndex: Int, extension: String): String {
        require(pageIndex >= 0) { "Page index cannot be negative" }
        val ext = extension.trim().lowercase().removePrefix(".")
        require(ext.matches(Regex("[a-z0-9]{1,8}"))) { "Unsafe page extension" }
        return chapterDirectory(chapterId) + "/page-" +
            pageIndex.toString().padStart(5, '0') + "." + ext
    }

    private fun numberSegment(value: Double?): String =
        value?.toString()?.replace('.', '_') ?: "na"

    private fun safeSegment(value: String): String =
        value.lowercase()
            .map { if (it.isLetterOrDigit() || it == '-' || it == '_') it else '_' }
            .joinToString("")
            .trim('_')
            .take(96)
            .ifEmpty { "unknown" }
}

private fun isSafeRelativePath(path: String): Boolean {
    if (path.isBlank() || path.startsWith("/") || path.startsWith("\\")) return false
    if (Regex("^[A-Za-z]:").containsMatchIn(path)) return false
    val segments = path.replace('\\', '/').split('/')
    return segments.none { it == ".." || it.isBlank() }
}
