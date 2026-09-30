package com.veilreader.app.manga.reader.screen

import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.MangaChapterAnchor
import com.veilreader.app.manga.reader.MangaReaderChapterRef
import com.veilreader.app.manga.reader.presentation.MangaChapterRoute
import com.veilreader.app.manga.source.MangaSourceCapability
import com.veilreader.app.manga.source.MangaSourceProvider
import com.veilreader.app.manga.source.SourceChapter
import java.util.UUID

/**
 * Product-boundary chapter description used to build one canonical Manga reader session.
 *
 * Ordering is authoritative and must already reflect the user's catalog/source reading order.
 * A null [provider] means the chapter is intentionally offline-only.
 */
data class MangaProductChapter(
    val sourceChapter: SourceChapter,
    val provider: MangaSourceProvider? = null
)

enum class MangaSessionUnavailableReason {
    NO_CHAPTERS,
    PROVIDER_SOURCE_MISMATCH,
    PROVIDER_CANNOT_LOAD_PAGES,
    DUPLICATE_LOGICAL_CHAPTER,
    MISSING_SOURCE_LINK,
    INITIAL_CHAPTER_NOT_FOUND
}

sealed interface MangaSessionAdapterResult {
    data class Ready(val session: MangaReaderSession) : MangaSessionAdapterResult
    data class Unavailable(
        val reason: MangaSessionUnavailableReason
    ) : MangaSessionAdapterResult
}

/**
 * Converts durable catalog/source facts into the dedicated Manga reader session contract.
 *
 * This adapter deliberately does not read Room, scan archives, query remote sources, sort chapters,
 * or invent missing metadata. Persistence and ingestion layers feed it source-neutral facts; the
 * reader then owns presentation, restore, progress and boundary navigation.
 */
class MangaProductSessionAdapter {

    fun build(
        mangaId: CanonicalMangaId,
        chaptersInReadingOrder: List<MangaProductChapter>,
        initialAnchor: MangaChapterAnchor? = null,
        options: MangaReaderSessionOptions = MangaReaderSessionOptions(),
        instanceId: String = UUID.randomUUID().toString()
    ): MangaSessionAdapterResult {
        if (chaptersInReadingOrder.isEmpty()) {
            return MangaSessionAdapterResult.Unavailable(
                MangaSessionUnavailableReason.NO_CHAPTERS
            )
        }

        val entries = ArrayList<MangaReaderChapterEntry>(chaptersInReadingOrder.size)
        chaptersInReadingOrder.forEach { chapter ->
            val provider = chapter.provider
            if (
                provider != null &&
                provider.descriptor.id != chapter.sourceChapter.sourceId
            ) {
                return MangaSessionAdapterResult.Unavailable(
                    MangaSessionUnavailableReason.PROVIDER_SOURCE_MISMATCH
                )
            }
            if (
                provider != null &&
                MangaSourceCapability.PAGES !in provider.capabilities
            ) {
                return MangaSessionAdapterResult.Unavailable(
                    MangaSessionUnavailableReason.PROVIDER_CANNOT_LOAD_PAGES
                )
            }

            val route = MangaChapterRoute.from(
                mangaId = mangaId,
                sourceChapter = chapter.sourceChapter
            )
            entries += MangaReaderChapterEntry(
                route = route,
                provider = provider
            )
        }

        for (left in entries.indices) {
            for (right in left + 1 until entries.size) {
                if (
                    entries[left].route.readerChapter.sameLogicalChapter(
                        entries[right].route.readerChapter
                    )
                ) {
                    return MangaSessionAdapterResult.Unavailable(
                        MangaSessionUnavailableReason.DUPLICATE_LOGICAL_CHAPTER
                    )
                }
            }
        }

        val initialIndex = if (initialAnchor == null) {
            0
        } else {
            val requested = MangaReaderChapterRef(
                mangaId = mangaId,
                anchor = initialAnchor
            )
            entries.indexOfFirst {
                it.route.readerChapter.sameLogicalChapter(requested)
            }.takeIf { it >= 0 } ?: return MangaSessionAdapterResult.Unavailable(
                MangaSessionUnavailableReason.INITIAL_CHAPTER_NOT_FOUND
            )
        }

        return MangaSessionAdapterResult.Ready(
            MangaReaderSession(
                entriesInReadingOrder = entries,
                initialIndex = initialIndex,
                options = options,
                instanceId = instanceId
            )
        )
    }
}
