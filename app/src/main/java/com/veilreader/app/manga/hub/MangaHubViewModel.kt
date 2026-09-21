package com.veilreader.app.manga.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.veilreader.app.manga.library.CanonicalManga
import com.veilreader.app.manga.reader.screen.MangaReaderSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class MangaHubTab {
    DISCOVER,
    SEARCH,
    LIBRARY
}

data class MangaHubUiState(
    val tab: MangaHubTab = MangaHubTab.DISCOVER,
    val query: String = "",
    val discoverItems: List<MangaHubCatalogItem> = emptyList(),
    val searchItems: List<MangaHubCatalogItem> = emptyList(),
    val libraryWorks: List<CanonicalManga> = emptyList(),
    val selectedDetails: MangaHubWorkDetails? = null,
    val activeReaderSession: MangaReaderSession? = null,
    val discoverLoading: Boolean = false,
    val searchLoading: Boolean = false,
    val libraryLoading: Boolean = false,
    val detailsLoading: Boolean = false,
    val readerLoading: Boolean = false,
    val errorMessage: String? = null,
    val sourceIssueCount: Int = 0
)

class MangaHubViewModel(
    private val service: MangaHubCatalogService
) : ViewModel() {

    private val _state = kotlinx.coroutines.flow.MutableStateFlow(MangaHubUiState())
    val state: kotlinx.coroutines.flow.StateFlow<MangaHubUiState> =
        _state.asStateFlow()

    private var discoverJob: Job? = null
    private var searchJob: Job? = null
    private var detailsJob: Job? = null
    private var readerJob: Job? = null
    private var searchGeneration: Long = 0L

    init {
        refreshDiscover()
        refreshLibrary()
    }

    fun selectTab(tab: MangaHubTab) {
        _state.value = _state.value.copy(
            tab = tab,
            selectedDetails = null,
            errorMessage = null
        )
        if (tab == MangaHubTab.LIBRARY) refreshLibrary()
    }

    fun updateQuery(query: String) {
        _state.value = _state.value.copy(query = query)
    }

    fun refreshDiscover() {
        discoverJob?.cancel()
        discoverJob = viewModelScope.launch {
            _state.value = _state.value.copy(discoverLoading = true)
            try {
                val result = service.discover()
                _state.value = _state.value.copy(
                    discoverItems = result.items,
                    discoverLoading = false,
                    sourceIssueCount = result.issues.size
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.value = _state.value.copy(
                    discoverLoading = false,
                    errorMessage = error.message ?: "Discover could not be refreshed."
                )
            }
        }
    }

    fun search() {
        val query = _state.value.query.trim()
        if (query.isEmpty()) {
            _state.value = _state.value.copy(
                searchItems = emptyList(),
                errorMessage = null
            )
            return
        }

        searchJob?.cancel()
        val generation = ++searchGeneration
        searchJob = viewModelScope.launch {
            _state.value = _state.value.copy(
                searchLoading = true,
                errorMessage = null
            )
            try {
                val result = service.search(query)
                if (generation != searchGeneration) return@launch
                _state.value = _state.value.copy(
                    searchItems = result.items,
                    searchLoading = false,
                    sourceIssueCount = result.issues.size
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation != searchGeneration) return@launch
                _state.value = _state.value.copy(
                    searchLoading = false,
                    errorMessage = error.message ?: "Search failed."
                )
            }
        }
    }

    fun openCatalogItem(item: MangaHubCatalogItem) {
        loadDetails {
            service.loadDetails(item.summary.ref)
        }
    }

    fun openLibraryWork(work: CanonicalManga) {
        loadDetails {
            service.loadLibraryDetails(work.id)
        }
    }

    fun addSelectedToLibrary() {
        val selected = _state.value.selectedDetails ?: return
        detailsJob?.cancel()
        detailsJob = viewModelScope.launch {
            _state.value = _state.value.copy(detailsLoading = true)
            try {
                val canonical = service.addToLibrary(selected.details)
                _state.value = _state.value.copy(
                    selectedDetails = selected.copy(canonical = canonical),
                    detailsLoading = false
                )
                refreshLibrary()
                refreshCatalogLibraryFlags()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.value = _state.value.copy(
                    detailsLoading = false,
                    errorMessage = error.message ?: "Could not add this Manga to the library."
                )
            }
        }
    }

    fun removeSelectedFromLibrary() {
        val selected = _state.value.selectedDetails ?: return
        val canonical = selected.canonical ?: return
        detailsJob?.cancel()
        detailsJob = viewModelScope.launch {
            _state.value = _state.value.copy(detailsLoading = true)
            try {
                service.removeFromLibrary(canonical.id)
                _state.value = _state.value.copy(
                    selectedDetails = selected.copy(canonical = null),
                    detailsLoading = false
                )
                refreshLibrary()
                refreshCatalogLibraryFlags()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.value = _state.value.copy(
                    detailsLoading = false,
                    errorMessage = error.message ?: "Could not remove this Manga."
                )
            }
        }
    }

    fun startReading() {
        val selected = _state.value.selectedDetails ?: return
        readerJob?.cancel()
        readerJob = viewModelScope.launch {
            _state.value = _state.value.copy(
                readerLoading = true,
                errorMessage = null
            )
            try {
                val session = selected.canonical?.let { canonical ->
                    service.openLibraryWork(
                        id = canonical.id,
                        preferredSourceId = selected.source.id
                    )
                } ?: service.openFromSource(selected.details)

                _state.value = _state.value.copy(
                    activeReaderSession = session,
                    readerLoading = false
                )
                refreshLibrary()
                refreshCatalogLibraryFlags()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.value = _state.value.copy(
                    readerLoading = false,
                    errorMessage = error.message ?: "This Manga could not be opened."
                )
            }
        }
    }

    fun closeReader() {
        _state.value = _state.value.copy(activeReaderSession = null)
        refreshLibrary()
    }

    fun closeDetails() {
        detailsJob?.cancel()
        _state.value = _state.value.copy(
            selectedDetails = null,
            detailsLoading = false
        )
    }

    fun consumeError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    private fun loadDetails(
        load: suspend () -> MangaHubWorkDetails
    ) {
        detailsJob?.cancel()
        detailsJob = viewModelScope.launch {
            _state.value = _state.value.copy(
                detailsLoading = true,
                errorMessage = null
            )
            try {
                val details = load()
                _state.value = _state.value.copy(
                    selectedDetails = details,
                    detailsLoading = false
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.value = _state.value.copy(
                    detailsLoading = false,
                    errorMessage = error.message ?: "Manga details could not be loaded."
                )
            }
        }
    }

    private fun refreshLibrary() {
        viewModelScope.launch {
            _state.value = _state.value.copy(libraryLoading = true)
            try {
                val works = service.library()
                _state.value = _state.value.copy(
                    libraryWorks = works,
                    libraryLoading = false
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.value = _state.value.copy(
                    libraryLoading = false,
                    errorMessage = error.message ?: "Manga library could not be loaded."
                )
            }
        }
    }

    private fun refreshCatalogLibraryFlags() {
        if (_state.value.discoverItems.isNotEmpty()) refreshDiscover()
        if (_state.value.searchItems.isNotEmpty() && _state.value.query.isNotBlank()) search()
    }

    companion object {
        fun factory(service: MangaHubCatalogService): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    MangaHubViewModel(service)
                }
            }
    }
}
