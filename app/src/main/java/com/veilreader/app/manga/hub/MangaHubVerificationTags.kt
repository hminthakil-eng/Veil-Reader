package com.veilreader.app.manga.hub

object MangaHubVerificationTags {
    const val ROOT = "manga-hub-root"
    const val DISCOVER = "manga-hub-tab-discover"
    const val SEARCH = "manga-hub-tab-search"
    const val LIBRARY = "manga-hub-tab-library"
    const val SEARCH_FIELD = "manga-hub-search-field"
    const val SEARCH_ACTION = "manga-hub-search-action"
    const val DETAILS = "manga-hub-details"
    const val ADD = "manga-hub-add"
    const val REMOVE = "manga-hub-remove"
    const val READ = "manga-hub-read"

    fun catalog(sourceId: String, sourceKey: String): String =
        "manga-hub-catalog-" + sourceId + "-" + sourceKey

    fun library(canonicalId: String): String =
        "manga-hub-library-" + canonicalId

    fun chapter(sourceId: String, chapterKey: String): String =
        "manga-hub-chapter-" + sourceId + "-" + chapterKey
}
