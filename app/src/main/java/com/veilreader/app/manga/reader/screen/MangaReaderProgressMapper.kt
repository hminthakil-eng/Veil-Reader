package com.veilreader.app.manga.reader.screen

import com.veilreader.app.manga.library.MangaReadingProgress
import com.veilreader.app.manga.reader.MangaReaderPosition
import com.veilreader.app.manga.reader.MangaReaderState

object MangaReaderProgressMapper {

    fun toProgress(
        reader: MangaReaderState,
        updatedAtEpochMs: Long
    ): MangaReadingProgress {
        val pageCount = reader.pageCount
        val index = reader.position.itemIndex
        val progression = when {
            pageCount == null || pageCount <= 1 -> 0.0

            reader.position is MangaReaderPosition.Webtoon -> {
                val position = reader.position
                ((position.itemIndex + position.offsetFraction) / pageCount.toDouble())
                    .coerceIn(0.0, 1.0)
            }

            else -> (index.toDouble() / (pageCount - 1).toDouble())
                .coerceIn(0.0, 1.0)
        }

        return MangaReadingProgress(
            mangaId = reader.chapter.mangaId,
            chapter = reader.chapter.anchor,
            pageIndex = index,
            pageCount = pageCount,
            chapterProgression = progression,
            updatedAtEpochMs = updatedAtEpochMs
        )
    }

    fun remappedItemIndex(
        progress: MangaReadingProgress,
        newPageCount: Int
    ): Int {
        require(newPageCount > 0)
        return if (progress.pageCount == newPageCount) {
            progress.pageIndex.coerceIn(0, newPageCount - 1)
        } else {
            progress.pageIndexFor(newPageCount)
        }
    }
}
