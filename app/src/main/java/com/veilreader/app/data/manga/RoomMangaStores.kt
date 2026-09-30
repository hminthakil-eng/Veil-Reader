package com.veilreader.app.data.manga

import com.veilreader.app.data.db.MangaChapterEntity
import com.veilreader.app.data.db.MangaOfflineChapterWithPages
import com.veilreader.app.data.db.MangaProgressEntity
import com.veilreader.app.data.db.MangaOfflineChapterEntity
import com.veilreader.app.data.db.MangaOfflinePageEntity
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.MangaCacheLayout
import com.veilreader.app.manga.library.MangaChapterAnchor
import com.veilreader.app.manga.library.MangaOfflineCacheIndex
import com.veilreader.app.manga.library.MangaOfflineChapterLocator
import com.veilreader.app.manga.library.MangaProgressStore
import com.veilreader.app.manga.library.MangaReadingProgress
import com.veilreader.app.manga.library.OfflineChapterId
import com.veilreader.app.manga.library.OfflineChapterManifest
import com.veilreader.app.manga.library.OfflinePageEntry
import com.veilreader.app.manga.reader.MangaReaderChapterRef
import com.veilreader.app.manga.source.SourceId

/**
 * Room-backed durable Manga progress.
 *
 * Persisted Manga identity is the owning BookEntity id. Progress is never allowed to invent a
 * chapter row; the catalog must establish chapter order/identity first.
 */
class RoomMangaProgressStore(
    database: VeilDatabase
) : MangaProgressStore {
    private val books = database.books()
    private val catalog = database.mangaCatalog()
    private val progress = database.mangaProgress()

    override suspend fun load(mangaId: CanonicalMangaId): MangaReadingProgress? {
        val stored = progress.find(mangaId.value) ?: return null
        val chapter = catalog.findChapter(stored.chapterId) ?: return null
        if (chapter.bookId != mangaId.value) return null

        val sourceHint = catalog.listChapterSources(chapter.id)
            .firstOrNull()
            ?.chapterKey

        return MangaReadingProgress(
            mangaId = mangaId,
            chapter = chapter.toAnchor(sourceHint),
            pageIndex = stored.pageIndex,
            pageCount = stored.pageCount,
            chapterProgression = stored.chapterProgression,
            updatedAtEpochMs = stored.updatedAtEpochMs
        )
    }

    override suspend fun save(progressValue: MangaReadingProgress) {
        val chapter = findPersistedChapter(progressValue)
            ?: throw IllegalStateException(
                "Manga progress cannot be persisted before its chapter is in the catalog"
            )

        progress.upsert(
            MangaProgressEntity(
                bookId = progressValue.mangaId.value,
                chapterId = chapter.id,
                pageIndex = progressValue.pageIndex,
                pageCount = progressValue.pageCount,
                chapterProgression = progressValue.chapterProgression,
                updatedAtEpochMs = progressValue.updatedAtEpochMs
            )
        )

        val chapters = catalog.listChapters(progressValue.mangaId.value)
        val chapterIndex = chapters.indexOfFirst { it.id == chapter.id }
        if (chapterIndex >= 0 && chapters.isNotEmpty()) {
            val overallProgress = (
                (chapterIndex.toDouble() + progressValue.chapterProgression) /
                    chapters.size.toDouble()
                ).coerceIn(0.0, 1.0)
            val finished =
                chapterIndex == chapters.lastIndex &&
                    progressValue.chapterProgression >= FINISHED_PROGRESSION
            books.updateMangaProgressSummary(
                id = progressValue.mangaId.value,
                progress = overallProgress.toFloat(),
                updatedAtEpochMs = progressValue.updatedAtEpochMs,
                finished = finished
            )
        }
    }

    override suspend fun delete(mangaId: CanonicalMangaId) {
        progress.delete(mangaId.value)
    }

    private suspend fun findPersistedChapter(
        value: MangaReadingProgress
    ): MangaChapterEntity? {
        MangaOfflineChapterLocator.idFor(value.mangaId, value.chapter)
            ?.let(MangaCacheLayout::chapterDirectory)
            ?.let { cacheKey ->
                catalog.findChapterByCacheKey(cacheKey)
                    ?.takeIf { it.bookId == value.mangaId.value }
            }
            ?.let { return it }

        val target = MangaReaderChapterRef(
            mangaId = value.mangaId,
            anchor = value.chapter
        )
        return catalog.listChapters(value.mangaId.value).firstOrNull { candidate ->
            MangaReaderChapterRef(
                mangaId = value.mangaId,
                anchor = candidate.toAnchor()
            ).sameLogicalChapter(target)
        }
    }
}

/**
 * Room-backed offline manifest index. File bytes remain in the explicit Veil Manga cache; Room
 * stores only verified manifest facts and paths relative to that cache root.
 */
class RoomMangaOfflineCacheIndex(
    database: VeilDatabase
) : MangaOfflineCacheIndex {
    private val catalog = database.mangaCatalog()
    private val offline = database.mangaOffline()

    override suspend fun load(chapterId: OfflineChapterId): OfflineChapterManifest? {
        val cacheKey = MangaCacheLayout.chapterDirectory(chapterId)
        val chapter = catalog.findChapterByCacheKey(cacheKey) ?: return null
        if (chapter.bookId != chapterId.mangaId.value) return null
        val bundle = offline.findChapter(chapter.id) ?: return null
        return bundle.toManifest(chapter, chapterId)
    }

    override suspend fun put(manifest: OfflineChapterManifest) {
        val cacheKey = MangaCacheLayout.chapterDirectory(manifest.chapterId)
        val chapter = catalog.findChapterByCacheKey(cacheKey)
            ?: throw IllegalStateException(
                "Offline Manga manifest cannot be persisted before its chapter is in the catalog"
            )
        check(chapter.bookId == manifest.chapterId.mangaId.value) {
            "Offline Manga manifest belongs to a different catalog book"
        }

        offline.replaceChapter(
            chapter = MangaOfflineChapterEntity(
                chapterId = chapter.id,
                originSourceId = manifest.originSourceId.value,
                originChapterKey = manifest.originChapterKey,
                completed = manifest.completed,
                updatedAtEpochMs = manifest.updatedAtEpochMs
            ),
            pages = manifest.pages
                .sortedBy(OfflinePageEntry::index)
                .map { page ->
                    MangaOfflinePageEntity(
                        chapterId = chapter.id,
                        pageIndex = page.index,
                        relativePath = page.relativePath,
                        byteSize = page.byteSize,
                        contentSha256 = page.contentSha256
                    )
                }
        )
    }

    override suspend fun remove(chapterId: OfflineChapterId) {
        val cacheKey = MangaCacheLayout.chapterDirectory(chapterId)
        val chapter = catalog.findChapterByCacheKey(cacheKey) ?: return
        if (chapter.bookId == chapterId.mangaId.value) {
            offline.deleteChapter(chapter.id)
        }
    }

    override suspend fun listForManga(
        mangaId: CanonicalMangaId
    ): List<OfflineChapterManifest> {
        val chaptersById = catalog.listChapters(mangaId.value).associateBy(MangaChapterEntity::id)
        return offline.listForBook(mangaId.value).mapNotNull { bundle ->
            val chapter = chaptersById[bundle.chapter.chapterId] ?: return@mapNotNull null
            val id = chapter.toOfflineChapterId(mangaId) ?: return@mapNotNull null
            bundle.toManifest(chapter, id)
        }
    }

    private fun MangaOfflineChapterWithPages.toManifest(
        chapterEntity: MangaChapterEntity,
        offlineId: OfflineChapterId
    ): OfflineChapterManifest =
        OfflineChapterManifest(
            chapterId = offlineId,
            anchor = chapterEntity.toAnchor(chapter.originChapterKey),
            pages = pages
                .sortedBy { it.pageIndex }
                .map { page ->
                    OfflinePageEntry(
                        index = page.pageIndex,
                        relativePath = page.relativePath,
                        byteSize = page.byteSize,
                        contentSha256 = page.contentSha256
                    )
                },
            originSourceId = SourceId(chapter.originSourceId),
            originChapterKey = chapter.originChapterKey,
            completed = chapter.completed,
            updatedAtEpochMs = chapter.updatedAtEpochMs
        )
}

private fun MangaChapterEntity.toAnchor(
    providerChapterKeyHint: String? = null
): MangaChapterAnchor =
    MangaChapterAnchor(
        volume = volume,
        number = number,
        languageTag = languageTag,
        normalizedTitle = normalizedTitle ?: title,
        providerChapterKeyHint = providerChapterKeyHint
    )

private fun MangaChapterEntity.toOfflineChapterId(
    mangaId: CanonicalMangaId
): OfflineChapterId? =
    MangaOfflineChapterLocator.idFor(
        mangaId = mangaId,
        anchor = toAnchor()
    )
