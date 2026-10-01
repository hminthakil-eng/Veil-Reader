package com.veilreader.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.data.settings.SettingsStore
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookFormat
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
