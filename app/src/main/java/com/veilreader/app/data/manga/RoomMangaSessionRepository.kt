package com.veilreader.app.data.manga

import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.reader.screen.MangaProductChapter
import com.veilreader.app.manga.reader.screen.MangaProductSessionAdapter
import com.veilreader.app.manga.reader.screen.MangaReaderSessionOptions
import com.veilreader.app.manga.reader.screen.MangaSessionAdapterResult
import com.veilreader.app.manga.reader.screen.MangaSessionUnavailableReason
import com.veilreader.app.manga.source.MangaSourceCapability
import com.veilreader.app.manga.source.SourceChapter
import com.veilreader.app.manga.source.SourceId
import com.veilreader.app.manga.source.SourceRegistry
import com.veilreader.app.manga.source.supports
import java.util.UUID

/**
 * Rehydrates the persisted Manga catalog into one reader session.
 *
 * Local/offline chapters do not require a provider instance. When a registered provider exists and
 * declares page delivery, it is attached; otherwise the same route remains valid for offline-only
 * loading.
 */
class RoomMangaSessionRepository(
    database: VeilDatabase,
    private val sourceRegistry: SourceRegistry = SourceRegistry(emptyList()),
    private val adapter: MangaProductSessionAdapter = MangaProductSessionAdapter()
) {
    private val catalog = database.mangaCatalog()

    suspend fun build(
        bookId: String,
        options: MangaReaderSessionOptions = MangaReaderSessionOptions(),
        instanceId: String = UUID.randomUUID().toString()
    ): MangaSessionAdapterResult {
        val chapters = catalog.listChapters(bookId)
        if (chapters.isEmpty()) {
            return MangaSessionAdapterResult.Unavailable(
                MangaSessionUnavailableReason.NO_CHAPTERS
            )
        }

        val productChapters = ArrayList<MangaProductChapter>(chapters.size)
        chapters.forEach { chapter ->
            val sourceLinks = catalog.listChapterSources(chapter.id)
            if (sourceLinks.isEmpty()) {
                return MangaSessionAdapterResult.Unavailable(
                    MangaSessionUnavailableReason.MISSING_SOURCE_LINK
                )
            }

            val selected = sourceLinks.firstOrNull { link ->
                val sourceId = runCatching { SourceId(link.sourceId) }.getOrNull()
                    ?: return@firstOrNull false
                sourceRegistry.find(sourceId)?.supports(MangaSourceCapability.PAGES) == true
            } ?: sourceLinks.first()

            val sourceId = runCatching { SourceId(selected.sourceId) }.getOrElse {
                return MangaSessionAdapterResult.Unavailable(
                    MangaSessionUnavailableReason.MISSING_SOURCE_LINK
                )
            }
            val provider = sourceRegistry.find(sourceId)
                ?.takeIf { it.supports(MangaSourceCapability.PAGES) }

            productChapters += MangaProductChapter(
                sourceChapter = SourceChapter(
                    sourceId = sourceId,
                    mangaKey = selected.mangaKey,
                    chapterKey = selected.chapterKey,
                    title = chapter.title,
                    number = chapter.number,
                    volume = chapter.volume,
                    languageTag = chapter.languageTag
                ),
                provider = provider
            )
        }

        return adapter.build(
            mangaId = CanonicalMangaId(bookId),
            chaptersInReadingOrder = productChapters,
            options = options,
            instanceId = instanceId
        )
    }
}
