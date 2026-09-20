package com.veilreader.app.manga.storage

import com.veilreader.app.manga.core.MangaChapterRef
import com.veilreader.app.manga.core.MangaPage
import com.veilreader.app.manga.core.MangaReadingProgress
import com.veilreader.app.manga.core.MangaRef
import com.veilreader.app.manga.core.MangaResourceRequest
import com.veilreader.app.manga.core.MangaSourceId
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileMangaStoresTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val manga = MangaRef(MangaSourceId("test.en"), "manga-1")
    private val chapter = MangaChapterRef(manga, "chapter-7")

    @Test
    fun offlineChapter_isOnlyAvailableAfterCompleteAtomicSave() = runBlocking {
        val root = temporaryFolder.newFolder("offline")
        val bytesByUrl = mapOf(
            "https://img/0.jpg" to byteArrayOf(1, 2, 3),
            "https://img/1.jpg" to byteArrayOf(4, 5, 6)
        )
        val store = FileMangaOfflineStore(root) { request ->
            bytesByUrl.getValue(request.url)
        }
        val pages = listOf(
            MangaPage(1, MangaResourceRequest("https://img/1.jpg")),
            MangaPage(0, MangaResourceRequest("https://img/0.jpg"))
        )

        assertFalse(store.isChapterAvailable(chapter))
        store.saveChapter(chapter, pages)

        assertTrue(store.isChapterAvailable(chapter))
        val restored = requireNotNull(store.loadChapter(chapter))
        assertEquals(listOf(0, 1), restored.map { it.index })
        assertTrue(restored.all { it.image.url.startsWith("file:") })
        assertEquals(byteArrayOf(1, 2, 3).toList(), File(java.net.URI(restored[0].image.url)).readBytes().toList())

        store.removeChapter(chapter)
        assertFalse(store.isChapterAvailable(chapter))
        assertNull(store.loadChapter(chapter))
    }

    @Test
    fun failedOfflineDownload_doesNotPublishPartialChapter() = runBlocking {
        val root = temporaryFolder.newFolder("partial")
        var calls = 0
        val store = FileMangaOfflineStore(root) {
            calls += 1
            if (calls == 2) error("network failed") else byteArrayOf(1)
        }
        val pages = listOf(
            MangaPage(0, MangaResourceRequest("https://img/0.jpg")),
            MangaPage(1, MangaResourceRequest("https://img/1.jpg"))
        )

        val error = runCatching { store.saveChapter(chapter, pages) }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertFalse(store.isChapterAvailable(chapter))
        assertNull(store.loadChapter(chapter))
    }

    @Test
    fun progress_survivesStoreRecreation() = runBlocking {
        val root = temporaryFolder.newFolder("progress")
        val first = FileMangaProgressStore(root)
        val expected = MangaReadingProgress(
            manga = manga,
            chapter = chapter,
            pageIndex = 12,
            updatedAtEpochMs = 123456789L
        )

        first.save(expected)

        val restored = FileMangaProgressStore(root).load(manga)
        assertEquals(expected, restored)
    }
}