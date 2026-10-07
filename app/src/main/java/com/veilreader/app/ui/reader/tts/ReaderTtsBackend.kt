package com.veilreader.app.ui.reader.tts

/** Why speech stopped. Transient focus loss may auto-resume; noisy/permanent loss must not. */
internal enum class ReaderTtsInterruption {
    TRANSIENT_FOCUS,
    PERMANENT_FOCUS,
    BECOMING_NOISY
}

/** Backend owns synthesis only. It never owns publication parsing or durable Reader position. */
internal interface ReaderTtsBackend {
    val voices: List<ReaderTtsVoice>
    var onInterruption: ((ReaderTtsInterruption) -> Unit)?
    var onFocusGained: (() -> Unit)?
    suspend fun initialize(): ReaderTtsProblem?
    suspend fun speak(text: String, languageTag: String, preferences: ReaderTtsPreferences): ReaderTtsProblem?

    /**
     * Stops the active synthesis request.
     *
     * For a transient audio-focus interruption the focus request must stay registered so Android
     * can deliver AUDIOFOCUS_GAIN and Veil can resume exactly the interrupted semantic segment.
     * User pause, noisy-route loss and permanent focus loss abandon focus.
     */
    fun stop(abandonAudioFocus: Boolean = true)
    fun close()
}
