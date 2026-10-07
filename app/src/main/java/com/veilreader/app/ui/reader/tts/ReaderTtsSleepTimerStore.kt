package com.veilreader.app.ui.reader.tts

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.veilTtsSleepTimerDataStore by preferencesDataStore(
    name = "veil_tts_sleep_timer"
)

/**
 * Durable service-owned sleep deadline.
 *
 * The timer never writes Reader progress and stores only an epoch deadline. No polling loop is
 * required: the service schedules a single delay for the remaining duration.
 */
internal class ReaderTtsSleepTimerStore(context: Context) {
    private val application = context.applicationContext

    val deadlineEpochMs: Flow<Long?> =
        application.veilTtsSleepTimerDataStore.data
            .map { preferences ->
                preferences[DEADLINE]?.takeIf { it > 0L }
            }
            .distinctUntilChanged()

    suspend fun read(): Long? = deadlineEpochMs.first()

    suspend fun save(deadlineEpochMs: Long?) {
        application.veilTtsSleepTimerDataStore.edit { preferences ->
            if (deadlineEpochMs == null || deadlineEpochMs <= 0L) {
                preferences.remove(DEADLINE)
            } else {
                preferences[DEADLINE] = deadlineEpochMs
            }
        }
    }

    private companion object {
        val DEADLINE = longPreferencesKey("deadline_epoch_ms_v1")
    }
}
