package com.veilreader.app.ui.reader.tts

import android.os.Bundle
import org.json.JSONObject

/**
 * App-private command payload for handing a visual Reader position to the background TTS owner.
 *
 * It deliberately carries no publication text and no visual Reader progress authority.
 */
internal data class ReaderTtsPlaybackRequest(
    val bookId: String,
    val locatorJson: String,
    val preferences: ReaderTtsPreferences = ReaderTtsPreferences()
) {
    fun normalized(): ReaderTtsPlaybackRequest? {
        val id = bookId.trim().takeIf { it.isNotEmpty() && it.length <= MAX_BOOK_ID_LENGTH }
            ?: return null
        val locator = locatorJson.trim().takeIf {
            it.length in 2..MAX_LOCATOR_JSON_LENGTH &&
                it.first() == '{' &&
                it.last() == '}'
        } ?: return null
        val locatorObject = runCatching { JSONObject(locator) }.getOrNull() ?: return null
        // Readium Locator JSON must identify a resource. Reject syntactically JSON-shaped garbage
        // before it can become a service-owned publication request.
        if (locatorObject.optString("href").isBlank()) return null
        return copy(
            bookId = id,
            locatorJson = locator,
            preferences = preferences.normalized()
        )
    }

    fun toBundle(): Bundle = Bundle().apply {
        putString(KEY_BOOK_ID, bookId)
        putString(KEY_LOCATOR_JSON, locatorJson)
        putFloat(KEY_SPEED, preferences.speed)
        putFloat(KEY_PITCH, preferences.pitch)
        putString(KEY_LANGUAGE_TAG, preferences.languageTag)
        putString(KEY_PREFERRED_VOICES, encodePreferredVoices(preferences.preferredVoiceIds))
    }

    fun toJson(): JSONObject = JSONObject()
        .put(KEY_BOOK_ID, bookId)
        .put(KEY_LOCATOR_JSON, locatorJson)
        .put(KEY_SPEED, preferences.speed.toDouble())
        .put(KEY_PITCH, preferences.pitch.toDouble())
        .put(KEY_LANGUAGE_TAG, preferences.languageTag)
        .put(KEY_PREFERRED_VOICES, encodePreferredVoices(preferences.preferredVoiceIds))

    companion object {
        const val ACTION_LOAD_AND_PLAY = "com.veilreader.app.tts.LOAD_AND_PLAY"
        const val ACTION_LOAD_PAUSED = "com.veilreader.app.tts.LOAD_PAUSED"

        private const val KEY_BOOK_ID = "book_id"
        private const val KEY_LOCATOR_JSON = "locator_json"
        private const val KEY_SPEED = "speed"
        private const val KEY_PITCH = "pitch"
        private const val KEY_LANGUAGE_TAG = "language_tag"
        private const val KEY_PREFERRED_VOICES = "preferred_voices_json"

        private const val MAX_BOOK_ID_LENGTH = 256
        private const val MAX_LOCATOR_JSON_LENGTH = 32 * 1024

        private fun encodePreferredVoices(values: Map<String, String>): String {
            val json = JSONObject()
            values.toSortedMap().forEach { (language, voiceId) ->
                json.put(language, voiceId)
            }
            return json.toString()
        }

        private fun decodePreferredVoices(raw: String?): Map<String, String> {
            if (raw.isNullOrBlank()) return emptyMap()
            val json = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyMap()
            return buildMap {
                val keys = json.keys()
                while (keys.hasNext()) {
                    val language = keys.next()
                    val voiceId = json.optString(language).takeIf { it.isNotBlank() } ?: continue
                    put(language, voiceId)
                }
            }
        }

        fun fromBundle(bundle: Bundle): ReaderTtsPlaybackRequest? =
            ReaderTtsPlaybackRequest(
                bookId = bundle.getString(KEY_BOOK_ID).orEmpty(),
                locatorJson = bundle.getString(KEY_LOCATOR_JSON).orEmpty(),
                preferences = ReaderTtsPreferences(
                    speed = bundle.getFloat(KEY_SPEED, 1f),
                    pitch = bundle.getFloat(KEY_PITCH, 1f),
                    languageTag = bundle.getString(KEY_LANGUAGE_TAG),
                    preferredVoiceIds = decodePreferredVoices(
                        bundle.getString(KEY_PREFERRED_VOICES)
                    )
                )
            ).normalized()

        fun fromJson(json: JSONObject): ReaderTtsPlaybackRequest? =
            ReaderTtsPlaybackRequest(
                bookId = json.optString(KEY_BOOK_ID),
                locatorJson = json.optString(KEY_LOCATOR_JSON),
                preferences = ReaderTtsPreferences(
                    speed = json.optDouble(KEY_SPEED, 1.0).toFloat(),
                    pitch = json.optDouble(KEY_PITCH, 1.0).toFloat(),
                    languageTag = json.optString(KEY_LANGUAGE_TAG).takeIf { it.isNotBlank() },
                    preferredVoiceIds = decodePreferredVoices(
                        json.optString(KEY_PREFERRED_VOICES).takeIf { it.isNotBlank() }
                    )
                )
            ).normalized()
    }
}

internal data class ReaderTtsCheckpoint(
    val request: ReaderTtsPlaybackRequest,
    val locatorJson: String,
    val phase: ReaderTtsPhase,
    val updatedAtEpochMs: Long
) {
    fun toJson(): String = JSONObject()
        .put("request", request.toJson())
        .put("locator_json", locatorJson)
        .put("phase", phase.name)
        .put("updated_at_epoch_ms", updatedAtEpochMs)
        .toString()

    companion object {
        fun fromJson(raw: String): ReaderTtsCheckpoint? = runCatching {
            val json = JSONObject(raw)
            val request = ReaderTtsPlaybackRequest.fromJson(json.getJSONObject("request"))
                ?: return@runCatching null
            val locator = json.optString("locator_json")
                .takeIf {
                    it.length in 2..32 * 1024 &&
                        it.first() == '{' &&
                        it.last() == '}'
                }
                ?: return@runCatching null
            val locatorObject = runCatching { JSONObject(locator) }.getOrNull()
                ?: return@runCatching null
            if (locatorObject.optString("href").isBlank()) return@runCatching null
            val phase = runCatching {
                ReaderTtsPhase.valueOf(json.optString("phase"))
            }.getOrDefault(ReaderTtsPhase.PAUSED)
            ReaderTtsCheckpoint(
                request = request,
                locatorJson = locator,
                phase = phase,
                updatedAtEpochMs = json.optLong("updated_at_epoch_ms").coerceAtLeast(0L)
            )
        }.getOrNull()
    }
}
