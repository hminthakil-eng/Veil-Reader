package com.veilreader.app.manga.core

@JvmInline
value class MangaProviderId(val value: String) {
    init { require(value.isNotBlank()) { "Manga provider id cannot be blank." } }
}

@JvmInline
value class MangaSourceId(val value: String) {
    init { require(value.isNotBlank()) { "Manga source id cannot be blank." } }
}

data class MangaSourceDescriptor(
    val id: MangaSourceId,
    val name: String,
    val language: String,
    val providerId: MangaProviderId = MangaProviderId(id.value.substringBefore('.')),
    val version: Int = 1,
    val capabilities: Set<MangaSourceCapability> = emptySet()
) {
    init {
        require(name.isNotBlank()) { "Manga source name cannot be blank." }
        require(language.isNotBlank()) { "Manga source language cannot be blank." }
        require(version > 0) { "Manga source version must be positive." }
    }
}

enum class MangaSourceCapability {
    SEARCH,
    POPULAR,
    LATEST,
    FILTERS,
    URL_RESOLUTION
}

data class MangaRef(val sourceId: MangaSourceId, val key: String) {
    init { require(key.isNotBlank()) { "Manga key cannot be blank." } }
}

data class MangaSummary(
    val ref: MangaRef,
    val title: String,
    val cover: MangaResourceRequest? = null
) {
    init { require(title.isNotBlank()) { "Manga title cannot be blank." } }
}

data class MangaDetails(
    val ref: MangaRef,
    val title: String,
    val description: String = "",
    val authors: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val status: MangaStatus = MangaStatus.UNKNOWN,
    val cover: MangaResourceRequest? = null
) {
    init { require(title.isNotBlank()) { "Manga title cannot be blank." } }
}

enum class MangaStatus { ONGOING, COMPLETED, HIATUS, CANCELLED, UNKNOWN }

data class MangaChapterRef(val manga: MangaRef, val key: String) {
    init { require(key.isNotBlank()) { "Chapter key cannot be blank." } }
}

data class MangaChapter(
    val ref: MangaChapterRef,
    val title: String = "",
    val chapterNumber: Double? = null,
    val volumeNumber: Double? = null,
    val publishedAtEpochMs: Long? = null,
    val scanlator: String? = null
)

data class MangaResourceRequest(
    val url: String,
    val headers: Map<String, String> = emptyMap()
) {
    init {
        require(
            url.startsWith("https://") ||
                url.startsWith("http://") ||
                url.startsWith("file:")
        ) {
            "Manga resource URL must use http, https, or file."
        }
    }
}

data class MangaPage(val index: Int, val image: MangaResourceRequest) {
    init { require(index >= 0) { "Manga page index cannot be negative." } }
}

data class MangaResultPage<T>(
    val items: List<T>,
    val nextCursor: String? = null
)