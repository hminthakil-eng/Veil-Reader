package com.veilreader.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.veilreader.app.data.settings.AppSettings
import com.veilreader.app.data.settings.SettingsStore
import com.veilreader.app.data.settings.SensorySettings
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.diagnostics.ReaderTrace
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderFixedLayoutSpread
import com.veilreader.app.domain.ReaderFocusGuideSettings
import com.veilreader.app.domain.ReaderHardwareKeyMap
import com.veilreader.app.domain.ReaderTapGrid
import com.veilreader.app.domain.ReaderTtsSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val store = SettingsStore(application.applicationContext)

    val settings: StateFlow<AppSettings> = store.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    fun setAppThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { store.setAppThemeMode(mode) }
    }

    fun setHighContrastEnabled(enabled: Boolean) {
        viewModelScope.launch { store.setHighContrastEnabled(enabled) }
    }

    fun saveReaderAppearance(appearance: ReaderAppearance) {
        val details = "theme=${appearance.theme} scroll=${appearance.scroll} pageTurn=${appearance.pageTurnStyle}"
        ReaderTrace.event("appearance_requested", details = details)
        viewModelScope.launch {
            store.saveReaderAppearance(appearance)
            ReaderTrace.event("appearance_persisted", details = details)
        }
    }

    fun saveReaderTapGrid(grid: ReaderTapGrid) {
        viewModelScope.launch { store.saveReaderTapGrid(grid) }
    }

    fun saveReaderHardwareKeys(mapping: ReaderHardwareKeyMap) {
        viewModelScope.launch { store.saveReaderHardwareKeys(mapping) }
    }

    fun saveReaderFocusGuide(settings: ReaderFocusGuideSettings) {
        viewModelScope.launch { store.saveReaderFocusGuide(settings) }
    }

    fun saveReaderTtsSettings(settings: ReaderTtsSettings) {
        viewModelScope.launch { store.saveReaderTtsSettings(settings) }
    }

    fun saveFixedLayoutSpread(
        bookId: String,
        mode: ReaderFixedLayoutSpread
    ) {
        viewModelScope.launch {
            store.saveFixedLayoutSpread(bookId, mode)
        }
    }

    fun saveSensorySettings(settings: SensorySettings) {
        viewModelScope.launch { store.saveSensorySettings(settings) }
    }
}
