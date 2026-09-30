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
import com.veilreader.app.data.db.MangaSourceLinkEntity
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.manga.importing.MangaCbzImportFailureReason
import com.veilreader.app.manga.importing.MangaCbzImportLimits
import com.veilreader.app.manga.importing.MangaCbzImportResult
import com.veilreader.app.manga.importing.MangaCbzIngestor
import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.MangaCacheLayout
import com.veilreader.app.manga.library.MangaChapterAnchor
import com.veilreader.app.manga.library.MangaOfflineChapterLocator
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
