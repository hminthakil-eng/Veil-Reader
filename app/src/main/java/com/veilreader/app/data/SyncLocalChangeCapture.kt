package com.veilreader.app.data

import com.veilreader.app.data.db.SyncOutboxEntity
import com.veilreader.app.data.db.SyncTombstoneEntity
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.domain.Bookmark
import com.veilreader.app.domain.Highlight
import java.util.UUID
import org.json.JSONObject

/**
 * Durable local change capture. Only called inside the same Room transaction as the canonical
 * mutation. V4 starts OFF; there is deliberately NO sync server, scheduler or network client.
 *
 * Publication fingerprints are hints for an eventual authenticated transport, not authorization.
 * Do not send sourceUri, local app paths, reading text excerpts or credentials to a server.
 */
internal class SyncLocalChangeCapture(private val database: VeilDatabase) {
    suspend fun bookmarkUpsert(bookmark: Bookmark) {
        enqueue(
            "BOOKMARK_UPSERT", bookmark.bookId, bookmark.id,
            JSONObject().put("id", bookmark.id)
                .put("label", bookmark.label)
                .put("locator", bookmark.locatorJson)
                .put("createdAt", bookmark.createdAtEpochMs)
        )
    }

    suspend fun highlightUpsert(highlight: Highlight) {
        enqueue(
            "HIGHLIGHT_UPSERT", highlight.bookId, highlight.id,
            JSONObject().put("id", highlight.id)
                .put("quote", highlight.quote)
                .put("note", highlight.note)
                .put("locator", highlight.locatorJson)
                .put("createdAt", highlight.createdAtEpochMs)
        )
    }

    suspend fun progress(bookId: String, progression: Double, locatorJson: String, finished: Boolean) {
        enqueue(
            "PROGRESS", bookId, bookId,
            JSONObject().put("progression", progression)
                .put("locator", locatorJson)
                .put("finished", finished)
        )
    }

    suspend fun deleted(type: String, bookId: String, entityId: String) {
        val preference = database.sync().preference()
        if (preference?.enabled != true || preference.deviceId.isBlank()) return
        require(type == "BOOKMARK" || type == "HIGHLIGHT")
        val now = System.currentTimeMillis()
        // Revision 0 is "awaiting remote assignment"; never purge based on it.
        database.sync().upsertTombstone(
            SyncTombstoneEntity(type, entityId, bookId, now, deleteRevision = 0L)
        )
        persist(preference.deviceId, "DELETE_$type", bookId, entityId,
            JSONObject().put("id", entityId).put("deletedAt", now), now)
    }

    private suspend fun enqueue(kind: String, bookId: String, entityId: String, body: JSONObject) {
        val preference = database.sync().preference()
        if (preference?.enabled != true || preference.deviceId.isBlank()) return
        persist(preference.deviceId, kind, bookId, entityId, body, System.currentTimeMillis())
    }

    private suspend fun persist(
        deviceId: String, kind: String, bookId: String, entityId: String,
        body: JSONObject, now: Long
    ) {
        val fingerprint = database.books().findEntity(bookId)?.contentFingerprint
        body.put("fingerprint", fingerprint?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
        val row = SyncOutboxEntity(
            eventId = UUID.randomUUID().toString(),
            bookId = bookId, sourceDeviceId = deviceId, kind = kind,
            entityId = entityId, payloadJson = body.toString(), createdAtEpochMs = now
        )
        check(database.sync().enqueue(row) != -1L) { "Sync outbox event collision" }
    }
}
