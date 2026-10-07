package com.veilreader.app.ui.reader.tts

import java.util.Locale

internal data class ReaderTtsPreferences(
    val speed: Float = 1f,
    val pitch: Float = 1f,
    val languageTag: String? = null,
    val preferredVoiceIds: Map<String, String> = emptyMap()
) {
    fun normalized(): ReaderTtsPreferences = copy(
        speed = if (speed.isFinite()) speed.coerceIn(0.5f, 3f) else 1f,
        pitch = if (pitch.isFinite()) pitch.coerceIn(0.5f, 2f) else 1f,
        languageTag = languageTag?.trim()?.takeIf { it.length <= 64 }
            ?.let(Locale::forLanguageTag)?.takeUnless { it.language.isBlank() || it.language == "und" }
            ?.toLanguageTag(),
        preferredVoiceIds = preferredVoiceIds.entries.asSequence()
            .mapNotNull { (rawLanguage, rawVoice) ->
                val locale = Locale.forLanguageTag(rawLanguage.trim())
                val language = locale.takeUnless {
                    it.language.isBlank() || it.language == "und"
                }?.toLanguageTag() ?: return@mapNotNull null
                val voice = rawVoice.trim()
                    .takeIf { it.isNotEmpty() && it.length <= 256 }
                    ?: return@mapNotNull null
                language to voice
            }
            .distinctBy { it.first }
            .take(16)
            .toMap()
    )

    fun preferredVoiceId(languageTag: String): String? {
        val locale = Locale.forLanguageTag(languageTag)
        if (locale.language.isBlank() || locale.language == "und") return null
        val safe = normalized().preferredVoiceIds
        return safe[locale.toLanguageTag()]
            ?: safe.entries.firstOrNull {
                Locale.forLanguageTag(it.key).language == locale.language
            }?.value
    }
}

internal data class ReaderTtsVoice(
    val id: String,
    val languageTag: String,
    val quality: Int,
    val requiresNetwork: Boolean,
    val installed: Boolean
)

/** Never delegate missing voice selection to setLanguage(), which may choose a network voice. */
internal fun selectOfflineTtsVoice(
    voices: List<ReaderTtsVoice>,
    languageTag: String,
    preferredId: String? = null
): ReaderTtsVoice? {
    val language = Locale.forLanguageTag(languageTag)
    if (language.language.isBlank() || language.language == "und") return null
    val candidates = voices.filter { voice ->
        val locale = Locale.forLanguageTag(voice.languageTag)
        voice.installed && !voice.requiresNetwork && locale.language == language.language &&
            (language.script.isBlank() || locale.script.isBlank() || language.script == locale.script)
    }
    if (preferredId != null) {
        // A persisted explicit choice is a contract. Never silently swap it for another voice.
        return candidates.firstOrNull { it.id == preferredId }
    }
    return candidates.sortedWith(
        compareByDescending<ReaderTtsVoice> {
            it.languageTag.equals(language.toLanguageTag(), ignoreCase = true)
        }.thenByDescending { it.quality }.thenBy { it.id }
    ).firstOrNull()
}

/** A native synthesis request must fit Android's UTF-16 limit without splitting a surrogate pair. */
internal fun ttsChunkEnd(text: String, start: Int, maximum: Int): Int {
    require(maximum >= 2)
    if (start !in 0 until text.length) return text.length
    var end = (start.toLong() + maximum).coerceAtMost(text.length.toLong()).toInt()
    if (end < text.length && text[end - 1].isHighSurrogate() && text[end].isLowSurrogate()) end -= 1
    // Prefer a nearby word boundary, without turning a long token into an empty request.
    if (end < text.length) {
        val floor = start + (end - start) / 2
        for (index in end - 1 downTo floor) {
            if (text[index].isWhitespace()) return index + 1
        }
    }
    return end
}

internal enum class ReaderTtsProblem {
    UNSUPPORTED,
    NO_ENGINE,
    NO_OFFLINE_VOICE,
    PREFERRED_VOICE_UNAVAILABLE,
    AUDIO_FOCUS,
    SYNTHESIS,
    CONTENT,
    TIMEOUT
}
internal enum class ReaderTtsPhase { STOPPED, PREPARING, PLAYING, PAUSED, ENDED, FAILED, CLOSED }

internal fun readerCanPlayForegroundTts(
    readerReady: Boolean,
    resumed: Boolean,
    closeInFlight: Boolean,
    preferencesSettling: Boolean,
    selectionActive: Boolean,
    blockingOverlayVisible: Boolean,
    noteLocatorJson: String?,
    accessibilityActive: Boolean
): Boolean = readerReady && resumed && !closeInFlight && !preferencesSettling &&
    !selectionActive && !blockingOverlayVisible && noteLocatorJson == null && !accessibilityActive

/** A suspended visible-block lookup cannot confer speech ownership on a replacement Reader. */
internal fun readerCanCompleteTtsStart(
    expectedOwnerId: String,
    currentOwnerId: String,
    navigatorStillOwned: Boolean,
    controlsVisible: Boolean,
    playbackAllowed: Boolean,
    requestSerial: Int = 0,
    currentSerial: Int = 0
): Boolean = requestSerial == currentSerial && expectedOwnerId == currentOwnerId &&
    navigatorStillOwned && controlsVisible && playbackAllowed
