package com.veilreader.app.ui.screens

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.veilreader.app.domain.ReaderFocusGuideSettings

/** Immediate toggle feedback while the existing DataStore write is acknowledged. */
@Stable
internal class ReaderFocusGuideState(initial: ReaderFocusGuideSettings) {
    var value by mutableStateOf(initial.normalized())
        private set
    private var pending: ReaderFocusGuideSettings? = null

    fun toggle(): ReaderFocusGuideSettings = value.toggled().also {
        value = it
        pending = it
    }

    fun acceptPersisted(settings: ReaderFocusGuideSettings) {
        val normalized = settings.normalized()
        if (pending == null || pending == normalized) {
            value = normalized
            pending = null
        }
    }
}
