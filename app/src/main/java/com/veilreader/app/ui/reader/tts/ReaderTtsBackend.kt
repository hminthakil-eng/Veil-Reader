package com.veilreader.app.ui.reader.tts

/** Backend owns synthesis only. It never owns publication parsing or durable Reader position. */
internal interface ReaderTtsBackend {
    val voices: List<ReaderTtsVoice>
    var onInterruption: (() -> Unit)?
    suspend fun initialize(): ReaderTtsProblem?
    suspend fun speak(text: String, languageTag: String, preferences: ReaderTtsPreferences): ReaderTtsProblem?
    fun stop()
    fun close()
}
