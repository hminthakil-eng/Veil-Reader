package com.veilreader.app.manga.importing

import com.veilreader.app.manga.library.MangaCacheLayout
import com.veilreader.app.manga.library.MangaChapterAnchor
import com.veilreader.app.manga.library.OfflineChapterId
import com.veilreader.app.manga.library.OfflineChapterManifest
import com.veilreader.app.manga.library.OfflinePageEntry
import com.veilreader.app.manga.source.SourceId
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

data class MangaCbzImportLimits(
    val maxArchiveBytes: Long = 4L * 1024L * 1024L * 1024L,
    val maxEntries: Int = 20_000,
    val maxPageBytes: Long = 128L * 1024L * 1024L,
    val maxExpandedBytes: Long = 4L * 1024L * 1024L * 1024L
) {
    init {
        require(maxArchiveBytes > 0)
        require(maxEntries > 0)
        require(maxPageBytes > 0)
        require(maxExpandedBytes > 0)
    }
}

enum class MangaCbzImportFailureReason {
    ARCHIVE_TOO_LARGE,
    TOO_MANY_ENTRIES,
    UNSAFE_ENTRY,
    NO_READABLE_IMAGES,
    PAGE_TOO_LARGE,
    EXPANDED_DATA_TOO_LARGE,
    IO_FAILURE,
    COMMIT_FAILED
}

sealed interface MangaCbzImportResult {
    data class Success(
        val manifest: OfflineChapterManifest
    ) : MangaCbzImportResult

    data class Failure(
        val reason: MangaCbzImportFailureReason
    ) : MangaCbzImportResult
}

/**
 * Safe local-first CBZ ingestion.
 *
 * The archive's own paths are never used as output paths. Pages are extracted into Veil-owned,
 * deterministic cache names, hashed while streaming, staged as one chapter directory, then swapped
 * into place only after every page succeeds. A failed import leaves the previous chapter directory
 * untouched.
 */
class MangaCbzIngestor(
    private val limits: MangaCbzImportLimits = MangaCbzImportLimits(),
    private val clock: () -> Long = System::currentTimeMillis
) {

    fun ingest(
        archiveFile: File,
        cacheRoot: File,
        chapterId: OfflineChapterId,
        anchor: MangaChapterAnchor,
        originChapterKey: String,
        originSourceId: SourceId = LOCAL_CBZ_SOURCE_ID
    ): MangaCbzImportResult {
        require(originChapterKey.isNotBlank()) { "Origin chapter key cannot be blank" }

        if (!archiveFile.isFile || archiveFile.length() > limits.maxArchiveBytes) {
            return MangaCbzImportResult.Failure(
                if (archiveFile.isFile) {
                    MangaCbzImportFailureReason.ARCHIVE_TOO_LARGE
                } else {
                    MangaCbzImportFailureReason.IO_FAILURE
                }
            )
        }

        val chapterRelativeDirectory = MangaCacheLayout.chapterDirectory(chapterId)
        val finalChapterDirectory = confinedFile(cacheRoot, chapterRelativeDirectory)
            ?: return MangaCbzImportResult.Failure(MangaCbzImportFailureReason.COMMIT_FAILED)
        val parent = finalChapterDirectory.parentFile
            ?: return MangaCbzImportResult.Failure(MangaCbzImportFailureReason.COMMIT_FAILED)
        if (!parent.exists() && !parent.mkdirs()) {
            return MangaCbzImportResult.Failure(MangaCbzImportFailureReason.IO_FAILURE)
        }

        val token = java.lang.Long.toUnsignedString(System.nanoTime(), 36)
        val stagingDirectory = File(parent, finalChapterDirectory.name + ".staging-" + token)
        val backupDirectory = File(parent, finalChapterDirectory.name + ".backup-" + token)
        stagingDirectory.deleteRecursively()
        backupDirectory.deleteRecursively()
        if (!stagingDirectory.mkdirs()) {
            return MangaCbzImportResult.Failure(MangaCbzImportFailureReason.IO_FAILURE)
        }

        val manifest = try {
            ZipFile(archiveFile).use { zip ->
                val allEntries = mutableListOf<ZipEntry>()
                val enumeration = zip.entries()
                while (enumeration.hasMoreElements()) {
                    allEntries += enumeration.nextElement()
                    if (allEntries.size > limits.maxEntries) {
                        throw ImportAbort(MangaCbzImportFailureReason.TOO_MANY_ENTRIES)
                    }
                }

                val imageEntries = allEntries
                    .asSequence()
                    .filterNot { it.isDirectory }
                    .filter { entry ->
                        val extension = extensionOf(entry.name)
                        extension != null
                    }
                    .onEach { entry ->
                        if (!isSafeArchiveEntryName(entry.name)) {
                            throw ImportAbort(MangaCbzImportFailureReason.UNSAFE_ENTRY)
                        }
                    }
                    .sortedWith(Comparator { left, right ->
                        compareNaturalArchiveNames(left.name, right.name)
                    })
                    .toList()

                if (imageEntries.isEmpty()) {
                    throw ImportAbort(MangaCbzImportFailureReason.NO_READABLE_IMAGES)
                }

                var expandedBytes = 0L
                val pages = ArrayList<OfflinePageEntry>(imageEntries.size)
                imageEntries.forEachIndexed { pageIndex, entry ->
                    val declaredSize = entry.size
                    if (declaredSize > limits.maxPageBytes) {
                        throw ImportAbort(MangaCbzImportFailureReason.PAGE_TOO_LARGE)
                    }

                    val extension = requireNotNull(extensionOf(entry.name))
                    val finalRelativePath = MangaCacheLayout.pagePath(
                        chapterId = chapterId,
                        pageIndex = pageIndex,
                        extension = extension
                    )
                    val stagedFile = File(stagingDirectory, File(finalRelativePath).name)
                    val digest = MessageDigest.getInstance("SHA-256")
                    var pageBytes = 0L

                    zip.getInputStream(entry).use { input ->
                        stagedFile.outputStream().buffered().use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                pageBytes += read
                                expandedBytes += read
                                if (pageBytes > limits.maxPageBytes) {
                                    throw ImportAbort(MangaCbzImportFailureReason.PAGE_TOO_LARGE)
                                }
                                if (expandedBytes > limits.maxExpandedBytes) {
                                    throw ImportAbort(
                                        MangaCbzImportFailureReason.EXPANDED_DATA_TOO_LARGE
                                    )
                                }
                                digest.update(buffer, 0, read)
                                output.write(buffer, 0, read)
                            }
                        }
                    }

                    pages += OfflinePageEntry(
                        index = pageIndex,
                        relativePath = finalRelativePath,
                        byteSize = pageBytes,
                        contentSha256 = digest.digest().toHex()
                    )
                }

                OfflineChapterManifest(
                    chapterId = chapterId,
                    anchor = anchor,
                    pages = pages,
                    originSourceId = originSourceId,
                    originChapterKey = originChapterKey,
                    completed = true,
                    updatedAtEpochMs = clock()
                )
            }
        } catch (abort: ImportAbort) {
            stagingDirectory.deleteRecursively()
            return MangaCbzImportResult.Failure(abort.reason)
        } catch (_: IOException) {
            stagingDirectory.deleteRecursively()
            return MangaCbzImportResult.Failure(MangaCbzImportFailureReason.IO_FAILURE)
        } catch (_: SecurityException) {
            stagingDirectory.deleteRecursively()
            return MangaCbzImportResult.Failure(MangaCbzImportFailureReason.IO_FAILURE)
        }

        val hadPrevious = finalChapterDirectory.exists()
        if (hadPrevious && !finalChapterDirectory.renameTo(backupDirectory)) {
            stagingDirectory.deleteRecursively()
            return MangaCbzImportResult.Failure(MangaCbzImportFailureReason.COMMIT_FAILED)
        }

        if (!stagingDirectory.renameTo(finalChapterDirectory)) {
            if (hadPrevious) {
                backupDirectory.renameTo(finalChapterDirectory)
            }
            stagingDirectory.deleteRecursively()
            return MangaCbzImportResult.Failure(MangaCbzImportFailureReason.COMMIT_FAILED)
        }

        if (backupDirectory.exists()) {
            backupDirectory.deleteRecursively()
        }
        return MangaCbzImportResult.Success(manifest)
    }

    private class ImportAbort(
        val reason: MangaCbzImportFailureReason
    ) : RuntimeException()

    companion object {
        val LOCAL_CBZ_SOURCE_ID = SourceId("local.cbz")
    }
}

internal fun compareNaturalArchiveNames(left: String, right: String): Int {
    val a = left.replace('\\', '/').lowercase(Locale.ROOT)
    val b = right.replace('\\', '/').lowercase(Locale.ROOT)
    var i = 0
    var j = 0

    while (i < a.length && j < b.length) {
        val aDigit = a[i].isDigit()
        val bDigit = b[j].isDigit()
        if (aDigit && bDigit) {
            val aStart = i
            val bStart = j
            while (i < a.length && a[i].isDigit()) i += 1
            while (j < b.length && b[j].isDigit()) j += 1

            val aRun = a.substring(aStart, i)
            val bRun = b.substring(bStart, j)
            val aSignificant = aRun.trimStart('0').ifEmpty { "0" }
            val bSignificant = bRun.trimStart('0').ifEmpty { "0" }
            if (aSignificant.length != bSignificant.length) {
                return aSignificant.length.compareTo(bSignificant.length)
            }
            val numericCompare = aSignificant.compareTo(bSignificant)
            if (numericCompare != 0) return numericCompare
            if (aRun.length != bRun.length) return aRun.length.compareTo(bRun.length)
        } else {
            val compare = a[i].compareTo(b[j])
            if (compare != 0) return compare
            i += 1
            j += 1
        }
    }
    return a.length.compareTo(b.length)
}

private fun extensionOf(name: String): String? {
    val extension = name.substringAfterLast('.', missingDelimiterValue = "")
        .lowercase(Locale.ROOT)
    return when (extension) {
        "jpg", "jpeg", "png", "webp" -> extension
        else -> null
    }
}

private fun isSafeArchiveEntryName(name: String): Boolean {
    if (name.isBlank() || '\u0000' in name) return false
    val normalized = name.replace('\\', '/')
    if (normalized.startsWith('/') || Regex("^[A-Za-z]:").containsMatchIn(normalized)) {
        return false
    }
    return normalized.split('/').none { it == ".." }
}

private fun confinedFile(root: File, relativePath: String): File? {
    val canonicalRoot = try {
        root.canonicalFile
    } catch (_: IOException) {
        return null
    }
    if (!canonicalRoot.exists() && !canonicalRoot.mkdirs()) return null

    val target = try {
        File(canonicalRoot, relativePath).canonicalFile
    } catch (_: IOException) {
        return null
    }
    val rootPath = canonicalRoot.path
    val targetPath = target.path
    return target.takeIf {
        targetPath == rootPath || targetPath.startsWith(rootPath + File.separator)
    }
}

private fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
    "%02x".format(Locale.ROOT, byte.toInt() and 0xff)
}
