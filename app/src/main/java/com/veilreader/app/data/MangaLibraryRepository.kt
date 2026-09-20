package com.veilreader.app.data

import androidx.room.withTransaction
import com.veilreader.app.data.db.MangaChapterBindingEntity
import com.veilreader.app.data.db.MangaChapterEntity
import com.veilreader.app.data.db.MangaSourceBindingEntity
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.BookMetadataUpdate
import com.veilreader.app.manga.core.MangaChapter
import com.veilreader.app.manga.core.MangaChapterRef
import com.veilreader.app.manga.core.MangaRef
import com.veilreader.app.manga.core.MangaSourceDescriptor
import com.veilreader.app.manga.core.MangaSourceId
import com.veilreader.app.manga.core.MangaUpdate
import java.util.UUID
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class MangaSyncResult(
    val bookId: String,
    val sourceId: MangaSourceId,
    val chapterCount: Int,
    val createdChapterIds: List<String>,
    val reusedChapterIds: List<String>
)

class MangaLibraryRepository internal constructor(
    private val database: VeilDatabase,
    private val library: LocalLibraryRepository
) {
    suspend fun syncSource(
        bookId: String,
        descriptor: MangaSourceDescriptor,
        sourceRef: MangaRef,
        update: MangaUpdate,
        makePreferred: Boolean = false,
        syncedAtEpochMs: Long = System.currentTimeMillis()
    ): MangaSyncResult {
        val book = requireNotNull(library.getBook(bookId)) {
            "Cannot sync manga source for a missing Veil library item."
        }
        require(book.format == BookFormat.COMIC) {
            "Manga source bindings can only attach to COMIC library items."
        }
        require(sourceRef.sourceId == descriptor.id) {
            "Manga source reference does not belong to the supplied source descriptor."
        }
        require(update.ref == sourceRef) {
            "Manga update does not belong to the requested source reference."
        }
        update.details?.let { details ->
            require(details.ref == sourceRef) {
                "Manga update details belong to a different source reference."
            }
        }
        update.chapters?.let { chapters ->
            require(chapters.all { it.ref.manga == sourceRef }) {
                "Manga update contains chapters from a different source reference."
            }
        }

        val created = mutableListOf<String>()
        val reused = mutableListOf<String>()

        database.withTransaction {
            val existingBindings = database.mangaSourceBindings().listForBook(bookId)
            val existingForSource = existingBindings.firstOrNull {
                it.sourceId == descriptor.id.value
            }
            val isPreferred =
                makePreferred ||
                    existingBindings.isEmpty() ||
                    existingForSource?.isPreferred == true

            database.mangaSourceBindings().upsert(
                MangaSourceBindingEntity(
                    bookId = bookId,
                    sourceId = descriptor.id.value,
                    providerId = descriptor.providerId.value,
                    sourceKey = sourceRef.key,
                    language = descriptor.language,
                    sourceVersion = descriptor.version,
                    isPreferred = isPreferred,
                    lastSyncedAtEpochMs = syncedAtEpochMs
                )
            )
            if (isPreferred) {
                database.mangaSourceBindings().setPreferred(bookId, descriptor.id.value)
            }

            val existingById = database.mangaChapters()
                .listForBook(bookId)
                .associateBy { it.id }

            update.chapters.orEmpty().forEachIndexed { displayOrder, sourceChapter ->
                val existingBinding = database.mangaChapterBindings().findByExternalRef(
                    sourceId = descriptor.id.value,
                    mangaSourceKey = sourceRef.key,
                    sourceChapterKey = sourceChapter.ref.key
                )
                val chapterId = existingBinding?.chapterId ?: UUID.randomUUID().toString()
                val existing = existingById[chapterId]
                require(existingBinding == null || existing != null) {
                    "Source chapter is already bound to a different Veil library item."
                }

                if (existing == null) created += chapterId else reused += chapterId

                database.mangaChapters().upsert(
                    sourceChapter.toEntity(
                        id = chapterId,
                        bookId = bookId,
                        displayOrder = displayOrder,
                        existing = existing
                    )
                )
                database.mangaChapterBindings().upsert(
                    MangaChapterBindingEntity(
                        chapterId = chapterId,
                        sourceId = descriptor.id.value,
                        mangaSourceKey = sourceRef.key,
                        sourceChapterKey = sourceChapter.ref.key,
                        lastSeenAtEpochMs = syncedAtEpochMs
                    )
                )
            }
        }

        update.details?.let { details ->
            library.editMetadata(
                BookMetadataUpdate(
                    bookId = bookId,
                    title = details.title,
                    author = details.authors.joinToString(", ").ifBlank { book.author },
                    collections = book.allCollections,
                    seriesName = book.seriesName,
                    seriesIndex = book.seriesIndex,
                    language = descriptor.language
                )
            )
        }

        return MangaSyncResult(
            bookId = bookId,
            sourceId = descriptor.id,
            chapterCount = update.chapters?.size ?: 0,
            createdChapterIds = created,
            reusedChapterIds = reused
        )
    }

    suspend fun preferredSourceRef(bookId: String): MangaRef? =
        database.mangaSourceBindings()
            .listForBook(bookId)
            .firstOrNull()
            ?.let { MangaRef(MangaSourceId(it.sourceId), it.sourceKey) }

    suspend fun chapterRefForReading(chapterId: String): MangaChapterRef? {
        val chapter = database.mangaChapters().findById(chapterId) ?: return null
        val sourceBindings = database.mangaSourceBindings().listForBook(chapter.bookId)
        val chapterBindings = database.mangaChapterBindings().listForChapter(chapterId)

        val chosen = sourceBindings
            .asSequence()
            .mapNotNull { source ->
                chapterBindings.firstOrNull { it.sourceId == source.sourceId }?.let { binding ->
                    source to binding
                }
            }
            .firstOrNull()
            ?: return null

        val (source, binding) = chosen
        return MangaChapterRef(
            manga = MangaRef(MangaSourceId(source.sourceId), source.sourceKey),
            key = binding.sourceChapterKey
        )
    }

    suspend fun saveReadingProgress(
        bookId: String,
        chapterId: String,
        pageIndex: Int,
        pageCount: Int,
        readAtEpochMs: Long = System.currentTimeMillis()
    ): Boolean {
        val chapters = database.mangaChapters().listForBook(bookId)
        require(chapters.isNotEmpty()) { "Cannot save manga progress without chapters." }

        val chapterPosition = chapters.indexOfFirst { it.id == chapterId }
        require(chapterPosition >= 0) { "Manga chapter does not belong to the requested book." }

        val safePageCount = pageCount.coerceAtLeast(1)
        val safePage = pageIndex.coerceIn(0, safePageCount - 1)
        val finishedChapter = safePage >= safePageCount - 1

        check(
            database.mangaChapters().updateReadProgress(
                chapterId = chapterId,
                lastPageIndex = safePage,
                pageCount = safePageCount,
                read = finishedChapter,
                lastReadAtEpochMs = readAtEpochMs
            ) == 1
        ) {
            "Manga chapter disappeared before progress could be persisted."
        }

        val progression = mangaOverallProgression(
            chapterPosition = chapterPosition,
            chapterCount = chapters.size,
            pageIndex = safePage,
            pageCount = safePageCount
        )
        val chapter = chapters[chapterPosition]
        val locatorJson = buildJsonObject {
            put("type", "manga")
            put("bookId", bookId)
            put("chapterId", chapterId)
            put("pageIndex", safePage)
            put("pageCount", safePageCount)
        }.toString()

        return library.saveProgress(
            id = bookId,
            progression = progression,
            locatorJson = locatorJson,
            currentChapter = chapter.title.ifBlank {
                chapter.chapterNumber?.let { "Chapter $it" } ?: "Manga"
            }
        )
    }
}

internal fun mangaOverallProgression(
    chapterPosition: Int,
    chapterCount: Int,
    pageIndex: Int,
    pageCount: Int
): Double {
    require(chapterCount > 0) { "Chapter count must be positive." }
    require(chapterPosition in 0 until chapterCount) { "Chapter position is out of range." }
    val safePageCount = pageCount.coerceAtLeast(1)
    val safePage = pageIndex.coerceIn(0, safePageCount - 1)
    val inChapter = if (safePageCount == 1) 1.0 else {
        safePage.toDouble() / (safePageCount - 1).toDouble()
    }
    return ((chapterPosition.toDouble() + inChapter) / chapterCount.toDouble())
        .coerceIn(0.0, 1.0)
}

private fun MangaChapter.toEntity(
    id: String,
    bookId: String,
    displayOrder: Int,
    existing: MangaChapterEntity?
): MangaChapterEntity = MangaChapterEntity(
    id = id,
    bookId = bookId,
    title = title,
    chapterNumber = chapterNumber,
    volumeNumber = volumeNumber,
    publishedAtEpochMs = publishedAtEpochMs,
    displayOrder = displayOrder,
    pageCount = existing?.pageCount ?: 0,
    lastPageIndex = existing?.lastPageIndex ?: 0,
    read = existing?.read ?: false,
    lastReadAtEpochMs = existing?.lastReadAtEpochMs
)
