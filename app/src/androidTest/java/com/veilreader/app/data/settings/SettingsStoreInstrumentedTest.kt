package com.veilreader.app.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderColumnMode
import com.veilreader.app.domain.ReaderFontFamily
import com.veilreader.app.domain.ReaderPreferenceToggle
import com.veilreader.app.domain.ReaderTextAlignment
import com.veilreader.app.domain.ReaderTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsStoreInstrumentedTest {

    @Test
    fun unanimatedPagedMode_survivesSettingsStoreRecreation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SettingsStore(context)
        val expected = ReaderAppearance(
            scroll = false,
            pageTurnStyle = PageTurnStyle.NONE
        )

        try {
            store.saveReaderAppearance(expected)

            val recreated = SettingsStore(context).settings.first()
            assertEquals(expected, recreated.readerAppearance)
        } finally {
            store.saveReaderAppearance(ReaderAppearance())
        }
    }

    @Test
    fun themeAndReaderMode_surviveSettingsStoreRecreation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SettingsStore(context)
        val expectedAppearance = ReaderAppearance(
            theme = ReaderTheme.SEPIA,
            fontScale = 1.32,
            lineHeight = 1.71,
            pageMargins = 1.23,
            scroll = true,
            publisherStyles = false,
            pageTurnStyle = PageTurnStyle.SLIDE,
            screenBrightness = 0.42,
            fontFamily = ReaderFontFamily.OPEN_DYSLEXIC,
            textAlignment = ReaderTextAlignment.JUSTIFY,
            columnMode = ReaderColumnMode.TWO,
            hyphenation = ReaderPreferenceToggle.ON,
            ligatures = ReaderPreferenceToggle.OFF,
            textNormalization = ReaderPreferenceToggle.ON,
            paragraphSpacing = 0.8,
            paragraphIndent = 1.2,
            letterSpacing = 0.08,
            wordSpacing = 0.24,
            typeScale = 1.15,
            paperPatina = 0.84
        )

        try {
            store.setAppThemeMode(AppThemeMode.DARK)
            store.saveReaderAppearance(expectedAppearance)

            val immediate = store.settings.first()
            assertEquals(AppThemeMode.DARK, immediate.appThemeMode)
            assertEquals(expectedAppearance, immediate.readerAppearance)

            val recreated = SettingsStore(context).settings.first()
            assertEquals(AppThemeMode.DARK, recreated.appThemeMode)
            assertEquals(expectedAppearance, recreated.readerAppearance)
        } finally {
            store.setAppThemeMode(AppThemeMode.SYSTEM)
            store.saveReaderAppearance(ReaderAppearance())
        }
    }
}