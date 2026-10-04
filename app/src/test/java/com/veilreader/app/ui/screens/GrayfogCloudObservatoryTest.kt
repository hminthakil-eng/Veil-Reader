package com.veilreader.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.domain.AppThemeMode
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.buildMemoryAtlas
import com.veilreader.app.ui.theme.VeilTheme
import java.text.NumberFormat
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GrayfogCloudObservatoryTest {
    @get:Rule val compose = createComposeRule()

    private fun checkDisclosure(scale: Float) {
        val books = (1..8).map { Book("folio-$it", "Folio $it", "Archivist $it", 0f,
            sourceUri = "veil-review://fictional/folio-$it", collections = listOf("Shared register")) }
        val atlas = buildMemoryAtlas(books, emptyList(), emptyList())
        val nodes = atlas.nodes.associateBy { it.book.id }
        val selected = mutableStateOf(nodes.getValue(books[0].id))
        var opened: String? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                VeilTheme(AppThemeMode.DARK) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        ObservatorySelection(selected.value, atlas.connectionsFor(selected.value.book.id), nodes,
                            onOpenBook = { opened = it.id })
                    }
                }
            }
        }
        val context = RuntimeEnvironment.getApplication()
        val count = NumberFormat.getNumberInstance(context.resources.configuration.locales[0]).format(7)
        val expand = context.getString(R.string.observatory_show_all_connections, count)
        compose.onAllNodesWithText("Folio ", substring = true).assertCountEquals(7)
        compose.onNodeWithText(expand).performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        compose.onAllNodesWithText("Folio ", substring = true).assertCountEquals(8)
        compose.runOnIdle { assertEquals(null, opened) }
        compose.onNodeWithText(context.getString(R.string.observatory_show_fewer_connections))
            .performScrollTo().performClick()
        compose.onAllNodesWithText("Folio ", substring = true).assertCountEquals(7)
        compose.onNodeWithText(expand).performScrollTo().performClick()
        compose.runOnIdle { selected.value = nodes.getValue(books[1].id) }
        compose.onAllNodesWithText("Folio ", substring = true).assertCountEquals(7)
        compose.onNodeWithText(context.getString(R.string.observatory_enter_volume))
            .performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(books[1].id, opened) }
    }

    @Test @Config(qualifiers = "en-w412dp-h900dp-mdpi")
    fun allRelationshipsRemainRetrievableAndSelectionResetsDisclosure() = checkDisclosure(1f)

    @Test @Config(qualifiers = "fa-rIR-w360dp-h800dp-mdpi")
    fun persianLargeTextKeepsDisclosureAndSourceReturnReachable() = checkDisclosure(2f)
}
