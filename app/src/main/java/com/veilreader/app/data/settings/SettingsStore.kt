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
import com.veilreader.app.domain.ReaderDarkImageTreatment
import com.veilreader.app.domain.ReaderFontFamily
import com.veilreader.app.domain.ReaderFixedLayoutSpread
import com.veilreader.app.domain.ReaderPreferenceToggle
import com.veilreader.app.domain.ReaderTapGrid
import com.veilreader.app.domain.ReaderTextAlignment
import com.veilreader.app.domain.ReaderTheme
import com.veilreader.app.domain.decodeReaderTapGrid
import com.veilreader.app.domain.encodeReaderTapGrid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

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
    val highContrastEnabled: Boolean = false,
    val readerAppearance: ReaderAppearance = ReaderAppearance(),
    val readerTapGrid: ReaderTapGrid = ReaderTapGrid(),
    val fixedLayoutSpreads: Map<String, ReaderFixedLayoutSpread> = emptyMap(),
    val sensory: SensorySettings = SensorySettings(),
    val dailyGoalMinutes: Int = 20,
    val gameVisible: Boolean = true,
    val legacyLibraryImported: Boolean = false,
    val legacyGameImported: Boolean = false
)

class SettingsStore(private val context: Context) {
    private object Keys {
        val appThemeMode = stringPreferencesKey("app_theme_mode")
        val highContrastEnabled = booleanPreferencesKey("accessibility_high_contrast")
        val theme = stringPreferencesKey("reader_theme")
        val fontScale = doublePreferencesKey("reader_font_scale")
        val lineHeight = doublePreferencesKey("reader_line_height")
        val pageMargins = doublePreferencesKey("reader_page_margins")
        val scroll = booleanPreferencesKey("reader_scroll")
        val publisherStyles = booleanPreferencesKey("reader_publisher_styles")
        val pageTurnStyle = stringPreferencesKey("reader_page_turn_style")
        val screenBrightness = doublePreferencesKey("reader_screen_brightness")
        val fontFamily = stringPreferencesKey("reader_font_family")
        val fontWeight = doublePreferencesKey("reader_font_weight")
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
        val darkImageTreatment = stringPreferencesKey("reader_dark_image_treatment")
        val paperPatina = doublePreferencesKey("reader_paper_patina")
        val tapGrid = stringPreferencesKey("reader_tap_grid")
        val fixedLayoutSpreads = stringPreferencesKey("reader_fixed_layout_spreads")
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
            highContrastEnabled = prefs[Keys.highContrastEnabled] ?: false,
            readerAppearance = ReaderAppearance(
                theme = runCatching {
                    ReaderTheme.valueOf(prefs[Keys.theme] ?: ReaderTheme.PAPER.name)
                }.getOrDefault(ReaderTheme.PAPER),
                fontScale = prefs[Keys.fontScale] ?: 1.0,
                lineHeight = prefs[Keys.lineHeight] ?: 1.45,
                pageMargins = prefs[Keys.pageMargins] ?: 1.0,
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
                fontWeight = prefs[Keys.fontWeight]
                    ?.takeIf { it.isFinite() }
                    ?.coerceIn(0.0, 2.5),
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
                darkImageTreatment = runCatching {
                    ReaderDarkImageTreatment.valueOf(
                        prefs[Keys.darkImageTreatment] ?: ReaderDarkImageTreatment.NONE.name
                    )
                }.getOrDefault(ReaderDarkImageTreatment.NONE),
                paperPatina = prefs[Keys.paperPatina] ?: 0.72
            ).normalized(),
            readerTapGrid = decodeReaderTapGrid(prefs[Keys.tapGrid]),
            fixedLayoutSpreads = decodeFixedLayoutSpreadOverrides(
                prefs[Keys.fixedLayoutSpreads]
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

    suspend fun setHighContrastEnabled(enabled: Boolean) {
        context.veilSettingsDataStore.edit { it[Keys.highContrastEnabled] = enabled }
    }

    suspend fun saveReaderAppearance(value: ReaderAppearance) {
        val normalized = value.normalized()
        context.veilSettingsDataStore.edit { prefs ->
            prefs[Keys.theme] = normalized.theme.name
            prefs[Keys.fontScale] = normalized.fontScale
            prefs[Keys.lineHeight] = normalized.lineHeight
            prefs[Keys.pageMargins] = normalized.pageMargins
            prefs[Keys.scroll] = normalized.scroll
            prefs[Keys.publisherStyles] = normalized.publisherStyles
            prefs[Keys.pageTurnStyle] = normalized.pageTurnStyle.name
            prefs[Keys.fontFamily] = normalized.fontFamily.name
            normalized.fontWeight?.takeIf { it.isFinite() }?.let {
                prefs[Keys.fontWeight] = it.coerceIn(0.0, 2.5)
            } ?: prefs.remove(Keys.fontWeight)
            prefs[Keys.textAlignment] = normalized.textAlignment.name
            prefs[Keys.columnMode] = normalized.columnMode.name
            prefs[Keys.hyphenation] = normalized.hyphenation.name
            prefs[Keys.ligatures] = normalized.ligatures.name
            prefs[Keys.textNormalization] = normalized.textNormalization.name
            normalized.screenBrightness?.takeIf { it.isFinite() }?.let {
                prefs[Keys.screenBrightness] = it.coerceIn(0.05, 1.0)
            } ?: prefs.remove(Keys.screenBrightness)
            normalized.paragraphSpacing?.takeIf { it.isFinite() }?.let {
                prefs[Keys.paragraphSpacing] = it.coerceIn(0.0, 2.0)
            } ?: prefs.remove(Keys.paragraphSpacing)
            normalized.paragraphIndent?.takeIf { it.isFinite() }?.let {
                prefs[Keys.paragraphIndent] = it.coerceIn(0.0, 3.0)
            } ?: prefs.remove(Keys.paragraphIndent)
            normalized.letterSpacing?.takeIf { it.isFinite() }?.let {
                prefs[Keys.letterSpacing] = it.coerceIn(0.0, 0.2)
            } ?: prefs.remove(Keys.letterSpacing)
            normalized.wordSpacing?.takeIf { it.isFinite() }?.let {
                prefs[Keys.wordSpacing] = it.coerceIn(0.0, 1.0)
            } ?: prefs.remove(Keys.wordSpacing)
            normalized.typeScale?.takeIf { it.isFinite() }?.let {
                prefs[Keys.typeScale] = it.coerceIn(1.0, 2.0)
            } ?: prefs.remove(Keys.typeScale)
            prefs[Keys.darkImageTreatment] = normalized.darkImageTreatment.name
            prefs[Keys.paperPatina] = normalized.paperPatina
                .takeIf { it.isFinite() }
                ?.coerceIn(0.0, 1.0)
                ?: 0.72
        }
    }

    suspend fun saveReaderTapGrid(value: ReaderTapGrid) {
        context.veilSettingsDataStore.edit { prefs ->
            prefs[Keys.tapGrid] = encodeReaderTapGrid(value)
        }
    }

    suspend fun saveFixedLayoutSpread(
        bookId: String,
        mode: ReaderFixedLayoutSpread
    ) {
        val id = bookId.trim()
        if (id.isEmpty()) return
        context.veilSettingsDataStore.edit { prefs ->
            val current = decodeFixedLayoutSpreadOverrides(
                prefs[Keys.fixedLayoutSpreads]
            ).toMutableMap()
            if (mode == ReaderFixedLayoutSpread.AUTO) {
                current.remove(id)
            } else {
                current[id] = mode
            }
            if (current.isEmpty()) {
                prefs.remove(Keys.fixedLayoutSpreads)
            } else {
                prefs[Keys.fixedLayoutSpreads] =
                    encodeFixedLayoutSpreadOverrides(current)
            }
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


internal fun decodeFixedLayoutSpreadOverrides(
    raw: String?
): Map<String, ReaderFixedLayoutSpread> {
    if (raw.isNullOrBlank()) return emptyMap()
    val json = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyMap()
    return buildMap {
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next().trim()
            if (key.isEmpty()) continue
            val mode = runCatching {
                ReaderFixedLayoutSpread.valueOf(json.optString(key))
            }.getOrNull() ?: continue
            if (mode != ReaderFixedLayoutSpread.AUTO) {
                put(key, mode)
            }
        }
    }
}

internal fun encodeFixedLayoutSpreadOverrides(
    values: Map<String, ReaderFixedLayoutSpread>
): String {
    val json = JSONObject()
    values
        .toSortedMap()
        .forEach { (bookId, mode) ->
            val id = bookId.trim()
            if (id.isNotEmpty() && mode != ReaderFixedLayoutSpread.AUTO) {
                json.put(id, mode.name)
            }
        }
    return json.toString()
}
