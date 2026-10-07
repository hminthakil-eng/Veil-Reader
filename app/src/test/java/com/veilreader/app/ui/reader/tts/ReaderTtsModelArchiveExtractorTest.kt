package com.veilreader.app.ui.reader.tts

import java.io.File
import java.io.FileOutputStream
import kotlin.io.path.createTempDirectory
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarConstants
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderTtsModelArchiveExtractorTest {
    @Test
    fun validTarBz2ExtractsRegularFilesWithinBudget() {
        val workspace = createTempDirectory("veil-tts-archive").toFile()
        try {
            val archive = File(workspace, "model.tar.bz2")
            writeTarBz2(
                archive,
                listOf(
                    Entry.File("model/model.onnx", ByteArray(1_024) { 7 }),
                    Entry.File("model/tokens.txt", "a\nb\nc\n".toByteArray()),
                    Entry.Directory("model/espeak-ng-data/")
                )
            )
            val destination = File(workspace, "out")
            val result = ReaderTtsModelArchiveExtractor(
                ReaderTtsArchiveLimits(
                    maxEntries = 16,
                    maxExpandedBytes = 4_096,
                    maxSingleFileBytes = 2_048
                )
            ).extractTarBz2(archive, destination)

            assertTrue(result is ReaderTtsArchiveExtractResult.Success)
            val success = (result as ReaderTtsArchiveExtractResult.Success).extraction
            assertEquals(2, success.files)
            assertEquals(1, success.directories)
            assertEquals(1_030L, success.expandedBytes)
            assertEquals(1_024L, File(destination, "model/model.onnx").length())
            assertEquals("a\nb\nc\n", File(destination, "model/tokens.txt").readText())
        } finally {
            workspace.deleteRecursively()
        }
    }

    @Test
    fun parentTraversalIsRejectedAndStagingIsRemoved() {
        val workspace = createTempDirectory("veil-tts-traversal").toFile()
        try {
            val archive = File(workspace, "model.tar.bz2")
            writeTarBz2(
                archive,
                listOf(Entry.File("../escape.onnx", ByteArray(32) { 1 }))
            )
            val destination = File(workspace, "out")
            val result = ReaderTtsModelArchiveExtractor().extractTarBz2(
                archive,
                destination
            )

            assertEquals(
                ReaderTtsArchiveExtractResult.Rejected(
                    ReaderTtsArchiveExtractResult.Reason.UNSAFE_PATH
                ),
                result
            )
            assertFalse(destination.exists())
            assertFalse(File(workspace.parentFile, "escape.onnx").exists())
        } finally {
            workspace.deleteRecursively()
        }
    }

    @Test
    fun symlinkAndHardlinkEntriesAreNeverMaterialized() {
        listOf(false, true).forEach { hardLink ->
            val workspace = createTempDirectory("veil-tts-link").toFile()
            try {
                val archive = File(workspace, "model.tar.bz2")
                writeTarBz2(
                    archive,
                    listOf(
                        Entry.Link(
                            name = if (hardLink) "hard" else "sym",
                            target = "model.onnx",
                            hard = hardLink
                        )
                    )
                )
                val destination = File(workspace, "out")
                val result = ReaderTtsModelArchiveExtractor().extractTarBz2(
                    archive,
                    destination
                )

                assertEquals(
                    ReaderTtsArchiveExtractResult.Rejected(
                        ReaderTtsArchiveExtractResult.Reason.LINK_NOT_ALLOWED
                    ),
                    result
                )
                assertFalse(destination.exists())
            } finally {
                workspace.deleteRecursively()
            }
        }
    }

    @Test
    fun declaredAndActualExpandedSizeAreBounded() {
        val workspace = createTempDirectory("veil-tts-bomb").toFile()
        try {
            val archive = File(workspace, "model.tar.bz2")
            writeTarBz2(
                archive,
                listOf(Entry.File("large.onnx", ByteArray(2_048) { 3 }))
            )
            val destination = File(workspace, "out")
            val result = ReaderTtsModelArchiveExtractor(
                ReaderTtsArchiveLimits(
                    maxEntries = 8,
                    maxExpandedBytes = 1_024,
                    maxSingleFileBytes = 1_024
                )
            ).extractTarBz2(archive, destination)

            assertTrue(result is ReaderTtsArchiveExtractResult.Rejected)
            val reason = (result as ReaderTtsArchiveExtractResult.Rejected).reason
            assertTrue(
                reason == ReaderTtsArchiveExtractResult.Reason.SINGLE_FILE_SIZE_LIMIT ||
                    reason == ReaderTtsArchiveExtractResult.Reason.EXPANDED_SIZE_LIMIT
            )
            assertFalse(destination.exists())
        } finally {
            workspace.deleteRecursively()
        }
    }

    @Test
    fun destinationMustBeDedicatedEmptyStagingDirectory() {
        val workspace = createTempDirectory("veil-tts-destination").toFile()
        try {
            val archive = File(workspace, "model.tar.bz2")
            writeTarBz2(
                archive,
                listOf(Entry.File("model.onnx", ByteArray(64) { 5 }))
            )
            val destination = File(workspace, "out").apply {
                mkdirs()
                resolve("foreign.txt").writeText("keep")
            }

            val result = ReaderTtsModelArchiveExtractor().extractTarBz2(
                archive,
                destination
            )

            assertEquals(
                ReaderTtsArchiveExtractResult.Rejected(
                    ReaderTtsArchiveExtractResult.Reason.DESTINATION_NOT_EMPTY
                ),
                result
            )
            assertEquals("keep", File(destination, "foreign.txt").readText())
        } finally {
            workspace.deleteRecursively()
        }
    }

    private fun writeTarBz2(target: File, entries: List<Entry>) {
        FileOutputStream(target).use { file ->
            BZip2CompressorOutputStream(file).use { bzip ->
                TarArchiveOutputStream(bzip).use { tar ->
                    tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
                    entries.forEach { entry ->
                        when (entry) {
                            is Entry.File -> {
                                val archiveEntry = TarArchiveEntry(entry.name).apply {
                                    size = entry.bytes.size.toLong()
                                }
                                tar.putArchiveEntry(archiveEntry)
                                tar.write(entry.bytes)
                                tar.closeArchiveEntry()
                            }

                            is Entry.Directory -> {
                                val archiveEntry = TarArchiveEntry(entry.name).apply {
                                    size = 0L
                                }
                                tar.putArchiveEntry(archiveEntry)
                                tar.closeArchiveEntry()
                            }

                            is Entry.Link -> {
                                val flag = if (entry.hard) {
                                    TarConstants.LF_LINK
                                } else {
                                    TarConstants.LF_SYMLINK
                                }
                                val archiveEntry = TarArchiveEntry(entry.name, flag).apply {
                                    linkName = entry.target
                                    size = 0L
                                }
                                tar.putArchiveEntry(archiveEntry)
                                tar.closeArchiveEntry()
                            }
                        }
                    }
                    tar.finish()
                }
            }
        }
    }

    private sealed interface Entry {
        data class File(val name: String, val bytes: ByteArray) : Entry
        data class Directory(val name: String) : Entry
        data class Link(
            val name: String,
            val target: String,
            val hard: Boolean
        ) : Entry
    }
}
