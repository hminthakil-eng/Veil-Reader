package com.veilreader.app.data.manga

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.withTransaction
import com.veilreader.app.data.BookImportResult
import com.veilreader.app.data.LocalLibraryRepository
import com.veilreader.app.data.db.MangaChapterEntity
import com.veilreader.app.data.db.MangaChapterSourceEntity
import com.veilreader.app.data.db.MangaMergeChapterEntity
import com.veilreader.app.data.db.MangaMergeMemberEntity
import com.veilreader.app.data.db.MangaMergeOriginalChapterEntity
import com.veilreader.app.data.db.MangaOfflineChapterEntity
import com.veilreader.app.data.db.MangaOfflinePageEntity
import com.veilreader.app.data.db.MangaProgressEntity
import com.veilreader.app.data.db.MangaSourceLinkEntity
import com.veilreader.app.data.db.MangaWorkMergeEntity
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.data.db.toDomain
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.manga.importing.MangaCbzImportFailureReason
import com.veilreader.app.manga.importing.MangaCbzImportLimits
import com.veilreader.app.manga.importing.MangaCbzImportResult
import com.veilreader.app.manga.importing.MangaCbzIngestor
import com.veilreader.app.manga.importing.compareNaturalArchiveNames
import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.MangaCacheLayout
import com.veilreader.app.manga.library.MangaChapterAnchor
import com.veilreader.app.manga.library.MangaOfflineChapterLocator
import com.veilreader.app.manga.library.MangaMergeChapterCandidate
import com.veilreader.app.manga.library.MangaMergeDisposition
import com.veilreader.app.manga.library.MangaMergeMember
import com.veilreader.app.manga.library.MangaMergePlanResult
import com.veilreader.app.manga.library.MangaMergeRejection
import com.veilreader.app.manga.library.MangaWorkMergePlan
import com.veilreader.app.manga.library.MangaWorkMergePlanner
import com.veilreader.app.manga.library.OfflineChapterManifest
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

class MangaLocalImportException(
    val reason: MangaCbzImportFailureReason
) : IllegalStateException("Local Manga import failed: $reason")

class MangaMergePreflightException(
    val reason: MangaMergeRejection,
    val conflictingSourceChapterId: String? = null,
    val conflictingTargetChapterId: String? = null
) : IllegalStateException("Local Manga merge preflight rejected: $reason")

class MangaMergeSourceIntegrityException(
    val bookId: String,
    val chapterId: String,
    message: String
) : IllegalStateException(message)

data class MangaLocalRestorePoint(
    val pageIndex: Int,
    val pageCount: Int?,
    val chapterProgression: Double,
    val updatedAtEpochMs: Long,
    val chapterReadingOrder: Int = 0
) {
    init {
        require(pageIndex >= 0)
        require(pageCount == null || pageCount > 0)
        require(chapterProgression.isFinite() && chapterProgression in 0.0..1.0)
        require(chapterReadingOrder >= 0)
    }
}

data class MangaLocalChapterMetadata(
    val title: String? = null,
    val volume: Double? = null,
    val number: Double? = null,
    val languageTag: String? = null
) {
    init {
        require(volume == null || volume.isFinite())
        require(number == null || number.isFinite())
    }
}

data class MangaLocalChapterImportResult(
    val book: Book,
    val chapterId: String,
    val readingOrder: Int,
    val duplicate: Boolean
)


data class MangaLocalBatchImportResult(
    val addedCount: Int,
    val duplicateCount: Int,
    val lastReadingOrder: Int?
)

data class MangaLocalChapterSummary(
    val id: String,
    val readingOrder: Int,
    val title: String,
    val volume: Double?,
    val number: Double?,
    val languageTag: String?,
    val pageCount: Int,
    val sourceBytes: Long,
    val cacheBytes: Long,
    val isPrimary: Boolean
) {
    init {
        require(id.isNotBlank())
        require(readingOrder >= 0)
        require(title.isNotBlank())
        require(pageCount >= 0)
        require(sourceBytes >= 0L)
        require(cacheBytes >= 0L)
    }
}

data class MangaLocalStorageSummary(
    val chapterCount: Int,
    val offlinePageCount: Int,
    val sourceBytes: Long,
    val cacheBytes: Long
) {
    val totalBytes: Long get() = sourceBytes + cacheBytes
}

data class MangaMergeExecutionResult(
    val mergeId: String,
    val targetBookId: String,
    val sourceBookIds: List<String>,
    val copiedChapterCount: Int,
    val deduplicatedChapterCount: Int
)

data class MangaSplitExecutionResult(
    val mergeId: String,
    val targetBookId: String,
    val restoredSourceBookIds: List<String>,
    val removedCopiedChapterCount: Int
)

private data class CopiedMergeArchive(
    val file: File,
    val createdByMerge: Boolean
)

private data class PreparedMergeCopy(
    val sourceBookId: String,
    val sourceChapter: MangaChapterEntity,
    val sourceChapterKey: String,
    val targetChapter: MangaChapterEntity,
    val targetArchive: CopiedMergeArchive,
    val cacheDirectory: File,
    val manifest: OfflineChapterManifest
)

/**
 * Atomic-enough coordinator for local CBZ import across filesystem + Room.
 *
 * Room owns catalog truth; the archive and extracted cache are compensating resources. Any failure
 * after Book commit deletes the Book again (cascading Manga rows) and removes the generated cache.
 */
class MangaLocalImportCoordinator(
    context: Context,
    private val library: LocalLibraryRepository,
    private val database: VeilDatabase = VeilDatabase.get(context),
    private val limits: MangaCbzImportLimits = MangaCbzImportLimits(),
    private val ingestor: MangaCbzIngestor = MangaCbzIngestor(limits)
) {
    private val appContext = context.applicationContext
    val cacheRoot: File = File(appContext.filesDir, "manga-cache")

    suspend fun listChapterSummaries(
        bookId: String
    ): List<MangaLocalChapterSummary> {
        val book = library.getBook(bookId)
            ?: return emptyList()
        if (book.format != BookFormat.COMIC) return emptyList()

        return database.mangaCatalog().listChapters(bookId).map { chapter ->
            val source = database.mangaCatalog()
                .listChapterSources(chapter.id)
                .firstOrNull {
                    it.sourceId == MangaCbzIngestor.LOCAL_CBZ_SOURCE_ID.value
                }
            val archive = source?.let {
                resolveLocalArchiveFile(book, it.chapterKey, chapter.readingOrder)
            }
            val offline = database.mangaOffline().findChapter(chapter.id)
            val cacheBytes = withContext(Dispatchers.IO) {
                offline?.pages.orEmpty().sumOf { page ->
                    File(cacheRoot, page.relativePath)
                        .takeIf(File::isFile)
                        ?.length()
                        ?: 0L
                }
            }
            MangaLocalChapterSummary(
                id = chapter.id,
                readingOrder = chapter.readingOrder,
                title = chapter.title
                    ?.takeIf(String::isNotBlank)
                    ?: chapter.normalizedTitle
                    ?.takeIf(String::isNotBlank)
                    ?: "Chapter " + (chapter.readingOrder + 1),
                volume = chapter.volume,
                number = chapter.number,
                languageTag = chapter.languageTag,
                pageCount = offline?.pages?.size ?: 0,
                sourceBytes = withContext(Dispatchers.IO) {
                    archive?.takeIf(File::isFile)?.length() ?: 0L
                },
                cacheBytes = cacheBytes,
                isPrimary = chapter.readingOrder == 0
            )
        }
    }

    /**
     * Updates identity-bearing local chapter metadata without orphaning the offline cache.
     *
     * Chapter number / volume / language participate in OfflineChapterId. When they change, Veil
     * re-ingests the immutable local CBZ into the new cache identity, swaps Room metadata, replaces
     * the manifest, and only then removes the old derived directory.
     */
    suspend fun updateChapterMetadata(
        bookId: String,
        chapterId: String,
        metadata: MangaLocalChapterMetadata
    ): Result<Unit> = runCatching {
        val book = library.getBook(bookId)
            ?: error("Manga Book is not present in the Library")
        require(book.format == BookFormat.COMIC)

        val current = database.mangaCatalog().findChapter(chapterId)
            ?: error("Manga chapter is not present")
        require(current.bookId == bookId) {
            "Manga chapter belongs to another book"
        }
        val source = database.mangaCatalog()
            .listChapterSources(chapterId)
            .firstOrNull { it.sourceId == MangaCbzIngestor.LOCAL_CBZ_SOURCE_ID.value }
            ?: error("Manga chapter has no local CBZ source")
        val archive = resolveLocalArchiveFile(
            book = book,
            chapterKey = source.chapterKey,
            readingOrder = current.readingOrder
        ) ?: error("Manga chapter source archive is missing")

        val title = metadata.title
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: current.title
            ?.takeIf(String::isNotBlank)
            ?: current.normalizedTitle
            ?.takeIf(String::isNotBlank)
            ?: "Chapter " + (current.readingOrder + 1)
        val number = metadata.number ?: current.number
            ?: error("Local Manga chapter number cannot be removed")
        require(number.isFinite() && number >= 0.0) {
            "Chapter number must be a finite non-negative value"
        }
        val volume = metadata.volume
        require(volume == null || (volume.isFinite() && volume >= 0.0)) {
            "Volume must be a finite non-negative value"
        }
        val languageTag = metadata.languageTag
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        val newAnchor = MangaChapterAnchor(
            volume = volume,
            number = number,
            languageTag = languageTag,
            normalizedTitle = title,
            providerChapterKeyHint = source.chapterKey
        )
        val newOfflineId = requireNotNull(
            MangaOfflineChapterLocator.idFor(CanonicalMangaId(bookId), newAnchor)
        )
        val newCacheKey = MangaCacheLayout.chapterDirectory(newOfflineId)
        val conflict = database.mangaCatalog().findChapterByCacheKey(newCacheKey)
        require(conflict == null || conflict.id == current.id) {
            "Another Manga chapter already owns this chapter identity"
        }

        if (newCacheKey == current.cacheKey) {
            check(
                database.mangaCatalog().updateChapterIdentityMetadata(
                    chapterId = current.id,
                    cacheKey = current.cacheKey,
                    title = title,
                    normalizedTitle = title,
                    volume = volume,
                    number = number,
                    languageTag = languageTag
                ) == 1
            )
            recomputeStoredMangaProgress(bookId)
            return@runCatching
        }

        val oldDirectory = File(cacheRoot, current.cacheKey)
        val newDirectory = File(cacheRoot, newCacheKey)
        var identityUpdated = false
        try {
            database.withTransaction {
                check(
                    database.mangaCatalog().updateChapterIdentityMetadata(
                        chapterId = current.id,
                        cacheKey = newCacheKey,
                        title = title,
                        normalizedTitle = title,
                        volume = volume,
                        number = number,
                        languageTag = languageTag
                    ) == 1
                )
            }
            identityUpdated = true

            val imported = ingestor.ingest(
                archiveFile = archive,
                cacheRoot = cacheRoot,
                chapterId = newOfflineId,
                anchor = newAnchor,
                originChapterKey = source.chapterKey,
                originSourceId = MangaCbzIngestor.LOCAL_CBZ_SOURCE_ID
            )
            val manifest = when (imported) {
                is MangaCbzImportResult.Success -> imported.manifest
                is MangaCbzImportResult.Failure ->
                    throw MangaLocalImportException(imported.reason)
            }
            recomputeStoredMangaProgress(bookId)
            RoomMangaOfflineCacheIndex(database).put(manifest)

            withContext(Dispatchers.IO) {
                deleteGeneratedChapterDirectory(oldDirectory)
                pruneEmptyMangaCacheParents(listOf(oldDirectory))
            }
        } catch (error: Throwable) {
            if (identityUpdated) {
                runCatching {
                    database.mangaCatalog().updateChapterIdentityMetadata(
                        chapterId = current.id,
                        cacheKey = current.cacheKey,
                        title = current.title ?: current.normalizedTitle ?: title,
                        normalizedTitle = current.normalizedTitle ?: current.title ?: title,
                        volume = current.volume,
                        number = current.number,
                        languageTag = current.languageTag
                    )
                }
            }
            withContext(Dispatchers.IO) {
                if (newDirectory != oldDirectory) {
                    deleteGeneratedChapterDirectory(newDirectory)
                    pruneEmptyMangaCacheParents(listOf(newDirectory))
                }
            }
            throw error
        }
    }

    suspend fun renameChapter(
        bookId: String,
        chapterId: String,
        title: String
    ): Result<Unit> = runCatching {
        val clean = title.trim()
        require(clean.isNotEmpty()) { "Chapter title cannot be empty" }
        val chapter = database.mangaCatalog().findChapter(chapterId)
            ?: error("Manga chapter is not present")
        require(chapter.bookId == bookId) { "Manga chapter belongs to another book" }
        require(chapter.number != null) {
            "Renaming an unnumbered chapter would change its offline identity"
        }
        check(
            database.mangaCatalog().updateChapterTitle(
                chapterId = chapterId,
                title = clean,
                normalizedTitle = clean
            ) == 1
        ) {
            "Manga chapter disappeared before rename"
        }
    }

    suspend fun moveChapter(
        bookId: String,
        chapterId: String,
        direction: Int
    ): Result<Int> = runCatching {
        require(direction == -1 || direction == 1)
        requireMangaStructureMutable(bookId)
        val chapters = database.mangaCatalog().listChapters(bookId)
        val current = chapters.firstOrNull { it.id == chapterId }
            ?: error("Manga chapter is not present")
        require(current.readingOrder > 0) {
            "The primary Manga chapter is pinned to reading order 0"
        }
        val destinationOrder = current.readingOrder + direction
        require(destinationOrder > 0 && destinationOrder <= chapters.lastIndex) {
            "Manga chapter cannot move beyond the managed chapter range"
        }
        val neighbor = chapters.firstOrNull { it.readingOrder == destinationOrder }
            ?: error("Manga chapter ordering is not contiguous")
        val temporaryOrder = (chapters.maxOfOrNull { it.readingOrder } ?: 0) + 1

        database.withTransaction {
            check(
                database.mangaCatalog().updateChapterReadingOrder(
                    current.id,
                    temporaryOrder
                ) == 1
            )
            check(
                database.mangaCatalog().updateChapterReadingOrder(
                    neighbor.id,
                    current.readingOrder
                ) == 1
            )
            check(
                database.mangaCatalog().updateChapterReadingOrder(
                    current.id,
                    destinationOrder
                ) == 1
            )
        }
        database.books().reopenMangaAfterExtension(bookId)
        recomputeStoredMangaProgress(bookId)
        destinationOrder
    }

    suspend fun deleteChapter(
        bookId: String,
        chapterId: String
    ): Result<Unit> = runCatching {
        requireMangaStructureMutable(bookId)
        val book = library.getBook(bookId)
            ?: error("Manga Book is not present in the Library")
        require(book.format == BookFormat.COMIC)

        val chapters = database.mangaCatalog().listChapters(bookId)
        val target = chapters.firstOrNull { it.id == chapterId }
            ?: error("Manga chapter is not present")
        require(target.readingOrder > 0) {
            "The primary Manga chapter cannot be removed independently"
        }
        val source = database.mangaCatalog()
            .listChapterSources(target.id)
            .firstOrNull { it.sourceId == MangaCbzIngestor.LOCAL_CBZ_SOURCE_ID.value }
        val archive = source?.let {
            resolveLocalArchiveFile(book, it.chapterKey, target.readingOrder)
        }
        val cacheDirectory = File(cacheRoot, target.cacheKey)
        val storedProgress = database.mangaProgress().find(bookId)
        val deletingCurrent = storedProgress?.chapterId == target.id
        val fallback = chapters
            .filter { it.readingOrder < target.readingOrder }
            .maxByOrNull { it.readingOrder }

        database.withTransaction {
            database.mangaCatalog().deleteChapter(target.id)
            chapters
                .filter { it.readingOrder > target.readingOrder }
                .sortedBy { it.readingOrder }
                .forEach { chapter ->
                    check(
                        database.mangaCatalog().updateChapterReadingOrder(
                            chapter.id,
                            chapter.readingOrder - 1
                        ) == 1
                    )
                }
        }

        withContext(Dispatchers.IO) {
            deleteGeneratedChapterDirectory(cacheDirectory)
            archive?.let(::deleteConfinedPublicationFile)
            pruneEmptyMangaCacheParents(listOf(cacheDirectory))
            archive?.let { pruneEmptyLocalArchiveParents(listOf(it)) }
        }

        database.books().reopenMangaAfterExtension(bookId)
        if (deletingCurrent && fallback != null) {
            val fallbackSource = database.mangaCatalog()
                .listChapterSources(fallback.id)
                .firstOrNull {
                    it.sourceId == MangaCbzIngestor.LOCAL_CBZ_SOURCE_ID.value
                }
                ?: error("Fallback Manga chapter has no local source")
            val offline = database.mangaOffline().findChapter(fallback.id)
            val pageCount = offline?.pages?.size?.takeIf { it > 0 }
            RoomMangaProgressStore(database).save(
                com.veilreader.app.manga.library.MangaReadingProgress(
                    mangaId = CanonicalMangaId(bookId),
                    chapter = MangaChapterAnchor(
                        volume = fallback.volume,
                        number = fallback.number,
                        languageTag = fallback.languageTag,
                        normalizedTitle = fallback.normalizedTitle ?: fallback.title,
                        providerChapterKeyHint = fallbackSource.chapterKey
                    ),
                    pageIndex = pageCount?.minus(1)?.coerceAtLeast(0) ?: 0,
                    pageCount = pageCount,
                    chapterProgression = 1.0,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
            )
        } else {
            recomputeStoredMangaProgress(bookId)
        }
    }

    private suspend fun recomputeStoredMangaProgress(bookId: String) {
        val store = RoomMangaProgressStore(database)
        store.load(CanonicalMangaId(bookId))?.let { store.save(it) }
    }

    suspend fun storageSummary(bookId: String): MangaLocalStorageSummary {
        val book = library.getBook(bookId)
            ?: return MangaLocalStorageSummary(0, 0, 0L, 0L)
        if (book.format != BookFormat.COMIC) {
            return MangaLocalStorageSummary(0, 0, 0L, 0L)
        }

        val chapters = database.mangaCatalog().listChapters(bookId)
        val archiveFiles = chapters.mapNotNull { chapter ->
            val source = database.mangaCatalog()
                .listChapterSources(chapter.id)
                .firstOrNull { it.sourceId == MangaCbzIngestor.LOCAL_CBZ_SOURCE_ID.value }
                ?: return@mapNotNull null
            resolveLocalArchiveFile(book, source.chapterKey, chapter.readingOrder)
        }.distinctBy { runCatching { it.canonicalPath }.getOrDefault(it.absolutePath) }

        val offline = database.mangaOffline().listForBook(bookId)
        val sourceBytes = withContext(Dispatchers.IO) {
            archiveFiles.sumOf { file -> file.takeIf(File::isFile)?.length() ?: 0L }
        }
        val cacheBytes = withContext(Dispatchers.IO) {
            offline.flatMap { it.pages }.sumOf { page ->
                val file = File(cacheRoot, page.relativePath)
                file.takeIf(File::isFile)?.length() ?: 0L
            }
        }

        return MangaLocalStorageSummary(
            chapterCount = chapters.size,
            offlinePageCount = offline.sumOf { it.pages.size },
            sourceBytes = sourceBytes,
            cacheBytes = cacheBytes
        )
    }

    suspend fun clearDerivedCache(bookId: String): Result<Long> = runCatching {
        val chapters = database.mangaCatalog().listChapters(bookId)
        val offline = database.mangaOffline().listForBook(bookId)
        val bytesBefore = withContext(Dispatchers.IO) {
            offline.flatMap { it.pages }.sumOf { page ->
                File(cacheRoot, page.relativePath)
                    .takeIf(File::isFile)
                    ?.length()
                    ?: 0L
            }
        }

        // Keep the small verified manifest rows. They preserve page counts and hashes while
        // the disposable image bytes are removed, and ensureLocalCache() will detect/rebuild the
        // missing files before the reader session is opened.
        withContext(Dispatchers.IO) {
            val directories = chapters.map { File(cacheRoot, it.cacheKey) }
            directories.forEach(::deleteGeneratedChapterDirectory)
            pruneEmptyMangaCacheParents(directories)
        }
        bytesBefore
    }

    suspend fun ensureLocalCache(bookId: String): Result<Int> = runCatching {
        val book = library.getBook(bookId)
            ?: error("Manga Book is not present in the Library")
        require(book.format == BookFormat.COMIC)

        val chapters = database.mangaCatalog().listChapters(bookId)
        val offlineStore = RoomMangaOfflineCacheIndex(database)
        var rebuilt = 0

        for (chapter in chapters) {
            val persisted = database.mangaOffline().findChapter(chapter.id)
            val usable = persisted != null &&
                persisted.chapter.completed &&
                persisted.pages.isNotEmpty() &&
                withContext(Dispatchers.IO) {
                    persisted.pages.all { page ->
                        val file = File(cacheRoot, page.relativePath)
                        file.isFile &&
                            file.length() == page.byteSize &&
                            page.byteSize > 0L
                    }
                }
            if (usable) continue

            val source = database.mangaCatalog()
                .listChapterSources(chapter.id)
                .firstOrNull { it.sourceId == MangaCbzIngestor.LOCAL_CBZ_SOURCE_ID.value }
                ?: error("Manga chapter has no local CBZ source")
            val archive = resolveLocalArchiveFile(
                book = book,
                chapterKey = source.chapterKey,
                readingOrder = chapter.readingOrder
            ) ?: error("Manga chapter source archive is missing")
            val anchor = MangaChapterAnchor(
                volume = chapter.volume,
                number = chapter.number,
                languageTag = chapter.languageTag,
                normalizedTitle = chapter.normalizedTitle ?: chapter.title,
                providerChapterKeyHint = source.chapterKey
            )
            val offlineId = requireNotNull(
                MangaOfflineChapterLocator.idFor(CanonicalMangaId(book.id), anchor)
            )
            check(MangaCacheLayout.chapterDirectory(offlineId) == chapter.cacheKey) {
                "Persisted Manga chapter cache identity changed"
            }

            val imported = ingestor.ingest(
                archiveFile = archive,
                cacheRoot = cacheRoot,
                chapterId = offlineId,
                anchor = anchor,
                originChapterKey = source.chapterKey,
                originSourceId = MangaCbzIngestor.LOCAL_CBZ_SOURCE_ID
            )
            val manifest = when (imported) {
                is MangaCbzImportResult.Success -> imported.manifest
                is MangaCbzImportResult.Failure ->
                    throw MangaLocalImportException(imported.reason)
            }
            offlineStore.put(manifest)
            rebuilt += 1
        }
        rebuilt
    }

    /**
     * Verifies an explicit local-work merge without mutating Room or app-private files.
     *
     * Every participating chapter must still have its original CBZ and the file's SHA-256 must
     * match the durable chapter key. Derived page cache is deliberately ignored because a later
     * executor must rebuild cache identities under the target Book rather than transplant them.
     */
    suspend fun preflightLocalMerge(
        targetBookId: String,
        sourceBookIds: List<String>
    ): Result<MangaWorkMergePlan> = runCatching {
        require(targetBookId.isNotBlank()) { "Target Manga Book id cannot be blank" }
        require(sourceBookIds.isNotEmpty()) { "At least one source Manga Book is required" }

        val requestedIds = listOf(targetBookId) + sourceBookIds
        requestedIds.forEach { bookId ->
            require(database.mangaMerges().findForTarget(bookId) == null) {
                "A Manga merge target cannot participate in another active merge"
            }
            require(database.mangaMerges().findForSource(bookId) == null) {
                "A Manga merge source cannot participate in another active merge"
            }
        }
        val books = requestedIds.map { bookId ->
            library.getBook(bookId)
                ?: error("Manga Book is not present in the Library: $bookId")
        }
        require(books.all { it.format == BookFormat.COMIC }) {
            "Only local COMIC books can participate in a Manga merge"
        }

        val members = books.map { book -> buildVerifiedMergeMember(book) }
        val planned = MangaWorkMergePlanner().plan(
            target = members.first(),
            sources = members.drop(1)
        )
        when (planned) {
            is MangaMergePlanResult.Ready -> planned.plan
            is MangaMergePlanResult.Rejected -> throw MangaMergePreflightException(
                reason = planned.reason,
                conflictingSourceChapterId = planned.conflictingSourceChapterId,
                conflictingTargetChapterId = planned.conflictingTargetChapterId
            )
        }
    }

    private suspend fun buildVerifiedMergeMember(book: Book): MangaMergeMember {
        val chapters = database.mangaCatalog().listChapters(book.id)
        val candidates = chapters.map { chapter ->
            val source = database.mangaCatalog()
                .listChapterSources(chapter.id)
                .singleOrNull {
                    it.sourceId == MangaCbzIngestor.LOCAL_CBZ_SOURCE_ID.value
                }
                ?: throw MangaMergeSourceIntegrityException(
                    bookId = book.id,
                    chapterId = chapter.id,
                    message = "Manga merge requires exactly one local CBZ source per chapter"
                )

            val expectedFingerprint = fingerprintFromChapterKey(source.chapterKey)
                ?: throw MangaMergeSourceIntegrityException(
                    bookId = book.id,
                    chapterId = chapter.id,
                    message = "Manga chapter does not carry a verifiable local CBZ fingerprint"
                )
            val archive = resolveLocalArchiveFile(
                book = book,
                chapterKey = source.chapterKey,
                readingOrder = chapter.readingOrder
            ) ?: throw MangaMergeSourceIntegrityException(
                bookId = book.id,
                chapterId = chapter.id,
                message = "Manga chapter source archive is missing"
            )
            val actualFingerprint = sha256(archive)
            if (!actualFingerprint.equals(expectedFingerprint, ignoreCase = true)) {
                throw MangaMergeSourceIntegrityException(
                    bookId = book.id,
                    chapterId = chapter.id,
                    message = "Manga chapter source archive failed SHA-256 verification"
                )
            }

            MangaMergeChapterCandidate(
                bookId = book.id,
                chapterId = chapter.id,
                readingOrder = chapter.readingOrder,
                cacheKey = chapter.cacheKey,
                chapterKey = source.chapterKey,
                volume = chapter.volume,
                number = chapter.number,
                languageTag = chapter.languageTag,
                normalizedTitle = chapter.normalizedTitle ?: chapter.title
            )
        }

        return MangaMergeMember(
            bookId = book.id,
            title = book.title,
            author = book.author,
            sourceUri = book.sourceUri,
            contentFingerprint = book.contentFingerprint,
            seriesName = book.seriesName,
            seriesIndex = book.seriesIndex,
            language = book.language,
            collections = book.allCollections,
            chapters = candidates
        )
    }

    /**
     * Executes a verified non-destructive local Manga merge.
     *
     * Source Books and source CBZ files remain untouched. New target chapters are rebuilt under the
     * target canonical identity, then one Room transaction publishes both the copies and the split
     * receipt. Only after that transaction do source Books disappear from the normal Library view.
     */
    suspend fun executeLocalMerge(
        targetBookId: String,
        sourceBookIds: List<String>
    ): Result<MangaMergeExecutionResult> = runCatching {
        val plan = preflightLocalMerge(targetBookId, sourceBookIds).getOrThrow()
        val targetBook = library.getBook(targetBookId)
            ?: error("Merge target disappeared before execution")
        val originalTargetChapters = database.mangaCatalog().listChapters(targetBookId)
        require(originalTargetChapters.isNotEmpty()) {
            "Merge target has no persisted Manga chapters"
        }
        require(
            originalTargetChapters.map { it.id } ==
                plan.splitReceiptSeed.targetOriginalChapterIds
        ) {
            "Merge target chapter set changed after preflight"
        }
        val sourceBooks = sourceBookIds.associateWith { sourceBookId ->
            library.getBook(sourceBookId)
                ?: error("Merge source disappeared before execution: $sourceBookId")
        }
        val originalTargetProgress = database.mangaProgress().find(targetBookId)
        val mergeId = UUID.randomUUID().toString()
        val originalTargetEvidence = originalTargetChapters.map { chapter ->
            val localSource = database.mangaCatalog()
                .listChapterSources(chapter.id)
                .singleOrNull {
                    it.sourceId == MangaCbzIngestor.LOCAL_CBZ_SOURCE_ID.value
                }
                ?: error("Merge target chapter lost its local CBZ identity")
            MangaMergeOriginalChapterEntity(
                mergeId = mergeId,
                readingOrder = chapter.readingOrder,
                chapterId = chapter.id,
                targetBookId = targetBook.id,
                chapterKey = localSource.chapterKey
            )
        }
        val prepared = mutableListOf<PreparedMergeCopy>()
        var committed = false

        try {
            for (action in plan.chapterActions) {
                if (action.disposition != MangaMergeDisposition.REBUILD_FROM_SOURCE_ARCHIVE) {
                    continue
                }
                val sourceBook = requireNotNull(sourceBooks[action.sourceBookId])
                val sourceChapter = database.mangaCatalog().findChapter(action.sourceChapterId)
                    ?: error("Merge source chapter disappeared before execution")
                require(sourceChapter.bookId == sourceBook.id)
                require(sourceChapter.readingOrder == action.sourceReadingOrder)

                val sourceLink = database.mangaCatalog()
                    .listChapterSources(sourceChapter.id)
                    .singleOrNull {
                        it.sourceId == MangaCbzIngestor.LOCAL_CBZ_SOURCE_ID.value
                    }
                    ?: error("Merge source chapter lost its local CBZ identity")
                val fingerprint = fingerprintFromChapterKey(sourceLink.chapterKey)
                    ?: error("Merge source chapter has no verifiable fingerprint")
                val sourceArchive = resolveLocalArchiveFile(
                    book = sourceBook,
                    chapterKey = sourceLink.chapterKey,
                    readingOrder = sourceChapter.readingOrder
                ) ?: error("Merge source archive disappeared before execution")
                require(sha256(sourceArchive).equals(fingerprint, ignoreCase = true)) {
                    "Merge source archive changed after preflight"
                }

                val anchor = MangaChapterAnchor(
                    volume = sourceChapter.volume,
                    number = sourceChapter.number,
                    languageTag = sourceChapter.languageTag,
                    normalizedTitle = sourceChapter.normalizedTitle ?: sourceChapter.title,
                    providerChapterKeyHint = sourceLink.chapterKey
                )
                val offlineId = requireNotNull(
                    MangaOfflineChapterLocator.idFor(
                        CanonicalMangaId(targetBook.id),
                        anchor
                    )
                )
                val cacheKey = MangaCacheLayout.chapterDirectory(offlineId)
                val targetChapterId = UUID.nameUUIDFromBytes(
                    ("veil-cbz:" + targetBook.id + ":" + cacheKey)
                        .toByteArray(Charsets.UTF_8)
                ).toString()
                require(targetChapterId == action.plannedTargetChapterId) {
                    "Merge plan target identity changed before execution"
                }
                require(database.mangaCatalog().findChapterByCacheKey(cacheKey) == null) {
                    "Merge target chapter identity is already occupied"
                }

                val targetArchive = copyVerifiedArchiveToTarget(
                    targetBook = targetBook,
                    sourceArchive = sourceArchive,
                    fingerprint = fingerprint
                )
                val cacheDirectory = File(cacheRoot, cacheKey)
                withContext(Dispatchers.IO) {
                    if (cacheDirectory.exists()) {
                        deleteGeneratedChapterDirectory(cacheDirectory)
                    }
                }

                val imported = ingestor.ingest(
                    archiveFile = targetArchive.file,
                    cacheRoot = cacheRoot,
                    chapterId = offlineId,
                    anchor = anchor,
                    originChapterKey = sourceLink.chapterKey,
                    originSourceId = MangaCbzIngestor.LOCAL_CBZ_SOURCE_ID
                )
                val manifest = when (imported) {
                    is MangaCbzImportResult.Success -> imported.manifest
                    is MangaCbzImportResult.Failure ->
                        throw MangaLocalImportException(imported.reason)
                }

                prepared += PreparedMergeCopy(
                    sourceBookId = sourceBook.id,
                    sourceChapter = sourceChapter,
                    sourceChapterKey = sourceLink.chapterKey,
                    targetChapter = MangaChapterEntity(
                        id = targetChapterId,
                        bookId = targetBook.id,
                        readingOrder = action.targetReadingOrder,
                        cacheKey = cacheKey,
                        title = sourceChapter.title,
                        normalizedTitle = sourceChapter.normalizedTitle,
                        volume = sourceChapter.volume,
                        number = sourceChapter.number,
                        languageTag = sourceChapter.languageTag
                    ),
                    targetArchive = targetArchive,
                    cacheDirectory = cacheDirectory,
                    manifest = manifest
                )
            }

            val existingTargetIds = database.mangaCatalog()
                .listChapters(targetBook.id)
                .mapTo(mutableSetOf(), MangaChapterEntity::id)
            val preparedTargetIds = prepared
                .mapTo(mutableSetOf()) { it.targetChapter.id }
            plan.chapterActions
                .filter { it.disposition == MangaMergeDisposition.DEDUPLICATE_EXACT_ARCHIVE }
                .forEach { action ->
                    require(
                        action.plannedTargetChapterId in existingTargetIds ||
                            action.plannedTargetChapterId in preparedTargetIds
                    ) {
                        "Merge deduplication target does not exist"
                    }
                }

            val localSource = MangaCbzIngestor.LOCAL_CBZ_SOURCE_ID
            val targetMangaKey = localMangaKey(targetBook.id)
            val now = System.currentTimeMillis()
            database.withTransaction {
                database.mangaCatalog().upsertSourceLink(
                    MangaSourceLinkEntity(
                        bookId = targetBook.id,
                        sourceId = localSource.value,
                        mangaKey = targetMangaKey
                    )
                )

                prepared.forEach { copy ->
                    database.mangaCatalog().upsertChapter(copy.targetChapter)
                    database.mangaCatalog().upsertChapterSource(
                        MangaChapterSourceEntity(
                            chapterId = copy.targetChapter.id,
                            bookId = targetBook.id,
                            sourceId = localSource.value,
                            mangaKey = targetMangaKey,
                            chapterKey = copy.sourceChapterKey
                        )
                    )
                    database.mangaOffline().replaceChapter(
                        chapter = MangaOfflineChapterEntity(
                            chapterId = copy.targetChapter.id,
                            originSourceId = copy.manifest.originSourceId.value,
                            originChapterKey = copy.manifest.originChapterKey,
                            completed = copy.manifest.completed,
                            updatedAtEpochMs = copy.manifest.updatedAtEpochMs
                        ),
                        pages = copy.manifest.pages.map { page ->
                            MangaOfflinePageEntity(
                                chapterId = copy.targetChapter.id,
                                pageIndex = page.index,
                                relativePath = page.relativePath,
                                byteSize = page.byteSize,
                                contentSha256 = page.contentSha256
                            )
                        }
                    )
                }

                database.mangaMerges().upsertMerge(
                    MangaWorkMergeEntity(
                        id = mergeId,
                        targetBookId = targetBook.id,
                        createdAtEpochMs = now,
                        receiptVersion = 1,
                        targetOriginalChapterCount = originalTargetChapters.size,
                        targetBookProgress = targetBook.progress.coerceIn(0f, 1f),
                        targetBookFinished = targetBook.finished,
                        targetBookLastOpenedAtEpochMs =
                            targetBook.lastOpenedAtEpochMs.coerceAtLeast(0L),
                        targetProgressChapterId = originalTargetProgress?.chapterId,
                        targetProgressPageIndex = originalTargetProgress?.pageIndex,
                        targetProgressPageCount = originalTargetProgress?.pageCount,
                        targetProgressChapterProgression =
                            originalTargetProgress?.chapterProgression,
                        targetProgressUpdatedAtEpochMs =
                            originalTargetProgress?.updatedAtEpochMs
                    )
                )
                database.mangaMerges().upsertOriginals(originalTargetEvidence)
                database.mangaMerges().upsertMembers(
                    sourceBookIds.mapIndexed { index, sourceBookId ->
                        MangaMergeMemberEntity(
                            mergeId = mergeId,
                            sourceBookId = sourceBookId,
                            sourceOrder = index
                        )
                    }
                )
                database.mangaMerges().upsertChapters(
                    plan.chapterActions.map { action ->
                        MangaMergeChapterEntity(
                            mergeId = mergeId,
                            sourceChapterId = action.sourceChapterId,
                            sourceBookId = action.sourceBookId,
                            targetChapterId = action.plannedTargetChapterId,
                            sourceReadingOrder = action.sourceReadingOrder,
                            targetReadingOrder = action.targetReadingOrder,
                            disposition = action.disposition.name
                        )
                    }
                )
            }
            committed = true

            check(database.books().reopenMangaAfterExtension(targetBook.id) == 1)
            recomputeStoredMangaProgress(targetBook.id)

            val receipt = database.mangaMerges().find(mergeId)
                ?: error("Merge receipt disappeared after commit")
            require(receipt.members.size == sourceBookIds.size)
            require(receipt.originals.size == originalTargetChapters.size)
            require(receipt.chapters.size == plan.chapterActions.size)
            prepared.forEach { copy ->
                require(copy.targetArchive.file.isFile)
                val offline = database.mangaOffline().findChapter(copy.targetChapter.id)
                    ?: error("Merged Manga copy has no offline manifest")
                require(offline.chapter.completed)
                require(
                    offline.pages.isNotEmpty() &&
                        offline.pages.all { page ->
                            File(cacheRoot, page.relativePath).isFile
                        }
                )
            }

            MangaMergeExecutionResult(
                mergeId = mergeId,
                targetBookId = targetBook.id,
                sourceBookIds = sourceBookIds.toList(),
                copiedChapterCount = prepared.size,
                deduplicatedChapterCount =
                    plan.chapterActions.count {
                        it.disposition == MangaMergeDisposition.DEDUPLICATE_EXACT_ARCHIVE
                    }
            )
        } catch (error: Throwable) {
            if (committed) {
                runCatching { splitCommittedMerge(mergeId) }
                    .exceptionOrNull()
                    ?.let(error::addSuppressed)
            } else {
                cleanupPreparedMergeCopies(prepared)
            }
            if (error is CancellationException) throw error
            throw error
        }
    }

    suspend fun splitLocalMerge(
        targetBookId: String
    ): Result<MangaSplitExecutionResult> = runCatching {
        val receipt = database.mangaMerges().findForTarget(targetBookId)
            ?: error("This Manga work has no active reversible merge")
        splitCommittedMerge(receipt.merge.id)
    }

    private suspend fun splitCommittedMerge(
        mergeId: String
    ): MangaSplitExecutionResult {
        val receipt = database.mangaMerges().find(mergeId)
            ?: error("Manga merge receipt is missing")
        val merge = receipt.merge
        val targetBook = database.books()
            .findWithCollections(merge.targetBookId)
            ?.toDomain()
            ?: error("Merged target Book is missing")

        val allTargetChapters = database.mangaCatalog().listChapters(targetBook.id)
        require(allTargetChapters.size >= merge.targetOriginalChapterCount) {
            "Merged target lost an original chapter before split"
        }
        val originalOrders = (0 until merge.targetOriginalChapterCount).toSet()
        require(
            allTargetChapters
                .take(merge.targetOriginalChapterCount)
                .map { it.readingOrder } == originalOrders.toList()
        ) {
            "Merged target original chapter boundary is corrupted"
        }
        val originals = receipt.originals.sortedBy { it.readingOrder }
        require(originals.size == merge.targetOriginalChapterCount) {
            "Merge receipt lost original target chapter identity evidence"
        }
        originals.forEachIndexed { expectedOrder, original ->
            require(original.readingOrder == expectedOrder)
            require(original.targetBookId == targetBook.id)
            val chapter = database.mangaCatalog().findChapter(original.chapterId)
                ?: error("Original target Manga chapter disappeared before split")
            require(chapter.bookId == targetBook.id && chapter.readingOrder == expectedOrder)
            val source = database.mangaCatalog()
                .listChapterSources(chapter.id)
                .singleOrNull {
                    it.sourceId == MangaCbzIngestor.LOCAL_CBZ_SOURCE_ID.value
                }
                ?: error("Original target Manga chapter lost its local source")
            require(source.chapterKey == original.chapterKey) {
                "Original target Manga chapter identity changed while merged"
            }
        }
        merge.targetProgressChapterId?.let { progressChapterId ->
            val progressChapter = database.mangaCatalog().findChapter(progressChapterId)
                ?: error("Pre-merge target progress chapter disappeared")
            require(progressChapter.bookId == targetBook.id)
            require(progressChapter.readingOrder in originalOrders) {
                "Pre-merge progress points outside the original target chapter boundary"
            }
        }

        val members = receipt.members.sortedBy { it.sourceOrder }
        require(members.isNotEmpty())
        members.forEachIndexed { index, member ->
            require(member.sourceOrder == index) {
                "Merge receipt source order is corrupted"
            }
        }
        val sourceChapterIds = buildSet {
            members.forEach { member ->
                val sourceBook = database.books().findEntity(member.sourceBookId)
                    ?: error("Merged source Book disappeared before split")
                require(sourceBook.format == BookFormat.COMIC.name)
                database.mangaCatalog()
                    .listChapters(member.sourceBookId)
                    .forEach { chapter -> add(chapter.id) }
            }
        }
        require(receipt.chapters.map { it.sourceChapterId }.toSet() == sourceChapterIds) {
            "Merge receipt does not map every intact source chapter exactly once"
        }

        receipt.chapters.forEach { mapping ->
            require(mapping.sourceBookId in members.map { it.sourceBookId })
            val sourceChapter = database.mangaCatalog().findChapter(mapping.sourceChapterId)
                ?: error("Merged source chapter disappeared before split")
            require(
                sourceChapter.bookId == mapping.sourceBookId &&
                    sourceChapter.readingOrder == mapping.sourceReadingOrder
            ) {
                "Merge receipt source chapter ownership/order is corrupted"
            }
            val targetChapter = database.mangaCatalog().findChapter(mapping.targetChapterId)
                ?: error("Merge target chapter disappeared before split")
            require(
                targetChapter.bookId == targetBook.id &&
                    targetChapter.readingOrder == mapping.targetReadingOrder
            ) {
                "Merge receipt target chapter ownership/order is corrupted"
            }
            val sourceKey = database.mangaCatalog()
                .listChapterSources(sourceChapter.id)
                .singleOrNull {
                    it.sourceId == MangaCbzIngestor.LOCAL_CBZ_SOURCE_ID.value
                }
                ?.chapterKey
                ?: error("Merged source chapter lost its local identity")
            val targetKey = database.mangaCatalog()
                .listChapterSources(targetChapter.id)
                .singleOrNull {
                    it.sourceId == MangaCbzIngestor.LOCAL_CBZ_SOURCE_ID.value
                }
                ?.chapterKey
                ?: error("Merge target chapter lost its local identity")
            require(sourceKey == targetKey) {
                "Merge receipt links chapters with different source archive identities"
            }
        }

        val copyMappings = receipt.chapters
            .filter {
                it.disposition == MangaMergeChapterEntity.REBUILD_FROM_SOURCE_ARCHIVE
            }
        require(copyMappings.all { it.targetReadingOrder >= merge.targetOriginalChapterCount }) {
            "Merge receipt attempts to classify an original target chapter as a removable copy"
        }
        val targetCopies = copyMappings.map { mapping ->
            val chapter = database.mangaCatalog().findChapter(mapping.targetChapterId)
                ?: error("Merged target chapter is missing")
            require(chapter.bookId == targetBook.id)
            val source = database.mangaCatalog()
                .listChapterSources(chapter.id)
                .singleOrNull {
                    it.sourceId == MangaCbzIngestor.LOCAL_CBZ_SOURCE_ID.value
                }
                ?: error("Merged target chapter lost its local source")
            Triple(
                chapter,
                File(cacheRoot, chapter.cacheKey),
                resolveLocalArchiveFile(
                    book = targetBook,
                    chapterKey = source.chapterKey,
                    readingOrder = chapter.readingOrder
                )
            )
        }

        database.withTransaction {
            database.mangaMerges().deleteMerge(mergeId)
            targetCopies.forEach { (chapter, _, _) ->
                database.mangaCatalog().deleteChapter(chapter.id)
            }

            if (merge.targetProgressChapterId != null) {
                database.mangaProgress().upsert(
                    MangaProgressEntity(
                        bookId = targetBook.id,
                        chapterId = merge.targetProgressChapterId,
                        pageIndex = requireNotNull(merge.targetProgressPageIndex),
                        pageCount = merge.targetProgressPageCount,
                        chapterProgression =
                            requireNotNull(merge.targetProgressChapterProgression),
                        updatedAtEpochMs =
                            requireNotNull(merge.targetProgressUpdatedAtEpochMs)
                    )
                )
            } else {
                database.mangaProgress().delete(targetBook.id)
            }
            check(
                database.books().restoreMangaMergeSummary(
                    id = targetBook.id,
                    progress = merge.targetBookProgress,
                    lastOpenedAtEpochMs = merge.targetBookLastOpenedAtEpochMs,
                    finished = merge.targetBookFinished
                ) == 1
            )
        }

        withContext(Dispatchers.IO) {
            targetCopies.forEach { (_, cacheDirectory, archive) ->
                deleteGeneratedChapterDirectory(cacheDirectory)
                archive?.let(::deleteConfinedPublicationFile)
            }
            pruneEmptyMangaCacheParents(targetCopies.map { it.second })
            pruneEmptyLocalArchiveParents(targetCopies.mapNotNull { it.third })
        }

        return MangaSplitExecutionResult(
            mergeId = mergeId,
            targetBookId = targetBook.id,
            restoredSourceBookIds = receipt.members
                .sortedBy { it.sourceOrder }
                .map { it.sourceBookId },
            removedCopiedChapterCount = targetCopies.size
        )
    }

    private suspend fun requireMangaStructureMutable(bookId: String) {
        require(database.mangaMerges().findForTarget(bookId) == null) {
            "Merged Manga chapters are locked until the work is split"
        }
        require(database.mangaMerges().findForSource(bookId) == null) {
            "A hidden Manga source cannot be structurally edited while merged"
        }
    }

    private suspend fun cleanupPreparedMergeCopies(
        prepared: List<PreparedMergeCopy>
    ) {
        withContext(Dispatchers.IO) {
            prepared.forEach { copy ->
                deleteGeneratedChapterDirectory(copy.cacheDirectory)
                if (copy.targetArchive.createdByMerge) {
                    deleteConfinedPublicationFile(copy.targetArchive.file)
                }
            }
            pruneEmptyMangaCacheParents(prepared.map { it.cacheDirectory })
            pruneEmptyLocalArchiveParents(
                prepared
                    .filter { it.targetArchive.createdByMerge }
                    .map { it.targetArchive.file }
            )
        }
    }

    suspend fun canImport(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val name = displayName(uri)?.lowercase(Locale.ROOT)
        if (name?.endsWith(".cbz") == true) return@withContext true
        appContext.contentResolver.getType(uri)?.lowercase(Locale.ROOT) in CBZ_MIME_TYPES
    }

    /**
     * Deletes one persisted Manga through the Book catalog owner, then removes every extracted
     * chapter directory that belonged to it. Database cascades run before filesystem cleanup so a
     * failed Room delete never destroys the user's only readable copy.
     */
    suspend fun deleteImportedManga(bookId: String): Result<Book?> = runCatching {
        require(bookId.isNotBlank())
        database.mangaMerges().findForTarget(bookId)?.let { activeMerge ->
            // Deleting a merged target first performs the same reversible split used by the UI.
            // Source Books become visible again and remain untouched; only then is the target
            // publication itself eligible for permanent removal.
            splitCommittedMerge(activeMerge.merge.id)
        }
        require(database.mangaMerges().findForSource(bookId) == null) {
            "A hidden Manga merge source must be split before permanent deletion"
        }
        val book = library.getBook(bookId)
            ?: return@runCatching null
        val chapters = database.mangaCatalog().listChapters(bookId)
        val chapterDirectories = chapters.map { chapter ->
            File(cacheRoot, chapter.cacheKey)
        }
        val additionalArchives = chapters.mapNotNull { chapter ->
            if (chapter.readingOrder == 0) return@mapNotNull null
            val local = database.mangaCatalog()
                .listChapterSources(chapter.id)
                .firstOrNull { it.sourceId == MangaCbzIngestor.LOCAL_CBZ_SOURCE_ID.value }
                ?: return@mapNotNull null
            resolveLocalArchiveFile(book, local.chapterKey, chapter.readingOrder)
        }

        val deleted = library.deleteImportedBook(bookId) ?: return@runCatching null

        withContext(Dispatchers.IO) {
            additionalArchives.forEach { archive ->
                deleteConfinedPublicationFile(archive)
            }
            chapterDirectories.forEach(::deleteGeneratedChapterDirectory)
            pruneEmptyMangaCacheParents(chapterDirectories)
            pruneEmptyLocalArchiveParents(additionalArchives)
        }
        deleted
    }

    /**
     * Rebuilds Manga-specific catalog/cache state for an already-restored Book row.
     *
     * The source CBZ is the backup authority. Extracted pages and cover thumbnails are
     * regenerated; only the small durable reader restore point is reapplied after ingestion.
     */
    suspend fun rebuildPersistedManga(
        book: Book,
        restorePoint: MangaLocalRestorePoint? = null,
        primaryMetadata: MangaLocalChapterMetadata? = null
    ): Result<Unit> = runCatching {
        require(book.format == BookFormat.COMIC) { "Only COMIC books use Manga rebuild" }
        val sourceFile = requireAppPrivatePublication(book)
        val fingerprint = sha256(sourceFile)
        val mangaId = CanonicalMangaId(book.id)
        val chapterKey = chapterKeyFor(fingerprint)
        val resolvedPrimary = primaryMetadata ?: MangaLocalChapterMetadata(
            title = book.title,
            number = 1.0
        )
        val primaryTitle = resolvedPrimary.title
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: book.title
        val anchor = MangaChapterAnchor(
            volume = resolvedPrimary.volume,
            number = resolvedPrimary.number ?: 1.0,
            languageTag = resolvedPrimary.languageTag,
            normalizedTitle = primaryTitle,
            providerChapterKeyHint = chapterKey
        )
        val offlineId = requireNotNull(
            MangaOfflineChapterLocator.idFor(mangaId, anchor)
        )
        val cacheKey = MangaCacheLayout.chapterDirectory(offlineId)
        val chapterId = UUID.nameUUIDFromBytes(
            ("veil-cbz:" + book.id + ":" + cacheKey).toByteArray(Charsets.UTF_8)
        ).toString()
        val localSource = MangaCbzIngestor.LOCAL_CBZ_SOURCE_ID
        val mangaKey = localMangaKey(book.id)

        database.withTransaction {
            database.mangaCatalog().deleteChaptersForBook(book.id)
            database.mangaCatalog().deleteSourceLinksForBook(book.id)
            database.mangaCatalog().upsertChapter(
                MangaChapterEntity(
                    id = chapterId,
                    bookId = book.id,
                    readingOrder = 0,
                    cacheKey = cacheKey,
                    title = primaryTitle,
                    normalizedTitle = primaryTitle,
                    volume = resolvedPrimary.volume,
                    number = resolvedPrimary.number ?: 1.0,
                    languageTag = resolvedPrimary.languageTag
                )
            )
            database.mangaCatalog().upsertSourceLink(
                MangaSourceLinkEntity(
                    bookId = book.id,
                    sourceId = localSource.value,
                    mangaKey = mangaKey
                )
            )
            database.mangaCatalog().upsertChapterSource(
                MangaChapterSourceEntity(
                    chapterId = chapterId,
                    bookId = book.id,
                    sourceId = localSource.value,
                    mangaKey = mangaKey,
                    chapterKey = chapterKey
                )
            )
        }

        val ingested = ingestor.ingest(
            archiveFile = sourceFile,
            cacheRoot = cacheRoot,
            chapterId = offlineId,
            anchor = anchor,
            originChapterKey = chapterKey,
            originSourceId = localSource
        )
        val manifest = when (ingested) {
            is MangaCbzImportResult.Success -> ingested.manifest
            is MangaCbzImportResult.Failure ->
                throw MangaLocalImportException(ingested.reason)
        }
        RoomMangaOfflineCacheIndex(database).put(manifest)
        val coverPath = cacheCover(manifest, book.id)
        library.updateCoverCachePath(book.id, coverPath)
        library.updateContentFingerprint(book.id, fingerprint)

        restorePoint
            ?.takeIf { it.chapterReadingOrder == 0 }
            ?.let { point ->
                RoomMangaProgressStore(database).save(
                    com.veilreader.app.manga.library.MangaReadingProgress(
                        mangaId = mangaId,
                        chapter = anchor,
                        pageIndex = point.pageIndex,
                        pageCount = point.pageCount,
                        chapterProgression = point.chapterProgression,
                        updatedAtEpochMs = point.updatedAtEpochMs
                    )
                )
            }
    }

    suspend fun restoreLocalProgress(
        bookId: String,
        point: MangaLocalRestorePoint
    ): Result<Unit> = runCatching {
        val chapter = database.mangaCatalog()
            .listChapters(bookId)
            .firstOrNull { it.readingOrder == point.chapterReadingOrder }
            ?: error("Restored Manga progress points to a missing chapter")
        val source = database.mangaCatalog()
            .listChapterSources(chapter.id)
            .firstOrNull { it.sourceId == MangaCbzIngestor.LOCAL_CBZ_SOURCE_ID.value }
            ?: error("Restored Manga chapter has no local source")
        RoomMangaProgressStore(database).save(
            com.veilreader.app.manga.library.MangaReadingProgress(
                mangaId = CanonicalMangaId(bookId),
                chapter = MangaChapterAnchor(
                    volume = chapter.volume,
                    number = chapter.number,
                    languageTag = chapter.languageTag,
                    normalizedTitle = chapter.normalizedTitle ?: chapter.title,
                    providerChapterKeyHint = source.chapterKey
                ),
                pageIndex = point.pageIndex,
                pageCount = point.pageCount,
                chapterProgression = point.chapterProgression,
                updatedAtEpochMs = point.updatedAtEpochMs
            )
        )
    }

    suspend fun appendChapters(
        bookId: String,
        uris: List<Uri>
    ): Result<MangaLocalBatchImportResult> = runCatching {
        require(bookId.isNotBlank())
        val distinct = uris.distinctBy { it.toString() }
        require(distinct.isNotEmpty()) { "No Manga chapter files were selected" }

        val sorted = withContext(Dispatchers.IO) {
            distinct
                .map { uri ->
                    uri to (
                        displayName(uri)
                            ?.substringBeforeLast('.', missingDelimiterValue = "")
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?: uri.toString()
                        )
                }
                .sortedWith(
                    Comparator { left, right ->
                        compareNaturalArchiveNames(left.second, right.second)
                    }
                )
                .map { it.first }
        }

        var added = 0
        var duplicates = 0
        var lastReadingOrder: Int? = null
        sorted.forEach { uri ->
            val result = appendChapter(bookId, uri).getOrThrow()
            if (result.duplicate) {
                duplicates += 1
            } else {
                added += 1
                lastReadingOrder = result.readingOrder
            }
        }
        MangaLocalBatchImportResult(
            addedCount = added,
            duplicateCount = duplicates,
            lastReadingOrder = lastReadingOrder
        )
    }

    /**
     * Adds another local CBZ as a chapter of an existing Manga Book.
     *
     * Book remains the single library/catalog identity. The chapter source fingerprint is durable,
     * while its app-private file path is derived from the Book's publication directory and can be
     * reconstructed during backup restore without adding another catalog table.
     */
    suspend fun appendChapter(
        bookId: String,
        uri: Uri,
        metadata: MangaLocalChapterMetadata? = null
    ): Result<MangaLocalChapterImportResult> = runCatching {
        require(bookId.isNotBlank())
        requireMangaStructureMutable(bookId)
        require(canImport(uri)) { "The selected document is not a CBZ publication" }

        val book = library.getBook(bookId)
            ?: error("Manga Book is not present in the Library")
        require(book.format == BookFormat.COMIC) {
            "Only COMIC books can accept Manga chapters"
        }

        val displayTitle = withContext(Dispatchers.IO) { displayName(uri) }
            ?.substringBeforeLast('.', missingDelimiterValue = "")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "Chapter"

        val staged = materialize(uri)
        var committedArchive: File? = null
        var chapterId: String? = null
        var cacheDirectory: File? = null

        try {
            val fingerprint = sha256(staged)
            val chapterKey = chapterKeyFor(fingerprint)
            val localSource = MangaCbzIngestor.LOCAL_CBZ_SOURCE_ID

            if (book.contentFingerprint.equals(fingerprint, ignoreCase = true)) {
                staged.delete()
                val first = database.mangaCatalog().listChapters(book.id)
                    .firstOrNull { it.readingOrder == 0 }
                    ?: error("Primary Manga archive has no persisted chapter")
                return@runCatching MangaLocalChapterImportResult(
                    book = book,
                    chapterId = first.id,
                    readingOrder = first.readingOrder,
                    duplicate = true
                )
            }

            val duplicateSource = database.mangaCatalog().findChapterSourceForBook(
                bookId = book.id,
                sourceId = localSource.value,
                chapterKey = chapterKey
            )
            if (duplicateSource != null) {
                staged.delete()
                val existing = database.mangaCatalog()
                    .findChapter(duplicateSource.chapterId)
                    ?: error("Duplicate Manga source points to a missing chapter")
                return@runCatching MangaLocalChapterImportResult(
                    book = book,
                    chapterId = existing.id,
                    readingOrder = existing.readingOrder,
                    duplicate = true
                )
            }

            val existingChapters = database.mangaCatalog().listChapters(book.id)
            val readingOrder = existingChapters.maxOfOrNull { it.readingOrder }
                ?.plus(1)
                ?: 0
            val inferred = inferChapterMetadata(displayTitle, readingOrder)
            val resolvedMetadata = MangaLocalChapterMetadata(
                title = metadata?.title?.trim()?.takeIf { it.isNotEmpty() }
                    ?: inferred.title,
                volume = metadata?.volume ?: inferred.volume,
                number = metadata?.number ?: inferred.number,
                languageTag = metadata?.languageTag
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: inferred.languageTag
            )

            val archive = commitAdditionalArchive(book, staged, fingerprint)
            committedArchive = archive

            val anchor = MangaChapterAnchor(
                volume = resolvedMetadata.volume,
                number = resolvedMetadata.number,
                languageTag = resolvedMetadata.languageTag,
                normalizedTitle = resolvedMetadata.title,
                providerChapterKeyHint = chapterKey
            )
            val mangaId = CanonicalMangaId(book.id)
            val offlineId = requireNotNull(
                MangaOfflineChapterLocator.idFor(mangaId, anchor)
            )
            val cacheKey = MangaCacheLayout.chapterDirectory(offlineId)
            val newChapterId = UUID.nameUUIDFromBytes(
                ("veil-cbz:" + book.id + ":" + cacheKey).toByteArray(Charsets.UTF_8)
            ).toString()
            chapterId = newChapterId
            cacheDirectory = File(cacheRoot, cacheKey)
            val mangaKey = localMangaKey(book.id)

            database.withTransaction {
                // Normalize pre-multi-chapter local rows to one source-stable work key.
                database.mangaCatalog().upsertSourceLink(
                    MangaSourceLinkEntity(
                        bookId = book.id,
                        sourceId = localSource.value,
                        mangaKey = mangaKey
                    )
                )
                existingChapters.forEach { existing ->
                    database.mangaCatalog()
                        .listChapterSources(existing.id)
                        .filter { it.sourceId == localSource.value }
                        .forEach { source ->
                            database.mangaCatalog().upsertChapterSource(
                                source.copy(mangaKey = mangaKey)
                            )
                        }
                }
                database.mangaCatalog().upsertChapter(
                    MangaChapterEntity(
                        id = newChapterId,
                        bookId = book.id,
                        readingOrder = readingOrder,
                        cacheKey = cacheKey,
                        title = resolvedMetadata.title,
                        normalizedTitle = resolvedMetadata.title,
                        volume = resolvedMetadata.volume,
                        number = resolvedMetadata.number,
                        languageTag = resolvedMetadata.languageTag
                    )
                )
                database.mangaCatalog().upsertChapterSource(
                    MangaChapterSourceEntity(
                        chapterId = newChapterId,
                        bookId = book.id,
                        sourceId = localSource.value,
                        mangaKey = mangaKey,
                        chapterKey = chapterKey
                    )
                )
            }

            val imported = ingestor.ingest(
                archiveFile = archive,
                cacheRoot = cacheRoot,
                chapterId = offlineId,
                anchor = anchor,
                originChapterKey = chapterKey,
                originSourceId = localSource
            )
            val manifest = when (imported) {
                is MangaCbzImportResult.Success -> imported.manifest
                is MangaCbzImportResult.Failure ->
                    throw MangaLocalImportException(imported.reason)
            }
            RoomMangaOfflineCacheIndex(database).put(manifest)

            database.books().reopenMangaAfterExtension(book.id)
            val progressStore = RoomMangaProgressStore(database)
            progressStore.load(mangaId)?.let { previous ->
                progressStore.save(previous)
            }

            MangaLocalChapterImportResult(
                book = book,
                chapterId = newChapterId,
                readingOrder = readingOrder,
                duplicate = false
            )
        } catch (error: Throwable) {
            val insertedChapterId = chapterId
            if (insertedChapterId != null) {
                runCatching { database.mangaCatalog().deleteChapter(insertedChapterId) }
            }
            cacheDirectory?.let(::deleteGeneratedChapterDirectory)
            committedArchive?.let(::deleteConfinedPublicationFile)
            staged.takeIf(File::exists)?.delete()
            if (error is CancellationException) throw error
            throw error
        }
    }

    suspend fun import(uri: Uri): Result<BookImportResult> {
        if (!canImport(uri)) {
            return Result.failure(
                IllegalArgumentException("The selected document is not a CBZ publication")
            )
        }

        var stagedBook: Book? = null
        var committedNew = false
        var offlineDirectory: File? = null

        try {
            val localFile = materialize(uri)
            val fingerprint = sha256(localFile)
            val displayTitle = withContext(Dispatchers.IO) { displayName(uri) }
                ?.substringBeforeLast('.', missingDelimiterValue = "")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: "Untitled"

            val book = Book(
                id = UUID.randomUUID().toString(),
                title = displayTitle,
                author = "",
                format = BookFormat.COMIC,
                sourceUri = Uri.fromFile(localFile).toString(),
                mediaType = CBZ_MEDIA_TYPE,
                contentFingerprint = fingerprint
            )
            stagedBook = book

            val commit = library.addImportedBook(book)
            if (commit.duplicate) {
                return Result.success(commit)
            }
            committedNew = true

            val mangaId = CanonicalMangaId(book.id)
            val chapterKey = chapterKeyFor(fingerprint)
            val anchor = MangaChapterAnchor(
                number = 1.0,
                normalizedTitle = book.title,
                providerChapterKeyHint = chapterKey
            )
            val offlineId = requireNotNull(
                MangaOfflineChapterLocator.idFor(mangaId, anchor)
            )
            val cacheKey = MangaCacheLayout.chapterDirectory(offlineId)
            val chapterId = UUID.nameUUIDFromBytes(
                "veil-cbz:${book.id}:$cacheKey".toByteArray(Charsets.UTF_8)
            ).toString()
            val localSource = MangaCbzIngestor.LOCAL_CBZ_SOURCE_ID
            offlineDirectory = File(cacheRoot, cacheKey)

            database.withTransaction {
                database.mangaCatalog().upsertChapter(
                    MangaChapterEntity(
                        id = chapterId,
                        bookId = book.id,
                        readingOrder = 0,
                        cacheKey = cacheKey,
                        title = null,
                        normalizedTitle = book.title,
                        number = 1.0
                    )
                )
                database.mangaCatalog().upsertSourceLink(
                    MangaSourceLinkEntity(
                        bookId = book.id,
                        sourceId = localSource.value,
                        mangaKey = localMangaKey(book.id)
                    )
                )
                database.mangaCatalog().upsertChapterSource(
                    MangaChapterSourceEntity(
                        chapterId = chapterId,
                        bookId = book.id,
                        sourceId = localSource.value,
                        mangaKey = localMangaKey(book.id),
                        chapterKey = chapterKey
                    )
                )
            }

            val ingested = ingestor.ingest(
                archiveFile = localFile,
                cacheRoot = cacheRoot,
                chapterId = offlineId,
                anchor = anchor,
                originChapterKey = chapterKey,
                originSourceId = localSource
            )
            val manifest = when (ingested) {
                is MangaCbzImportResult.Success -> ingested.manifest
                is MangaCbzImportResult.Failure ->
                    throw MangaLocalImportException(ingested.reason)
            }
            RoomMangaOfflineCacheIndex(database).put(manifest)
            val coverPath = cacheCover(manifest, book.id)
            library.updateCoverCachePath(book.id, coverPath)

            return Result.success(commit)
        } catch (error: Throwable) {
            offlineDirectory?.let(::deleteGeneratedChapterDirectory)
            val book = stagedBook
            if (book != null) {
                if (committedNew) {
                    runCatching { library.rollbackImportedBook(book) }
                } else {
                    deleteStagedPublication(book)
                }
            }
            if (error is CancellationException) throw error
            return Result.failure(error)
        }
    }

    private suspend fun materialize(source: Uri): File = withContext(Dispatchers.IO) {
        val declaredSize = querySize(source)
        if (declaredSize != null && declaredSize > limits.maxArchiveBytes) {
            throw MangaLocalImportException(
                MangaCbzImportFailureReason.ARCHIVE_TOO_LARGE
            )
        }

        val importsDir = File(appContext.filesDir, "publications").apply { mkdirs() }
        val target = File(importsDir, UUID.randomUUID().toString() + ".cbz")
        val partial = File(importsDir, target.name + ".partial")

        try {
            appContext.contentResolver.openInputStream(source)?.use { input ->
                partial.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var copied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (read == 0) continue
                        copied += read
                        if (copied > limits.maxArchiveBytes) {
                            throw MangaLocalImportException(
                                MangaCbzImportFailureReason.ARCHIVE_TOO_LARGE
                            )
                        }
                        output.write(buffer, 0, read)
                    }
                }
            } ?: throw IOException("Android could not read the selected CBZ")
            if (partial.length() == 0L) {
                throw IOException("The selected CBZ is empty")
            }
            if (!partial.renameTo(target)) {
                throw IOException("Could not commit the app-private CBZ copy")
            }
            target
        } catch (error: Throwable) {
            partial.delete()
            target.delete()
            throw error
        }
    }

    fun resolveLocalArchiveFile(
        book: Book,
        chapterKey: String,
        readingOrder: Int
    ): File? {
        if (readingOrder == 0) {
            return runCatching { requireAppPrivatePublication(book) }.getOrNull()
        }
        val fingerprint = fingerprintFromChapterKey(chapterKey) ?: return null
        val primary = runCatching { requireAppPrivatePublication(book) }.getOrNull()
            ?: return null
        val candidate = File(
            primary.parentFile,
            "manga/" + book.id + "/" + fingerprint + ".cbz"
        )
        return runCatching {
            val root = File(appContext.filesDir, "publications").canonicalFile
            val canonical = candidate.canonicalFile
            canonical.takeIf {
                it.toPath().startsWith(root.toPath()) && it.isFile
            }
        }.getOrNull()
    }

    private suspend fun copyVerifiedArchiveToTarget(
        targetBook: Book,
        sourceArchive: File,
        fingerprint: String
    ): CopiedMergeArchive = withContext(Dispatchers.IO) {
        require(sourceArchive.isFile) { "Merge source archive is missing" }
        require(sha256(sourceArchive).equals(fingerprint, ignoreCase = true)) {
            "Merge source archive fingerprint changed before copy"
        }

        val primary = requireAppPrivatePublication(targetBook)
        val root = File(appContext.filesDir, "publications").canonicalFile
        val directory = File(primary.parentFile, "manga/" + targetBook.id).canonicalFile
        require(directory.toPath().startsWith(root.toPath())) {
            "Derived Manga merge archive directory escaped app-private publications"
        }
        check(directory.mkdirs() || directory.isDirectory) {
            "Could not create target Manga merge archive directory"
        }

        val target = File(directory, fingerprint + ".cbz").canonicalFile
        require(target.toPath().startsWith(root.toPath())) {
            "Derived Manga merge archive path escaped app-private publications"
        }
        val temporary = File(
            directory,
            fingerprint + ".merge-" + UUID.randomUUID() + ".partial"
        ).canonicalFile
        require(temporary.toPath().startsWith(root.toPath()))

        try {
            sourceArchive.inputStream().buffered().use { input ->
                temporary.outputStream().buffered().use { output ->
                    input.copyTo(output)
                }
            }
            check(temporary.length() == sourceArchive.length() && temporary.length() > 0L) {
                "Merged Manga archive copy length mismatch"
            }
            check(sha256(temporary).equals(fingerprint, ignoreCase = true)) {
                "Merged Manga archive copy failed SHA-256 verification"
            }
            if (target.exists()) {
                check(target.delete()) {
                    "Could not replace stale target Manga merge archive"
                }
            }
            if (!temporary.renameTo(target)) {
                temporary.inputStream().use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                check(
                    target.length() == sourceArchive.length() &&
                        sha256(target).equals(fingerprint, ignoreCase = true)
                ) {
                    target.delete()
                    "Could not verify committed Manga merge archive"
                }
                temporary.delete()
            }
            CopiedMergeArchive(
                file = target,
                createdByMerge = true
            )
        } catch (error: Throwable) {
            temporary.delete()
            target.takeIf(File::exists)?.let(::deleteConfinedPublicationFile)
            throw error
        }
    }

    private suspend fun commitAdditionalArchive(
        book: Book,
        staged: File,
        fingerprint: String
    ): File = withContext(Dispatchers.IO) {
        val primary = requireAppPrivatePublication(book)
        val root = File(appContext.filesDir, "publications").canonicalFile
        val directory = File(primary.parentFile, "manga/" + book.id).canonicalFile
        require(directory.toPath().startsWith(root.toPath())) {
            "Derived Manga archive directory escaped app-private publications"
        }
        check(directory.mkdirs() || directory.isDirectory) {
            "Could not create Manga chapter publication directory"
        }
        val target = File(directory, fingerprint + ".cbz").canonicalFile
        require(target.toPath().startsWith(root.toPath())) {
            "Derived Manga archive path escaped app-private publications"
        }

        if (target.exists()) {
            if (sha256(target) == fingerprint) {
                staged.delete()
                return@withContext target
            }
            check(target.delete()) { "Could not replace a corrupt staged Manga chapter" }
        }
        if (!staged.renameTo(target)) {
            staged.inputStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            check(target.length() == staged.length() && sha256(target) == fingerprint) {
                target.delete()
                "Could not verify copied Manga chapter"
            }
            staged.delete()
        }
        target
    }

    private fun inferChapterMetadata(
        displayTitle: String,
        readingOrder: Int
    ): MangaLocalChapterMetadata {
        val clean = displayTitle.trim().ifEmpty { "Chapter " + (readingOrder + 1) }
        val volume = VOLUME_PATTERN.find(clean)
            ?.groupValues
            ?.getOrNull(1)
            ?.toDoubleOrNull()
        val number = CHAPTER_PATTERN.find(clean)
            ?.groupValues
            ?.getOrNull(1)
            ?.toDoubleOrNull()
            ?: TRAILING_NUMBER_PATTERN.find(clean)
                ?.groupValues
                ?.getOrNull(1)
                ?.toDoubleOrNull()
            ?: (readingOrder + 1).toDouble()
        return MangaLocalChapterMetadata(
            title = clean,
            volume = volume,
            number = number
        )
    }

    private fun localMangaKey(bookId: String): String = "local:" + bookId

    private fun chapterKeyFor(fingerprint: String): String = "cbz-" + fingerprint

    private fun fingerprintFromChapterKey(chapterKey: String): String? =
        chapterKey
            .takeIf { it.startsWith("cbz-") }
            ?.removePrefix("cbz-")
            ?.takeIf { value ->
                value.length == 64 && value.all { it in '0'..'9' || it in 'a'..'f' }
            }

    private fun deleteConfinedPublicationFile(file: File) {
        runCatching {
            val root = File(appContext.filesDir, "publications").canonicalFile
            val candidate = file.canonicalFile
            if (candidate.toPath().startsWith(root.toPath()) && candidate.isFile) {
                candidate.delete()
            }
        }
    }

    private fun pruneEmptyLocalArchiveParents(files: List<File>) {
        val root = runCatching {
            File(appContext.filesDir, "publications").canonicalFile
        }.getOrNull() ?: return
        files.forEach { file ->
            var current = runCatching { file.canonicalFile.parentFile }.getOrNull()
            while (
                current != null &&
                current != root &&
                current.toPath().startsWith(root.toPath())
            ) {
                val children = current.listFiles()
                if (children != null && children.isEmpty()) {
                    val parent = current.parentFile
                    if (!current.delete()) break
                    current = parent
                } else {
                    break
                }
            }
        }
    }

    private fun requireAppPrivatePublication(book: Book): File {
        val uri = book.sourceUri?.let(Uri::parse)
            ?: error("Restored Manga has no source file")
        require(uri.scheme == "file") { "Restored Manga source must be app-private" }
        val root = File(appContext.filesDir, "publications").canonicalFile
        val file = File(requireNotNull(uri.path)).canonicalFile
        require(file.toPath().startsWith(root.toPath()) && file.isFile) {
            "Restored Manga source is outside app-private publications"
        }
        return file
    }

    private suspend fun sha256(file: File): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString(separator = "") { byte ->
            "%02x".format(Locale.ROOT, byte.toInt() and 0xff)
        }
    }

    private suspend fun cacheCover(
        manifest: OfflineChapterManifest,
        bookId: String
    ): String = withContext(Dispatchers.IO) {
        runCatching {
            val firstPage = manifest.pages.minByOrNull { it.index } ?: return@runCatching ""
            val root = cacheRoot.canonicalFile
            val source = File(root, firstPage.relativePath).canonicalFile
            check(source.toPath().startsWith(root.toPath()) && source.isFile)

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(source.absolutePath, bounds)
            check(bounds.outWidth > 0 && bounds.outHeight > 0)

            var sampleSize = 1
            while (
                bounds.outWidth / sampleSize > COVER_DECODE_MAX_WIDTH ||
                bounds.outHeight / sampleSize > COVER_DECODE_MAX_HEIGHT
            ) {
                sampleSize *= 2
            }

            val decoded = BitmapFactory.decodeFile(
                source.absolutePath,
                BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
            ) ?: return@runCatching ""

            val scale = min(
                COVER_MAX_WIDTH.toFloat() / decoded.width.toFloat(),
                COVER_MAX_HEIGHT.toFloat() / decoded.height.toFloat()
            ).coerceAtMost(1f)
            val output = if (scale < 0.999f) {
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).toInt().coerceAtLeast(1),
                    (decoded.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                decoded
            }

            try {
                val coversDir = File(appContext.filesDir, "covers").apply { mkdirs() }
                val safeName = UUID.nameUUIDFromBytes(
                    "veil-cover:$bookId".toByteArray(Charsets.UTF_8)
                ).toString()
                val target = File(coversDir, "$safeName.jpg")
                val temporary = File(coversDir, "$safeName.tmp")

                temporary.outputStream().buffered().use { stream ->
                    check(output.compress(Bitmap.CompressFormat.JPEG, COVER_JPEG_QUALITY, stream))
                }
                check(temporary.length() > 0L)
                if (target.exists() && !target.delete()) {
                    temporary.delete()
                    return@runCatching ""
                }
                if (!temporary.renameTo(target)) {
                    temporary.delete()
                    return@runCatching ""
                }
                target.absolutePath
            } finally {
                if (output !== decoded) output.recycle()
                decoded.recycle()
            }
        }.getOrDefault("")
    }

    private fun querySize(uri: Uri): Long? = runCatching {
        appContext.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index < 0 || cursor.isNull(index)) null else cursor.getLong(index)
        }
    }.getOrNull()

    private fun displayName(uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        return runCatching {
            appContext.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        }.getOrNull()
    }

    private fun pruneEmptyMangaCacheParents(chapterDirectories: List<File>) {
        val root = runCatching { cacheRoot.canonicalFile }.getOrNull() ?: return
        chapterDirectories.forEach { directory ->
            var current = runCatching { directory.canonicalFile.parentFile }.getOrNull()
            while (
                current != null &&
                current != root &&
                current.toPath().startsWith(root.toPath())
            ) {
                val children = current.listFiles()
                if (children != null && children.isEmpty()) {
                    val parent = current.parentFile
                    if (!current.delete()) break
                    current = parent
                } else {
                    break
                }
            }
        }
    }

    private fun deleteGeneratedChapterDirectory(directory: File) {
        runCatching {
            val root = cacheRoot.canonicalFile
            val candidate = directory.canonicalFile
            if (
                candidate.toPath().startsWith(root.toPath()) &&
                candidate != root
            ) {
                candidate.deleteRecursively()
            }
        }
    }

    private fun deleteStagedPublication(book: Book) {
        runCatching {
            val path = book.sourceUri
                ?.let(Uri::parse)
                ?.takeIf { it.scheme == "file" }
                ?.path
                ?: return
            val root = File(appContext.filesDir, "publications").canonicalFile
            val candidate = File(path).canonicalFile
            if (candidate.toPath().startsWith(root.toPath()) && candidate.isFile) {
                candidate.delete()
            }
        }
    }

    private companion object {
        const val CBZ_MEDIA_TYPE = "application/vnd.comicbook+zip"
        val CBZ_MIME_TYPES = setOf(
            CBZ_MEDIA_TYPE,
            "application/x-cbz"
        )
        const val COVER_MAX_WIDTH = 600
        const val COVER_MAX_HEIGHT = 900
        const val COVER_DECODE_MAX_WIDTH = 1_200
        const val COVER_DECODE_MAX_HEIGHT = 1_800
        const val COVER_JPEG_QUALITY = 88
        private val VOLUME_PATTERN =
            Regex("""(?i)(?:^|[\s._-])(?:vol(?:ume)?|v)[\s._-]*(\d+(?:\.\d+)?)""")
        private val CHAPTER_PATTERN =
            Regex("""(?i)(?:^|[\s._-])(?:ch(?:apter)?|c)[\s._-]*(\d+(?:\.\d+)?)""")
        private val TRAILING_NUMBER_PATTERN =
            Regex("""(\d+(?:\.\d+)?)(?:[\s._-]*)$""")
    }
}
