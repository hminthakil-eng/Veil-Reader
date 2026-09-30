package com.veilreader.app.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderOpenResourceGuardTest {

    @Test
    fun untransferredResource_closesExactlyOnce() {
        val resource = FakeCloseable()
        val guard = ReaderOpenResourceGuard(resource)

        guard.closeIfUntransferred()
        guard.closeIfUntransferred()

        assertEquals(1, resource.closeCount)
    }

    @Test
    fun transferredResource_remainsOwnedByCaller() {
        val resource = FakeCloseable()
        val guard = ReaderOpenResourceGuard(resource)

        assertEquals(resource, guard.transfer())
        guard.closeIfUntransferred()

        assertEquals(0, resource.closeCount)
        resource.close()
        assertEquals(1, resource.closeCount)
    }

    private class FakeCloseable : AutoCloseable {
        var closeCount = 0
            private set

        override fun close() {
            closeCount += 1
        }
    }
}
