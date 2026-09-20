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
    val reusedChapterIds: List<String>,
    val matchedAlternativeChapterIds: List<String> = emptyList()
)

data class MangaSourceAlternativePlan(
    val bookId: String,
    val sourceId: MangaSourceId,
    val existingChapterCount: Int,
    val incomingChapterCount: Int,
    val alreadyBoundCount: Int,
    val matchedExistingCount: Int,
    val newIncomingCount: Int,
    val conflictingIncomingCount: Int,
    val resumeChapterCovered: Boolean
) {
    val existingCoverageRatio: Double
        get() = if (existingChapterCount == 0) {
            1.0
        } else {
            matchedExistingCount.toDouble() / existingChapterCount.toDouble()
        }
}

class MangaLibraryRepository internal constructor(
    private val database: VeilDatabase,
    private val library: LocalLibraryRepository
) {
    suspend fun planAlternativeSource(
        bookId: String,
        descriptor: MangaSourceDescriptor,
        sourceRef: MangaRef,
        update: MangaUpdate
    ): MangaSourceAlternativePlan {
        requireSourceUpdate(bookId, descriptor, sourceRef, update)

        val existing = database.mangaChapters().listForBook(bookId)
        val existingById = existing.associateBy(MangaChapterEntity::id)
        val sourceBindings = database.mangaChapterBindings()
            .listForSourceManga(descriptor.id.value, sourceRef.key)
        val sourceByKey = sourceBindings.associateBy(MangaChapterBindingEntity::sourceChapterKey)
        val incoming = update.chapters.orEmpty()

        val alreadyBound = incoming.mapIndexedNotNull { index, chapter ->
            val binding = sourceByKey[chapter.ref.key] ?: return@mapIndexedNotNull null
            index to binding
        }
        val conflicts = alreadyBound.count { (_, binding) ->
            binding.chapterId !in existingById
        }
        val validAlreadyBound = alreadyBound.filter { (_, binding) ->
            binding.chapterId in existingById
        }

        val boundChapterIds = sourceBindings.mapTo(mutableSetOf()) { it.chapterId }
        val crossSourceCandidates = existing.filterNot { it.id in boundChapterIds }
        val unboundIncoming = incoming.mapIndexedNotNull { index, chapter ->
            if (chapter.ref.key in sourceByKey) {
                null
            } else {
                IncomingMangaChapterIdentity(
                    index = index,
                    title = chapter.title,
                    chapterNumber = chapter.chapterNumber,
                    volumeNumber = chapter.volumeNumber
                )
            }
        }

        val matchPlan = planMangaChapterMatches(
            existing = crossSourceCandidates.map(MangaChapterEntity::toMatchIdentity),
            incoming = unboundIncoming
        )

        val matchedIds = buildSet {
            validAlreadyBound.forEach { (_, binding) -> add(binding.chapterId) }
            matchPlan.matches.forEach { add(it.existingChapterId) }
        }
        val resumeChapter = existing
            .filter { it.lastReadAtEpochMs != null }
            .maxWithOrNull(
                compareBy<MangaChapterEntity> { it.lastReadAtEpochMs ?: Long.MIN_VALUE }
                    .thenBy(MangaChapterEntity::displayOrder)
            )

        return MangaSourceAlternativePlan(
            bookId = bookId,
            sourceId = descriptor.id,
            existingChapterCount = existing.size,
            incomingChapterCount = incoming.size,
            alreadyBoundCount = validAlreadyBound.size,
            matchedExistingCount = matchedIds.size,
            newIncomingCount = matchPlan.unmatchedIncomingIndices.size,
            conflictingIncomingCount = conflicts,
            resumeChapterCovered = resumeChapter == null || resumeChapter.id in matchedIds
        )
    }

    suspend fun syncSource(
        bookId: String,
        descriptor: MangaSourceDescriptor,
        sourceRef: MangaRef,
        update: MangaUpdate,
        makePreferred: Boolean = false,
        syncedAtEpochMs: Long = System.currentTimeMillis()
    ): MangaSyncResult {
        val book = requireSourceUpdate(bookId, descriptor, sourceRef, update)
        val created = mutableListOf<String>()
        val reused = mutableListOf<String>()
        val matchedAlternative = mutableListOf<String>()
        var sourceIsPreferredAfterSync = false

        database.withTransaction {
            val existingBindings = database.mangaSourceBindings().listForBook(bookId)
            val existingForSource = existingBindings.firstOrNull {
                it.sourceId == descriptor.id.value
            }
            val isPreferred =
                makePreferred ||
                    existingBindings.isEmpty() ||
                    existingForSource?.isPreferred == true
            sourceIsPreferredAfterSync = isPreferred

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

            val existingChapters = database.mangaChapters().listForBook(bookId)
            val existingById = existingChapters
                .associateBy(MangaChapterEntity::id)
                .toMutableMap()
            val sourceBindings = database.mangaChapterBindings()
                .listForSourceManga(descriptor.id.value, sourceRef.key)
            val sourceByKey = sourceBindings.associateBy(MangaChapterBindingEntity::sourceChapterKey)
            val sourceBoundChapterIds = sourceBindings.mapTo(mutableSetOf()) { it.chapterId }
            val incomingChapters = update.chapters.orEmpty()

            incomingChapters.forEach { sourceChapter ->
                val existingBinding = sourceByKey[sourceChapter.ref.key] ?: return@forEach
                require(existingBinding.chapterId in existingById) {
                    "Source chapter is already bound to a different Veil library item."
                }
            }

            val unboundIncoming = incomingChapters.mapIndexedNotNull { index, sourceChapter ->
                if (sourceChapter.ref.key in sourceByKey) {
                    null
                } else {
                    IncomingMangaChapterIdentity(
                        index = index,
                        title = sourceChapter.title,
                        chapterNumber = sourceChapter.chapterNumber,
                        volumeNumber = sourceChapter.volumeNumber
                    )
                }
            }
            val crossSourceCandidates = existingChapters.filterNot {
                it.id in sourceBoundChapterIds
            }
            val crossSourcePlan = planMangaChapterMatches(
                existing = crossSourceCandidates.map(MangaChapterEntity::toMatchIdentity),
                incoming = unboundIncoming
            )
            val matchedByIncoming = crossSourcePlan.matches.associate {
                it.incomingIndex to it.existingChapterId
            }

            val preferredOtherSourceExists = existingBindings.any {
                it.isPreferred && it.sourceId != descriptor.id.value
            }

            incomingChapters.forEachIndexed { displayOrder, sourceChapter ->
                val existingBinding = sourceByKey[sourceChapter.ref.key]
                val matchedAlternativeId = matchedByIncoming[displayOrder]
                val chapterId = existingBinding?.chapterId
                    ?: matchedAlternativeId
                    ?: UUID.randomUUID().toString()
                val existing = existingById[chapterId]

                require(existingBinding == null || existing != null) {
                    "Source chapter is already bound to a different Veil library item."
                }

                if (existing == null) {
                    created += chapterId
                } else {
                    reused += chapterId
                }
                if (matchedAlternativeId != null) {
                    matchedAlternative += chapterId
                }

                val preserveCanonicalMetadata =
                    existing != null && !isPreferred && preferredOtherSourceExists

                val entity = sourceChapter.toEntity(
                    id = chapterId,
                    bookId = bookId,
                    displayOrder = displayOrder,
                    existing = existing,
                    preserveCanonicalMetadata = preserveCanonicalMetadata
                )
                database.mangaChapters().upsert(entity)
                existingById[chapterId] = entity

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

        if (sourceIsPreferredAfterSync) {
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
        }

        return MangaSyncResult(
            bookId = bookId,
            sourceId = descriptor.id,
            chapterCount = update.chapters?.size ?: 0,
            createdChapterIds = created,
            reusedChapterIds = reused,
            matchedAlternativeChapterIds = matchedAlternative
        )
    }

    suspend fun replacePreferredSource(
        bookId: String,
        descriptor: MangaSourceDescriptor,
        sourceRef: MangaRef,
        update: MangaUpdate,
        syncedAtEpochMs: Long = System.currentTimeMillis()
    ): MangaSyncResult = syncSource(
        bookId = bookId,
        descriptor = descriptor,
        sourceRef = sourceRef,
        update = update,
        makePreferred = true,
        syncedAtEpochMs = syncedAtEpochMs
    )

    suspend fun preferExistingSource(
        bookId: String,
        sourceId: MangaSourceId
    ) {
        val bindings = database.mangaSourceBindings().listForBook(bookId)
        require(bindings.any { it.sourceId == sourceId.value }) {
            "Cannot prefer a manga source that is not attached to this library item."
        }
        database.mangaSourceBindings().setPreferred(bookId, sourceId.value)
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
                chapter.chapterNumber?.let { "Chapter " + it } ?: "Manga"
            }
        )
    }

    private fun requireSourceUpdate(
        bookId: String,
        descriptor: MangaSourceDescriptor,
        sourceRef: MangaRef,
        update: MangaUpdate
    ): com.veilreader.app.domain.Book {
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
        return book
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

private fun MangaChapterEntity.toMatchIdentity() = ExistingMangaChapterIdentity(
    id = id,
    title = title,
    chapterNumber = chapterNumber,
    volumeNumber = volumeNumber
)

private fun MangaChapter.toEntity(
    id: String,
    bookId: String,
    displayOrder: Int,
    existing: MangaChapterEntity?,
    preserveCanonicalMetadata: Boolean
): MangaChapterEntity = MangaChapterEntity(
    id = id,
    bookId = bookId,
    title = if (preserveCanonicalMetadata) {
        existing?.title.orEmpty().ifBlank { title }
    } else {
        title
    },
    chapterNumber = if (preserveCanonicalMetadata) {
        existing?.chapterNumber ?: chapterNumber
    } else {
        chapterNumber
    },
    volumeNumber = if (preserveCanonicalMetadata) {
        existing?.volumeNumber ?: volumeNumber
    } else {
        volumeNumber
    },
    publishedAtEpochMs = if (preserveCanonicalMetadata) {
        existing?.publishedAtEpochMs ?: publishedAtEpochMs
    } else {
        publishedAtEpochMs
    },
    displayOrder = if (preserveCanonicalMetadata) {
        existing?.displayOrder ?: displayOrder
    } else {
        displayOrder
    },
    pageCount = existing?.pageCount ?: 0,
    lastPageIndex = existing?.lastPageIndex ?: 0,
    read = existing?.read ?: false,
    lastReadAtEpochMs = existing?.lastReadAtEpochMs
)
