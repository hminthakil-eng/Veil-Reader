package com.veilreader.app.manga.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.veilreader.app.data.MangaLibraryRepository
import com.veilreader.app.manga.core.MangaChapterRef
import com.veilreader.app.manga.core.MangaHub
import com.veilreader.app.manga.core.MangaOfflineStore
import com.veilreader.app.manga.core.MangaPage
import com.veilreader.app.manga.core.MangaSourceException
import java.util.LinkedHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class MangaReaderFailureKind {
    RATE_LIMITED,
    AUTH_REQUIRED,
    BLOCKED,
    NOT_FOUND,
    TEMPORARILY_UNAVAILABLE,
    NETWORK,
    SOURCE_CHANGED,
    PARSE,
    UNSUPPORTED,
    UNKNOWN
}

data class MangaReaderFailure(
    val kind: MangaReaderFailureKind,
    val message: String,
    val retryable: Boolean
)

data class MangaReaderUiState(
    val bookId: String? = null,
    val chapterId: String? = null,
    val pages: List<MangaPage> = emptyList(),
    val pageIndex: Int = 0,
    val preferences: MangaReaderPreferences = MangaReaderPreferences(),
    val previousChapterId: String? = null,
    val nextChapterId: String? = null,
    val loading: Boolean = false,
    val failure: MangaReaderFailure? = null
) {
    val totalPages: Int get() = pages.size
    val canGoPreviousChapter: Boolean get() = previousChapterId != null
    val canGoNextChapter: Boolean get() = nextChapterId != null
}

internal fun loadingMangaReaderState(
    current: MangaReaderUiState,
    bookId: String,
    chapterId: String,
    preferences: MangaReaderPreferences
): MangaReaderUiState {
    val switchingChapter = current.chapterId != chapterId
    return current.copy(
        bookId = bookId,
        chapterId = chapterId,
        pages = if (switchingChapter) emptyList() else current.pages,
        pageIndex = if (switchingChapter) 0 else current.pageIndex,
        preferences = preferences,
        previousChapterId = if (switchingChapter) null else current.previousChapterId,
        nextChapterId = if (switchingChapter) null else current.nextChapterId,
        loading = true,
        failure = null
    )
}

internal suspend fun resolveMangaChapterPages(
    ref: MangaChapterRef,
    cached: List<MangaPage>?,
    offlineStore: MangaOfflineStore?,
    remote: suspend () -> List<MangaPage>
): List<MangaPage> {
    cached?.let { return it }

    val offline = try {
        offlineStore?.loadChapter(ref)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Throwable) {
        null
    }
    return offline ?: remote()
}

class MangaReaderViewModel(
    private val mangaLibrary: MangaLibraryRepository,
    private val mangaHub: MangaHub,
    private val offlineStore: MangaOfflineStore? = null,
    private val preferencesPersistence: MangaReaderPreferencesPersistence? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(MangaReaderUiState())
    val uiState: StateFlow<MangaReaderUiState> = _uiState.asStateFlow()

    private val chapterPageCache = object : LinkedHashMap<String, List<MangaPage>>(
        MAX_CACHED_CHAPTERS,
        0.75f,
        true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, List<MangaPage>>
        ): Boolean = size > MAX_CACHED_CHAPTERS
    }

    private var loadJob: Job? = null
    private var progressJob: Job? = null
    private var preloadJob: Job? = null
    private var preferenceJob: Job? = null

    fun open(
        bookId: String,
        chapterId: String? = null,
        preferences: MangaReaderPreferences? = null
    ) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val resolvedPreferences = preferences ?: loadPersistedPreferences()
            val target = chapterId ?: mangaLibrary.resumeChapterId(bookId)
            if (target == null) {
                _uiState.value = MangaReaderUiState(
                    bookId = bookId,
                    preferences = resolvedPreferences,
                    failure = MangaReaderFailure(
                        kind = MangaReaderFailureKind.NOT_FOUND,
                        message = "No readable manga chapter is available.",
                        retryable = false
                    )
                )
                return@launch
            }
            loadChapter(bookId, target, resolvedPreferences, restorePage = true)
        }
    }

    private suspend fun loadPersistedPreferences(): MangaReaderPreferences {
        val persistence = preferencesPersistence ?: return _uiState.value.preferences
        return try {
            persistence.current()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            _uiState.value.preferences
        }
    }

    fun setPreferences(value: MangaReaderPreferences) {
        _uiState.value = _uiState.value.copy(preferences = value)
        val persistence = preferencesPersistence ?: return
        preferenceJob?.cancel()
        preferenceJob = viewModelScope.launch {
            try {
                persistence.save(value)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                // Preferences stay usable in-memory even if a local persistence write fails.
            }
        }
    }

    fun onPageSettled(pageIndex: Int) {
        val state = _uiState.value
        val bookId = state.bookId ?: return
        val chapterId = state.chapterId ?: return
        if (state.pages.isEmpty()) return

        val safePage = pageIndex.coerceIn(0, state.pages.lastIndex)
        if (safePage != state.pageIndex) {
            _uiState.value = state.copy(pageIndex = safePage)
        }

        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            delay(PROGRESS_DEBOUNCE_MS)
            mangaLibrary.saveReadingProgress(
                bookId = bookId,
                chapterId = chapterId,
                pageIndex = safePage,
                pageCount = state.pages.size
            )
        }

        if (
            state.nextChapterId != null &&
            shouldPreloadNextChapter(safePage, state.pages.size)
        ) {
            preloadNextChapter(state.nextChapterId)
        }
    }

    fun goToPreviousChapter() {
        navigateToAdjacentChapter(previous = true)
    }

    fun goToNextChapter() {
        navigateToAdjacentChapter(previous = false)
    }

    private fun navigateToAdjacentChapter(previous: Boolean) {
        val state = _uiState.value
        if (state.loading) return
        val bookId = state.bookId ?: return
        val target = (
            if (previous) state.previousChapterId else state.nextChapterId
        ) ?: return

        progressJob?.cancel()
        _uiState.value = loadingMangaReaderState(
            current = state,
            bookId = bookId,
            chapterId = target,
            preferences = state.preferences
        )
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            persistProgress(state)
            loadChapter(
                bookId = bookId,
                chapterId = target,
                preferences = state.preferences,
                restorePage = true
            )
        }
    }

    fun flushProgress() {
        val state = _uiState.value
        if (state.bookId == null || state.chapterId == null || state.pages.isEmpty()) return

        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            persistProgress(state)
        }
    }

    private suspend fun persistProgress(state: MangaReaderUiState) {
        val bookId = state.bookId ?: return
        val chapterId = state.chapterId ?: return
        if (state.pages.isEmpty()) return
        mangaLibrary.saveReadingProgress(
            bookId = bookId,
            chapterId = chapterId,
            pageIndex = state.pageIndex,
            pageCount = state.pages.size
        )
    }

    fun retry() {
        val state = _uiState.value
        val bookId = state.bookId ?: return
        val chapterId = state.chapterId ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            loadChapter(
                bookId = bookId,
                chapterId = chapterId,
                preferences = state.preferences,
                restorePage = true
            )
        }
    }

    private suspend fun loadChapter(
        bookId: String,
        chapterId: String,
        preferences: MangaReaderPreferences,
        restorePage: Boolean
    ) {
        _uiState.value = loadingMangaReaderState(
            current = _uiState.value,
            bookId = bookId,
            chapterId = chapterId,
            preferences = preferences
        )

        try {
            val window = mangaLibrary.chapterWindow(bookId, chapterId)
            val cachedPages = synchronized(chapterPageCache) {
                chapterPageCache[chapterId]
            }
            val sourceRef = requireNotNull(mangaLibrary.chapterRefForReading(chapterId)) {
                "No source binding is available for this manga chapter."
            }
            val pages = resolveMangaChapterPages(
                ref = sourceRef,
                cached = cachedPages,
                offlineStore = offlineStore
            ) {
                mangaHub.pages(sourceRef)
            }.also { loaded ->
                synchronized(chapterPageCache) {
                    chapterPageCache[chapterId] = loaded
                }
            }

            val restoredPage = if (restorePage) {
                mangaLibrary.chapterLastPageIndex(chapterId)
            } else {
                0
            }
            val safePage = if (pages.isEmpty()) 0 else restoredPage.coerceIn(0, pages.lastIndex)

            _uiState.value = MangaReaderUiState(
                bookId = bookId,
                chapterId = chapterId,
                pages = pages,
                pageIndex = safePage,
                preferences = preferences,
                previousChapterId = window.previousChapterId,
                nextChapterId = window.nextChapterId,
                loading = false,
                failure = null
            )

            if (
                window.nextChapterId != null &&
                shouldPreloadNextChapter(safePage, pages.size)
            ) {
                preloadNextChapter(window.nextChapterId)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            _uiState.value = _uiState.value.copy(
                loading = false,
                failure = error.toReaderFailure()
            )
        }
    }

    private fun preloadNextChapter(chapterId: String) {
        synchronized(chapterPageCache) {
            if (chapterPageCache.containsKey(chapterId)) return
        }
        if (preloadJob?.isActive == true) return

        preloadJob = viewModelScope.launch {
            try {
                val sourceRef = mangaLibrary.chapterRefForReading(chapterId) ?: return@launch
                val pages = resolveMangaChapterPages(
                    ref = sourceRef,
                    cached = null,
                    offlineStore = offlineStore
                ) {
                    mangaHub.pages(sourceRef)
                }
                synchronized(chapterPageCache) {
                    chapterPageCache[chapterId] = pages
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                // Preloading is opportunistic. The real chapter load owns user-visible failures.
            }
        }
    }

    private fun Throwable.toReaderFailure(): MangaReaderFailure = when (this) {
        is MangaSourceException.RateLimited -> MangaReaderFailure(
            MangaReaderFailureKind.RATE_LIMITED,
            message ?: "Source rate limit reached.",
            retryable = true
        )
        is MangaSourceException.AuthRequired -> MangaReaderFailure(
            MangaReaderFailureKind.AUTH_REQUIRED,
            message ?: "Source authentication is required.",
            retryable = false
        )
        is MangaSourceException.Blocked -> MangaReaderFailure(
            MangaReaderFailureKind.BLOCKED,
            message ?: "Source request was blocked.",
            retryable = false
        )
        is MangaSourceException.NotFound -> MangaReaderFailure(
            MangaReaderFailureKind.NOT_FOUND,
            message ?: "Chapter was not found.",
            retryable = false
        )
        is MangaSourceException.TemporarilyUnavailable -> MangaReaderFailure(
            MangaReaderFailureKind.TEMPORARILY_UNAVAILABLE,
            message ?: "Source is temporarily unavailable.",
            retryable = true
        )
        is MangaSourceException.NetworkFailure -> MangaReaderFailure(
            MangaReaderFailureKind.NETWORK,
            message ?: "Network request failed.",
            retryable = true
        )
        is MangaSourceException.SourceChanged -> MangaReaderFailure(
            MangaReaderFailureKind.SOURCE_CHANGED,
            message ?: "Source structure changed.",
            retryable = false
        )
        is MangaSourceException.ParseFailure -> MangaReaderFailure(
            MangaReaderFailureKind.PARSE,
            message ?: "Source response could not be parsed.",
            retryable = false
        )
        is MangaSourceException.Unsupported -> MangaReaderFailure(
            MangaReaderFailureKind.UNSUPPORTED,
            message ?: "Reader operation is unsupported.",
            retryable = false
        )
        else -> MangaReaderFailure(
            MangaReaderFailureKind.UNKNOWN,
            message ?: "Unexpected manga reader error.",
            retryable = true
        )
    }

    companion object {
        private const val MAX_CACHED_CHAPTERS = 2
        private const val PROGRESS_DEBOUNCE_MS = 200L

        fun factory(
            mangaLibrary: MangaLibraryRepository,
            mangaHub: MangaHub,
            offlineStore: MangaOfflineStore? = null,
            preferencesPersistence: MangaReaderPreferencesPersistence? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(MangaReaderViewModel::class.java))
                return MangaReaderViewModel(
                    mangaLibrary = mangaLibrary,
                    mangaHub = mangaHub,
                    offlineStore = offlineStore,
                    preferencesPersistence = preferencesPersistence
                ) as T
            }
        }
    }
}
