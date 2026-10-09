package com.veilreader.app.ui.reader

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderRequestDeadlineTest {
    @Test
    fun completionCancelsDeadlineWithoutReportingTimeout() = runBlocking {
        var timeouts = 0
        runReaderRequestWithDeadline(1_000, { timeouts++ }) { }
        assertEquals(0, timeouts)
    }

    @Test
    fun deadlineReleasesUiBeforeNonCancellableWorkerFinishes() = runBlocking {
        val timedOut = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var cancellationObserved = false
        val job = launch {
            try {
                runReaderRequestWithDeadline(5, { timedOut.complete(Unit) }) {
                    withContext(NonCancellable) { withTimeout(3_000) { release.await() } }
                }
            } catch (_: CancellationException) {
                cancellationObserved = true
            }
        }
        try {
            withTimeout(3_000) { timedOut.await() }
            assertFalse(job.isCompleted)
        } finally {
            release.complete(Unit)
            job.join()
        }
        assertTrue(cancellationObserved)
    }

    @Test
    fun explicitCancellationDoesNotReportDeadlineFailure() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var timeouts = 0
        val job = launch {
            runReaderRequestWithDeadline(10_000, { timeouts++ }) {
                withContext(NonCancellable) {
                    started.complete(Unit)
                    withTimeout(3_000) { release.await() }
                }
            }
        }
        try {
            withTimeout(3_000) { started.await() }
            job.cancel()
        } finally {
            release.complete(Unit)
            job.join()
        }
        assertEquals(0, timeouts)
    }

    @Test
    fun loaderFailurePropagatesWithoutReportingTimeout() = runBlocking {
        var timeouts = 0
        try {
            runReaderRequestWithDeadline(1_000, { timeouts++ }) { error("read failed") }
            error("Expected failure")
        } catch (expected: IllegalStateException) {
            assertEquals("read failed", expected.message)
        }
        assertEquals(0, timeouts)
    }
}
