package com.veilreader.app.ui.reader.tts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Locator

@RunWith(AndroidJUnit4::class)
class ForegroundTtsCheckpointInstrumentedTest {
    @Test
    fun checkpointAcknowledgementIsReadableFromNewOwnerWithoutPublicationText() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = ReaderTtsCheckpointStore(context)
        store.clear()
        try {
            val first = ReaderForegroundTtsCheckpointController("foreground-qa", store::read, store::save)
            val locator = requireNotNull(Locator.fromJSON(JSONObject(
                """{"href":"chapter.xhtml","type":"application/xhtml+xml","locations":{"progression":0.3},"text":{"highlight":"private passage"}}"""
            )))
            first.commit(locator, ReaderTtsPreferences(speed = 1.2f))
            val reopenedStore = ReaderTtsCheckpointStore(context)
            val persisted = requireNotNull(reopenedStore.read())
            assertEquals("foreground-qa", persisted.request.bookId)
            assertFalse(persisted.toJson().contains("private passage"))
            assertEquals(0.3, JSONObject(persisted.locatorJson).getJSONObject("locations").getDouble("progression"), 0.001)
            assertEquals(1.2f, persisted.request.preferences.speed, 0.001f)
            assertEquals(ReaderTtsPhase.PAUSED, persisted.phase)
        } finally { store.clear() }
    }
}
