package com.veilreader.app.data.settings

import com.veilreader.app.data.toJson
import com.veilreader.app.domain.PageMaterial
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderNavigationMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MaterialPreferencesTest {
    @Test fun materialBackupRoundTripKeepsGatePresetAgeAndNavigationMode() {
        for (material in PageMaterial.entries) for (mode in ReaderNavigationMode.entries) {
            val source = ReaderAppearance(materialEngineEnabled = true, pageMaterial = material,
                paperPatina = .21).withNavigationMode(mode)
            val decoded = com.veilreader.app.data.appearanceFromJson(source.toJson())
            assertEquals(source, decoded)
        }
        val legacy = com.veilreader.app.data.appearanceFromJson(org.json.JSONObject())
        assertFalse(legacy.materialEngineEnabled)
        assertEquals(PageMaterial.MATTE, legacy.pageMaterial)
        val malformed = com.veilreader.app.data.appearanceFromJson(org.json.JSONObject()
            .put("pageMaterial", "unknown").put("paperPatina", "invalid"))
        assertEquals(PageMaterial.MATTE, malformed.pageMaterial)
        assertEquals(.72, malformed.paperPatina, 0.0)
    }

    @Test fun materialAndGatePersistAcrossStoreRecreationWithoutChangingMode() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val store = SettingsStore(context)
        for (material in PageMaterial.entries) for (mode in ReaderNavigationMode.entries) {
            val expected = ReaderAppearance(materialEngineEnabled = true, pageMaterial = material,
                paperPatina = .31).withNavigationMode(mode)
            store.saveReaderAppearance(expected)
            val restored = SettingsStore(context).settings.first().readerAppearance
            assertEquals(expected, restored)
        }
        store.saveReaderAppearance(ReaderAppearance())
        val rollback = SettingsStore(context).settings.first().readerAppearance
        assertFalse(rollback.materialEngineEnabled)
        assertEquals(ReaderNavigationMode.PAPER_CURL, rollback.navigationMode)
    }
}
