package com.veilreader.app.ui.screens

import com.veilreader.app.domain.Book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookArtifactTest {
    private val day = 86_400_000L
    private val now = 200L * day

    @Test
    fun `unopened books remain pristine and do not invent history`() {
        val state = bookArtifactState(
            Book(
                id = "a",
                title = "Untouched",
                author = "Archive",
                addedAtEpochMs = now - 2L * day
            ),
            nowEpochMs = now
        )

        assertEquals(BookPresence.PRISTINE, state.presence)
        assertEquals(BookArchiveAge.NEW, state.archiveAge)
        assertFalse(state.completed)
        assertFalse(state.recentlyOpened)
        assertEquals(0f, state.leftStack, 0.0001f)
        assertEquals(1f, state.rightStack, 0.0001f)
    }

    @Test
    fun `reading progress physically transfers the page stack`() {
        val state = bookArtifactState(
            Book(
                id = "b",
                title = "Halfway",
                author = "Reader",
                progress = 0.64f,
                addedAtEpochMs = now - 80L * day,
                lastOpenedAtEpochMs = now - 2L * 60L * 60L * 1000L
            ),
            nowEpochMs = now
        )

        assertEquals(BookPresence.READING, state.presence)
        assertEquals(BookArchiveAge.AGED, state.archiveAge)
        assertEquals(0.64f, state.leftStack, 0.0001f)
        assertEquals(0.36f, state.rightStack, 0.0001f)
        assertTrue(state.recentlyOpened)
        assertTrue(state.patina > 0.20f)
    }

    @Test
    fun `completed old volumes receive deep archive treatment`() {
        val state = bookArtifactState(
            Book(
                id = "c",
                title = "Long Memory",
                author = "Veil",
                progress = 1f,
                finished = true,
                favorite = true,
                addedAtEpochMs = now - 190L * day,
                lastOpenedAtEpochMs = now - 50L * day
            ),
            nowEpochMs = now
        )

        assertEquals(BookPresence.COMPLETED, state.presence)
        assertEquals(BookArchiveAge.ARCHIVAL, state.archiveAge)
        assertTrue(state.completed)
        assertTrue(state.favorite)
        assertEquals(1f, state.leftStack, 0.0001f)
        assertEquals(0f, state.rightStack, 0.0001f)
    }

    @Test
    fun `future timestamps never create false recent-return state`() {
        val state = bookArtifactState(
            Book(
                id = "d",
                title = "Future",
                author = "Clock",
                addedAtEpochMs = now + day,
                lastOpenedAtEpochMs = now + day
            ),
            nowEpochMs = now
        )

        assertEquals(BookArchiveAge.NEW, state.archiveAge)
        assertFalse(state.recentlyOpened)
    }
    @Test
    fun `archive age and recent return agree with the library at boundary dates`() {
        val book = Book(
            id = "boundary",
            title = "Return",
            author = "Archive",
            addedAtEpochMs = now - 50L * day,
            lastOpenedAtEpochMs = now - 40L * 60L * 60L * 1000L
        )
        val cover = bookArtifactState(book, nowEpochMs = now)
        val library = com.veilreader.app.ui.books.bookArtifactState(book, nowEpochMs = now)

        assertEquals(BookArchiveAge.AGED, cover.archiveAge)
        assertEquals(com.veilreader.app.ui.books.BookPatina.AGED, library.patina)
        assertEquals(BookPresence.OPENED, cover.presence)
        assertEquals(library.recentlyOpened, cover.recentlyOpened)
        assertTrue(cover.recentlyOpened)
    }

    @Test
    fun `finished favorite gets full progress and additive patina on every cover`() {
        val book = Book(
            id = "sealed",
            title = "Sealed",
            author = "Archive",
            progress = 0.64f,
            finished = true,
            favorite = true,
            addedAtEpochMs = now - 2L * day
        )
        val cover = bookArtifactState(book, nowEpochMs = now)
        val library = com.veilreader.app.ui.books.bookArtifactState(book, nowEpochMs = now)

        assertEquals(library.progress, cover.progress, 0f)
        assertEquals(1f, cover.leftStack, 0f)
        assertEquals(0f, cover.rightStack, 0f)
        assertEquals(0.33f, cover.patina, 0.0001f)
        assertTrue(cover.completed)
    }


    @Test
    fun `book detail identity supplies truthful fallbacks without blank geometry`() {
        val identity = bookDetailIdentityText(
            book = Book(
                id = "identity-empty",
                title = "   ",
                author = "\t",
                seriesName = "   ",
                seriesIndex = 7.0
            ),
            untitledBook = "Untitled book",
            unknownAuthor = "Unknown author"
        )

        assertEquals("Untitled book", identity.title)
        assertEquals("Unknown author", identity.author)
        assertNull(identity.seriesName)
        assertNull(identity.seriesIndex)
        assertTrue(identity.collections.isEmpty())
    }

    @Test
    fun `book detail identity preserves mixed script and deduplicates collection context`() {
        val longMixedTitle = "رازهای مه — The Archive Beyond the Seventh Threshold"
        val identity = bookDetailIdentityText(
            book = Book(
                id = "identity-mixed",
                title = "  $longMixedTitle  ",
                author = "  نویسنده A  ",
                seriesName = "  The Sequence  ",
                seriesIndex = 2.5,
                collections = listOf(" Mystery ", "mystery", "حافظه")
            ),
            untitledBook = "Untitled",
            unknownAuthor = "Unknown"
        )

        assertEquals(longMixedTitle, identity.title)
        assertEquals("نویسنده A", identity.author)
        assertEquals("The Sequence", identity.seriesName)
        assertEquals(2.5, identity.seriesIndex)
        assertEquals(listOf("Mystery", "حافظه"), identity.collections)
    }

    @Test
    fun `series index never survives without a visible series identity`() {
        val identity = bookDetailIdentityText(
            book = Book(
                id = "identity-index",
                title = "Book",
                author = "Author",
                seriesName = null,
                seriesIndex = 4.0
            ),
            untitledBook = "Untitled",
            unknownAuthor = "Unknown"
        )

        assertNull(identity.seriesName)
        assertNull(identity.seriesIndex)
    }


    @Test
    fun `book detail journey never invents active progress for an unopened book`() {
        val state = bookDetailJourneyState(
            book = Book(
                id = "journey-new",
                title = "New",
                author = "Reader",
                currentChapter = "Chapter 1"
            ),
            progress = 0f
        )

        assertEquals(BookDetailJourneyPhase.NOT_STARTED, state.phase)
        assertEquals(BookDetailJourneyAction.UNAVAILABLE, state.action)
        assertEquals(0f, state.progress, 0f)
        assertNull(state.chapter)
    }

    @Test
    fun `active journey exposes only meaningful chapter context`() {
        val state = bookDetailJourneyState(
            book = Book(
                id = "journey-active",
                title = "Active",
                author = "Reader",
                sourceUri = "file:///reader.epub",
                currentChapter = "  فصل پنجم — The Fifth Threshold  "
            ),
            progress = 0.42f
        )

        assertEquals(BookDetailJourneyPhase.READING, state.phase)
        assertEquals(BookDetailJourneyAction.CONTINUE, state.action)
        assertEquals(0.42f, state.progress, 0.0001f)
        assertEquals("فصل پنجم — The Fifth Threshold", state.chapter)
    }

    @Test
    fun `completed journey owns full progress and read-again action`() {
        val state = bookDetailJourneyState(
            book = Book(
                id = "journey-complete",
                title = "Complete",
                author = "Reader",
                sourceUri = "file:///reader.epub",
                currentChapter = "Finale",
                progress = 0.61f,
                finished = true
            ),
            progress = 0.61f
        )

        assertEquals(BookDetailJourneyPhase.COMPLETED, state.phase)
        assertEquals(BookDetailJourneyAction.READ_AGAIN, state.action)
        assertEquals(1f, state.progress, 0f)
        assertNull(state.chapter)
    }

    @Test
    fun `unavailable publication keeps truthful reading history but disables open action`() {
        val state = bookDetailJourneyState(
            book = Book(
                id = "journey-missing",
                title = "Missing",
                author = "Archive",
                progress = 0.57f,
                currentChapter = "Chapter 9"
            ),
            progress = 0.57f
        )

        assertEquals(BookDetailJourneyPhase.READING, state.phase)
        assertEquals(BookDetailJourneyAction.UNAVAILABLE, state.action)
        assertEquals(0.57f, state.progress, 0.0001f)
        assertEquals("Chapter 9", state.chapter)
    }

    @Test
    fun `journey clamps invalid progress without fabricating a reading state`() {
        val state = bookDetailJourneyState(
            book = Book(
                id = "journey-invalid",
                title = "Invalid",
                author = "Archive",
                sourceUri = "file:///reader.epub"
            ),
            progress = Float.NaN
        )

        assertEquals(BookDetailJourneyPhase.NOT_STARTED, state.phase)
        assertEquals(BookDetailJourneyAction.OPEN, state.action)
        assertEquals(0f, state.progress, 0f)
    }


    @Test
    fun `preserved memory distinguishes quote note and combined artifacts`() {
        val memory = bookDetailPreservedMemory(
            listOf(
                com.veilreader.app.domain.Highlight("q", "book", " Quote ", "{}", createdAtEpochMs = 10L),
                com.veilreader.app.domain.Highlight("n", "book", " ", "{}", note = " Note ", createdAtEpochMs = 20L),
                com.veilreader.app.domain.Highlight("both", "book", " Both quote ", "{}", note = " Both note ", createdAtEpochMs = 30L)
            )
        )
        assertEquals(3, memory.totalUseful)
        assertEquals(PreservedMemoryKind.QUOTE_AND_NOTE, memory.fragments[0].kind)
        assertEquals("Both quote", memory.fragments[0].quote)
        assertEquals("Both note", memory.fragments[0].note)
        assertEquals(PreservedMemoryKind.NOTE_ONLY, memory.fragments[1].kind)
        assertEquals(PreservedMemoryKind.QUOTE_ONLY, memory.fragments[2].kind)
    }

    @Test
    fun `preserved memory is recent first bounded and ignores empty artifacts`() {
        val memory = bookDetailPreservedMemory(
            listOf(
                com.veilreader.app.domain.Highlight("old", "book", "old", "{}", createdAtEpochMs = 1L),
                com.veilreader.app.domain.Highlight("empty", "book", "   ", "{}", note = " ", createdAtEpochMs = 99L),
                com.veilreader.app.domain.Highlight("new", "book", "new", "{}", createdAtEpochMs = 5L),
                com.veilreader.app.domain.Highlight("mid", "book", "", "{}", note = "mid note", createdAtEpochMs = 3L),
                com.veilreader.app.domain.Highlight("older", "book", "older", "{}", createdAtEpochMs = 2L)
            ),
            sampleLimit = 3
        )
        assertEquals(4, memory.totalUseful)
        assertEquals(1, memory.hiddenCount)
        assertEquals(listOf("new", "mid", "older"), memory.fragments.map { it.id })
    }

    @Test
    fun `preserved memory stays a sample rather than a notebook`() {
        val memory = bookDetailPreservedMemory(
            (1..8).map { index ->
                com.veilreader.app.domain.Highlight(
                    id = "h$index",
                    bookId = "book",
                    quote = "memory $index",
                    locatorJson = "{}",
                    createdAtEpochMs = index.toLong()
                )
            }
        )
        assertEquals(8, memory.totalUseful)
        assertEquals(3, memory.fragments.size)
        assertEquals(5, memory.hiddenCount)
        assertEquals(listOf("h8", "h7", "h6"), memory.fragments.map { it.id })
    }

}
