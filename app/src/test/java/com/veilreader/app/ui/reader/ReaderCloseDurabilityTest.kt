package com.veilreader.app.ui.reader

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderCloseDurabilityTest {
    @Test
    fun routeClose_waitsForDurableStorageAcknowledgement() = runBlocking {
        val durabilityEntered = CompletableDeferred<Unit>()
        val releaseDurability = CompletableDeferred<Unit>()
        var sessionFinalized = false
        var routeCleared = false

        val closeJob = launch {
            awaitDurableReaderClose(
                finalizeSession = { sessionFinalized = true },
                awaitDurability = {
                    durabilityEntered.complete(Unit)
                    releaseDurability.await()
                },
                clearRoute = { routeCleared = true }
            )
        }

        durabilityEntered.await()

        assertTrue(sessionFinalized)
        assertFalse("SavedState route/checkpoint must remain while Room durability is pending", routeCleared)

        releaseDurability.complete(Unit)
        closeJob.join()

        assertTrue(routeCleared)
    }

    @Test
    fun durabilityFailure_preservesReaderRouteAndCheckpoint() = runBlocking {
        var routeCleared = false

        val result = runCatching {
            awaitDurableReaderClose(
                finalizeSession = {},
                awaitDurability = { error("storage failed") },
                clearRoute = { routeCleared = true }
            )
        }

        assertTrue(result.isFailure)
        assertFalse("A failed durability barrier must not clear the Reader route", routeCleared)
    }
}
