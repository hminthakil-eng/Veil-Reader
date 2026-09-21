package com.veilreader.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.data.settings.SettingsStore
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.manga.core.MangaChapter
import com.veilreader.app.manga.core.MangaChapterRef
import com.veilreader.app.manga.core.MangaDetails
import com.veilreader.app.manga.core.MangaProviderId
import com.veilreader.app.manga.core.MangaRef
import com.veilreader.app.manga.core.MangaSourceDescriptor
import com.veilreader.app.manga.core.MangaSourceId
import com.veilreader.app.manga.core.MangaUpdate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MangaRuntimeRepositoryInstrumentedTest {
    private lateinit var context: Context
    private lateinit var db: VeilDatabase
    private lateinit var library: LocalLibraryRepository
    private lateinit var manga: MangaLibraryRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, VeilDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        library = LocalLibraryRepository(
            appContext = context,
            database = db,
            settings = SettingsStore(context),
            runLegacyMigration = false
        )
        manga = MangaLibraryRepository(
            database = db,
            library = library
        )
    }

    @After
    fun tearDown() {
        library.closeForTest()
        db.close()
    }

    @Test
    fun sourceBackedManga_syncProgressRefreshAndResume_preserveDurableState() = runBlocking<Unit> {
        val descriptor = MangaSourceDescriptor(
            id = MangaSourceId("mangadex.en"),
            name = "MangaDex",
            language = "en",
            providerId = MangaProviderId("mangadex")
        )
        val ref = MangaRef(descriptor.id, "series-123")
        val firstRef = MangaChapterRef(ref, "chapter-1")
        val secondRef = MangaChapterRef(ref, "chapter-2")
        val chapters = listOf(
            MangaChapter(
                ref = firstRef,
                title = "Chapter One",
                chapterNumber = 1.0
            ),
            MangaChapter(
                ref = secondRef,
                title = "Chapter Two",
                chapterNumber = 2.0
            )
        )

        val firstSync = manga.importOrSyncSource(
            descriptor = descriptor,
            update = MangaUpdate(
                ref = ref,
                details = MangaDetails(
                    ref = ref,
                    title = "Original Title",
                    authors = listOf("Author")
                ),
                chapters = chapters
            )
        )
        library.flushWrites()

        val bookId = firstSync.bookId
        val firstChapterId = requireNotNull(manga.chapterIdForSourceRef(firstRef))
        val secondChapterId = requireNotNull(manga.chapterIdForSourceRef(secondRef))

        val stored = db.books().findEntity(bookId)
        assertNotNull(stored)
        assertEquals(BookFormat.COMIC.name, stored?.format)
        assertNull(stored?.sourceUri)
        assertEquals("Original Title", stored?.title)
        assertEquals(2, db.mangaChapters().listForBook(bookId).size)
        assertEquals(2, db.mangaChapterBindings().listAll().size)
        assertEquals(firstChapterId, manga.resumeReadableChapterId(bookId))

        library.toggleFavorite(bookId)
        manga.saveReadingProgress(
            bookId = bookId,
            chapterId = secondChapterId,
            pageIndex = 2,
            pageCount = 5,
            readAtEpochMs = 1234L
        )
        library.flushWrites()

        assertEquals(secondChapterId, manga.resumeReadableChapterId(bookId))
        assertEquals(2, manga.chapterLastPageIndex(secondChapterId))

        val refresh = manga.importOrSyncSource(
            descriptor = descriptor,
            update = MangaUpdate(
                ref = ref,
                details = MangaDetails(
                    ref = ref,
                    title = "Refreshed Title",
                    authors = listOf("Author", "Coauthor")
                ),
                chapters = chapters.reversed()
            )
        )
        library.flushWrites()

        assertEquals(bookId, refresh.bookId)
        assertTrue(refresh.reusedChapterIds.containsAll(listOf(firstChapterId, secondChapterId)))

        val refreshed = db.books().findEntity(bookId) ?: error("manga book missing")
        assertEquals("Refreshed Title", refreshed.title)
        assertTrue(refreshed.favorite)
        assertTrue(refreshed.progress > 0f)
        assertEquals(secondChapterId, manga.resumeReadableChapterId(bookId))

        val secondWindow = manga.chapterWindow(bookId, secondChapterId)
        assertNull(secondWindow.previousChapterId)
        assertEquals(firstChapterId, secondWindow.nextChapterId)
    }
}
