package com.veilreader.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderProgressWriteOrderTest {

    @Test
    fun newerSequenceWinsWithinSameWriterEpoch() {
        assertTrue(
            shouldReplacePendingProgress(
                current = ReaderProgressWriteOrder(epoch = 4L, sequence = 8L),
                incoming = ReaderProgressWriteOrder(epoch = 4L, sequence = 9L)
            )
        )
    }

    @Test
    fun olderSequenceCannotReplaceNewerPendingWrite() {
        assertFalse(
            shouldReplacePendingProgress(
                current = ReaderProgressWriteOrder(epoch = 4L, sequence = 9L),
                incoming = ReaderProgressWriteOrder(epoch = 4L, sequence = 8L)
            )
        )
    }

    @Test
    fun freshWriterEpochWinsEvenWhenItsSequenceRestarts() {
        assertTrue(
            shouldReplacePendingProgress(
                current = ReaderProgressWriteOrder(epoch = 7L, sequence = 412L),
                incoming = ReaderProgressWriteOrder(epoch = 8L, sequence = 1L)
            )
        )
    }

    @Test
    fun staleWriterEpochCannotOverwriteFreshSession() {
        assertFalse(
            shouldReplacePendingProgress(
                current = ReaderProgressWriteOrder(epoch = 8L, sequence = 1L),
                incoming = ReaderProgressWriteOrder(epoch = 7L, sequence = 999L)
            )
        )
    }

    @Test
    fun sameLogicalOrderIsIdempotentlyReplaceable() {
        val order = ReaderProgressWriteOrder(epoch = 8L, sequence = 5L)
        assertTrue(shouldReplacePendingProgress(current = order, incoming = order))
    }

    @Test
    fun unorderedLegacyValuesRemainCompatibleOutsideActiveReaderOwnership() {
        assertTrue(
            shouldReplacePendingProgress(
                current = null,
                incoming = ReaderProgressWriteOrder(epoch = 1L, sequence = 1L)
            )
        )
        assertTrue(
            shouldReplacePendingProgress(
                current = ReaderProgressWriteOrder(epoch = 1L, sequence = 1L),
                incoming = null
            )
        )
    }
}
