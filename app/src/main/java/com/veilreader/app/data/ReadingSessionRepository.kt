package com.veilreader.app.data

import android.content.Context
import com.veilreader.app.data.db.ReadingSessionEntity
import com.veilreader.app.data.db.VeilDatabase
import com.veilreader.app.domain.ReadingSessionSnapshot

/** Durable storage for reader-session analytics. */
class ReadingSessionRepository internal constructor(
    private val database: VeilDatabase
) {
    constructor(context: Context) : this(VeilDatabase.get(context.applicationContext))

    suspend fun save(snapshot: ReadingSessionSnapshot) {
        database.readingSessions().upsert(
            ReadingSessionEntity(
                id = snapshot.id,
                bookId = snapshot.bookId,
                startedAtEpochMs = snapshot.startedAtEpochMs,
                endedAtEpochMs = snapshot.endedAtEpochMs,
                activeMillis = snapshot.activeMillis,
                pacedPageTurns = snapshot.pacedPageTurns,
                highlightCount = snapshot.highlightCount,
                noteCount = snapshot.noteCount
            )
        )
    }

    suspend fun totalActiveMillis(): Long = database.readingSessions().totalActiveMillis()
}
