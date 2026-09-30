package com.veilreader.app.data.manga

import android.content.Context
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
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MangaLocalImportException(
    val reason: MangaCbzImportFailureReason
) : IllegalStateException("Local Manga import failed: $reason")

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

    fun canImport(uri: Uri): Boolean {
        val name = displayName(uri)?.lowercase(Locale.ROOT)
        if (name?.endsWith(".cbz") == true) return true
        return appContext.contentResolver.getType(uri)?.lowercase(Locale.ROOT) in CBZ_MIME_TYPES
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
            val displayTitle = displayName(uri)
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
            val chapterKey = "cbz-" + fingerprint.take(24)
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
                        mangaKey = "cbz-$fingerprint"
                    )
                )
                database.mangaCatalog().upsertChapterSource(
                    MangaChapterSourceEntity(
                        chapterId = chapterId,
                        bookId = book.id,
                        sourceId = localSource.value,
                        mangaKey = "cbz-$fingerprint",
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
        val importsDir = File(appContext.filesDir, "publications").apply { mkdirs() }
        val target = File(importsDir, UUID.randomUUID().toString() + ".cbz")

        try {
            appContext.contentResolver.openInputStream(source)?.use { input ->
                target.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var copied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
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
            if (target.length() == 0L) {
                throw IOException("The selected CBZ is empty")
            }
            target
        } catch (error: Throwable) {
            target.delete()
            throw error
        }
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
    }
}
