package com.veilreader.app.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VeilTab(val label: String, val glyph: String) {
    READING("Reading", "◉"),
    LIBRARY("Library", "▦"),
    CASTLE("Castle", "♜"),
    PATH("Path", "✦"),
    PROFILE("Profile", "◎")
}

data class VeilRouteState(
    val selectedTab: VeilTab = VeilTab.READING,
    val showArchive: Boolean = false,
    val activeChamber: String? = null,
    val activeBookId: String? = null,
    val locatorOverrideJson: String? = null
)

/**
 * Restorable app-level navigation state.
 *
 * Only small primitives are saved. Readium Publication/Navigator objects are deliberately never
 * serialized; after recreation the app reopens [activeBookId] from Room at its durable locator.
 */
class VeilAppViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _route = MutableStateFlow(readSavedRoute())
    val route: StateFlow<VeilRouteState> = _route.asStateFlow()

    fun selectTab(tab: VeilTab) = update {
        copy(selectedTab = tab, showArchive = false, activeChamber = null)
    }

    fun openArchive() = update {
        copy(showArchive = true, activeChamber = null)
    }

    fun closeArchive() = update { copy(showArchive = false) }

    fun openChamber(chamberId: String) = update {
        copy(activeChamber = chamberId, showArchive = false)
    }

    fun closeChamber() = update { copy(activeChamber = null) }

    fun requestBook(bookId: String, locatorOverrideJson: String? = null) = update {
        copy(
            activeBookId = bookId,
            locatorOverrideJson = locatorOverrideJson,
            showArchive = false,
            activeChamber = null
        )
    }

    /** The explicit locator has been handed to Readium and is now persisted by the library. */
    fun readerOpened(bookId: String) {
        if (_route.value.activeBookId != bookId) return
        update { copy(locatorOverrideJson = null) }
    }

    fun bookOpenFailed(bookId: String) {
        if (_route.value.activeBookId != bookId) return
        update { copy(activeBookId = null, locatorOverrideJson = null) }
    }

    fun closeReader() = update {
        copy(
            selectedTab = VeilTab.LIBRARY,
            activeBookId = null,
            locatorOverrideJson = null,
            showArchive = false,
            activeChamber = null
        )
    }

    private fun update(transform: VeilRouteState.() -> VeilRouteState) {
        val next = _route.value.transform()
        _route.value = next
        savedStateHandle[KEY_TAB] = next.selectedTab.name
        savedStateHandle[KEY_ARCHIVE] = next.showArchive
        if (next.activeChamber == null) savedStateHandle.remove<String>(KEY_CHAMBER)
        else savedStateHandle[KEY_CHAMBER] = next.activeChamber
        if (next.activeBookId == null) savedStateHandle.remove<String>(KEY_BOOK)
        else savedStateHandle[KEY_BOOK] = next.activeBookId
        if (next.locatorOverrideJson == null) savedStateHandle.remove<String>(KEY_LOCATOR)
        else savedStateHandle[KEY_LOCATOR] = next.locatorOverrideJson
    }

    private fun readSavedRoute(): VeilRouteState {
        val tab = savedStateHandle.get<String>(KEY_TAB)
            ?.let { raw -> runCatching { VeilTab.valueOf(raw) }.getOrNull() }
            ?: VeilTab.READING
        val chamber = savedStateHandle.get<String>(KEY_CHAMBER)
            ?.takeIf { it in RESTORABLE_CHAMBERS }
        val bookId = savedStateHandle.get<String>(KEY_BOOK)?.takeIf(String::isNotBlank)
        return VeilRouteState(
            selectedTab = tab,
            showArchive = savedStateHandle.get<Boolean>(KEY_ARCHIVE) == true,
            activeChamber = chamber,
            activeBookId = bookId,
            locatorOverrideJson = savedStateHandle.get<String>(KEY_LOCATOR)
                ?.takeIf { it.isNotBlank() && bookId != null }
        )
    }

    companion object {
        private const val KEY_TAB = "veil.route.tab"
        private const val KEY_ARCHIVE = "veil.route.archive"
        private const val KEY_CHAMBER = "veil.route.chamber"
        private const val KEY_BOOK = "veil.route.book"
        private const val KEY_LOCATOR = "veil.route.locator"
        private val RESTORABLE_CHAMBERS = setOf("treasury", "sanctum")
    }
}
