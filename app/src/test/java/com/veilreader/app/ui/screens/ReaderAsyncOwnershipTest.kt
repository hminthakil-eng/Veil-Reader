package com.veilreader.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderAsyncOwnershipTest {

    @Test
    fun resultFromCurrentSession_isAccepted() {
        assertTrue(
            readerAsyncResultBelongsToSession(
                currentSessionInstanceId = "session-b",
                expectedSessionInstanceId = "session-b"
            )
        )
    }

    @Test
    fun resultFromPreviousSameBookSession_isRejected() {
        assertFalse(
            readerAsyncResultBelongsToSession(
                currentSessionInstanceId = "session-b",
                expectedSessionInstanceId = "session-a"
            )
        )
    }
}
