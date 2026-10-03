package com.veilreader.app.ui.screens

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.veilreader.app.domain.ReaderTtsSettings

/** Preserve the latest local slider choice while older DataStore acknowledgements arrive. */
@Stable
internal class ReaderTtsSettingsState(initial: ReaderTtsSettings) {
    var value by mutableStateOf(initial.normalized())
        private set
    private var pending: ReaderTtsSettings? = null
    fun update(settings: ReaderTtsSettings): ReaderTtsSettings = settings.normalized().also {
        value = it
        pending = it
    }
    fun acceptPersisted(settings: ReaderTtsSettings) {
        val safe = settings.normalized()
        if (pending == null || pending == safe) {
            value = safe
            pending = null
        }
    }
}
