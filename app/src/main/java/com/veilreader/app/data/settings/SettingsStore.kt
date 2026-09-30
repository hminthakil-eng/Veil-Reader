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
import com.veilreader.app.domain.PerformanceTier
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderTheme
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
    val readerAppearance: ReaderAppearance = ReaderAppearance(),
    val readerAppearanceOverrides: Map<String, ReaderAppearance> = emptyMap(),
    val sensory: SensorySettings = SensorySettings(),
    val performanceTier: PerformanceTier = PerformanceTier.FULL,
    val dailyGoalMinutes: Int = 20,
    val gameVisible: Boolean = true,
    val legacyLibraryImported: Boolean = false,
    val legacyGameImported: Boolean = false
)

private fun ReaderAppearance.toSettingsJson(): JSONObject =
    JSONObject().apply {
        put("theme", theme.name)
        put("fontScale", fontScale)
        put("lineHeight", lineHeight)
        put("pageMargins", pageMargins)
        put("scroll", scroll)
        put("publisherStyles", publisherStyles)
        put("pageTurnStyle", pageTurnStyle.name)
        screenBrightness
            ?.takeIf { it.isFinite() }
            ?.coerceIn(0.05, 1.0)
            ?.let { put("screenBrightness", it) }
    }

private fun readerAppearanceFromSettingsJson(value: JSONObject): ReaderAppearance =
    ReaderAppearance(
        theme = runCatching {
            ReaderTheme.valueOf(value.optString("theme", ReaderTheme.PAPER.name))
        }.getOrDefault(ReaderTheme.PAPER),
        fontScale = value.optDouble("fontScale", 1.0)
            .takeIf { it.isFinite() }
            ?.coerceIn(0.75, 1.8)
            ?: 1.0,
        lineHeight = value.optDouble("lineHeight", 1.45)
            .takeIf { it.isFinite() }
            ?.coerceIn(1.1, 2.0)
            ?: 1.45,
        pageMargins = value.optDouble("pageMargins", 1.0)
            .takeIf { it.isFinite() }
            ?.coerceIn(0.5, 2.0)
            ?: 1.0,
        scroll = value.optBoolean("scroll", false),
        publisherStyles = value.optBoolean("publisherStyles", true),
        pageTurnStyle = runCatching {
            PageTurnStyle.valueOf(
                value.optString("pageTurnStyle", PageTurnStyle.PAPER.name)
            )
        }.getOrDefault(PageTurnStyle.PAPER),
        screenBrightness = if (
            value.has("screenBrightness") && !value.isNull("screenBrightness")
        ) {
            value.optDouble("screenBrightness")
                .takeIf { it.isFinite() }
                ?.coerceIn(0.05, 1.0)
        } else {
            null
        }
    ).canonicalizedNavigation()

private fun decodeReaderAppearanceOverrides(
    raw: String?
): Map<String, ReaderAppearance> {
    if (raw.isNullOrBlank()) return emptyMap()
    return runCatching {
        val root = JSONObject(raw)
        val result = linkedMapOf<String, ReaderAppearance>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val bookId = keys.next()
            val encoded = root.optJSONObject(bookId) ?: continue
            result[bookId] = readerAppearanceFromSettingsJson(encoded)
        }
        result
    }.getOrDefault(emptyMap())
}

private fun encodeReaderAppearanceOverrides(
    values: Map<String, ReaderAppearance>
): String =
    JSONObject().apply {
        values
            .toSortedMap()
            .forEach { (bookId, appearance) ->
                put(bookId, appearance.toSettingsJson())
            }
    }.toString()

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
        val readerAppearanceOverrides = stringPreferencesKey("reader_book_appearance_overrides_v1")
        val dailyGoalMinutes = intPreferencesKey("daily_goal_minutes")
        val sensoryHaptics = booleanPreferencesKey("sensory_haptics")
        val sensoryInteractionSounds = booleanPreferencesKey("sensory_interaction_sounds")
        val sensoryAmbient = stringPreferencesKey("sensory_ambient")
        val sensoryAudioVolume = doublePreferencesKey("sensory_audio_volume")
        val performanceTier = stringPreferencesKey("performance_tier")
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
                    ?.coerceIn(0.05, 1.0)
            ).canonicalizedNavigation(),
            readerAppearanceOverrides = decodeReaderAppearanceOverrides(
                prefs[Keys.readerAppearanceOverrides]
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
            performanceTier = runCatching {
                PerformanceTier.valueOf(
                    prefs[Keys.performanceTier] ?: PerformanceTier.FULL.name
                )
            }.getOrDefault(PerformanceTier.FULL),
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
        val canonical = value.canonicalizedNavigation()
        context.veilSettingsDataStore.edit { prefs ->
            prefs[Keys.theme] = canonical.theme.name
            prefs[Keys.fontScale] = canonical.fontScale
            prefs[Keys.lineHeight] = canonical.lineHeight
            prefs[Keys.pageMargins] = canonical.pageMargins
            prefs[Keys.scroll] = canonical.scroll
            prefs[Keys.publisherStyles] = canonical.publisherStyles
            prefs[Keys.pageTurnStyle] = canonical.pageTurnStyle.name
            canonical.screenBrightness?.takeIf { it.isFinite() }?.let {
                prefs[Keys.screenBrightness] = it.coerceIn(0.05, 1.0)
            } ?: prefs.remove(Keys.screenBrightness)
        }
    }

    suspend fun saveBookReaderAppearance(bookId: String, value: ReaderAppearance) {
        if (bookId.isBlank()) return
        val canonical = value.canonicalizedNavigation()
        context.veilSettingsDataStore.edit { prefs ->
            val current = decodeReaderAppearanceOverrides(
                prefs[Keys.readerAppearanceOverrides]
            ).toMutableMap()
            current[bookId] = canonical
            prefs[Keys.readerAppearanceOverrides] = encodeReaderAppearanceOverrides(current)
        }
    }

    suspend fun clearBookReaderAppearance(bookId: String) {
        if (bookId.isBlank()) return
        context.veilSettingsDataStore.edit { prefs ->
            val current = decodeReaderAppearanceOverrides(
                prefs[Keys.readerAppearanceOverrides]
            ).toMutableMap()
            current.remove(bookId)
            if (current.isEmpty()) {
                prefs.remove(Keys.readerAppearanceOverrides)
            } else {
                prefs[Keys.readerAppearanceOverrides] = encodeReaderAppearanceOverrides(current)
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

    suspend fun setPerformanceTier(tier: PerformanceTier) {
        context.veilSettingsDataStore.edit { it[Keys.performanceTier] = tier.name }
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
