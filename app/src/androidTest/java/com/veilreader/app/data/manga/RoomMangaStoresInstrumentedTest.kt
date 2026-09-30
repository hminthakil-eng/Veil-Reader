package com.veilreader.app.data.manga

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veilreader.app.data.db.BookEntity
import com.veilreader.app.data.db.MangaChapterEntity
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.manga.library.CanonicalMangaId
import com.veilreader.app.manga.library.MangaCacheLayout
import com.veilreader.app.manga.library.MangaChapterAnchor
import com.veilreader.app.manga.library.MangaOfflineChapterLocator
import com.veilreader.app.manga.library.MangaReadingProgress
import com.veilreader.app.manga.library.OfflineChapterManifest
import com.veilreader.app.manga.library.OfflinePageEntry
import com.veilreader.app.manga.source.SourceId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomMangaStoresInstrumentedTest {

    private lateinit var db: VeilDatabase

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, VeilDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        db.books().upsert(
            BookEntity(
                id = BOOK_ID,
                title = "Local Manga",
                author = "Veil",
                progress = 0f,
                currentChapter = "",
                totalPages = 0,
                pagesRead = 0,
                format = BookFormat.COMIC.name,
                sourceUri = "file:///local.cbz",
                mediaType = "application/vnd.comicbook+zip",
                locatorJson = null,
                addedAtEpochMs = 1L,
                lastOpenedAtEpochMs = 0L,
                finished = false,
                favorite = false
            )
        )

        val offlineId = requireNotNull(
            MangaOfflineChapterLocator.idFor(MANGA_ID, ANCHOR)
        )
        db.mangaCatalog().upsertChapter(
            MangaChapterEntity(
                id = CHAPTER_ID,
                bookId = BOOK_ID,
                readingOrder = 0,
                cacheKey = MangaCacheLayout.chapterDirectory(offlineId),
                title = "Chapter 1",
                normalizedTitle = "Chapter 1",
                number = 1.0,
                languageTag = "en"
            )
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun progressRoundTripsThroughCanonicalChapterIdentity() = runBlocking {
        val store = RoomMangaProgressStore(db)
        val expected = MangaReadingProgress(
            mangaId = MANGA_ID,
            chapter = ANCHOR,
            pageIndex = 4,
            pageCount = 20,
            chapterProgression = 4.0 / 19.0,
            updatedAtEpochMs = 900L
        )

        store.save(expected)
        val restored = requireNotNull(store.load(MANGA_ID))

        assertEquals(expected.mangaId, restored.mangaId)
        assertEquals(1.0, restored.chapter.number ?: error("missing number"), 0.0001)
        assertEquals(4, restored.pageIndex)
        assertEquals(20, restored.pageCount)
        assertEquals(expected.chapterProgression, restored.chapterProgression, 0.000001)
        assertEquals(900L, restored.updatedAtEpochMs)

        val bookSummary = requireNotNull(db.books().findEntity(BOOK_ID))
        assertEquals(expected.chapterProgression.toFloat(), bookSummary.progress, 0.000001f)
        assertEquals(900L, bookSummary.lastOpenedAtEpochMs)
        assertEquals(false, bookSummary.finished)

        store.save(
            expected.copy(
                pageIndex = 19,
                chapterProgression = 1.0,
                updatedAtEpochMs = 901L
            )
        )
        val finishedSummary = requireNotNull(db.books().findEntity(BOOK_ID))
        assertEquals(1f, finishedSummary.progress, 0.000001f)
        assertEquals(901L, finishedSummary.lastOpenedAtEpochMs)
        assertEquals(true, finishedSummary.finished)

        store.delete(MANGA_ID)
        assertNull(store.load(MANGA_ID))
    }

    @Test
    fun offlineManifestRoundTripsInPageOrderAndCascadeDeletesWithBook() = runBlocking {
        val store = RoomMangaOfflineCacheIndex(db)
        val offlineId = requireNotNull(
            MangaOfflineChapterLocator.idFor(MANGA_ID, ANCHOR)
        )
        val manifest = OfflineChapterManifest(
            chapterId = offlineId,
            anchor = ANCHOR,
            pages = listOf(
                OfflinePageEntry(
                    index = 1,
                    relativePath = MangaCacheLayout.pagePath(offlineId, 1, "jpg"),
                    byteSize = 22L,
                    contentSha256 = "bb"
                ),
                OfflinePageEntry(
                    index = 0,
                    relativePath = MangaCacheLayout.pagePath(offlineId, 0, "jpg"),
                    byteSize = 11L,
                    contentSha256 = "aa"
                )
            ),
            originSourceId = SourceId("local.cbz"),
            originChapterKey = "chapter-1",
            completed = true,
            updatedAtEpochMs = 901L
        )

        store.put(manifest)
        val restored = requireNotNull(store.load(offlineId))

        assertEquals(listOf(0, 1), restored.pages.map { it.index })
        assertEquals(listOf(11L, 22L), restored.pages.map { it.byteSize })
        assertEquals("chapter-1", restored.originChapterKey)
        assertTrue(restored.completed)
        assertEquals(1, store.listForManga(MANGA_ID).size)

        db.books().deleteById(BOOK_ID)

        assertNull(store.load(offlineId))
        assertTrue(store.listForManga(MANGA_ID).isEmpty())
    }

    private companion object {
        const val BOOK_ID = "manga-book"
        const val CHAPTER_ID = "chapter-1"
        val MANGA_ID = CanonicalMangaId(BOOK_ID)
        val ANCHOR = MangaChapterAnchor(
            number = 1.0,
            languageTag = "en",
            normalizedTitle = "Chapter 1",
            providerChapterKeyHint = "chapter-1"
        )
    }
}
