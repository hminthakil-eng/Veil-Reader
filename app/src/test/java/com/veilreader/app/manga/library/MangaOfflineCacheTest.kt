package com.veilreader.app.manga.library

import com.veilreader.app.manga.source.SourceId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaOfflineCacheTest {

    @Test
    fun cacheOwnershipSurvivesSourceReplacement() = runBlocking {
        val mangaId = CanonicalMangaId("work-cache")
        val chapterId = OfflineChapterId(
            mangaId = mangaId,
            languageTag = "en",
            volume = 1.0,
            number = 7.0
        )
        val manifest = OfflineChapterManifest(
            chapterId = chapterId,
            anchor = MangaChapterAnchor(volume = 1.0, number = 7.0, languageTag = "en"),
            pages = listOf(
                OfflinePageEntry(
                    index = 0,
                    relativePath = MangaCacheLayout.pagePath(chapterId, 0, "jpg"),
                    byteSize = 100
                )
            ),
            originSourceId = SourceId("old.source"),
            originChapterKey = "remote-old-7",
            completed = true,
            updatedAtEpochMs = 100L
        )
        val index = InMemoryMangaOfflineCacheIndex()

        index.put(manifest)

        val loaded = index.load(chapterId)
        assertEquals(mangaId, loaded?.chapterId?.mangaId)
        assertEquals(SourceId("old.source"), loaded?.originSourceId)
        assertEquals(1, index.listForManga(mangaId).size)
    }

    @Test
    fun cacheLayoutIsRelativeStableAndDoesNotContainRemoteDomain() {
        val chapterId = OfflineChapterId(
            mangaId = CanonicalMangaId("work-123"),
            languageTag = "fa-IR",
            volume = null,
            number = 12.5,
            discriminator = "main"
        )

        val path = MangaCacheLayout.pagePath(chapterId, 9, ".webp")

        assertEquals(
            "manga/work-123/fa-ir/vna/c12_5-main/page-00009.webp",
            path
        )
        assertFalse(path.contains("http"))
        assertFalse(path.contains("example.com"))
    }

    @Test
    fun offlinePageRejectsTraversalAndAbsolutePaths() {
        assertThrows(IllegalArgumentException::class.java) {
            OfflinePageEntry(0, "../secret.jpg", 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            OfflinePageEntry(0, "/absolute/page.jpg", 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            OfflinePageEntry(0, "C:\\temp\\page.jpg", 1)
        }

        assertTrue(
            runCatching { OfflinePageEntry(0, "manga/work/chapter/page.jpg", 1) }.isSuccess
        )
    }
}
