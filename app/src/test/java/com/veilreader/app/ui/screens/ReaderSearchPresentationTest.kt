package com.veilreader.app.ui.screens

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.veilreader.app.R
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.ui.theme.VeilTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Verify the visible shared search, rather than a helper unused by production. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderSearchPresentationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun typingOrEditingAQueryNeverClaimsACompletedEmptySearch() {
        val state = mutableStateOf(ReaderSearchStatus.IDLE)
        compose.setContent { VeilTheme(AppThemeMode.LIGHT) {
            ReaderBookSearchContent("Klein", emptyList(), state.value, false, false,
                {}, {}, {}, {}, {})
        } }
        val empty = compose.onNodeWithText(RuntimeEnvironment.getApplication().getString(R.string.reader_search_empty))
        empty.assertDoesNotExist()
        compose.runOnIdle { state.value = ReaderSearchStatus.RESULTS }
        empty.assertExists()
        compose.runOnIdle { state.value = ReaderSearchStatus.IDLE }
        empty.assertDoesNotExist()
    }

    @Test
    fun onlyACompletedExhaustedQueryMayShowNoMatches() {
        val loading = mutableStateOf(true)
        val more = mutableStateOf(true)
        val state = mutableStateOf(ReaderSearchStatus.RESULTS)
        compose.setContent { VeilTheme(AppThemeMode.LIGHT) {
            ReaderBookSearchContent("Klein", emptyList(), state.value, loading.value, more.value,
                {}, {}, {}, {}, {})
        } }
        val empty = compose.onNodeWithText(RuntimeEnvironment.getApplication().getString(R.string.reader_search_empty))
        empty.assertDoesNotExist()
        compose.runOnIdle { loading.value = false }
        empty.assertDoesNotExist() // An empty Readium resource page is not the end.
        compose.runOnIdle { more.value = false }
        empty.assertExists()
        compose.runOnIdle { state.value = ReaderSearchStatus.ERROR }
        empty.assertDoesNotExist()
    }
}
