package com.veilreader.app.data.settings

import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.veilreader.app.domain.ReaderFocusGuideMode
import com.veilreader.app.domain.ReaderFocusGuideSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderFocusGuidePreferencesTest {
    @Test
    fun `absent and invalid enums fail calm`() {
        assertEquals(ReaderFocusGuideSettings(), decodeReaderFocusGuidePreferences(emptyPreferences()))
        assertEquals(ReaderFocusGuideSettings(), decodeReaderFocusGuidePreferences(preferencesOf(
            stringPreferencesKey("reader_focus_guide_mode") to "UNKNOWN",
            stringPreferencesKey("reader_focus_guide_last_active_mode") to "UNKNOWN"
        )))
    }

    @Test
    fun `wrong persisted types and nonfinite numbers do not break decoding`() {
        val decoded = decodeReaderFocusGuidePreferences(preferencesOf(
            doublePreferencesKey("reader_focus_guide_mode") to 1.0,
            stringPreferencesKey("reader_focus_guide_position") to "not a number",
            doublePreferencesKey("reader_focus_guide_band") to Double.NaN,
            doublePreferencesKey("reader_focus_guide_dim") to Double.POSITIVE_INFINITY
        ))
        assertEquals(ReaderFocusGuideSettings(), decoded)
    }

    @Test
    fun `off with persisted line resumes line and preserves tuning`() {
        val decoded = decodeReaderFocusGuidePreferences(preferencesOf(
            stringPreferencesKey("reader_focus_guide_mode") to "OFF",
            stringPreferencesKey("reader_focus_guide_last_active_mode") to "LINE",
            doublePreferencesKey("reader_focus_guide_position") to 0.63,
            doublePreferencesKey("reader_focus_guide_band") to 0.22,
            doublePreferencesKey("reader_focus_guide_dim") to 0.41
        ))
        assertEquals(ReaderFocusGuideMode.LINE, decoded.toggled().mode)
        assertEquals(0.63, decoded.toggled().verticalPosition, 0.0)
    }
}
