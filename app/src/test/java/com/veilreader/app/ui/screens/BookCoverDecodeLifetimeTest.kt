package com.veilreader.app.ui.screens

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BookCoverDecodeLifetimeTest {
    @Test
    fun `queued canceled decode retires without starting its native allocation`() = runTest {
        val queued = java.util.ArrayDeque<Runnable>()
        val dispatcher = object : CoroutineDispatcher() {
            override fun dispatch(context: CoroutineContext, block: Runnable) { queued.addLast(block) }
        }
        var ran = false
        val job = launch { withBookCoverDecodeLease(dispatcher) { ran = true } }
        runCurrent()
        assertEquals(1, bookCoverDecodesInFlight())
        job.cancel()
        while (queued.isNotEmpty()) queued.removeFirst().run()
        runCurrent()
        job.join()
        assertFalse(ran)
        assertEquals(0, bookCoverDecodesInFlight())
    }

    @Test
    fun `cancel cannot retire a native decode before its worker exits`() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        var published = false
        val job = launch {
            withBookCoverDecodeLease {
                entered.complete(Unit)
                check(release.await(5, TimeUnit.SECONDS))
            }
            published = true
        }
        try {
            entered.await()
            job.cancel()
            assertEquals(1, bookCoverDecodesInFlight())
            assertFalse(job.isCompleted)
        } finally {
            release.countDown()
            job.join()
        }
        assertEquals(0, bookCoverDecodesInFlight())
        assertFalse(published)
    }

    @Test
    fun `decode failure retires its lease and restores the IO worker loader`() = runBlocking {
        val expectedLoader = Thread.currentThread().contextClassLoader
        val failure = runCatching {
            withBookCoverDecodeLease {
                assertSame(expectedLoader, Thread.currentThread().contextClassLoader)
                error("decode failed")
            }
        }
        assertTrue(failure.isFailure)
        assertEquals(0, bookCoverDecodesInFlight())
    }
}
