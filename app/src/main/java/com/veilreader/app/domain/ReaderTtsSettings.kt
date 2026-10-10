package com.veilreader.app.domain

import java.util.Locale

/**
 * Explicit local speech engine selection. No automatic switch from a chosen
 * narrator or network fallback. SHERPA_ONNX refers to the separate official
 * Android TTS engine app, not a bundled or already-installed model.
 */
enum class ReaderTtsEngineChoice {
    SYSTEM,
    SHERPA_ONNX
}

data class ReaderTtsSettings(
    val speed: Double = 1.0,
    val pitch: Double = 1.0,
    val preferredVoiceIds: Map<String, String> = emptyMap(),
    val engine: ReaderTtsEngineChoice = ReaderTtsEngineChoice.SYSTEM
) {
    fun normalized(): ReaderTtsSettings =
        copy(
            speed = speed
                .takeIf { it.isFinite() }
                ?.coerceIn(0.50, 3.00)
                ?: 1.0,
            pitch = pitch
                .takeIf { it.isFinite() }
                ?.coerceIn(0.60, 1.40)
                ?: 1.0,
            preferredVoiceIds = preferredVoiceIds
                .entries
                .asSequence()
                .mapNotNull { (rawLanguage, rawVoice) ->
                    val locale = Locale.forLanguageTag(rawLanguage.trim())
                    val language = locale
                        .takeUnless { it.language.isBlank() || it.language == "und" }
                        ?.toLanguageTag()
                        ?: return@mapNotNull null
                    val voice = rawVoice.trim()
                        .takeIf { it.isNotEmpty() && it.length <= 256 }
                        ?: return@mapNotNull null
                    language to voice
                }
                .distinctBy { it.first }
                .take(MAX_PREFERRED_VOICES)
                .toMap()
        )

    fun preferredVoiceId(languageTag: String?): String? {
        val locale = languageTag
            ?.let(Locale::forLanguageTag)
            ?.takeUnless { it.language.isBlank() || it.language == "und" }
            ?: return null
        val safe = normalized().preferredVoiceIds
        return safe[locale.toLanguageTag()]
            ?: safe.entries.firstOrNull {
                Locale.forLanguageTag(it.key).language == locale.language
            }?.value
    }

    fun withPreferredVoice(languageTag: String, voiceId: String?): ReaderTtsSettings {
        val locale = Locale.forLanguageTag(languageTag)
        if (locale.language.isBlank() || locale.language == "und") return this
        val key = locale.toLanguageTag()
        val updated = preferredVoiceIds.toMutableMap()
        val safeVoice = voiceId?.trim()?.takeIf { it.isNotEmpty() && it.length <= 256 }
        if (safeVoice == null) updated.remove(key) else updated[key] = safeVoice
        return copy(preferredVoiceIds = updated).normalized()
    }

    private companion object {
        const val MAX_PREFERRED_VOICES = 16
    }
}
