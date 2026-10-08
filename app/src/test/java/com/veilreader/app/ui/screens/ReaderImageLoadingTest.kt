package com.veilreader.app.ui.screens

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.use

class ReaderImageLoadingTest {
    @Test
    fun exactLimitIsAcceptedWithOneByteOverflowProbe() = runBlocking {
        val resource = ImageResource(byteArrayOf(1, 2, 3, 4))
        val bytes = resource.use { readReaderImageBytes(it, maxBytes = 4) }
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), bytes)
        assertEquals(0L..4L, resource.requestedRange)
        assertEquals(1, resource.closes)
    }

    @Test
    fun oversizedResultIsRejectedAndResourceStillCloses() {
        val resource = ImageResource(ByteArray(5))
        assertThrows(ReaderImageTooLargeException::class.java) {
            runBlocking { resource.use { readReaderImageBytes(it, maxBytes = 4) } }
        }
        assertEquals(1, resource.closes)
    }

    @Test
    fun emptyAndFailedReadsDoNotReachDecoder() = runBlocking {
        assertNull(ImageResource(ByteArray(0)).use { readReaderImageBytes(it, 4) })
        assertNull(ImageResource(null).use { readReaderImageBytes(it, 4) })
    }

    @Test
    fun defaultReadIsCappedWithoutCallingPotentiallyUnboundedLength() = runBlocking {
        val resource = ImageResource(byteArrayOf(1))
        resource.use { readReaderImageBytes(it) }
        assertEquals(0L..READER_IMAGE_MAX_BYTES.toLong(), resource.requestedRange)
    }

    @Test
    fun invalidBudgetFailsBeforeReading() {
        val resource = ImageResource(byteArrayOf(1))
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { readReaderImageBytes(resource, 0) }
        }
        assertNull(resource.requestedRange)
    }

    private class ImageResource(private val bytes: ByteArray?) : Resource {
        var requestedRange: LongRange? = null
        var closes = 0
        override val sourceUrl: AbsoluteUrl? = null
        override suspend fun properties(): Try<Resource.Properties, ReadError> = Try.success(Resource.Properties())
        override suspend fun length(): Try<Long, ReadError> = error("Length must never be requested")
        override suspend fun read(range: LongRange?): Try<ByteArray, ReadError> {
            requestedRange = range
            return bytes?.let { Try.success(it) } ?: Try.failure(ReadError.Decoding("unreadable"))
        }
        override fun close() { closes++ }
    }
}
