package com.veilreader.app.ui.reader.tts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import org.readium.r2.shared.publication.Locator

/** Durable listening position, independent of visual progress and never an autoplay request. */
internal class ReaderForegroundTtsCheckpointController(
    private val bookId: String,
    private val read: suspend () -> ReaderTtsCheckpoint?,
    private val save: suspend (ReaderTtsCheckpoint) -> Unit,
    private val nowEpochMs: () -> Long = System::currentTimeMillis
) {
    private val mutableCheckpoint = MutableStateFlow<ReaderTtsCheckpoint?>(null)
    val checkpoint = mutableCheckpoint.asStateFlow()

    suspend fun commit(locator: Locator, preferences: ReaderTtsPreferences) {
        val json = locator.toJSON().apply { remove("text") }.toString()
        val request = requireNotNull(ReaderTtsPlaybackRequest(bookId, json, preferences).normalized())
        val value = ReaderTtsCheckpoint(request, json, ReaderTtsPhase.PAUSED, nowEpochMs())
        save(value)
        mutableCheckpoint.value = value
    }

    suspend fun restore(
        session: ReaderTtsSession,
        preferences: ReaderTtsPreferences,
        isCurrent: () -> Boolean
    ): Boolean {
        val generation = session.sourceGeneration
        val saved = read()?.takeIf { it.request.bookId == bookId } ?: return false
        if (!isCurrent() || session.sourceGeneration != generation ||
            session.state.value.phase != ReaderTtsPhase.STOPPED
        ) return false
        val locator = Locator.fromJSON(JSONObject(saved.locatorJson)) ?: return false
        session.load(locator, preferences, autoplay = false)
        if (session.state.value.phase != ReaderTtsPhase.PAUSED) return false
        mutableCheckpoint.value = saved
        return true
    }
}
