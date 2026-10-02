package com.veilreader.rd

import kotlin.test.Test
import kotlin.test.assertEquals

class SyncLabTest {
    @Test fun newerRecordWins() {
        val local = SyncEnvelope("book:1", "a", 10, "A")
        val remote = SyncEnvelope("book:1", "b", 20, "B")
        assertEquals(remote, SyncConflictResolver.choose(local, remote))
    }

    @Test fun tombstoneWinsTimestampTie() {
        val live = SyncEnvelope("note:1", "text", 20, "A", tombstone = false)
        val deleted = SyncEnvelope<String>("note:1", null, 20, "B", tombstone = true)
        assertEquals(deleted, SyncConflictResolver.choose(live, deleted))
    }
}
