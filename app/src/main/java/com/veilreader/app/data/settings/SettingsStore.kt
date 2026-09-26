package com.veilreader.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.veilSettingsDataStore by preferencesDataStore(name = "veil_settings")

data class AppSettings(
    val appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val readerAppearance: ReaderAppearance = ReaderAppearance(),
    val dailyGoalMinutes: Int = 20,
    val gameVisible: Boolean = true,
    val welcomeSeen: Boolean = false,
    val legacyLibraryImported: Boolean = false,
    val legacyGameImported: Boolean = false
)

class SettingsStore(private val context: Context) {
    private object Keys {
        val appThemeMode = stringPreferencesKey("app_theme_mode")
        val theme = stringPreferencesKey("reader_theme")
        val fontScale = doublePreferencesKey("reader_font_scale")
        val lineHeight = doublePreferencesKey("reader_line_height")
        val pageMargins = doublePreferencesKey("reader_page_margins")
        val scroll = booleanPreferencesKey("reader_scroll")
        val publisherStyles = booleanPreferencesKey("reader_publisher_styles")
        val pageTurnStyle = stringPreferencesKey("reader_page_turn_style")
        val screenBrightness = doublePreferencesKey("reader_screen_brightness")
        val dailyGoalMinutes = intPreferencesKey("daily_goal_minutes")
        val gameVisible = booleanPreferencesKey("game_visible")
        val welcomeSeen = booleanPreferencesKey("welcome_seen")
        val legacyLibraryImported = booleanPreferencesKey("legacy_library_imported")
        val legacyGameImported = booleanPreferencesKey("legacy_game_imported")
    }

    val settings: Flow<AppSettings> = context.veilSettingsDataStore.data.map { prefs ->
        AppSettings(
            appThemeMode = runCatching {
                AppThemeMode.valueOf(prefs[Keys.appThemeMode] ?: AppThemeMode.SYSTEM.name)
            }.getOrDefault(AppThemeMode.SYSTEM),
            readerAppearance = ReaderAppearance(
                theme = runCatching {
                    ReaderTheme.valueOf(prefs[Keys.theme] ?: ReaderTheme.DUSK.name)
                }.getOrDefault(ReaderTheme.DUSK),
                fontScale = (prefs[Keys.fontScale] ?: 1.0).coerceIn(0.75, 1.8),
                lineHeight = (prefs[Keys.lineHeight] ?: 1.45).coerceIn(1.1, 2.0),
                pageMargins = (prefs[Keys.pageMargins] ?: 1.0).coerceIn(0.5, 2.0),
                scroll = prefs[Keys.scroll] ?: false,
                publisherStyles = prefs[Keys.publisherStyles] ?: true,
                pageTurnStyle = runCatching {
                    PageTurnStyle.valueOf(prefs[Keys.pageTurnStyle] ?: PageTurnStyle.PAPER.name)
                }.getOrDefault(PageTurnStyle.PAPER),
                screenBrightness = prefs[Keys.screenBrightness]
                    ?.takeIf { it.isFinite() }
                    ?.coerceIn(0.05, 1.0)
            ),
            dailyGoalMinutes = (prefs[Keys.dailyGoalMinutes] ?: 20).coerceIn(5, 180),
            gameVisible = prefs[Keys.gameVisible] ?: true,
            welcomeSeen = prefs[Keys.welcomeSeen] ?: false,
            legacyLibraryImported = prefs[Keys.legacyLibraryImported] ?: false,
            legacyGameImported = prefs[Keys.legacyGameImported] ?: false
        )
    }

    suspend fun setAppThemeMode(mode: AppThemeMode) {
        context.veilSettingsDataStore.edit { it[Keys.appThemeMode] = mode.name }
    }

    suspend fun saveReaderAppearance(value: ReaderAppearance) {
        context.veilSettingsDataStore.edit { prefs ->
            prefs[Keys.theme] = value.theme.name
            prefs[Keys.fontScale] = value.fontScale
            prefs[Keys.lineHeight] = value.lineHeight
            prefs[Keys.pageMargins] = value.pageMargins
            prefs[Keys.scroll] = value.scroll
            prefs[Keys.publisherStyles] = value.publisherStyles
            prefs[Keys.pageTurnStyle] = value.pageTurnStyle.name
            value.screenBrightness?.takeIf { it.isFinite() }?.let {
                prefs[Keys.screenBrightness] = it.coerceIn(0.05, 1.0)
            } ?: prefs.remove(Keys.screenBrightness)
        }
    }

    suspend fun setDailyGoal(minutes: Int) {
        context.veilSettingsDataStore.edit { it[Keys.dailyGoalMinutes] = minutes.coerceIn(5, 180) }
    }

    suspend fun setGameVisible(visible: Boolean) {
        context.veilSettingsDataStore.edit { it[Keys.gameVisible] = visible }
    }

    suspend fun markWelcomeSeen() {
        context.veilSettingsDataStore.edit { it[Keys.welcomeSeen] = true }
    }

    suspend fun markLegacyLibraryImported() {
        context.veilSettingsDataStore.edit { it[Keys.legacyLibraryImported] = true }
    }

    suspend fun markLegacyGameImported() {
        context.veilSettingsDataStore.edit { it[Keys.legacyGameImported] = true }
    }

    internal suspend fun clearAllForTest() {
        context.veilSettingsDataStore.edit { it.clear() }
    }
}
