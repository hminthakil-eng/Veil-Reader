package com.veilreader.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.veilSettingsDataStore by preferencesDataStore(name = "veil_settings")

data class AppSettings(
    val readerAppearance: ReaderAppearance = ReaderAppearance(),
    val dailyGoalMinutes: Int = 20,
    val gameVisible: Boolean = true,
    val legacyLibraryImported: Boolean = false,
    val legacyGameImported: Boolean = false
)

class SettingsStore(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("reader_theme")
        val fontScale = doublePreferencesKey("reader_font_scale")
        val lineHeight = doublePreferencesKey("reader_line_height")
        val pageMargins = doublePreferencesKey("reader_page_margins")
        val scroll = booleanPreferencesKey("reader_scroll")
        val publisherStyles = booleanPreferencesKey("reader_publisher_styles")
        val dailyGoalMinutes = intPreferencesKey("daily_goal_minutes")
        val gameVisible = booleanPreferencesKey("game_visible")
        val legacyLibraryImported = booleanPreferencesKey("legacy_library_imported")
        val legacyGameImported = booleanPreferencesKey("legacy_game_imported")
    }

    val settings: Flow<AppSettings> = context.veilSettingsDataStore.data.map { prefs ->
        AppSettings(
            readerAppearance = ReaderAppearance(
                theme = runCatching {
                    ReaderTheme.valueOf(prefs[Keys.theme] ?: ReaderTheme.DUSK.name)
                }.getOrDefault(ReaderTheme.DUSK),
                fontScale = (prefs[Keys.fontScale] ?: 1.0).coerceIn(0.75, 1.8),
                lineHeight = (prefs[Keys.lineHeight] ?: 1.45).coerceIn(1.1, 2.0),
                pageMargins = (prefs[Keys.pageMargins] ?: 1.0).coerceIn(0.5, 2.0),
                scroll = prefs[Keys.scroll] ?: false,
                publisherStyles = prefs[Keys.publisherStyles] ?: true
            ),
            dailyGoalMinutes = (prefs[Keys.dailyGoalMinutes] ?: 20).coerceIn(5, 180),
            gameVisible = prefs[Keys.gameVisible] ?: true,
            legacyLibraryImported = prefs[Keys.legacyLibraryImported] ?: false,
            legacyGameImported = prefs[Keys.legacyGameImported] ?: false
        )
    }

    suspend fun saveReaderAppearance(value: ReaderAppearance) {
        context.veilSettingsDataStore.edit { prefs ->
            prefs[Keys.theme] = value.theme.name
            prefs[Keys.fontScale] = value.fontScale
            prefs[Keys.lineHeight] = value.lineHeight
            prefs[Keys.pageMargins] = value.pageMargins
            prefs[Keys.scroll] = value.scroll
            prefs[Keys.publisherStyles] = value.publisherStyles
        }
    }

    suspend fun setDailyGoal(minutes: Int) {
        context.veilSettingsDataStore.edit { it[Keys.dailyGoalMinutes] = minutes.coerceIn(5, 180) }
    }

    suspend fun setGameVisible(visible: Boolean) {
        context.veilSettingsDataStore.edit { it[Keys.gameVisible] = visible }
    }

    suspend fun markLegacyLibraryImported() {
        context.veilSettingsDataStore.edit { it[Keys.legacyLibraryImported] = true }
    }

    suspend fun markLegacyGameImported() {
        context.veilSettingsDataStore.edit { it[Keys.legacyGameImported] = true }
    }
}
