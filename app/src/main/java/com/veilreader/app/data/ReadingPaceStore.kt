package com.veilreader.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.veilreader.app.domain.ReadingPaceProfile
import com.veilreader.app.domain.recordReadingPaceInterval
import java.io.IOException
import kotlinx.coroutines.flow.catch
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.veilReadingPaceDataStore by preferencesDataStore(
    name = "veil_reading_pace"
)

/**
 * Small derived-analytics cache kept separate from Room's canonical reading history.
 *
 * Pace data can be discarded or its algorithm changed without migrating annotations, progress,
 * sessions or books. Raw timestamps are never retained; only Welford aggregate statistics are.
 */
internal class ReadingPaceStore(
    context: Context
) {
    private val appContext = context.applicationContext

    val profiles: Flow<Map<String, ReadingPaceProfile>> =
        appContext.veilReadingPaceDataStore.data.catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }.map { prefs ->
            decodeReadingPaceProfiles(prefs[PROFILES])
        }

    suspend fun recordInterval(
        bookId: String,
        intervalMillis: Long,
        shouldRecord: () -> Boolean = { true }
    ) {
        val id = bookId.trim()
        if (id.isEmpty()) return

        appContext.veilReadingPaceDataStore.edit { prefs ->
            if (!shouldRecord()) return@edit
            val profiles = decodeReadingPaceProfiles(prefs[PROFILES])
                .toMutableMap()
            val previous = profiles[id] ?: ReadingPaceProfile()
            val updated = recordReadingPaceInterval(
                previous,
                intervalMillis
            )
            if (updated == previous) return@edit

            profiles[id] = updated
            prefs[PROFILES] = encodeReadingPaceProfiles(profiles)
        }
    }

    suspend fun clear() {
        appContext.veilReadingPaceDataStore.edit { it.remove(PROFILES) }
    }

    suspend fun remove(bookId: String) {
        val id = bookId.trim()
        if (id.isEmpty()) return

        appContext.veilReadingPaceDataStore.edit { prefs ->
            val profiles = decodeReadingPaceProfiles(prefs[PROFILES])
                .toMutableMap()
            if (profiles.remove(id) == null) return@edit

            if (profiles.isEmpty()) {
                prefs.remove(PROFILES)
            } else {
                prefs[PROFILES] = encodeReadingPaceProfiles(profiles)
            }
        }
    }

    companion object {
        private val PROFILES = stringPreferencesKey("book_pace_profiles")
    }
}

internal fun decodeReadingPaceProfiles(
    raw: String?
): Map<String, ReadingPaceProfile> {
    if (raw.isNullOrBlank()) return emptyMap()
    val root = runCatching { JSONObject(raw) }.getOrNull()
        ?: return emptyMap()

    return buildMap {
        val keys = root.keys()
        while (keys.hasNext()) {
            val id = keys.next().trim()
            if (id.isEmpty()) continue
            val item = root.optJSONObject(id) ?: continue

            val profile = ReadingPaceProfile(
                sampleCount = item.optInt("n", 0),
                meanMillisPerPage = item.optDouble("mean", 0.0),
                m2MillisSquared = item.optDouble("m2", 0.0),
                totalObservedMillis = item.optLong("total", 0L)
            ).normalized()

            if (
                profile.sampleCount > 0 &&
                profile.meanMillisPerPage > 0.0
            ) {
                put(id, profile)
            }
        }
    }
}

internal fun encodeReadingPaceProfiles(
    values: Map<String, ReadingPaceProfile>
): String {
    val root = JSONObject()
    values.toSortedMap().forEach { (rawId, rawProfile) ->
        val id = rawId.trim()
        val profile = rawProfile.normalized()
        if (
            id.isEmpty() ||
            profile.sampleCount <= 0 ||
            profile.meanMillisPerPage <= 0.0
        ) {
            return@forEach
        }

        root.put(
            id,
            JSONObject().apply {
                put("n", profile.sampleCount)
                put("mean", profile.meanMillisPerPage)
                put("m2", profile.m2MillisSquared)
                put("total", profile.totalObservedMillis)
            }
        )
    }
    return root.toString()
}
