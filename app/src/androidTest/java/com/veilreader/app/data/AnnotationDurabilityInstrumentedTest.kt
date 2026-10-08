package com.veilreader.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.data.settings.SettingsStore
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookFormat
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression coverage for the physical-device P0 in #32.
 *
 * Reader UI is allowed to acknowledge an annotation save only after flushWrites() returns. This
 * test proves the highlight and its attached note are in Room at those acknowledgement points.
 */
@RunWith(AndroidJUnit4::class)
class AnnotationDurabilityInstrumentedTest {
    private lateinit var context: Context
    private lateinit var db: VeilDatabase
    private lateinit var repository: LocalLibraryRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, VeilDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LocalLibraryRepository(
            appContext = context,
            database = db,
            settings = SettingsStore(context),
            runLegacyMigration = false
        )
    }

    @After
    fun tearDown() = runBlocking {
        repository.closeForTest()
        db.close()
    }

    @Test
    fun bookmarkAcknowledgement_isAlreadyCommitted_withoutFlushOrObserverWait() = runBlocking {
        importBookmarkBook()
        assertTrue(repository.addBookmark("bookmark-book", "Saved", "{\"href\":\"one.xhtml\"}"))

        // Query Room directly at the acknowledgement boundary, without flushing the write queue.
        val stored = db.bookmarks().listAll().single()
        assertEquals("Saved", stored.label)
        assertFalse(repository.addBookmark("bookmark-book", "Duplicate", stored.locatorJson))
        assertEquals(1, db.bookmarks().listAll().size)
    }

    @Test
    fun concurrentBookmarkRequests_acknowledgeExactlyOneDurableRecord() = runBlocking {
        importBookmarkBook()
        val results = List(20) {
            async {
                repository.addBookmark("bookmark-book", "Rapid tap", "{\"href\":\"same.xhtml\"}")
            }
        }.awaitAll()
        assertEquals(1, results.count { it })
        assertEquals(1, db.bookmarks().listAll().size)
    }

    @Test
    fun rejectedBookmarkWrite_doesNotAcknowledgeOrPublishPhantomRecord() = runBlocking {
        importBookmarkBook()
        repository.bookmarks.first { it.isEmpty() }
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_bookmark BEFORE INSERT ON bookmarks " +
                "BEGIN SELECT RAISE(ABORT, 'injected bookmark storage failure'); END"
        )

        var failure: Exception? = null
        try {
            repository.addBookmark("bookmark-book", "Must fail", "{\"href\":\"failure.xhtml\"}")
        } catch (error: Exception) {
            failure = error
        }
        assertTrue("Storage failure must reach the caller", failure != null)
        assertTrue(db.bookmarks().listAll().isEmpty())
        assertTrue(repository.bookmarks.value.isEmpty())
    }

    @Test
    fun bookmarkDeletion_isCommittedBeforeReturn_andIsIdempotent() = runBlocking {
        importBookmarkBook()
        repository.addBookmark("bookmark-book", "Saved", "{\"href\":\"delete.xhtml\"}")
        val id = db.bookmarks().listAll().single().id
        repository.deleteBookmark(id)
        assertTrue(db.bookmarks().listAll().isEmpty())
        repository.deleteBookmark(id)
        assertTrue(db.bookmarks().listAll().isEmpty())
    }

    @Test
    fun rejectedBookmarkDeletion_preservesDurableAndVisibleRecord() = runBlocking {
        importBookmarkBook()
        repository.addBookmark("bookmark-book", "Keep", "{\"href\":\"keep.xhtml\"}")
        val saved = db.bookmarks().listAll().single()
        repository.bookmarks.first { bookmarks -> bookmarks.any { it.id == saved.id } }
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_bookmark_delete BEFORE DELETE ON bookmarks " +
                "BEGIN SELECT RAISE(ABORT, 'injected bookmark deletion failure'); END"
        )

        var failure: Exception? = null
        try {
            repository.deleteBookmark(saved.id)
        } catch (error: Exception) {
            failure = error
        }
        assertTrue("Deletion failure must reach the caller", failure != null)
        assertEquals(saved, db.bookmarks().listAll().single())
        assertEquals(saved.id, repository.bookmarks.value.single().id)
    }

    private suspend fun importBookmarkBook() {
        repository.addImportedBook(
            Book(
                id = "bookmark-book",
                title = "Bookmark durability",
                author = "QA",
                format = BookFormat.EPUB,
                sourceUri = "file:///bookmark.epub",
                mediaType = "application/epub+zip",
                addedAtEpochMs = 1L
            )
        )
        repository.flushWrites()
    }

    @Test
    fun selectionNote_commitsOneFinalRecord_editsInPlace_andNeverRecreatesMissingTarget() =
        runBlocking {
            repository.addImportedBook(
                Book(
                    id = "selection-note-book",
                    title = "Atomic Marginalia",
                    author = "QA",
                    format = BookFormat.EPUB,
                    sourceUri = "file:///selection-note.epub",
                    mediaType = "application/epub+zip",
                    addedAtEpochMs = 1L
                )
            )

            val locator = "{\"href\":\"chapter.xhtml\"}"
            assertTrue(db.highlights().listAll().isEmpty())

            val fresh = requireNotNull(
                repository.commitSelectionNote(
                    bookId = "selection-note-book",
                    quote = "A passage becomes durable only on Save.",
                    locatorJson = locator,
                    existingHighlightId = null,
                    note = "First margin note"
                )
            )
            assertTrue(fresh.created)
            assertEquals("First margin note", fresh.highlight.note)

            repository.flushWrites()

            var stored = db.highlights().listAll().single()
            assertEquals(fresh.highlight.id, stored.id)
            assertEquals("A passage becomes durable only on Save.", stored.quote)
            assertEquals("First margin note", stored.note)

            val edited = requireNotNull(
                repository.commitSelectionNote(
                    bookId = "selection-note-book",
                    quote = stored.quote,
                    locatorJson = stored.locatorJson,
                    existingHighlightId = stored.id,
                    note = "Edited margin note"
                )
            )
            assertFalse(edited.created)
            assertEquals(stored.id, edited.highlight.id)

            repository.flushWrites()

            stored = db.highlights().listAll().single()
            assertEquals(fresh.highlight.id, stored.id)
            assertEquals("Edited margin note", stored.note)

            repository.deleteHighlight(stored.id)
            repository.flushWrites()
            assertTrue(db.highlights().listAll().isEmpty())

            val missingTarget = repository.commitSelectionNote(
                bookId = "selection-note-book",
                quote = stored.quote,
                locatorJson = stored.locatorJson,
                existingHighlightId = stored.id,
                note = "Must not resurrect"
            )
            assertNull(missingTarget)

            repository.flushWrites()
            assertTrue(db.highlights().listAll().isEmpty())
        }

    @Test
    fun selectionNote_matchesExactDurablePassage_whenOneLocatorHasMultipleSelections() = runBlocking {
        repository.addImportedBook(
            Book(
                id = "shared-locator-book",
                title = "Shared Locator",
                author = "QA",
                format = BookFormat.EPUB,
                sourceUri = "file:///shared-locator.epub",
                mediaType = "application/epub+zip",
                addedAtEpochMs = 1L
            )
        )

        val locator = "{\"href\":\"chapter.xhtml\",\"locations\":{\"progression\":0.5}}"
        val first = repository.addHighlight(
            bookId = "shared-locator-book",
            quote = "First selected sentence.",
            locatorJson = locator
        )
        val second = repository.addHighlight(
            bookId = "shared-locator-book",
            quote = "Second selected sentence.",
            locatorJson = locator
        )
        repository.flushWrites()

        val committed = requireNotNull(
            repository.commitSelectionNote(
                bookId = "shared-locator-book",
                quote = second.quote,
                locatorJson = locator,
                existingHighlightId = null,
                note = "Belongs only to the second passage"
            )
        )
        assertFalse(committed.created)
        assertEquals(second.id, committed.highlight.id)

        repository.flushWrites()
        val persisted = db.highlights().listAll().associateBy { it.id }
        assertEquals(2, persisted.size)
        assertEquals("", persisted.getValue(first.id).note)
        assertEquals(
            "Belongs only to the second passage",
            persisted.getValue(second.id).note
        )
    }

    @Test
    fun highlightAndNote_areDurableWhenSaveAcknowledgementIsAllowed() = runBlocking {
        repository.addImportedBook(
            Book(
                id = "annotation-book",
                title = "Annotation Tome",
                author = "QA",
                format = BookFormat.EPUB,
                sourceUri = "file:///annotation.epub",
                mediaType = "application/epub+zip",
                addedAtEpochMs = 1L
            )
        )

        val highlight = repository.addHighlight(
            bookId = "annotation-book",
            quote = "This passage must survive.",
            locatorJson = "{\"href\":\"chapter.xhtml\"}"
        )
        repository.flushWrites()

        var stored = db.highlights().listAll().single()
        assertEquals(highlight.id, stored.id)
        assertEquals("This passage must survive.", stored.quote)

        repository.updateHighlightNote(highlight.id, "Durable note")
        repository.flushWrites()

        stored = db.highlights().listAll().single()
        assertEquals("Durable note", stored.note)
    }
}
