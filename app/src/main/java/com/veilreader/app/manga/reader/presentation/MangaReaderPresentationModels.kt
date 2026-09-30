package com.veilreader.app.manga.reader.presentation

import com.veilreader.app.manga.reader.MangaReaderChapterRef
import com.veilreader.app.manga.source.SourceFailure

sealed interface MangaPageAsset {
    val index: Int

    data class Local(
        override val index: Int,
        val relativePath: String,
        val byteSize: Long,
        val contentSha256: String? = null
    ) : MangaPageAsset {
        init {
            require(index >= 0)
            require(relativePath.isNotBlank())
            require(byteSize >= 0)
        }
    }

    /**
     * Headers live in memory only and must never be copied into SavedState, logs or durable reader
     * progress. Providers may require short-lived Referer/Cookie-like request metadata.
     */
    data class Remote(
        override val index: Int,
        val imageUrl: String,
        val requestHeaders: Map<String, String> = emptyMap()
    ) : MangaPageAsset {
        init {
            require(index >= 0)
            require(imageUrl.isNotBlank())
        }

        override fun toString(): String =
            "Remote(index=$index, imageUrl=<redacted>, requestHeaders=<redacted>)"
    }
}

enum class MangaChapterAvailability {
    COMPLETE_OFFLINE,
    COMPLETE_ONLINE,
    HYBRID,
    PARTIAL_OFFLINE
}

data class MangaReadyPresentation(
    val chapter: MangaReaderChapterRef,
    val pages: List<MangaPageAsset>,
    val availability: MangaChapterAvailability,
    val complete: Boolean
) {
    init {
        require(pages.isNotEmpty()) { "Ready presentation needs at least one page" }
        require(pages.map { it.index }.distinct().size == pages.size) {
            "Presentation page indices must be unique"
        }
        require(pages.sortedBy { it.index }.map { it.index } == pages.indices.toList()) {
            "Presentation pages must be contiguous from index 0"
        }
        require(
            complete == (availability != MangaChapterAvailability.PARTIAL_OFFLINE)
        ) { "Only PARTIAL_OFFLINE may be incomplete" }
    }

    val pageCount: Int get() = pages.size

    fun page(index: Int): MangaPageAsset? =
        pages.getOrNull(index)?.takeIf { it.index == index }
}

enum class MangaPresentationErrorKind {
    OFFLINE_UNAVAILABLE,
    SOURCE_FAILURE,
    EMPTY_CHAPTER,
    INVALID_PAGE_SET,
    REQUEST_MISMATCH
}

data class MangaPresentationError(
    val kind: MangaPresentationErrorKind,
    val message: String,
    val retryable: Boolean,
    val sourceFailure: SourceFailure? = null
)

sealed interface MangaReaderPresentationState {
    data class Loading(
        val chapter: MangaReaderChapterRef
    ) : MangaReaderPresentationState

    data class Ready(
        val value: MangaReadyPresentation
    ) : MangaReaderPresentationState

    data class Error(
        val chapter: MangaReaderChapterRef,
        val error: MangaPresentationError
    ) : MangaReaderPresentationState
}
