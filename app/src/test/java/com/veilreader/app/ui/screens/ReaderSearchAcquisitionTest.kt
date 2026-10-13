@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.veilreader.app.ui.screens

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.veilreader.app.R
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.ui.theme.VeilTheme
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.services.search.*
import org.readium.r2.shared.util.Try

/** Reproduce cancellation while a cursor is acquired, before IO can deliver it to Compose. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderSearchAcquisitionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun dismissedSearchClosesAnAcquiredButUninstalledCursor() {
        val entered = AtomicBoolean(false)
        val closes = AtomicInteger(0)
        val released = CompletableDeferred<Unit>()
        val cursor = object : SearchIterator {
            override suspend fun next(): SearchTry<LocatorCollection?> = Try.success(null)
            override fun close() { closes.incrementAndGet() }
        }
        val service = object : SearchService {
            override val options = SearchService.Options()
            override suspend fun search(query: String, options: SearchService.Options?): SearchIterator {
                entered.set(true)
                // Models a native/third-party open operation that completes after cancellation.
                withContext(NonCancellable) { released.await() }
                return cursor
            }
        }
        val publication = Publication(Manifest(metadata = Metadata(identifier = "cancel-fixture")),
            servicesBuilder = Publication.ServicesBuilder(search = { service }))
        val visible = mutableStateOf(true)
        var results = 0
        compose.setContent { VeilTheme(AppThemeMode.LIGHT) {
            if (visible.value) ReaderBookSearchPanel(publication, onResult = { results++ })
        } }
        val context = RuntimeEnvironment.getApplication()
        compose.onNode(hasSetTextAction()).performTextInput("fog")
        compose.onNodeWithText(context.getString(R.string.reader_search_submit)).performClick()
        compose.waitUntil(10_000) { entered.get() }
        compose.runOnIdle { visible.value = false }
        compose.waitForIdle()
        released.complete(Unit)
        compose.waitUntil(10_000) { closes.get() == 1 }
        compose.runOnIdle { assertEquals(0, results); assertEquals(1, closes.get()) }
        publication.close()
    }
}
