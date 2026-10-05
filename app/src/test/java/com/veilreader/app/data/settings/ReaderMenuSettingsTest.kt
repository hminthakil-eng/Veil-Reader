package com.veilreader.app.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderMenuSettingsTest {
    @Test fun menuPreferencePersistsIndependentlyOfPublicationLayoutAndInput() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SettingsStore(context)
        val original = store.settings.first()
        try {
            for (enabled in listOf(false, true)) {
                store.setReaderChromeAutoHideEnabled(enabled)
                val restored = SettingsStore(context).settings.first()
                assertEquals(enabled, restored.readerChromeAutoHideEnabled)
                assertEquals(original.readerAppearance, restored.readerAppearance)
                assertEquals(original.readerTapGrid, restored.readerTapGrid)
                assertEquals(original.readerHardwareKeys, restored.readerHardwareKeys)
                assertEquals(original.readerTts, restored.readerTts)
            }
        } finally {
            store.setReaderChromeAutoHideEnabled(original.readerChromeAutoHideEnabled)
        }
    }
}
