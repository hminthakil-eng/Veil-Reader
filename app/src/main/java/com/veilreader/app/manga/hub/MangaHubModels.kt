package com.veilreader.app.manga.hub

import com.veilreader.app.manga.library.CanonicalManga
import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.source.MangaSourceDescriptor
import com.veilreader.app.manga.source.SourceChapter
import com.veilreader.app.manga.source.SourceFailure
import com.veilreader.app.manga.source.SourceMangaDetails
import com.veilreader.app.manga.source.SourceMangaRef
import com.veilreader.app.manga.source.SourceMangaSummary

data class MangaHubCatalogItem(
    val summary: SourceMangaSummary,
    val source: MangaSourceDescriptor,
    val canonicalId: CanonicalMangaId? = null
) {
    val inLibrary: Boolean get() = canonicalId != null
}

data class MangaHubWorkDetails(
    val details: SourceMangaDetails,
    val chaptersInReadingOrder: List<SourceChapter>,
    val source: MangaSourceDescriptor,
    val canonical: CanonicalManga? = null
)

data class MangaHubSourceIssue(
    val source: MangaSourceDescriptor,
    val failure: SourceFailure
)

data class MangaHubBatchResult<T>(
    val items: List<T>,
    val issues: List<MangaHubSourceIssue> = emptyList()
)

class MangaHubException(
    message: String,
    val failure: SourceFailure? = null
) : IllegalStateException(message)
