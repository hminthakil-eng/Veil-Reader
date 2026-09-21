package com.veilreader.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veilreader.app.data.migration.LegacyLibraryMigrator
import com.veilreader.app.data.migration.buildLegacyImportPlan
import com.veilreader.app.data.settings.SettingsStore
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.Bookmark
import com.veilreader.app.domain.Highlight
import com.veilreader.app.domain.ReaderTheme
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun actualLegacyMigrator_movesPreferences_marksCompletion_andSkipsSecondRun() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val legacyPrefs = context.getSharedPreferences("veil_library_v1", Context.MODE_PRIVATE)
        legacyPrefs.edit().clear().commit()

        val settings = SettingsStore(context)
        settings.clearAllForTest()

        val books = JSONArray().put(JSONObject().apply {
            put("id", "legacy-book")
            put("title", "Legacy Tome")
            put("author", "Old Reader")
            put("progress", 0.66)
            put("currentChapter", "Old Chapter")
            put("totalPages", 300)
            put("pagesRead", 198)
            put("format", "EPUB")
            put("sourceUri", "file:///legacy.epub")
            put("mediaType", "application/epub+zip")
            put("locatorJson", "{\"href\":\"old.xhtml\"}")
            put("addedAt", 111L)
            put("lastOpenedAt", 222L)
            put("finished", false)
            put("favorite", true)
            put("collection", "Legacy Shelf")
        })
        val highlights = JSONArray().put(JSONObject().apply {
            put("id", "legacy-highlight")
            put("bookId", "legacy-book")
            put("quote", "Remember this")
            put("locatorJson", "{\"href\":\"old.xhtml\"}")
            put("note", "Migrated note")
            put("createdAt", 333L)
        })
        val bookmarks = JSONArray().put(JSONObject().apply {
            put("id", "legacy-bookmark")
            put("bookId", "legacy-book")
            put("label", "Return here")
            put("locatorJson", "{\"href\":\"old.xhtml\"}")
            put("createdAt", 444L)
        })
        val appearance = JSONObject().apply {
            put("theme", "SEPIA")
            put("fontScale", 1.2)
            put("lineHeight", 1.6)
            put("scroll", true)
            put("publisherStyles", false)
        }
        legacyPrefs.edit()
            .putString("books", books.toString())
            .putString("highlights", highlights.toString())
            .putString("bookmarks", bookmarks.toString())
            .putString("appearance", appearance.toString())
            .commit()

        val migrator = LegacyLibraryMigrator(context, db, settings)
        val first = migrator.migrateIfNeeded()

        assertFalse(first.alreadyMigrated)
        assertEquals(1, first.importedBooks)
        assertEquals(1, first.importedHighlights)
        assertEquals(1, first.importedBookmarks)
        assertEquals(1, first.importedCollections)
        assertEquals(0, first.skippedOrphans)
        assertEquals(1, db.books().count())

        val migratedBook = db.books().findWithCollections("legacy-book") ?: error("migrated book missing")
        assertEquals("Legacy Tome", migratedBook.book.title)
        assertEquals(0.66f, migratedBook.book.progress)
        assertEquals(listOf("Legacy Shelf"), migratedBook.collections.map { it.name })
        assertEquals("Migrated note", db.highlights().observeAll().first().single().note)
        assertEquals("Return here", db.bookmarks().observeAll().first().single().label)

        val migratedSettings = settings.settings.first()
        assertTrue(migratedSettings.legacyLibraryImported)
        assertEquals(ReaderTheme.SEPIA, migratedSettings.readerAppearance.theme)
        assertEquals(1.2, migratedSettings.readerAppearance.fontScale, 0.0001)
        assertEquals(1.6, migratedSettings.readerAppearance.lineHeight, 0.0001)
        assertTrue(migratedSettings.readerAppearance.scroll)
        assertFalse(migratedSettings.readerAppearance.publisherStyles)

        val second = migrator.migrateIfNeeded()
        assertTrue(second.alreadyMigrated)
        assertEquals(1, db.books().count())
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
