package com.veilreader.app.manga.source

/**
 * Stable source identifier owned by Veil, not by a remote website.
 *
 * Keep this independent from package names or URLs so domains can change without rewriting
 * persisted library identities.
 */
@JvmInline
value class SourceId(val value: String) {
    init {
        require(value.matches(Regex("[a-z0-9][a-z0-9._-]{1,63}"))) {
            "SourceId must be 2-64 lowercase characters: $value"
        }
    }

    override fun toString(): String = value
}

enum class MangaContentType {
    MANGA,
    MANHWA,
    MANHUA,
    WEBTOON,
    COMIC,
    UNKNOWN
}

enum class MangaPublicationStatus {
    ONGOING,
    COMPLETED,
    HIATUS,
    CANCELLED,
    UNKNOWN
}

enum class MangaSourceCapability {
    SEARCH,
    DETAILS,
    CHAPTERS,
    PAGES,
    FILTERS,
    RELATED,
    AUTHENTICATION,
    DOMAIN_FAILOVER,
    BROWSER_CHALLENGE,
    OFFLINE_CACHE
}

data class MangaSourceDescriptor(
    val id: SourceId,
    val displayName: String,
    /**
     * Ordered domains. Index 0 is primary; remaining entries are mirrors/fallbacks.
     * No reader state should persist a domain as the source identity.
     */
    val domains: List<String>,
    val localeTags: Set<String> = emptySet(),
    val contentTypes: Set<MangaContentType> = setOf(MangaContentType.UNKNOWN)
) {
    init {
        require(displayName.isNotBlank()) { "Source display name cannot be blank" }
        require(domains.isNotEmpty()) { "A source must declare at least one domain" }
        require(domains.none(String::isBlank)) { "Source domains cannot be blank" }
        require(domains.distinct().size == domains.size) { "Source domains must be unique" }
    }

    val primaryDomain: String get() = domains.first()
}

data class SourceMangaRef(
    val sourceId: SourceId,
    /** Stable key inside the provider. It must not include a mutable host/domain. */
    val key: String,
    val publicUrl: String? = null
) {
    init {
        require(key.isNotBlank()) { "Source manga key cannot be blank" }
    }
}

data class SourceMangaSummary(
    val ref: SourceMangaRef,
    val title: String,
    val alternativeTitles: Set<String> = emptySet(),
    val coverUrl: String? = null,
    val languageTag: String? = null,
    val contentType: MangaContentType = MangaContentType.UNKNOWN
)

data class SourceMangaDetails(
    val summary: SourceMangaSummary,
    val description: String? = null,
    val authors: Set<String> = emptySet(),
    val status: MangaPublicationStatus = MangaPublicationStatus.UNKNOWN,
    val tags: Set<String> = emptySet()
)

data class SourceChapter(
    val sourceId: SourceId,
    val mangaKey: String,
    val chapterKey: String,
    val title: String? = null,
    val number: Double? = null,
    val volume: Double? = null,
    val languageTag: String? = null,
    val scanlator: String? = null,
    val publishedAtEpochMs: Long? = null
) {
    init {
        require(mangaKey.isNotBlank()) { "Chapter manga key cannot be blank" }
        require(chapterKey.isNotBlank()) { "Chapter key cannot be blank" }
    }
}

data class MangaPageImage(
    val index: Int,
    val imageUrl: String,
    /** Provider-specific headers such as Referer. Never persist secrets here. */
    val requestHeaders: Map<String, String> = emptyMap()
) {
    init {
        require(index >= 0) { "Page index cannot be negative" }
        require(imageUrl.isNotBlank()) { "Page image URL cannot be blank" }
    }
}

data class SourceSearchRequest(
    val query: String,
    val continuationToken: String? = null,
    val limit: Int = 30
) {
    init {
        require(query.isNotBlank()) { "Search query cannot be blank" }
        require(limit in 1..100) { "Search limit must be between 1 and 100" }
    }
}

data class PagedSourceResult<T>(
    val items: List<T>,
    val nextContinuationToken: String? = null
)
