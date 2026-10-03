package com.veilreader.app.ui.reader.tts

import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.readium.r2.shared.publication.Locator

internal data class ReaderTtsState(
    val phase: ReaderTtsPhase = ReaderTtsPhase.STOPPED,
    val problem: ReaderTtsProblem? = null,
    val sourceLocator: Locator? = null
)

/**
 * One Reader owner's foreground speech session. Speech locators are Readium content locators;
 * this object has no library/progress writer and never drives the visual Navigator implicitly.
 * All control methods are called on the main dispatcher by the Reader owner.
 */
internal class ReaderTtsSession(
    private val contentFactory: (Locator) -> ReaderTtsContent?,
    private val backendFactory: () -> ReaderTtsBackend,
    private val publicationLanguage: String?,
    private val canPlay: () -> Boolean,
    dispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.Main.immediate,
    private val initializationTimeoutMs: Long = 5_000L,
    private val utteranceTimeoutMs: Long = 600_000L
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val mutableState = MutableStateFlow(ReaderTtsState())
    val state = mutableState.asStateFlow()
    private var backend: ReaderTtsBackend? = null
    private var content: ReaderTtsContent? = null
    private var current: ReaderTtsUtterance? = null
    private var preferences = ReaderTtsPreferences()
    private var playJob: Job? = null
    private var monitorJob: Job? = null
    private var serial = 0L
    private var closed = false

    fun start(locator: Locator, requestedPreferences: ReaderTtsPreferences = ReaderTtsPreferences()) {
        if (closed || !canPlay()) return
        stop()
        preferences = requestedPreferences.normalized()
        content = try { contentFactory(locator) } catch (_: Exception) {
            mutableState.value = ReaderTtsState(ReaderTtsPhase.FAILED, ReaderTtsProblem.CONTENT)
            return
        }
        if (content == null) {
            mutableState.value = ReaderTtsState(ReaderTtsPhase.FAILED, ReaderTtsProblem.UNSUPPORTED)
            return
        }
        resume()
    }

    fun resume() {
        if (closed || playJob?.isActive == true || content == null || !canPlay()) return
        val ownerSerial = ++serial
        mutableState.value = mutableState.value.copy(phase = ReaderTtsPhase.PREPARING, problem = null)
        playJob = scope.launch {
            try {
                val first = current ?: content?.next()
                if (ownerSerial != serial || closed) return@launch
                if (first == null) { fail(ReaderTtsProblem.UNSUPPORTED); return@launch }
                current = first
                val engine = backend ?: backendFactory().also {
                    backend = it
                    it.onInterruption = ::pause
                }
                val problem = withTimeout(initializationTimeoutMs.coerceAtLeast(1L)) { engine.initialize() }
                if (problem != null) { fail(problem); return@launch }
                while (ownerSerial == serial && !closed) {
                    if (!canPlay()) { pause(); return@launch }
                    val utterance = current ?: content?.next()
                    if (ownerSerial != serial || closed) return@launch
                    current = utterance
                    if (utterance == null) {
                        engine.stop()
                        mutableState.value = mutableState.value.copy(phase = ReaderTtsPhase.ENDED)
                        return@launch
                    }
                    if (!canPlay()) { pause(); return@launch }
                    val language = preferences.languageTag ?: utterance.languageTag ?: publicationLanguage
                        ?: Locale.getDefault().toLanguageTag()
                    mutableState.value = ReaderTtsState(ReaderTtsPhase.PLAYING, sourceLocator = utterance.locator)
                    val result = withTimeout(utteranceTimeoutMs.coerceAtLeast(1L)) {
                        engine.speak(utterance.text, language, preferences)
                    }
                    if (ownerSerial != serial || closed) return@launch
                    if (result != null) { fail(result); return@launch }
                    current = null
                }
            } catch (_: TimeoutCancellationException) {
                if (ownerSerial == serial && !closed) fail(ReaderTtsProblem.TIMEOUT)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (ownerSerial == serial && !closed) fail(ReaderTtsProblem.CONTENT)
            } finally {
                if (ownerSerial == serial) playJob = null
            }
        }
        // This monitor also covers spoken accessibility changes on API 26–32, which do not expose
        // a services-state listener. It owns no frame loop and only runs during active speech.
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (ownerSerial == serial && !closed && playJob?.isActive == true) {
                delay(500)
                if (!canPlay()) { pause(); return@launch }
            }
        }
    }

    fun pause() {
        if (closed) return
        serial += 1
        monitorJob?.cancel()
        monitorJob = null
        playJob?.cancel()
        playJob = null
        backend?.stop()
        if (mutableState.value.phase == ReaderTtsPhase.PLAYING || mutableState.value.phase == ReaderTtsPhase.PREPARING) {
            mutableState.value = mutableState.value.copy(phase = ReaderTtsPhase.PAUSED)
        }
    }

    fun stop() {
        if (closed) return
        pause()
        content = null
        current = null
        mutableState.value = ReaderTtsState()
    }

    fun close() {
        if (closed) return
        stop()
        closed = true
        serial += 1
        scope.cancel()
        backend?.onInterruption = null
        backend?.close()
        backend = null
        mutableState.value = ReaderTtsState(ReaderTtsPhase.CLOSED)
    }

    suspend fun awaitClosed() {
        scope.coroutineContext[Job]?.join()
    }

    private fun fail(problem: ReaderTtsProblem) {
        backend?.stop()
        mutableState.value = mutableState.value.copy(phase = ReaderTtsPhase.FAILED, problem = problem)
    }
}
