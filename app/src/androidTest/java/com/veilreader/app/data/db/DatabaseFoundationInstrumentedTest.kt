package com.veilreader.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veilreader.app.data.migration.buildLegacyImportPlan
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.Bookmark
import com.veilreader.app.domain.Highlight
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseFoundationInstrumentedTest {
    private lateinit var db: VeilDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, VeilDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun legacyPlan_roundTripsIntoRoom_andCanBeAppliedTwice() = runBlocking {
        val locator = "{\"href\":\"chapter7.xhtml\",\"locations\":{\"progression\":0.42}}"
        val books = listOf(
            Book(
                id = "epub-a",
                title = "The Glass Archive",
                author = "A. Reader",
                progress = 0.42f,
                currentChapter = "Chapter 7",
                totalPages = 420,
                pagesRead = 176,
                format = BookFormat.EPUB,
                sourceUri = "file:///data/user/0/com.veilreader.app/files/publications/a.epub",
                mediaType = "application/epub+zip",
                locatorJson = locator,
                addedAtEpochMs = 1000L,
                lastOpenedAtEpochMs = 9000L,
                favorite = true,
                collection = "Mystery"
            ),
            Book(
                id = "pdf-b",
                title = "Field Notes",
                author = "B. Scholar",
                progress = 0.10f,
                format = BookFormat.PDF,
                sourceUri = "file:///data/user/0/com.veilreader.app/files/publications/b.pdf",
                mediaType = "application/pdf",
                addedAtEpochMs = 2000L,
                collection = "mystery"
            )
        )
        val highlights = listOf(
            Highlight(
                id = "highlight-1",
                bookId = "epub-a",
                quote = "A preserved clue",
                locatorJson = "{\"href\":\"chapter7.xhtml\"}",
                note = "Keep this detail.",
                createdAtEpochMs = 3333L
            )
        )
        val bookmarks = listOf(
            Bookmark(
                id = "bookmark-1",
                bookId = "pdf-b",
                label = "Diagram",
                locatorJson = "{\"href\":\"12\"}",
                createdAtEpochMs = 4444L
            )
        )
        val plan = buildLegacyImportPlan(books, highlights, bookmarks)

        suspend fun applyPlan() {
            db.withTransaction {
                db.books().upsertAll(plan.books)
                db.collections().upsertAll(plan.collections)
                db.collections().attachAll(plan.collectionLinks)
                db.highlights().upsertAll(plan.highlights)
                db.bookmarks().upsertAll(plan.bookmarks)
            }
        }

        applyPlan()
        applyPlan()

        assertEquals(2, db.books().count())

        val stored = db.books().findWithCollections("epub-a") ?: error("book not found")
        assertEquals("The Glass Archive", stored.book.title)
        assertEquals(0.42f, stored.book.progress)
        assertEquals(9000L, stored.book.lastOpenedAtEpochMs)
        assertEquals(true, stored.book.favorite)
        assertEquals(locator, stored.book.locatorJson)
        assertEquals(listOf("Mystery"), stored.collections.map { it.name })

        val storedHighlight = db.highlights().observeForBook("epub-a").first().single()
        assertEquals("highlight-1", storedHighlight.id)
        assertEquals("Keep this detail.", storedHighlight.note)
        assertEquals(3333L, storedHighlight.createdAtEpochMs)

        val storedBookmark = db.bookmarks().observeForBook("pdf-b").first().single()
        assertEquals("bookmark-1", storedBookmark.id)
        assertEquals("Diagram", storedBookmark.label)
        assertEquals(4444L, storedBookmark.createdAtEpochMs)
    }

    @Test
    fun deletingBook_cascadesAnnotationsAndMembership_butKeepsHistoricalSession() = runBlocking {
        val book = BookEntity(
            id = "book",
            title = "Book",
            author = "Author",
            progress = 0f,
            currentChapter = "Not started",
            totalPages = 0,
            pagesRead = 0,
            format = BookFormat.EPUB.name,
            sourceUri = "file:///book.epub",
            mediaType = "application/epub+zip",
            locatorJson = null,
            addedAtEpochMs = 1L,
            lastOpenedAtEpochMs = 0L,
            finished = false,
            favorite = false
        )
        val collection = CollectionEntity("collection", "Fantasy", 1L)

        db.withTransaction {
            db.books().upsert(book)
            db.collections().upsert(collection)
            db.collections().attach(BookCollectionCrossRef("book", "collection"))
            db.highlights().upsert(HighlightEntity("h", "book", "Quote", "{}", "", 2L))
            db.bookmarks().upsert(BookmarkEntity("b", "book", "Page", "{}", 3L))
            db.readingSessions().upsert(
                ReadingSessionEntity(
                    id = "session",
                    bookId = "book",
                    startedAtEpochMs = 10L,
                    endedAtEpochMs = 20L,
                    activeMillis = 10L,
                    pacedPageTurns = 1,
                    highlightCount = 1,
                    noteCount = 0
                )
            )
        }

        db.books().deleteById("book")

        assertEquals(emptyList<HighlightEntity>(), db.highlights().observeAll().first())
        assertEquals(emptyList<BookmarkEntity>(), db.bookmarks().observeAll().first())
        assertEquals(1, db.collections().observeAll().first().size)
        val session = db.readingSessions().observeAll().first().single()
        assertEquals("session", session.id)
        assertNull(session.bookId)
    }
}
