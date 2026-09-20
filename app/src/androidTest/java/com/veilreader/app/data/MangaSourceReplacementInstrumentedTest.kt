package com.veilreader.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.data.settings.SettingsStore
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.manga.core.MangaChapter
import com.veilreader.app.manga.core.MangaChapterRef
import com.veilreader.app.manga.core.MangaProviderId
import com.veilreader.app.manga.core.MangaRef
import com.veilreader.app.manga.core.MangaSourceDescriptor
import com.veilreader.app.manga.core.MangaSourceId
import com.veilreader.app.manga.core.MangaUpdate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MangaSourceReplacementInstrumentedTest {
    private lateinit var context: Context
    private lateinit var database: VeilDatabase
    private lateinit var library: LocalLibraryRepository
    private lateinit var mangaLibrary: MangaLibraryRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, VeilDatabase::class.java)
            .build()
        library = LocalLibraryRepository(
            appContext = context,
            database = database,
            settings = SettingsStore(context),
            runLegacyMigration = false
        )
        mangaLibrary = MangaLibraryRepository(database, library)
    }

    @After
    fun tearDown() = runBlocking {
        library.flushWrites()
        library.closeForTest()
        database.close()
    }

    @Test
    fun replacement_reusesStableChapterIdsAndKeepsProgress() = runBlocking {
        addComic("book-1", "Veil Knight")

        val sourceA = descriptor("source-a.en", "source-a")
        val refA = MangaRef(sourceA.id, "series-a")
        val firstSync = mangaLibrary.syncSource(
            bookId = "book-1",
            descriptor = sourceA,
            sourceRef = refA,
            update = update(
                refA,
                chapter("a-1", refA, "Chapter 1", 1.0),
                chapter("a-2", refA, "Chapter 2", 2.0)
            )
        )
        val stableChapterTwo = firstSync.createdChapterIds[1]

        mangaLibrary.saveReadingProgress(
            bookId = "book-1",
            chapterId = stableChapterTwo,
            pageIndex = 4,
            pageCount = 10,
            readAtEpochMs = 1_000L
        )

        val sourceB = descriptor("source-b.en", "source-b")
        val refB = MangaRef(sourceB.id, "series-b")
        val updateB = update(
            refB,
            chapter("b-one", refB, "Ch. One", 1.0),
            chapter("b-two", refB, "Ch. Two", 2.0)
        )

        val plan = mangaLibrary.planAlternativeSource(
            bookId = "book-1",
            descriptor = sourceB,
            sourceRef = refB,
            update = updateB
        )

        assertEquals(2, plan.matchedExistingCount)
        assertEquals(0, plan.newIncomingCount)
        assertEquals(0, plan.conflictingIncomingCount)
        assertEquals(1.0, plan.existingCoverageRatio, 0.0)
        assertTrue(plan.resumeChapterCovered)

        val replacement = mangaLibrary.replacePreferredSource(
            bookId = "book-1",
            descriptor = sourceB,
            sourceRef = refB,
            update = updateB
        )

        assertTrue(stableChapterTwo in replacement.matchedAlternativeChapterIds)

        val persisted = database.mangaChapters().findById(stableChapterTwo)
        assertNotNull(persisted)
        assertEquals(4, persisted?.lastPageIndex)
        assertEquals(10, persisted?.pageCount)
        assertEquals(1_000L, persisted?.lastReadAtEpochMs)

        val bindings = database.mangaChapterBindings().listForChapter(stableChapterTwo)
        assertEquals(2, bindings.size)
        assertEquals(setOf("series-a", "series-b"), bindings.map { it.mangaSourceKey }.toSet())

        assertEquals(sourceB.id, mangaLibrary.preferredSourceRef("book-1")?.sourceId)
        assertEquals("b-two", mangaLibrary.chapterRefForReading(stableChapterTwo)?.key)
    }

    @Test
    fun sameChapterKey_canExistInTwoMangaFromTheSameSource() = runBlocking {
        addComic("book-a", "First")
        addComic("book-b", "Second")

        val source = descriptor("shared.en", "shared")
        val firstManga = MangaRef(source.id, "first-series")
        val secondManga = MangaRef(source.id, "second-series")

        mangaLibrary.syncSource(
            "book-a",
            source,
            firstManga,
            update(
                firstManga,
                chapter("chapter-1", firstManga, "Chapter 1", 1.0)
            )
        )
        mangaLibrary.syncSource(
            "book-b",
            source,
            secondManga,
            update(
                secondManga,
                chapter("chapter-1", secondManga, "Chapter 1", 1.0)
            )
        )

        val firstBinding = database.mangaChapterBindings().findByExternalRef(
            sourceId = source.id.value,
            mangaSourceKey = firstManga.key,
            sourceChapterKey = "chapter-1"
        )
        val secondBinding = database.mangaChapterBindings().findByExternalRef(
            sourceId = source.id.value,
            mangaSourceKey = secondManga.key,
            sourceChapterKey = "chapter-1"
        )

        assertNotNull(firstBinding)
        assertNotNull(secondBinding)
        assertNotEquals(firstBinding?.chapterId, secondBinding?.chapterId)
    }

    private suspend fun addComic(id: String, title: String) {
        library.addImportedBook(
            Book(
                id = id,
                title = title,
                author = "Unknown",
                format = BookFormat.COMIC
            )
        )
    }

    private fun descriptor(
        sourceId: String,
        providerId: String
    ) = MangaSourceDescriptor(
        id = MangaSourceId(sourceId),
        providerId = MangaProviderId(providerId),
        version = 1,
        name = providerId,
        language = "en"
    )

    private fun update(
        ref: MangaRef,
        vararg chapters: MangaChapter
    ) = MangaUpdate(
        ref = ref,
        chapters = chapters.toList()
    )

    private fun chapter(
        key: String,
        manga: MangaRef,
        title: String,
        number: Double
    ) = MangaChapter(
        ref = MangaChapterRef(manga, key),
        title = title,
        chapterNumber = number
    )
}
