package com.veilreader.app.ui.reader.tts

import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

internal data class ReaderTtsArchiveLimits(
    val maxEntries: Int = 8_192,
    val maxExpandedBytes: Long = 4L * 1024L * 1024L * 1024L,
    val maxSingleFileBytes: Long = 2L * 1024L * 1024L * 1024L
) {
    init {
        require(maxEntries in 1..100_000)
        require(maxExpandedBytes > 0L)
        require(maxSingleFileBytes > 0L)
        require(maxSingleFileBytes <= maxExpandedBytes)
    }
}

internal data class ReaderTtsArchiveExtraction(
    val files: Int,
    val directories: Int,
    val expandedBytes: Long
)

internal sealed interface ReaderTtsArchiveExtractResult {
    data class Success(val extraction: ReaderTtsArchiveExtraction) :
        ReaderTtsArchiveExtractResult

    data class Rejected(val reason: Reason) : ReaderTtsArchiveExtractResult

    enum class Reason {
        SOURCE_MISSING,
        DESTINATION_NOT_EMPTY,
        TOO_MANY_ENTRIES,
        EXPANDED_SIZE_LIMIT,
        SINGLE_FILE_SIZE_LIMIT,
        UNSAFE_PATH,
        LINK_NOT_ALLOWED,
        UNSUPPORTED_ENTRY,
        IO_ERROR
    }
}

/**
 * Fail-closed extractor for upstream neural TTS tar.bz2 packages.
 *
 * The destination must be a dedicated staging directory. Links, devices, FIFOs, absolute paths,
 * parent traversal and Windows-style path tricks are rejected. Limits are enforced from both tar
 * metadata and actual bytes read because archive metadata is untrusted.
 */
internal class ReaderTtsModelArchiveExtractor(
    private val limits: ReaderTtsArchiveLimits = ReaderTtsArchiveLimits()
) {
    fun extractTarBz2(
        archive: File,
        destination: File
    ): ReaderTtsArchiveExtractResult {
        if (!archive.isFile) {
            return ReaderTtsArchiveExtractResult.Rejected(
                ReaderTtsArchiveExtractResult.Reason.SOURCE_MISSING
            )
        }
        if (destination.exists() && destination.listFiles()?.isNotEmpty() == true) {
            return ReaderTtsArchiveExtractResult.Rejected(
                ReaderTtsArchiveExtractResult.Reason.DESTINATION_NOT_EMPTY
            )
        }

        return try {
            if (!destination.exists() && !destination.mkdirs()) {
                return ReaderTtsArchiveExtractResult.Rejected(
                    ReaderTtsArchiveExtractResult.Reason.IO_ERROR
                )
            }

            val root = destination.canonicalFile
            var entries = 0
            var files = 0
            var directories = 0
            var expandedBytes = 0L

            BufferedInputStream(archive.inputStream()).use { raw ->
                BZip2CompressorInputStream(raw, true).use { bzip ->
                    TarArchiveInputStream(bzip).use { tar ->
                        while (true) {
                            val entry = tar.nextEntry as? TarArchiveEntry ?: break
                            entries += 1
                            if (entries > limits.maxEntries) {
                                return rejectAndClean(
                                    destination,
                                    ReaderTtsArchiveExtractResult.Reason.TOO_MANY_ENTRIES
                                )
                            }

                            val target = safeTarget(root, entry.name)
                                ?: return rejectAndClean(
                                    destination,
                                    ReaderTtsArchiveExtractResult.Reason.UNSAFE_PATH
                                )

                            if (entry.isSymbolicLink || entry.isLink) {
                                return rejectAndClean(
                                    destination,
                                    ReaderTtsArchiveExtractResult.Reason.LINK_NOT_ALLOWED
                                )
                            }

                            when {
                                entry.isDirectory -> {
                                    if (!target.exists() && !target.mkdirs()) {
                                        return rejectAndClean(
                                            destination,
                                            ReaderTtsArchiveExtractResult.Reason.IO_ERROR
                                        )
                                    }
                                    directories += 1
                                }

                                entry.isFile -> {
                                    val declared = entry.size
                                    if (declared < 0L || declared > limits.maxSingleFileBytes) {
                                        return rejectAndClean(
                                            destination,
                                            ReaderTtsArchiveExtractResult.Reason.SINGLE_FILE_SIZE_LIMIT
                                        )
                                    }
                                    if (
                                        declared > limits.maxExpandedBytes - expandedBytes
                                    ) {
                                        return rejectAndClean(
                                            destination,
                                            ReaderTtsArchiveExtractResult.Reason.EXPANDED_SIZE_LIMIT
                                        )
                                    }

                                    val parent = target.parentFile
                                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                                        return rejectAndClean(
                                            destination,
                                            ReaderTtsArchiveExtractResult.Reason.IO_ERROR
                                        )
                                    }
                                    if (!isInside(root, target)) {
                                        return rejectAndClean(
                                            destination,
                                            ReaderTtsArchiveExtractResult.Reason.UNSAFE_PATH
                                        )
                                    }

                                    var fileBytes = 0L
                                    FileOutputStream(target).use { output ->
                                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                        while (true) {
                                            val read = tar.read(buffer)
                                            if (read < 0) break
                                            if (read == 0) continue

                                            fileBytes += read.toLong()
                                            expandedBytes += read.toLong()
                                            if (fileBytes > limits.maxSingleFileBytes) {
                                                return rejectAndClean(
                                                    destination,
                                                    ReaderTtsArchiveExtractResult.Reason.SINGLE_FILE_SIZE_LIMIT
                                                )
                                            }
                                            if (expandedBytes > limits.maxExpandedBytes) {
                                                return rejectAndClean(
                                                    destination,
                                                    ReaderTtsArchiveExtractResult.Reason.EXPANDED_SIZE_LIMIT
                                                )
                                            }
                                            output.write(buffer, 0, read)
                                        }
                                        output.flush()
                                        output.fd.sync()
                                    }
                                    if (declared != fileBytes) {
                                        return rejectAndClean(
                                            destination,
                                            ReaderTtsArchiveExtractResult.Reason.IO_ERROR
                                        )
                                    }
                                    files += 1
                                }

                                else -> {
                                    return rejectAndClean(
                                        destination,
                                        ReaderTtsArchiveExtractResult.Reason.UNSUPPORTED_ENTRY
                                    )
                                }
                            }
                        }
                    }
                }
            }

            ReaderTtsArchiveExtractResult.Success(
                ReaderTtsArchiveExtraction(
                    files = files,
                    directories = directories,
                    expandedBytes = expandedBytes
                )
            )
        } catch (_: Exception) {
            destination.deleteRecursively()
            ReaderTtsArchiveExtractResult.Rejected(
                ReaderTtsArchiveExtractResult.Reason.IO_ERROR
            )
        }
    }

    private fun safeTarget(root: File, rawName: String): File? {
        val name = rawName.trim()
        if (name.isEmpty()) return null
        if (
            name.startsWith("/") ||
            name.startsWith("\\") ||
            name.contains('\\') ||
            WINDOWS_DRIVE_PREFIX.matches(name)
        ) {
            return null
        }

        val segments = name.split('/')
        if (
            segments.any { segment ->
                segment.isEmpty() ||
                    segment == "." ||
                    segment == ".." ||
                    segment.indexOf('\u0000') >= 0
            }
        ) {
            return null
        }

        val target = runCatching { File(root, name).canonicalFile }.getOrNull() ?: return null
        return target.takeIf { isInside(root, it) }
    }

    private fun isInside(root: File, target: File): Boolean {
        val rootPath = root.path
        val targetPath = target.path
        return targetPath == rootPath ||
            targetPath.startsWith(rootPath + File.separator)
    }

    private fun rejectAndClean(
        destination: File,
        reason: ReaderTtsArchiveExtractResult.Reason
    ): ReaderTtsArchiveExtractResult.Rejected {
        destination.deleteRecursively()
        return ReaderTtsArchiveExtractResult.Rejected(reason)
    }

    private companion object {
        val WINDOWS_DRIVE_PREFIX = Regex("^[A-Za-z]:.*")
    }
}
