package com.veilreader.app.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderColumnMode
import com.veilreader.app.domain.ReaderDarkImageTreatment
import com.veilreader.app.domain.ReaderFontFamily
import com.veilreader.app.domain.ReaderFixedLayoutSpread
import com.veilreader.app.domain.ReaderHardwareKeyAction
import com.veilreader.app.domain.ReaderHardwareKeyMap
import com.veilreader.app.domain.ReaderPreferenceToggle
import com.veilreader.app.domain.ReaderTapAction
import com.veilreader.app.domain.ReaderTapGrid
import com.veilreader.app.domain.ReaderTapZone
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
            fontWeight = 1.75,
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
            darkImageTreatment = ReaderDarkImageTreatment.INVERT,
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
    @Test
    fun highContrastPreference_survivesSettingsStoreRecreation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SettingsStore(context)

        try {
            store.setHighContrastEnabled(true)

            val immediate = store.settings.first()
            assertEquals(true, immediate.highContrastEnabled)

            val recreated = SettingsStore(context).settings.first()
            assertEquals(true, recreated.highContrastEnabled)
        } finally {
            store.setHighContrastEnabled(false)
        }
    }

    @Test
    fun hardwareKeyMapping_survivesSettingsStoreRecreation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SettingsStore(context)
        val expected = ReaderHardwareKeyMap(
            volumeUp = ReaderHardwareKeyAction.PREVIOUS_PAGE,
            volumeDown = ReaderHardwareKeyAction.NEXT_PAGE
        )

        try {
            store.saveReaderHardwareKeys(expected)
            val recreated = SettingsStore(context).settings.first()
            assertEquals(expected, recreated.readerHardwareKeys)
        } finally {
            store.saveReaderHardwareKeys(ReaderHardwareKeyMap())
        }
    }

    @Test
    fun tapMatrix_survivesSettingsStoreRecreation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SettingsStore(context)
        val expected = ReaderTapGrid()
            .withAction(ReaderTapZone.TOP_LEFT, ReaderTapAction.RENDERER)
            .withAction(ReaderTapZone.BOTTOM_RIGHT, ReaderTapAction.TOGGLE_CONTROLS)

        try {
            store.saveReaderTapGrid(expected)
            val recreated = SettingsStore(context).settings.first()
            assertEquals(expected, recreated.readerTapGrid)
        } finally {
            store.saveReaderTapGrid(ReaderTapGrid())
        }
    }

    @Test
    fun fixedLayoutSpreadOverride_isPublicationSpecific_andAutoRemovesOverride() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SettingsStore(context)
        val firstBook = "spread-test-a"
        val secondBook = "spread-test-b"

        try {
            store.saveFixedLayoutSpread(firstBook, ReaderFixedLayoutSpread.DUAL)
            store.saveFixedLayoutSpread(secondBook, ReaderFixedLayoutSpread.SINGLE)

            val saved = SettingsStore(context).settings.first()
            assertEquals(
                ReaderFixedLayoutSpread.DUAL,
                saved.fixedLayoutSpreads[firstBook]
            )
            assertEquals(
                ReaderFixedLayoutSpread.SINGLE,
                saved.fixedLayoutSpreads[secondBook]
            )

            store.saveFixedLayoutSpread(firstBook, ReaderFixedLayoutSpread.AUTO)
            val afterAuto = SettingsStore(context).settings.first()
            assertEquals(null, afterAuto.fixedLayoutSpreads[firstBook])
            assertEquals(
                ReaderFixedLayoutSpread.SINGLE,
                afterAuto.fixedLayoutSpreads[secondBook]
            )
        } finally {
            store.saveFixedLayoutSpread(firstBook, ReaderFixedLayoutSpread.AUTO)
            store.saveFixedLayoutSpread(secondBook, ReaderFixedLayoutSpread.AUTO)
        }
    }

    @Test
    fun malformedSpreadPreferencePayload_failsCalm() {
        assertEquals(emptyMap<String, ReaderFixedLayoutSpread>(), decodeFixedLayoutSpreadOverrides(null))
        assertEquals(emptyMap<String, ReaderFixedLayoutSpread>(), decodeFixedLayoutSpreadOverrides("{bad"))

        val decoded = decodeFixedLayoutSpreadOverrides(
            """{"book-a":"DUAL","book-b":"SINGLE","book-c":"AUTO","bad":"UNKNOWN"}"""
        )
        assertEquals(2, decoded.size)
        assertEquals(ReaderFixedLayoutSpread.DUAL, decoded["book-a"])
        assertEquals(ReaderFixedLayoutSpread.SINGLE, decoded["book-b"])
    }

}