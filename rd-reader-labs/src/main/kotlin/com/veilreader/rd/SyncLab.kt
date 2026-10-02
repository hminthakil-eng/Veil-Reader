package com.veilreader.rd

data class SyncEnvelope<T>(
    val key: String,
    val value: T?,
    val modifiedAtEpochMs: Long,
    val deviceId: String,
    val tombstone: Boolean = false
) {
    init {
        require(key.isNotBlank())
        require(modifiedAtEpochMs >= 0)
        require(deviceId.isNotBlank())
    }
}

object SyncConflictResolver {
    fun <T> choose(local: SyncEnvelope<T>, remote: SyncEnvelope<T>): SyncEnvelope<T> {
        require(local.key == remote.key)
        return when {
            local.modifiedAtEpochMs > remote.modifiedAtEpochMs -> local
            remote.modifiedAtEpochMs > local.modifiedAtEpochMs -> remote
            local.tombstone != remote.tombstone -> if (local.tombstone) local else remote
            local.deviceId >= remote.deviceId -> local
            else -> remote
        }
    }
}
