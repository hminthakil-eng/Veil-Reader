package com.veilreader.app.ui.reader.tts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Real platform binding/cleanup test; no voice pack, network or audible speech is required. */
@RunWith(AndroidJUnit4::class)
class ReaderTtsBackendInstrumentedTest {
    @Test
    fun cancellingInitializationAndClosingDoesNotLeaveAnOwnedRequest() = runBlocking {
        withContext(Dispatchers.Main) {
            val backend = AndroidReaderTtsBackend(ApplicationProvider.getApplicationContext<Context>())
            try {
                val request = async(start = CoroutineStart.UNDISPATCHED) { backend.initialize() }
                request.cancelAndJoin()
                assertTrue(request.isCompleted)
                backend.close()
                assertTrue(backend.voices.isEmpty())
                assertEquals(ReaderTtsProblem.NO_ENGINE, backend.initialize())
                backend.close()
            } finally { backend.close() }
        }
    }
}
