package com.veilreader.app.ui.reader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderSessionOwnershipTest {

    @Test
    fun currentSession_acceptsOwnedEvent() {
        assertTrue(
            readerEventBelongsToSession(
                activeOpenInstanceId = "session-b",
                expectedOpenInstanceId = "session-b",
                activeBookId = "book",
                expectedBookId = "book"
            )
        )
    }

    @Test
    fun staleSameBookSession_isRejected() {
        assertFalse(
            readerEventBelongsToSession(
                activeOpenInstanceId = "session-b",
                expectedOpenInstanceId = "session-a",
                activeBookId = "book",
                expectedBookId = "book"
            )
        )
    }

    @Test
    fun currentSessionWithWrongBook_isRejected() {
        assertFalse(
            readerEventBelongsToSession(
                activeOpenInstanceId = "session-b",
                expectedOpenInstanceId = "session-b",
                activeBookId = "book-b",
                expectedBookId = "book-a"
            )
        )
    }

    @Test
    fun sessionOnlyEvent_requiresAnActiveTrackerBook() {
        assertFalse(
            readerEventBelongsToSession(
                activeOpenInstanceId = "session-b",
                expectedOpenInstanceId = "session-b",
                activeBookId = null
            )
        )
    }
}
