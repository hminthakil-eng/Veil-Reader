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
import com.veilreader.app.domain.ReaderColumnMode
import com.veilreader.app.domain.ReaderFontFamily
import com.veilreader.app.domain.ReaderPreferenceToggle
import com.veilreader.app.domain.ReaderTextAlignment
import com.veilreader.app.domain.ReaderTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.veilSettingsDataStore by preferencesDataStore(name = "veil_settings")

enum class AmbientSound {
    OFF,
    LIBRARY,
    RAIN,
    FIRE
}

data class SensorySettings(
    val hapticsEnabled: Boolean = true,
    val interactionSoundsEnabled: Boolean = false,
    val ambientSound: AmbientSound = AmbientSound.OFF,
    val audioVolume: Double = 0.18
)

data class AppSettings(
    val appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val readerAppearance: ReaderAppearance = ReaderAppearance(),
    val sensory: SensorySettings = SensorySettings(),
    val dailyGoalMinutes: Int = 20,
    val gameVisible: Boolean = true,
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
        val fontFamily = stringPreferencesKey("reader_font_family")
        val textAlignment = stringPreferencesKey("reader_text_alignment")
        val columnMode = stringPreferencesKey("reader_column_mode")
        val hyphenation = stringPreferencesKey("reader_hyphenation")
        val ligatures = stringPreferencesKey("reader_ligatures")
        val textNormalization = stringPreferencesKey("reader_text_normalization")
        val paragraphSpacing = doublePreferencesKey("reader_paragraph_spacing")
        val paragraphIndent = doublePreferencesKey("reader_paragraph_indent")
        val letterSpacing = doublePreferencesKey("reader_letter_spacing")
        val wordSpacing = doublePreferencesKey("reader_word_spacing")
        val typeScale = doublePreferencesKey("reader_type_scale")
        val paperPatina = doublePreferencesKey("reader_paper_patina")
        val dailyGoalMinutes = intPreferencesKey("daily_goal_minutes")
        val sensoryHaptics = booleanPreferencesKey("sensory_haptics")
        val sensoryInteractionSounds = booleanPreferencesKey("sensory_interaction_sounds")
        val sensoryAmbient = stringPreferencesKey("sensory_ambient")
        val sensoryAudioVolume = doublePreferencesKey("sensory_audio_volume")
        val gameVisible = booleanPreferencesKey("game_visible")
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
                    ReaderTheme.valueOf(prefs[Keys.theme] ?: ReaderTheme.PAPER.name)
                }.getOrDefault(ReaderTheme.PAPER),
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
                    ?.coerceIn(0.05, 1.0),
                fontFamily = runCatching {
                    ReaderFontFamily.valueOf(
                        prefs[Keys.fontFamily] ?: ReaderFontFamily.PUBLISHER.name
                    )
                }.getOrDefault(ReaderFontFamily.PUBLISHER),
                textAlignment = runCatching {
                    ReaderTextAlignment.valueOf(
                        prefs[Keys.textAlignment] ?: ReaderTextAlignment.PUBLISHER.name
                    )
                }.getOrDefault(ReaderTextAlignment.PUBLISHER),
                columnMode = runCatching {
                    ReaderColumnMode.valueOf(
                        prefs[Keys.columnMode] ?: ReaderColumnMode.AUTO.name
                    )
                }.getOrDefault(ReaderColumnMode.AUTO),
                hyphenation = runCatching {
                    ReaderPreferenceToggle.valueOf(
                        prefs[Keys.hyphenation] ?: ReaderPreferenceToggle.DEFAULT.name
                    )
                }.getOrDefault(ReaderPreferenceToggle.DEFAULT),
                ligatures = runCatching {
                    ReaderPreferenceToggle.valueOf(
                        prefs[Keys.ligatures] ?: ReaderPreferenceToggle.DEFAULT.name
                    )
                }.getOrDefault(ReaderPreferenceToggle.DEFAULT),
                textNormalization = runCatching {
                    ReaderPreferenceToggle.valueOf(
                        prefs[Keys.textNormalization] ?: ReaderPreferenceToggle.DEFAULT.name
                    )
                }.getOrDefault(ReaderPreferenceToggle.DEFAULT),
                paragraphSpacing = prefs[Keys.paragraphSpacing]
                    ?.takeIf { it.isFinite() }
                    ?.coerceIn(0.0, 2.0),
                paragraphIndent = prefs[Keys.paragraphIndent]
                    ?.takeIf { it.isFinite() }
                    ?.coerceIn(0.0, 3.0),
                letterSpacing = prefs[Keys.letterSpacing]
                    ?.takeIf { it.isFinite() }
                    ?.coerceIn(0.0, 0.2),
                wordSpacing = prefs[Keys.wordSpacing]
                    ?.takeIf { it.isFinite() }
                    ?.coerceIn(0.0, 1.0),
                typeScale = prefs[Keys.typeScale]
                    ?.takeIf { it.isFinite() }
                    ?.coerceIn(1.0, 2.0),
                paperPatina = (prefs[Keys.paperPatina] ?: 0.72)
                    .takeIf { it.isFinite() }
                    ?.coerceIn(0.0, 1.0)
                    ?: 0.72
            ),
            sensory = SensorySettings(
                hapticsEnabled = prefs[Keys.sensoryHaptics] ?: true,
                interactionSoundsEnabled = prefs[Keys.sensoryInteractionSounds] ?: false,
                ambientSound = runCatching {
                    AmbientSound.valueOf(
                        prefs[Keys.sensoryAmbient] ?: AmbientSound.OFF.name
                    )
                }.getOrDefault(AmbientSound.OFF),
                audioVolume = (prefs[Keys.sensoryAudioVolume] ?: 0.18)
                    .takeIf { it.isFinite() }
                    ?.coerceIn(0.0, 0.55)
                    ?: 0.18
            ),
            dailyGoalMinutes = (prefs[Keys.dailyGoalMinutes] ?: 20).coerceIn(5, 180),
            gameVisible = prefs[Keys.gameVisible] ?: true,
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
            prefs[Keys.fontFamily] = value.fontFamily.name
            prefs[Keys.textAlignment] = value.textAlignment.name
            prefs[Keys.columnMode] = value.columnMode.name
            prefs[Keys.hyphenation] = value.hyphenation.name
            prefs[Keys.ligatures] = value.ligatures.name
            prefs[Keys.textNormalization] = value.textNormalization.name
            value.screenBrightness?.takeIf { it.isFinite() }?.let {
                prefs[Keys.screenBrightness] = it.coerceIn(0.05, 1.0)
            } ?: prefs.remove(Keys.screenBrightness)
            value.paragraphSpacing?.takeIf { it.isFinite() }?.let {
                prefs[Keys.paragraphSpacing] = it.coerceIn(0.0, 2.0)
            } ?: prefs.remove(Keys.paragraphSpacing)
            value.paragraphIndent?.takeIf { it.isFinite() }?.let {
                prefs[Keys.paragraphIndent] = it.coerceIn(0.0, 3.0)
            } ?: prefs.remove(Keys.paragraphIndent)
            value.letterSpacing?.takeIf { it.isFinite() }?.let {
                prefs[Keys.letterSpacing] = it.coerceIn(0.0, 0.2)
            } ?: prefs.remove(Keys.letterSpacing)
            value.wordSpacing?.takeIf { it.isFinite() }?.let {
                prefs[Keys.wordSpacing] = it.coerceIn(0.0, 1.0)
            } ?: prefs.remove(Keys.wordSpacing)
            value.typeScale?.takeIf { it.isFinite() }?.let {
                prefs[Keys.typeScale] = it.coerceIn(1.0, 2.0)
            } ?: prefs.remove(Keys.typeScale)
            prefs[Keys.paperPatina] = value.paperPatina
                .takeIf { it.isFinite() }
                ?.coerceIn(0.0, 1.0)
                ?: 0.72
        }
    }

    suspend fun saveSensorySettings(value: SensorySettings) {
        context.veilSettingsDataStore.edit { prefs ->
            prefs[Keys.sensoryHaptics] = value.hapticsEnabled
            prefs[Keys.sensoryInteractionSounds] = value.interactionSoundsEnabled
            prefs[Keys.sensoryAmbient] = value.ambientSound.name
            prefs[Keys.sensoryAudioVolume] = value.audioVolume
                .takeIf { it.isFinite() }
                ?.coerceIn(0.0, 0.55)
                ?: 0.18
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

    internal suspend fun clearAllForTest() {
        context.veilSettingsDataStore.edit { it.clear() }
    }
}
