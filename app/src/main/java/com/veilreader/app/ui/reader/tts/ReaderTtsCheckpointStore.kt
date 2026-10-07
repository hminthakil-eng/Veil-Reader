package com.veilreader.app.ui.reader.tts

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.veilTtsCheckpointDataStore by preferencesDataStore(
    name = "veil_tts_checkpoint"
)

/**
 * Dedicated listening checkpoint. This store is intentionally separate from Reader progress so
 * visual reading and background listening can never race through last-write-wins semantics.
 */
internal class ReaderTtsCheckpointStore(context: Context) {
    private val application = context.applicationContext

    val checkpoints: Flow<ReaderTtsCheckpoint?> =
        application.veilTtsCheckpointDataStore.data
            .map { preferences ->
                preferences[CHECKPOINT]?.let(ReaderTtsCheckpoint::fromJson)
            }
            .distinctUntilChanged()

    suspend fun read(): ReaderTtsCheckpoint? = checkpoints.first()

    suspend fun save(value: ReaderTtsCheckpoint) {
        application.veilTtsCheckpointDataStore.edit { preferences ->
            preferences[CHECKPOINT] = value.toJson()
        }
    }

    suspend fun clear() {
        application.veilTtsCheckpointDataStore.edit { preferences ->
            preferences.remove(CHECKPOINT)
        }
    }

    private companion object {
        val CHECKPOINT = stringPreferencesKey("checkpoint_v1")
    }
}
