package com.veilreader.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * V4 is a local-only, opt-in staging layer. No connection, upload, or account is created here.
 * A row with enabled=true may only be created after an explicit user consent flow.
 */
@Entity(tableName = "sync_preferences")
data class SyncPreferenceEntity(
    @PrimaryKey val id: Int = 1,
    val enabled: Boolean = false,
    val deviceId: String,
    val updatedAtEpochMs: Long
)

/** Events are durable until a future authenticated transport ACKs each eventId. */
@Entity(
    tableName = "sync_outbox",
    indices = [Index(value = ["bookId"]), Index(value = ["createdAtEpochMs"])]
)
data class SyncOutboxEntity(
    @PrimaryKey val eventId: String,
    val bookId: String,
    val sourceDeviceId: String,
    val kind: String,
    val entityId: String,
    val payloadJson: String,
    val createdAtEpochMs: Long
)

/** Deletions survive until every *registered* device has acknowledged their revision. */
@Entity(tableName = "sync_tombstones", primaryKeys = ["entityType", "entityId"], indices = [Index(value = ["bookId"])])
data class SyncTombstoneEntity(
    val entityType: String,
    val entityId: String,
    val bookId: String,
    val deletedAtEpochMs: Long,
    val deleteRevision: Long
)

@Entity(tableName = "sync_devices")
data class SyncDeviceEntity(
    @PrimaryKey val deviceId: String,
    val active: Boolean,
    val lastSeenAtEpochMs: Long
)

/** Acks are independently keyed by deleted entity AND device; never use a global high-water mark. */
@Entity(tableName = "sync_deletion_acks", primaryKeys = ["entityType", "entityId", "deviceId"])
data class SyncDeletionAckEntity(
    val entityType: String,
    val entityId: String,
    val deviceId: String,
    val acknowledgedRevision: Long
)

/** Conflicts are held for explicit user/server resolution, never silently overwritten. */
@Entity(tableName = "sync_conflicts", indices = [Index(value = ["bookId"])])
data class SyncConflictEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val entityType: String,
    val entityId: String,
    val localPayloadJson: String,
    val remotePayloadJson: String,
    val createdAtEpochMs: Long,
    val status: String = "OPEN"
)
