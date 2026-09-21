package com.veilreader.app.manga.reader

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.mangaReaderPreferencesDataStore by preferencesDataStore(
    name = "manga_reader_preferences"
)

interface MangaReaderPreferencesPersistence {
    val preferences: Flow<MangaReaderPreferences>

    suspend fun current(): MangaReaderPreferences = preferences.first()

    suspend fun save(value: MangaReaderPreferences)
}

class MangaReaderPreferencesStore(
    context: Context
) : MangaReaderPreferencesPersistence {
    private val appContext = context.applicationContext

    private object Keys {
        val layout = stringPreferencesKey("layout")
        val direction = stringPreferencesKey("direction")
        val imageFit = stringPreferencesKey("image_fit")
        val pageLayout = stringPreferencesKey("page_layout")
        val cropBorders = booleanPreferencesKey("crop_borders")
        val pageGapDp = intPreferencesKey("page_gap_dp")
        val dataSaver = booleanPreferencesKey("data_saver")
        val background = stringPreferencesKey("background")
    }

    override val preferences: Flow<MangaReaderPreferences> =
        appContext.mangaReaderPreferencesDataStore.data.map(::decode)

    override suspend fun save(value: MangaReaderPreferences) {
        appContext.mangaReaderPreferencesDataStore.edit { prefs ->
            prefs[Keys.layout] = value.layout.name
            prefs[Keys.direction] = value.direction.name
            prefs[Keys.imageFit] = value.imageFit.name
            prefs[Keys.pageLayout] = value.pageLayout.name
            prefs[Keys.cropBorders] = value.cropBorders
            prefs[Keys.pageGapDp] = value.pageGapDp.coerceIn(0, 64)
            prefs[Keys.dataSaver] = value.dataSaver
            prefs[Keys.background] = value.background.name
        }
    }

    private fun decode(prefs: Preferences): MangaReaderPreferences =
        MangaReaderPreferences(
            layout = enumValueOrDefault(
                prefs[Keys.layout],
                MangaReaderLayout.PAGED
            ),
            direction = enumValueOrDefault(
                prefs[Keys.direction],
                MangaReadingDirection.RIGHT_TO_LEFT
            ),
            imageFit = enumValueOrDefault(
                prefs[Keys.imageFit],
                MangaImageFit.SCREEN
            ),
            pageLayout = enumValueOrDefault(
                prefs[Keys.pageLayout],
                MangaPageLayout.SINGLE
            ),
            cropBorders = prefs[Keys.cropBorders] ?: false,
            pageGapDp = (prefs[Keys.pageGapDp] ?: 0).coerceIn(0, 64),
            dataSaver = prefs[Keys.dataSaver] ?: false,
            background = enumValueOrDefault(
                prefs[Keys.background],
                MangaReaderBackground.BLACK
            )
        )
}

internal inline fun <reified T : Enum<T>> enumValueOrDefault(
    raw: String?,
    fallback: T
): T = raw
    ?.let { value -> runCatching { enumValueOf<T>(value) }.getOrNull() }
    ?: fallback
