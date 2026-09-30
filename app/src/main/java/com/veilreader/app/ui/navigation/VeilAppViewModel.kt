package com.veilreader.app.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class VeilTab(val label: String, val glyph: String) {
    READING("Reading", "◉"),
    LIBRARY("Library", "▦"),
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
    val readerSessionInstanceId: String? = null,
    val locatorOverrideJson: String? = null,
    val readerLocatorCheckpointJson: String? = null
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

    init {
        // Normalization may mint a missing reader-session id for legacy saved state. Persist it
        // immediately so process recreation keeps the same ViewModel/SavedStateHandle key.
        persist(_route.value)
    }

    fun selectTab(tab: VeilTab) = update {
        copy(
            selectedTab = tab,
            showSettings = false,
            showArchive = false,
            activeChamber = null,
            activeBookId = null,
            readerSessionInstanceId = null,
            locatorOverrideJson = null,
            readerLocatorCheckpointJson = null
        )
    }

    fun openArchive() = update {
        copy(
            showSettings = false,
            showArchive = true,
            activeChamber = null,
            activeBookId = null,
            readerSessionInstanceId = null,
            locatorOverrideJson = null,
            readerLocatorCheckpointJson = null
        )
    }

    fun closeArchive() = update { copy(showArchive = false) }

    fun openSettings() = update {
        copy(
            showSettings = true,
            showArchive = false,
            activeChamber = null,
            activeBookId = null,
            readerSessionInstanceId = null,
            locatorOverrideJson = null,
            readerLocatorCheckpointJson = null
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
                locatorOverrideJson = null
            )
        }
    }

    fun closeChamber() = update { copy(activeChamber = null) }

    fun requestBook(bookId: String, locatorOverrideJson: String? = null) {
        if (bookId.isBlank()) return
        val explicitLocator = locatorOverrideJson?.takeIf(String::isNotBlank)
        update {
            copy(
                activeBookId = bookId,
                readerSessionInstanceId = UUID.randomUUID().toString(),
                locatorOverrideJson = explicitLocator,
                readerLocatorCheckpointJson = explicitLocator,
                showSettings = false,
                showArchive = false,
                activeChamber = null
            )
        }
    }

    /**
     * The explicit locator has been handed to Readium and durably flushed.
     *
     * If the process-death checkpoint still points at that same explicit locator it is safe to clear
     * both. A newer checkpoint written while the flush was running is preserved.
     */
    fun readerOpened(bookId: String) {
        val current = _route.value
        if (current.activeBookId != bookId) return
        val explicit = current.locatorOverrideJson
        update {
            copy(
                locatorOverrideJson = null,
                readerLocatorCheckpointJson =
                    if (readerLocatorCheckpointJson == explicit) null else readerLocatorCheckpointJson
            )
        }
    }

    fun checkpointReaderLocator(bookId: String, locatorJson: String) {
        val clean = locatorJson.takeIf(String::isNotBlank) ?: return
        val current = _route.value
        if (current.activeBookId != bookId || current.readerLocatorCheckpointJson == clean) return
        update { copy(readerLocatorCheckpointJson = clean) }
    }

    fun readerCheckpointPersisted(bookId: String, locatorJson: String) {
        val current = _route.value
        if (
            current.activeBookId != bookId ||
            current.readerLocatorCheckpointJson != locatorJson
        ) return
        update { copy(readerLocatorCheckpointJson = null) }
    }

    fun bookOpenFailed(bookId: String) {
        if (_route.value.activeBookId != bookId) return
        update {
            copy(
                activeBookId = null,
                readerSessionInstanceId = null,
            locatorOverrideJson = null,
                readerLocatorCheckpointJson = null
            )
        }
    }

    fun closeReader() = update {
        copy(
            selectedTab = VeilTab.LIBRARY,
            showSettings = false,
            activeBookId = null,
            readerSessionInstanceId = null,
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
        savedStateHandle[KEY_SETTINGS] = next.showSettings
        savedStateHandle[KEY_ARCHIVE] = next.showArchive
        if (next.activeChamber == null) savedStateHandle.remove<String>(KEY_CHAMBER)
        else savedStateHandle[KEY_CHAMBER] = next.activeChamber
        if (next.activeBookId == null) savedStateHandle.remove<String>(KEY_BOOK)
        else savedStateHandle[KEY_BOOK] = next.activeBookId
        if (next.readerSessionInstanceId == null) {
            savedStateHandle.remove<String>(KEY_READER_SESSION)
        } else {
            savedStateHandle[KEY_READER_SESSION] = next.readerSessionInstanceId
        }
        if (next.locatorOverrideJson == null) savedStateHandle.remove<String>(KEY_LOCATOR)
        else savedStateHandle[KEY_LOCATOR] = next.locatorOverrideJson
        if (next.readerLocatorCheckpointJson == null) {
            savedStateHandle.remove<String>(KEY_READER_CHECKPOINT)
        } else {
            savedStateHandle[KEY_READER_CHECKPOINT] = next.readerLocatorCheckpointJson
        }
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
            readerSessionInstanceId = savedStateHandle.get<String>(KEY_READER_SESSION),
            locatorOverrideJson = savedStateHandle.get<String>(KEY_LOCATOR),
            readerLocatorCheckpointJson = savedStateHandle.get<String>(KEY_READER_CHECKPOINT)
        ).normalized()
    }

    private fun VeilRouteState.normalized(): VeilRouteState {
        val cleanBookId = activeBookId?.takeIf(String::isNotBlank)
        if (cleanBookId != null) {
            return copy(
                showSettings = false,
                showArchive = false,
                activeChamber = null,
                activeBookId = cleanBookId,
                readerSessionInstanceId = readerSessionInstanceId
                    ?.takeIf(String::isNotBlank)
                    ?: UUID.randomUUID().toString(),
                locatorOverrideJson = locatorOverrideJson?.takeIf(String::isNotBlank),
                readerLocatorCheckpointJson = readerLocatorCheckpointJson?.takeIf(String::isNotBlank)
            )
        }
        val cleanChamber = activeChamber?.takeIf { it in RESTORABLE_CHAMBERS }
        val cleanSettings = showSettings && cleanChamber == null
        return copy(
            showSettings = cleanSettings,
            showArchive = showArchive && cleanChamber == null && !cleanSettings,
            activeChamber = cleanChamber,
            activeBookId = null,
            readerSessionInstanceId = null,
            locatorOverrideJson = null,
            readerLocatorCheckpointJson = null
        )
    }

    companion object {
        private const val KEY_TAB = "veil.route.tab"
        private const val KEY_SETTINGS = "veil.route.settings"
        private const val KEY_ARCHIVE = "veil.route.archive"
        private const val KEY_CHAMBER = "veil.route.chamber"
        private const val KEY_BOOK = "veil.route.book"
        private const val KEY_READER_SESSION = "veil.route.reader_session"
        private const val KEY_LOCATOR = "veil.route.locator"
        private const val KEY_READER_CHECKPOINT = "veil.route.reader_checkpoint"
        private val RESTORABLE_CHAMBERS = setOf("observatory", "treasury", "sanctum", "mirror", "manga")
    }
}
