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

fun visibleTabs(gameVisible: Boolean): List<VeilTab> =
    VeilTab.entries.filter { gameVisible || it !in setOf(VeilTab.CASTLE, VeilTab.PATH) }

data class VeilRouteState(
    val selectedTab: VeilTab = VeilTab.READING,
    val showArchive: Boolean = false,
    val settingsSection: String? = null,
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
        copy(
            selectedTab = tab,
            settingsSection = null,
            showArchive = false,
            activeChamber = null,
            activeBookId = null,
            locatorOverrideJson = null
        )
    }

    fun openArchive() = update {
        copy(
            showArchive = true,
            settingsSection = null,
            activeChamber = null,
            activeBookId = null,
            locatorOverrideJson = null
        )
    }

    fun closeArchive() = update { copy(showArchive = false) }

    fun openSettings(section: String = "general") = update {
        copy(settingsSection = section.takeIf { it in SETTINGS_SECTIONS } ?: "general",
            showArchive = false, activeChamber = null, activeBookId = null, locatorOverrideJson = null)
    }

    fun closeSettings() = update { copy(settingsSection = null) }

    /** Hide world destinations without interrupting an active book, notes, or settings. */
    fun applyGameVisibility(visible: Boolean) {
        if (visible) return
        update {
            copy(selectedTab = selectedTab.takeIf { it in visibleTabs(false) } ?: VeilTab.READING,
                activeChamber = null)
        }
    }

    fun openChamber(chamberId: String) {
        if (chamberId !in RESTORABLE_CHAMBERS) return
        update {
            copy(
                activeChamber = chamberId,
                settingsSection = null,
                showArchive = false,
                activeBookId = null,
                locatorOverrideJson = null
            )
        }
    }

    fun closeChamber() = update { copy(activeChamber = null) }

    fun requestBook(bookId: String, locatorOverrideJson: String? = null) {
        if (bookId.isBlank()) return
        update {
            copy(
                activeBookId = bookId,
                settingsSection = null,
                locatorOverrideJson = locatorOverrideJson?.takeIf(String::isNotBlank),
                showArchive = false,
                activeChamber = null
            )
        }
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
            settingsSection = null,
            activeBookId = null,
            locatorOverrideJson = null,
            showArchive = false,
            activeChamber = null
        )
    }

    private fun update(transform: VeilRouteState.() -> VeilRouteState) {
        val next = _route.value.transform().normalized()
        _route.value = next
        persist(next)
    }

    private fun persist(next: VeilRouteState) {
        savedStateHandle[KEY_TAB] = next.selectedTab.name
        savedStateHandle[KEY_ARCHIVE] = next.showArchive
        if (next.settingsSection == null) savedStateHandle.remove<String>(KEY_SETTINGS)
        else savedStateHandle[KEY_SETTINGS] = next.settingsSection
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
        return VeilRouteState(
            selectedTab = tab,
            showArchive = savedStateHandle.get<Boolean>(KEY_ARCHIVE) == true,
            settingsSection = savedStateHandle.get<String>(KEY_SETTINGS),
            activeChamber = savedStateHandle.get<String>(KEY_CHAMBER),
            activeBookId = savedStateHandle.get<String>(KEY_BOOK),
            locatorOverrideJson = savedStateHandle.get<String>(KEY_LOCATOR)
        ).normalized()
    }

    private fun VeilRouteState.normalized(): VeilRouteState {
        val cleanBookId = activeBookId?.takeIf(String::isNotBlank)
        if (cleanBookId != null) {
            return copy(
                showArchive = false,
                settingsSection = null,
                activeChamber = null,
                activeBookId = cleanBookId,
                locatorOverrideJson = locatorOverrideJson?.takeIf(String::isNotBlank)
            )
        }
        val cleanChamber = activeChamber?.takeIf { it in RESTORABLE_CHAMBERS }
        val cleanSettings = settingsSection?.takeIf { it in SETTINGS_SECTIONS }
        return copy(
            showArchive = showArchive && cleanChamber == null && cleanSettings == null,
            settingsSection = cleanSettings,
            activeChamber = cleanChamber.takeIf { cleanSettings == null },
            activeBookId = null,
            locatorOverrideJson = null
        )
    }

    companion object {
        private const val KEY_TAB = "veil.route.tab"
        private const val KEY_ARCHIVE = "veil.route.archive"
        private const val KEY_SETTINGS = "veil.route.settings"
        private const val KEY_CHAMBER = "veil.route.chamber"
        private const val KEY_BOOK = "veil.route.book"
        private const val KEY_LOCATOR = "veil.route.locator"
        private val RESTORABLE_CHAMBERS = setOf("treasury", "sanctum")
        private val SETTINGS_SECTIONS = setOf("general", "reading", "data")
    }
}
