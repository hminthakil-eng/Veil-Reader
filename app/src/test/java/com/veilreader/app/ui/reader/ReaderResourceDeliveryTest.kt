package com.veilreader.app.ui.reader

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderResourceDeliveryTest {
    @Test
    fun acceptedResourceIsOwnedByConsumer() = runBlocking {
        val resource = Resource()
        deliverReaderResource(Dispatchers.Default, { resource }) { true }
        assertEquals(0, resource.closes)
        resource.close()
        assertEquals(1, resource.closes)
    }

    @Test
    fun rejectedStaleResourceIsClosed() = runBlocking {
        val resource = Resource()
        deliverReaderResource(Dispatchers.Default, { resource }) { false }
        assertEquals(1, resource.closes)
    }

    @Test
    fun consumerFailureClosesUntransferredResource() = runBlocking {
        val resource = Resource()
        try {
            deliverReaderResource(Dispatchers.Default, { resource }) { error("consumer failed") }
            error("Expected failure")
        } catch (expected: IllegalStateException) {
            assertEquals("consumer failed", expected.message)
        }
        assertEquals(1, resource.closes)
    }

    @Test
    fun emptyResultReachesConsumerWithoutAResourceToDispose() = runBlocking {
        var calls = 0
        deliverReaderResource<Resource>(Dispatchers.Default, { null }) {
            assertNull(it)
            calls++
            false
        }
        assertEquals(1, calls)
    }

    @Test
    fun loaderFailureDoesNotDeliverAResult() = runBlocking {
        var delivered = false
        try {
            deliverReaderResource<Resource>(Dispatchers.Default, { error("loader failed") }) {
                delivered = true
                true
            }
            error("Expected failure")
        } catch (expected: IllegalStateException) {
            assertEquals("loader failed", expected.message)
        }
        assertFalse(delivered)
    }

    @Test
    fun cancellationWhileWorkerFinishesClosesResultInsteadOfDeliveringIt() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val resource = Resource()
        var delivered = false
        val job = launch {
            deliverReaderResource(
                dispatcher = Dispatchers.Default,
                load = {
                    // Bitmap decoding may finish after cancellation. Exercise the dispatch back
                    // to the caller, where withContext would otherwise discard the resource.
                    withContext(NonCancellable) {
                        started.complete(Unit)
                        withTimeout(5_000) { release.await() }
                    }
                    resource
                },
                accept = {
                    delivered = true
                    true
                }
            )
        }
        try {
            withTimeout(5_000) { started.await() }
            job.cancel()
        } finally {
            release.complete(Unit)
            job.join()
        }
        assertFalse(delivered)
        assertEquals(1, resource.closes)
    }

    private class Resource : AutoCloseable {
        var closes = 0
        override fun close() { closes++ }
    }
}
