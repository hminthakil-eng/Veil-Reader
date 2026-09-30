package com.veilreader.app.manga.reader.presentation

import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.MangaChapterAnchor
import com.veilreader.app.manga.reader.MangaReaderBoundary
import com.veilreader.app.manga.reader.MangaReaderChapterRef
import com.veilreader.app.manga.source.SourceChapter

data class MangaChapterRoute(
    val readerChapter: MangaReaderChapterRef,
    val sourceChapter: SourceChapter
) {
    init {
        require(sourceChapter.sourceId.value.isNotBlank())
    }

    companion object {
        fun from(
            mangaId: CanonicalMangaId,
            sourceChapter: SourceChapter
        ): MangaChapterRoute = MangaChapterRoute(
            readerChapter = MangaReaderChapterRef(
                mangaId = mangaId,
                anchor = MangaChapterAnchor(
                    volume = sourceChapter.volume,
                    number = sourceChapter.number,
                    languageTag = sourceChapter.languageTag,
                    normalizedTitle = sourceChapter.title,
                    providerChapterKeyHint = sourceChapter.chapterKey
                )
            ),
            sourceChapter = sourceChapter
        )
    }
}

sealed interface MangaChapterNavigationResult {
    data class Target(
        val route: MangaChapterRoute
    ) : MangaChapterNavigationResult

    data object SeriesBoundary : MangaChapterNavigationResult
    data object CurrentChapterNotFound : MangaChapterNavigationResult
    data object BlockedByPartialOffline : MangaChapterNavigationResult
}

/**
 * [routesInReadingOrder] must be ordered from the first readable chapter to the last. This class
 * deliberately does not guess provider ordering or sort specials by title/number.
 */
class MangaChapterNavigationResolver(
    private val routesInReadingOrder: List<MangaChapterRoute>
) {
    init {
        require(routesInReadingOrder.isNotEmpty()) { "Chapter routes cannot be empty" }
    }

    fun resolve(
        current: MangaReadyPresentation,
        boundary: MangaReaderBoundary
    ): MangaChapterNavigationResult {
        if (!current.complete) {
            return MangaChapterNavigationResult.BlockedByPartialOffline
        }
        if (boundary == MangaReaderBoundary.NONE) {
            return MangaChapterNavigationResult.CurrentChapterNotFound
        }

        val currentIndex = routesInReadingOrder.indexOfFirst {
            it.readerChapter.sameLogicalChapter(current.chapter)
        }
        if (currentIndex < 0) {
            return MangaChapterNavigationResult.CurrentChapterNotFound
        }

        val targetIndex = when (boundary) {
            MangaReaderBoundary.PREVIOUS_CHAPTER -> currentIndex - 1
            MangaReaderBoundary.NEXT_CHAPTER -> currentIndex + 1
            MangaReaderBoundary.NONE -> currentIndex
        }

        return routesInReadingOrder.getOrNull(targetIndex)
            ?.let(MangaChapterNavigationResult::Target)
            ?: MangaChapterNavigationResult.SeriesBoundary
    }
}
