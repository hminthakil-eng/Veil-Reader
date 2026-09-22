package com.veilreader.app.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderBrightness
import com.veilreader.app.domain.ReaderBrightnessMode
import com.veilreader.app.domain.ReaderTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsStoreInstrumentedTest {

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
            brightness = ReaderBrightness(
                mode = ReaderBrightnessMode.OVERRIDE,
                level = 0.42
            ),
            pageTurnStyle = PageTurnStyle.SLIDE
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