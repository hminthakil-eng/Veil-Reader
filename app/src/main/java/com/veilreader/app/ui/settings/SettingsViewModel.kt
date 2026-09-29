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

    fun saveReaderAppearance(appearance: ReaderAppearance) {
        val canonical = appearance.canonicalizedNavigation()
        val details = "theme=${canonical.theme} scroll=${canonical.scroll} pageTurn=${canonical.pageTurnStyle}"
        ReaderTrace.event("appearance_requested", details = details)
        viewModelScope.launch {
            store.saveReaderAppearance(canonical)
            ReaderTrace.event("appearance_persisted", details = details)
        }
    }

    fun saveBookReaderAppearance(bookId: String, appearance: ReaderAppearance) {
        val canonical = appearance.canonicalizedNavigation()
        val details = "book=$bookId theme=${canonical.theme} scroll=${canonical.scroll} pageTurn=${canonical.pageTurnStyle}"
        ReaderTrace.event("book_appearance_requested", details = details)
        viewModelScope.launch {
            store.saveBookReaderAppearance(bookId, canonical)
            ReaderTrace.event("book_appearance_persisted", details = details)
        }
    }

    fun clearBookReaderAppearance(bookId: String) {
        viewModelScope.launch {
            store.clearBookReaderAppearance(bookId)
            ReaderTrace.event("book_appearance_cleared", details = "book=$bookId")
        }
    }
    fun saveSensorySettings(settings: SensorySettings) {
        viewModelScope.launch { store.saveSensorySettings(settings) }
    }
}
