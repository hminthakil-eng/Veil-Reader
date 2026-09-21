package com.veilreader.app.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VeilTab(val label: String, val glyph: String) {
    READING("Reading", "◉"),
    LIBRARY("Library", "▦"),
    MANGA("Manga", "▤"),
    CASTLE("Castle", "♜"),
    PATH("Path", "✦"),
    PROFILE("Profile", "◎")
}

data class VeilRouteState(
    val selectedTab: VeilTab = VeilTab.READING,
    val showSettings: Boolean = false,
    val showArchive: Boolean = false,
    val activeChamber: String? = null,
    val activeBookId: String? = null,
    val locatorOverrideJson: String? = null,
    val activeMangaBookId: String? = null,
    val activeMangaChapterId: String? = null
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
            showSettings = false,
            showArchive = false,
            activeChamber = null,
            activeBookId = null,
            locatorOverrideJson = null,
            activeMangaBookId = null,
            activeMangaChapterId = null
        )
    }

    fun openArchive() = update {
        copy(
            showSettings = false,
            showArchive = true,
            activeChamber = null,
            activeBookId = null,
            locatorOverrideJson = null,
            activeMangaBookId = null,
            activeMangaChapterId = null
        )
    }

    fun closeArchive() = update { copy(showArchive = false) }

    fun openSettings() = update {
        copy(
            showSettings = true,
            showArchive = false,
            activeChamber = null,
            activeBookId = null,
            locatorOverrideJson = null,
            activeMangaBookId = null,
            activeMangaChapterId = null
        )
    }

    fun closeSettings() = update { copy(showSettings = false) }

    fun openChamber(chamberId: String) {
        if (chamberId !in RESTORABLE_CHAMBERS) return
        update {
            copy(
                activeChamber = chamberId,
                showSettings = false,
                showArchive = false,
                activeBookId = null,
                locatorOverrideJson = null,
                activeMangaBookId = null,
                activeMangaChapterId = null
            )
        }
    }

    fun closeChamber() = update { copy(activeChamber = null) }

    fun requestBook(bookId: String, locatorOverrideJson: String? = null) {
        if (bookId.isBlank()) return
        update {
            copy(
                activeBookId = bookId,
                locatorOverrideJson = locatorOverrideJson?.takeIf(String::isNotBlank),
                showSettings = false,
                showArchive = false,
                activeChamber = null,
                activeMangaBookId = null,
                activeMangaChapterId = null
            )
        }
    }

    fun requestMangaChapter(bookId: String, chapterId: String) {
        if (bookId.isBlank() || chapterId.isBlank()) return
        update {
            copy(
                selectedTab = VeilTab.MANGA,
                showSettings = false,
                showArchive = false,
                activeChamber = null,
                activeBookId = null,
                locatorOverrideJson = null,
                activeMangaBookId = bookId,
                activeMangaChapterId = chapterId
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
            showSettings = false,
            activeBookId = null,
            locatorOverrideJson = null,
            activeMangaBookId = null,
            activeMangaChapterId = null,
            showArchive = false,
            activeChamber = null
        )
    }

    fun closeMangaReader() = update {
        copy(
            selectedTab = VeilTab.MANGA,
            showSettings = false,
            showArchive = false,
            activeChamber = null,
            activeBookId = null,
            locatorOverrideJson = null,
            activeMangaBookId = null,
            activeMangaChapterId = null
        )
    }

    private fun update(transform: VeilRouteState.() -> VeilRouteState) {
        val next = _route.value.transform().normalized()
        _route.value = next
        persist(next)
    }

    private fun persist(next: VeilRouteState) {
        savedStateHandle[KEY_TAB] = next.selectedTab.name
        savedStateHandle[KEY_SETTINGS] = next.showSettings
        savedStateHandle[KEY_ARCHIVE] = next.showArchive
        if (next.activeChamber == null) savedStateHandle.remove<String>(KEY_CHAMBER)
        else savedStateHandle[KEY_CHAMBER] = next.activeChamber
        if (next.activeBookId == null) savedStateHandle.remove<String>(KEY_BOOK)
        else savedStateHandle[KEY_BOOK] = next.activeBookId
        if (next.locatorOverrideJson == null) savedStateHandle.remove<String>(KEY_LOCATOR)
        else savedStateHandle[KEY_LOCATOR] = next.locatorOverrideJson
        if (next.activeMangaBookId == null) savedStateHandle.remove<String>(KEY_MANGA_BOOK)
        else savedStateHandle[KEY_MANGA_BOOK] = next.activeMangaBookId
        if (next.activeMangaChapterId == null) savedStateHandle.remove<String>(KEY_MANGA_CHAPTER)
        else savedStateHandle[KEY_MANGA_CHAPTER] = next.activeMangaChapterId
    }

    private fun readSavedRoute(): VeilRouteState {
        val tab = savedStateHandle.get<String>(KEY_TAB)
            ?.let { raw -> runCatching { VeilTab.valueOf(raw) }.getOrNull() }
            ?: VeilTab.READING
        return VeilRouteState(
            selectedTab = tab,
            showSettings = savedStateHandle.get<Boolean>(KEY_SETTINGS) == true,
            showArchive = savedStateHandle.get<Boolean>(KEY_ARCHIVE) == true,
            activeChamber = savedStateHandle.get<String>(KEY_CHAMBER),
            activeBookId = savedStateHandle.get<String>(KEY_BOOK),
            locatorOverrideJson = savedStateHandle.get<String>(KEY_LOCATOR),
            activeMangaBookId = savedStateHandle.get<String>(KEY_MANGA_BOOK),
            activeMangaChapterId = savedStateHandle.get<String>(KEY_MANGA_CHAPTER)
        ).normalized()
    }

    private fun VeilRouteState.normalized(): VeilRouteState {
        val cleanMangaBookId = activeMangaBookId?.takeIf(String::isNotBlank)
        val cleanMangaChapterId = activeMangaChapterId?.takeIf(String::isNotBlank)
        if (cleanMangaBookId != null && cleanMangaChapterId != null) {
            return copy(
                selectedTab = VeilTab.MANGA,
                showSettings = false,
                showArchive = false,
                activeChamber = null,
                activeBookId = null,
                locatorOverrideJson = null,
                activeMangaBookId = cleanMangaBookId,
                activeMangaChapterId = cleanMangaChapterId
            )
        }

        val cleanBookId = activeBookId?.takeIf(String::isNotBlank)
        if (cleanBookId != null) {
            return copy(
                showSettings = false,
                showArchive = false,
                activeChamber = null,
                activeBookId = cleanBookId,
                locatorOverrideJson = locatorOverrideJson?.takeIf(String::isNotBlank),
                activeMangaBookId = null,
                activeMangaChapterId = null
            )
        }
        val cleanChamber = activeChamber?.takeIf { it in RESTORABLE_CHAMBERS }
        val cleanSettings = showSettings && cleanChamber == null
        return copy(
            showSettings = cleanSettings,
            showArchive = showArchive && cleanChamber == null && !cleanSettings,
            activeChamber = cleanChamber,
            activeBookId = null,
            locatorOverrideJson = null,
            activeMangaBookId = null,
            activeMangaChapterId = null
        )
    }

    companion object {
        private const val KEY_TAB = "veil.route.tab"
        private const val KEY_SETTINGS = "veil.route.settings"
        private const val KEY_ARCHIVE = "veil.route.archive"
        private const val KEY_CHAMBER = "veil.route.chamber"
        private const val KEY_BOOK = "veil.route.book"
        private const val KEY_LOCATOR = "veil.route.locator"
        private const val KEY_MANGA_BOOK = "veil.route.manga.book"
        private const val KEY_MANGA_CHAPTER = "veil.route.manga.chapter"
        private val RESTORABLE_CHAMBERS = setOf("treasury", "sanctum")
    }
}
