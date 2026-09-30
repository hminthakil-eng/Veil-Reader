package com.veilreader.app.manga.importing

import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.MangaChapterAnchor
import com.veilreader.app.manga.library.OfflineChapterId
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MangaCbzIngestorTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun naturalPageOrderIsStableAndManifestPathsAreVeilOwned() {
        val archive = temp.newFile("chapter.cbz")
        writeZip(
            archive,
            linkedMapOf(
                "pages/10.jpg" to byteArrayOf(10),
                "ComicInfo.xml" to "<xml/>".toByteArray(),
                "pages/2.jpg" to byteArrayOf(2),
                "pages/001.jpg" to byteArrayOf(1)
            )
        )
        val cache = temp.newFolder("cache")
        val result = MangaCbzIngestor(clock = { 55L }).ingest(
            archiveFile = archive,
            cacheRoot = cache,
            chapterId = chapterId(),
            anchor = anchor(),
            originChapterKey = "local-chapter"
        )

        assertTrue(result is MangaCbzImportResult.Success)
        val manifest = (result as MangaCbzImportResult.Success).manifest
        assertEquals(listOf(1, 2, 10), manifest.pages.map { page ->
            File(cache, page.relativePath).readBytes().single().toInt()
        })
        assertTrue(manifest.pages.all { it.relativePath.contains("page-") })
        assertTrue(manifest.pages.all { it.contentSha256?.length == 64 })
        assertEquals(55L, manifest.updatedAtEpochMs)
    }

    @Test
    fun unsafeArchiveEntryIsRejectedWithoutReplacingExistingChapter() {
        val cache = temp.newFolder("cache")
        val finalDir = File(cache, "manga/work/en/vna/c1_0-main").apply { mkdirs() }
        val sentinel = File(finalDir, "sentinel.txt").apply { writeText("keep") }

        val archive = temp.newFile("unsafe.cbz")
        writeZip(
            archive,
            linkedMapOf("../escape.jpg" to byteArrayOf(1, 2, 3))
        )

        val result = MangaCbzIngestor().ingest(
            archiveFile = archive,
            cacheRoot = cache,
            chapterId = chapterId(),
            anchor = anchor(),
            originChapterKey = "local-chapter"
        )

        assertEquals(
            MangaCbzImportFailureReason.UNSAFE_ENTRY,
            (result as MangaCbzImportResult.Failure).reason
        )
        assertEquals("keep", sentinel.readText())
        assertFalse(File(cache.parentFile, "escape.jpg").exists())
    }

    @Test
    fun oversizedExpandedPageFailsAndLeavesNoCommittedChapter() {
        val archive = temp.newFile("large.cbz")
        writeZip(
            archive,
            linkedMapOf("001.jpg" to ByteArray(32) { 7 })
        )
        val cache = temp.newFolder("cache")
        val result = MangaCbzIngestor(
            limits = MangaCbzImportLimits(
                maxArchiveBytes = 1024,
                maxEntries = 10,
                maxPageBytes = 8,
                maxExpandedBytes = 64
            )
        ).ingest(
            archiveFile = archive,
            cacheRoot = cache,
            chapterId = chapterId(),
            anchor = anchor(),
            originChapterKey = "local-chapter"
        )

        assertEquals(
            MangaCbzImportFailureReason.PAGE_TOO_LARGE,
            (result as MangaCbzImportResult.Failure).reason
        )
        assertFalse(File(cache, "manga/work/en/vna/c1_0-main").exists())
    }

    @Test
    fun naturalComparatorOrdersNumericRunsByValue() {
        val names = listOf("p10.jpg", "p2.jpg", "p001.jpg", "p20.jpg")
            .sortedWith(Comparator { left, right -> compareNaturalArchiveNames(left, right) })

        assertEquals(listOf("p001.jpg", "p2.jpg", "p10.jpg", "p20.jpg"), names)
    }

    private fun chapterId() = OfflineChapterId(
        mangaId = CanonicalMangaId("work"),
        languageTag = "en",
        volume = null,
        number = 1.0
    )

    private fun anchor() = MangaChapterAnchor(
        number = 1.0,
        languageTag = "en",
        providerChapterKeyHint = "local-chapter"
    )

    private fun writeZip(file: File, entries: LinkedHashMap<String, ByteArray>) {
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
    }
}
