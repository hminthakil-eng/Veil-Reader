package com.veilreader.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

/**
 * Local V4 sync infrastructure. Nothing here performs network I/O.
 * The sync transport must authenticate and verify book fingerprints before using queued events.
 */
@Dao
interface SyncDao {
    @Query("SELECT * FROM sync_preferences WHERE id = 1 LIMIT 1")
    suspend fun preference(): SyncPreferenceEntity?

    @Upsert
    suspend fun setPreference(value: SyncPreferenceEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(value: SyncOutboxEntity): Long

    @Query("SELECT * FROM sync_outbox ORDER BY createdAtEpochMs, eventId LIMIT :limit")
    suspend fun pending(limit: Int = 100): List<SyncOutboxEntity>

    /** Delete only after the authenticated remote endpoint acknowledges this exact event ID. */
    @Query("DELETE FROM sync_outbox WHERE eventId = :eventId")
    suspend fun acknowledgeEvent(eventId: String): Int

    @Upsert
    suspend fun upsertTombstone(value: SyncTombstoneEntity)

    @Query("SELECT * FROM sync_tombstones WHERE entityType = :type AND entityId = :id LIMIT 1")
    suspend fun tombstone(type: String, id: String): SyncTombstoneEntity?

    @Upsert
    suspend fun upsertDevice(value: SyncDeviceEntity)

    @Query("SELECT * FROM sync_devices WHERE active = 1")
    suspend fun activeDevices(): List<SyncDeviceEntity>

    @Upsert
    suspend fun upsertDeletionAck(value: SyncDeletionAckEntity)

    @Query("SELECT * FROM sync_deletion_acks WHERE entityType = :type AND entityId = :id")
    suspend fun deletionAcks(type: String, id: String): List<SyncDeletionAckEntity>

    @Upsert
    suspend fun upsertConflict(value: SyncConflictEntity)

    /**
     * A restored backup replaces local authority. Explicitly clear prior sync journals, conflicts,
     * and trusted peer metadata atomically with the restore, and require fresh consent.
     */
    @Query("DELETE FROM sync_outbox")
    suspend fun clearOutbox()

    @Query("DELETE FROM sync_tombstones")
    suspend fun clearTombstones()

    @Query("DELETE FROM sync_deletion_acks")
    suspend fun clearDeletionAcks()

    @Query("DELETE FROM sync_devices")
    suspend fun clearDevices()

    @Query("DELETE FROM sync_conflicts")
    suspend fun clearConflicts()

    @Query("UPDATE sync_preferences SET enabled = 0 WHERE id = 1")
    suspend fun disableCapture()

    suspend fun resetForRestore() {
        clearOutbox()
        clearTombstones()
        clearDeletionAcks()
        clearDevices()
        clearConflicts()
        disableCapture()
    }

    @Query("SELECT * FROM sync_conflicts WHERE status = 'OPEN' ORDER BY createdAtEpochMs")
    suspend fun openConflicts(): List<SyncConflictEntity>
}
