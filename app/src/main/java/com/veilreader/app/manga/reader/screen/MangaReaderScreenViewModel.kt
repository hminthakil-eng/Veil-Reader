package com.veilreader.app.manga.reader.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.veilreader.app.manga.library.MangaProgressStore
import com.veilreader.app.manga.library.MangaReadingProgress
import com.veilreader.app.manga.reader.MangaReaderController
import com.veilreader.app.manga.reader.MangaReaderPosition
import com.veilreader.app.manga.reader.MangaReaderState
import com.veilreader.app.manga.reader.presentation.MangaChapterNavigationResolver
import com.veilreader.app.manga.reader.presentation.MangaChapterPresentationLoader
import com.veilreader.app.manga.reader.presentation.MangaReaderPresentationCoordinator
import com.veilreader.app.manga.reader.presentation.MangaReaderPresentationEffect
import com.veilreader.app.manga.reader.presentation.MangaReaderPresentationState
import com.veilreader.app.manga.reader.ui.MangaReaderSavedStateHandleAdapter
import com.veilreader.app.manga.reader.ui.MangaReaderUiEffect
import com.veilreader.app.manga.reader.ui.MangaReaderUiIntent
import com.veilreader.app.manga.reader.ui.MangaReaderUiReducer
import com.veilreader.app.manga.reader.ui.MangaReaderUiState
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MangaReaderScreenMessage {
    data object PartialOfflineBoundary : MangaReaderScreenMessage
    data object SeriesBoundary : MangaReaderScreenMessage
    data object ChapterRouteUnavailable : MangaReaderScreenMessage
}

data class MangaReaderScreenState(
    val entry: MangaReaderChapterEntry,
    val readerUi: MangaReaderUiState,
    val presentation: MangaReaderPresentationState,
    val message: MangaReaderScreenMessage? = null
)

class MangaReaderScreenViewModel(
    savedStateHandle: SavedStateHandle,
    private val session: MangaReaderSession,
    private val loader: MangaChapterPresentationLoader,
    private val progressStore: MangaProgressStore,
    private val controller: MangaReaderController = MangaReaderController(),
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val savedState = MangaReaderSavedStateHandleAdapter(savedStateHandle)
    private val reducer = MangaReaderUiReducer(controller)
    private val presentationCoordinator = MangaReaderPresentationCoordinator(
        MangaChapterNavigationResolver(session.routes)
    )
    private val generation = AtomicLong(0L)

    private var loadJob: Job? = null
    private var progressSaveJob: Job? = null

    private val processSnapshot = savedState.load()
    private var currentEntry = processSnapshot
        ?.let(session::entryForChapter)
        ?: session.initialEntry

    private val initialReader = processSnapshot
        ?.takeIf { currentEntry.route.readerChapter.sameLogicalChapter(it.chapter) }
        ?.let {
            controller.restore(
                snapshot = it,
                currentChapter = currentEntry.route.readerChapter,
                currentPageCount = null
            )
        }
        ?: controller.initial(
            chapter = currentEntry.route.readerChapter,
            mode = session.options.mode,
            direction = session.options.direction,
            orientationPolicy = session.options.orientationPolicy
        )

    private val _state = MutableStateFlow(
        MangaReaderScreenState(
            entry = currentEntry,
            readerUi = MangaReaderUiState(initialReader),
            presentation = MangaReaderPresentationState.Loading(currentEntry.route.readerChapter)
        )
    )
    val state: StateFlow<MangaReaderScreenState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (processSnapshot == null) {
                restorePersistentProgress()
            }
            startLoad()
        }
    }

    fun onIntent(intent: MangaReaderUiIntent) {
        val current = _state.value
        val reduction = reducer.reduce(current.readerUi, intent)
        _state.value = current.copy(readerUi = reduction.state)

        reduction.effects.forEach { effect ->
            when (effect) {
                is MangaReaderUiEffect.SnapshotChanged -> {
                    savedState.save(effect.snapshot)
                    scheduleProgressSave(reduction.state.reader)
                }

                is MangaReaderUiEffect.ChapterBoundaryRequested ->
                    handleBoundary(effect)
            }
        }
    }

    fun retry() {
        startLoad()
    }

    fun dismissMessage() {
        _state.value = _state.value.copy(message = null)
    }

    fun onBackgrounded() {
        val reader = _state.value.readerUi.reader
        progressSaveJob?.cancel()
        progressSaveJob = viewModelScope.launch {
            progressStore.save(toProgress(reader))
        }
        savedState.save(controller.snapshot(reader))
    }

    private suspend fun restorePersistentProgress() {
        val progress = progressStore.load(session.mangaId) ?: return
        val progressChapter = com.veilreader.app.manga.reader.MangaReaderChapterRef(
            mangaId = progress.mangaId,
            anchor = progress.chapter
        )
        val entry = session.entryForChapter(progressChapter) ?: return

        currentEntry = entry
        val reader = controller.initial(
            chapter = entry.route.readerChapter,
            mode = session.options.mode,
            direction = session.options.direction,
            orientationPolicy = session.options.orientationPolicy,
            pageCount = progress.pageCount,
            initialItemIndex = progress.pageIndex
        )
        _state.value = MangaReaderScreenState(
            entry = entry,
            readerUi = MangaReaderUiState(reader),
            presentation = MangaReaderPresentationState.Loading(entry.route.readerChapter)
        )
        savedState.save(controller.snapshot(reader))
    }

    private fun startLoad() {
        loadJob?.cancel()
        val token = generation.incrementAndGet()
        val entry = currentEntry

        _state.value = _state.value.copy(
            entry = entry,
            presentation = MangaReaderPresentationState.Loading(entry.route.readerChapter),
            message = null
        )

        loadJob = viewModelScope.launch {
            val result = try {
                loader.load(entry.request())
            } catch (cancelled: CancellationException) {
                throw cancelled
            }

            if (
                generation.get() != token ||
                !sameEntry(currentEntry, entry)
            ) {
                return@launch
            }

            when (result) {
                is MangaReaderPresentationState.Ready -> {
                    val current = _state.value
                    val reduction = reducer.reduce(
                        current.readerUi,
                        MangaReaderUiIntent.PageCountResolved(result.value.pageCount)
                    )
                    _state.value = current.copy(
                        readerUi = reduction.state,
                        presentation = result
                    )
                    reduction.effects.forEach { effect ->
                        if (effect is MangaReaderUiEffect.SnapshotChanged) {
                            savedState.save(effect.snapshot)
                            scheduleProgressSave(reduction.state.reader)
                        }
                    }
                }

                is MangaReaderPresentationState.Error,
                is MangaReaderPresentationState.Loading -> {
                    _state.value = _state.value.copy(presentation = result)
                }
            }
        }
    }

    private fun handleBoundary(effect: MangaReaderUiEffect.ChapterBoundaryRequested) {
        val ready = (_state.value.presentation as? MangaReaderPresentationState.Ready)
            ?.value
            ?: return

        val presentationEffects = presentationCoordinator.onUiEffect(ready, effect)
        presentationEffects.forEach { presentationEffect ->
            when (presentationEffect) {
                is MangaReaderPresentationEffect.NavigateToChapter -> {
                    val entry = session.entryForRoute(presentationEffect.route)
                    if (entry == null) {
                        _state.value = _state.value.copy(
                            message = MangaReaderScreenMessage.ChapterRouteUnavailable
                        )
                    } else {
                        navigateTo(entry)
                    }
                }

                MangaReaderPresentationEffect.PartialOfflineBoundaryBlocked ->
                    _state.value = _state.value.copy(
                        message = MangaReaderScreenMessage.PartialOfflineBoundary
                    )

                MangaReaderPresentationEffect.SeriesBoundaryReached ->
                    _state.value = _state.value.copy(
                        message = MangaReaderScreenMessage.SeriesBoundary
                    )

                MangaReaderPresentationEffect.ChapterRouteUnavailable ->
                    _state.value = _state.value.copy(
                        message = MangaReaderScreenMessage.ChapterRouteUnavailable
                    )
            }
        }
    }

    private fun navigateTo(entry: MangaReaderChapterEntry) {
        if (sameEntry(currentEntry, entry)) return

        val previousReader = _state.value.readerUi.reader
        progressSaveJob?.cancel()
        viewModelScope.launch {
            progressStore.save(toProgress(previousReader))
        }

        loadJob?.cancel()
        generation.incrementAndGet()
        currentEntry = entry

        val nextReader = controller.initial(
            chapter = entry.route.readerChapter,
            mode = previousReader.mode,
            direction = previousReader.direction,
            orientationPolicy = previousReader.orientationPolicy
        )
        savedState.save(controller.snapshot(nextReader))

        _state.value = MangaReaderScreenState(
            entry = entry,
            readerUi = MangaReaderUiState(nextReader),
            presentation = MangaReaderPresentationState.Loading(entry.route.readerChapter)
        )
        startLoad()
    }

    private fun scheduleProgressSave(reader: MangaReaderState) {
        progressSaveJob?.cancel()
        val progress = toProgress(reader)
        progressSaveJob = viewModelScope.launch {
            delay(PROGRESS_SAVE_DEBOUNCE_MS)
            progressStore.save(progress)
        }
    }

    private fun toProgress(reader: MangaReaderState): MangaReadingProgress {
        val pageCount = reader.pageCount
        val index = reader.position.itemIndex
        val progression = when {
            pageCount == null || pageCount <= 1 -> 0.0
            reader.position is MangaReaderPosition.Webtoon -> {
                val position = reader.position
                ((position.itemIndex + position.offsetFraction) / pageCount.toDouble())
                    .coerceIn(0.0, 1.0)
            }
            else -> (index.toDouble() / (pageCount - 1).toDouble())
                .coerceIn(0.0, 1.0)
        }

        return MangaReadingProgress(
            mangaId = reader.chapter.mangaId,
            chapter = reader.chapter.anchor,
            pageIndex = index,
            pageCount = pageCount,
            chapterProgression = progression,
            updatedAtEpochMs = clock()
        )
    }

    private fun sameEntry(
        left: MangaReaderChapterEntry,
        right: MangaReaderChapterEntry
    ): Boolean =
        left.route.sourceChapter.sourceId == right.route.sourceChapter.sourceId &&
            left.route.sourceChapter.chapterKey == right.route.sourceChapter.chapterKey

    companion object {
        private const val PROGRESS_SAVE_DEBOUNCE_MS = 300L

        fun factory(
            session: MangaReaderSession,
            loader: MangaChapterPresentationLoader,
            progressStore: MangaProgressStore,
            controller: MangaReaderController = MangaReaderController()
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MangaReaderScreenViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    session = session,
                    loader = loader,
                    progressStore = progressStore,
                    controller = controller
                )
            }
        }
    }
}
