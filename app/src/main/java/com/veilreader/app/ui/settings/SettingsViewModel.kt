package com.veilreader.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.veilreader.app.data.settings.AppSettings
import com.veilreader.app.data.settings.SettingsStore
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.diagnostics.ReaderTrace
import com.veilreader.app.domain.ReaderAppearance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val store = SettingsStore(application.applicationContext)
    private val _settingsLoaded = MutableStateFlow(false)
    val settingsLoaded: StateFlow<Boolean> = _settingsLoaded.asStateFlow()

    val settings: StateFlow<AppSettings> = store.settings
        .onEach { _settingsLoaded.value = true }
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    fun setAppThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { store.setAppThemeMode(mode) }
    }

    fun markWelcomeSeen() {
        viewModelScope.launch { store.markWelcomeSeen() }
    }

    fun saveReaderAppearance(appearance: ReaderAppearance) {
        val details = "theme=${appearance.theme} scroll=${appearance.scroll} pageTurn=${appearance.pageTurnStyle}"
        ReaderTrace.event("appearance_requested", details = details)
        viewModelScope.launch {
            store.saveReaderAppearance(appearance)
            ReaderTrace.event("appearance_persisted", details = details)
        }
    }
}
