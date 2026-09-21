package com.veilreader.app.manga

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.veilreader.app.manga.core.MangaChapter
import com.veilreader.app.manga.core.MangaDetails
import com.veilreader.app.manga.core.MangaSourceCapability
import com.veilreader.app.manga.core.MangaSourceDescriptor
import com.veilreader.app.manga.core.MangaSummary
import com.veilreader.app.manga.core.MangaUpdate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MangaReaderTarget(
    val bookId: String,
    val chapterId: String
)

data class MangaHubUiState(
    val query: String = "",
    val searching: Boolean = false,
    val loadingDetails: Boolean = false,
    val openingChapter: Boolean = false,
    val results: List<MangaSummary> = emptyList(),
    val details: MangaDetails? = null,
    val chapters: List<MangaChapter> = emptyList(),
    val readerTarget: MangaReaderTarget? = null,
    val errorMessage: String? = null
)

class MangaHubViewModel(
    private val runtime: MangaAppRuntime
) : ViewModel() {
    private val _uiState = MutableStateFlow(MangaHubUiState())
    val uiState: StateFlow<MangaHubUiState> = _uiState.asStateFlow()

    private var selectedUpdate: MangaUpdate? = null
    private var selectedDescriptor: MangaSourceDescriptor? = null

    fun setQuery(value: String) {
        _uiState.value = _uiState.value.copy(query = value)
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                searching = true,
                details = null,
                chapters = emptyList(),
                errorMessage = null
            )
            selectedUpdate = null
            selectedDescriptor = null
            try {
                val source = runtime.catalog.snapshot().entries.firstOrNull { entry ->
                    entry.enabled && MangaSourceCapability.SEARCH in entry.descriptor.capabilities
                }?.descriptor ?: error("No enabled manga search source is available.")

                val page = runtime.hub.search(source.id, query)
                _uiState.value = _uiState.value.copy(
                    searching = false,
                    results = page.items,
                    errorMessage = null
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _uiState.value = _uiState.value.copy(
                    searching = false,
                    errorMessage = error.message ?: "Manga search failed."
                )
            }
        }
    }

    fun select(summary: MangaSummary) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                loadingDetails = true,
                details = null,
                chapters = emptyList(),
                errorMessage = null
            )
            try {
                val descriptor = runtime.catalog.snapshot().entries
                    .firstOrNull { it.descriptor.id == summary.ref.sourceId }
                    ?.descriptor
                    ?: error("The selected manga source is no longer registered.")
                val update = runtime.hub.update(summary.ref)
                val details = requireNotNull(update.details) {
                    "The manga source did not return title details."
                }
                val chapters = update.chapters.orEmpty()

                selectedUpdate = update
                selectedDescriptor = descriptor
                _uiState.value = _uiState.value.copy(
                    loadingDetails = false,
                    details = details,
                    chapters = chapters,
                    errorMessage = null
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                selectedUpdate = null
                selectedDescriptor = null
                _uiState.value = _uiState.value.copy(
                    loadingDetails = false,
                    errorMessage = error.message ?: "Could not load manga details."
                )
            }
        }
    }

    fun openChapter(chapter: MangaChapter) {
        if (_uiState.value.openingChapter) return
        val update = selectedUpdate ?: return
        val descriptor = selectedDescriptor ?: return
        if (chapter.ref.manga != update.ref) return

        _uiState.value = _uiState.value.copy(openingChapter = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val sync = runtime.library.importOrSyncSource(
                    descriptor = descriptor,
                    update = update,
                    makePreferred = true
                )
                val chapterId = requireNotNull(
                    runtime.library.chapterIdForSourceRef(chapter.ref)
                ) {
                    "The selected chapter could not be mapped into the local library."
                }
                _uiState.value = _uiState.value.copy(
                    readerTarget = MangaReaderTarget(
                        bookId = sync.bookId,
                        chapterId = chapterId
                    ),
                    openingChapter = false,
                    errorMessage = null
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _uiState.value = _uiState.value.copy(
                    openingChapter = false,
                    errorMessage = error.message ?: "Could not open this manga chapter."
                )
            }
        }
    }

    fun resumeBook(bookId: String) {
        viewModelScope.launch {
            try {
                val chapterId = requireNotNull(runtime.library.resumeReadableChapterId(bookId)) {
                    "This manga has no readable chapter yet."
                }
                _uiState.value = _uiState.value.copy(
                    readerTarget = MangaReaderTarget(bookId, chapterId),
                    errorMessage = null
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = error.message ?: "Could not resume this manga."
                )
            }
        }
    }

    fun consumeReaderTarget() {
        _uiState.value = _uiState.value.copy(readerTarget = null)
    }

    fun closeReader() {
        _uiState.value = _uiState.value.copy(readerTarget = null)
    }

    fun backToResults() {
        selectedUpdate = null
        selectedDescriptor = null
        _uiState.value = _uiState.value.copy(
            details = null,
            chapters = emptyList(),
            loadingDetails = false,
            errorMessage = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    companion object {
        fun factory(runtime: MangaAppRuntime): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(MangaHubViewModel::class.java))
                    return MangaHubViewModel(runtime) as T
                }
            }
    }
}
