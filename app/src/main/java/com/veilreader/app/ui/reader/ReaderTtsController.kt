package com.veilreader.app.ui.reader

import android.app.Application
import com.veilreader.app.domain.ReaderTtsSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.readium.navigator.media.tts.AndroidTtsNavigator
import org.readium.navigator.media.tts.AndroidTtsNavigatorFactory
import org.readium.navigator.media.tts.TtsNavigator
import org.readium.navigator.media.tts.android.AndroidTtsPreferences
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.getOrElse

internal enum class ReaderTtsError {
    UNSUPPORTED_PUBLICATION,
    INITIALIZATION,
    PLAYBACK
}

internal data class ReaderTtsUiState(
    val supported: Boolean,
    val starting: Boolean = false,
    val active: Boolean = false,
    val playing: Boolean = false,
    val utterance: String = "",
    val error: ReaderTtsError? = null
)

@OptIn(ExperimentalReadiumApi::class)
internal class ReaderTtsController(
    application: Application,
    publication: Publication,
    private val scope: CoroutineScope
) {
    private val factory = AndroidTtsNavigatorFactory(application, publication)
    private var navigator: AndroidTtsNavigator? = null
    private var startJob: Job? = null
    private var playbackJob: Job? = null
    private var locationJob: Job? = null

    private val _state = MutableStateFlow(
        ReaderTtsUiState(supported = factory != null)
    )
    val state: StateFlow<ReaderTtsUiState> = _state.asStateFlow()

    private val _utteranceLocator = MutableStateFlow<Locator?>(null)
    val utteranceLocator: StateFlow<Locator?> = _utteranceLocator.asStateFlow()

    fun start(
        visualNavigator: Navigator?,
        settings: ReaderTtsSettings
    ) {
        if (navigator != null) {
            navigator?.play()
            return
        }
        if (startJob != null) return

        val navigatorFactory = factory
        if (navigatorFactory == null) {
            _state.value = _state.value.copy(
                supported = false,
                error = ReaderTtsError.UNSUPPORTED_PUBLICATION
            )
            return
        }

        startJob = scope.launch {
            _state.value = _state.value.copy(
                starting = true,
                error = null
            )
            try {
                val initialLocator = try {
                    (visualNavigator as? VisualNavigator)
                        ?.firstVisibleElementLocator()
                } catch (_: Exception) {
                    null
                }

                val normalized = settings.normalized()
                val created = navigatorFactory.createNavigator(
                    listener = object : TtsNavigator.Listener {
                        override fun onStopRequested() {
                            stop()
                        }
                    },
                    initialLocator = initialLocator,
                    initialPreferences = AndroidTtsPreferences(
                        speed = normalized.speed,
                        pitch = normalized.pitch
                    )
                ).getOrElse {
                    _state.value = _state.value.copy(
                        starting = false,
                        active = false,
                        playing = false,
                        error = ReaderTtsError.INITIALIZATION
                    )
                    return@launch
                }

                navigator = created
                bind(created)
                _state.value = _state.value.copy(
                    starting = false,
                    active = true,
                    error = null
                )
                created.play()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.value = _state.value.copy(
                    starting = false,
                    active = false,
                    playing = false,
                    error = ReaderTtsError.INITIALIZATION
                )
            } finally {
                startJob = null
            }
        }
    }

    fun play() {
        navigator?.play()
    }

    fun pause() {
        navigator?.pause()
    }

    fun previous() {
        navigator?.skipToPreviousUtterance()
    }

    fun next() {
        navigator?.skipToNextUtterance()
    }

    fun submitSettings(settings: ReaderTtsSettings) {
        val normalized = settings.normalized()
        navigator?.submitPreferences(
            AndroidTtsPreferences(
                speed = normalized.speed,
                pitch = normalized.pitch
            )
        )
    }

    fun stop() {
        startJob?.cancel()
        startJob = null
        playbackJob?.cancel()
        playbackJob = null
        locationJob?.cancel()
        locationJob = null
        navigator?.close()
        navigator = null
        _utteranceLocator.value = null
        _state.value = ReaderTtsUiState(
            supported = factory != null
        )
    }

    fun close() {
        stop()
    }

    private fun bind(ttsNavigator: AndroidTtsNavigator) {
        playbackJob?.cancel()
        playbackJob = ttsNavigator.playback
            .onEach { playback ->
                val playbackError =
                    playback.state is TtsNavigator.State.Failure
                _state.value = _state.value.copy(
                    active = true,
                    playing = playback.playWhenReady,
                    error = if (playbackError) {
                        ReaderTtsError.PLAYBACK
                    } else {
                        _state.value.error
                    }
                )

                if (playback.state == TtsNavigator.State.Ended) {
                    stop()
                }
            }
            .launchIn(scope)

        locationJob?.cancel()
        locationJob = ttsNavigator.location
            .onEach { location ->
                _utteranceLocator.value = location.utteranceLocator
                _state.value = _state.value.copy(
                    utterance = location.utterance
                )
            }
            .launchIn(scope)
    }
}
