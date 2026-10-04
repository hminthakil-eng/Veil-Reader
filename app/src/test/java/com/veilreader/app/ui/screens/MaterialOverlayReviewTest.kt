package com.veilreader.app.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.veilreader.app.ui.reader.material.*
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w412dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MaterialOverlayReviewTest {
    @get:Rule val compose = createComposeRule()

    @Test fun materialControlsExposeSingleToggleAndIndependentMaterialChoices() {
        val appearance = mutableStateOf(com.veilreader.app.domain.ReaderAppearance())
        compose.setContent { MaterialEngineControls(appearance.value) { appearance.value = it } }
        val context = RuntimeEnvironment.getApplication()
        compose.onNodeWithText(context.getString(com.veilreader.app.R.string.material_engine_enable))
            .assertHasClickAction().performClick()
        compose.runOnIdle { assertTrue(appearance.value.materialEngineEnabled) }
        compose.onNodeWithText(context.getString(com.veilreader.app.R.string.material_papyrus)).performClick()
        compose.runOnIdle {
            assertEquals(com.veilreader.app.domain.PageMaterial.PAPYRUS, appearance.value.pageMaterial)
            assertEquals(com.veilreader.app.domain.ReaderNavigationMode.PAPER_CURL, appearance.value.navigationMode)
        }
        compose.onNodeWithText(context.getString(com.veilreader.app.R.string.material_engine_enable)).performClick()
        compose.runOnIdle { assertFalse(appearance.value.materialEngineEnabled) }
    }

    @Test fun slideAndReducedMotionUseProductionSurfaces() {
        val names = MaterialReviewStates.slide + "reduced-motion"
        val current = mutableStateOf(names.first())
        val fixtures = names.associateWith { name ->
            publicationFixture(MaterialReviewState(name, persian = name == "persian", rtl = name == "rtl"),
                large = name == "large-text")
        }
        val slide = SlidePageState()
        val source = object : View(RuntimeEnvironment.getApplication()) {
            override fun onDraw(canvas: Canvas) { canvas.drawBitmap(fixtures.getValue(current.value), 0f, 0f, null) }
        }.apply { layout(0, 0, 412, 900) }
        compose.setContent {
            key(current.value) {
                Box(Modifier.fillMaxSize()) {
                    Image(fixtures.getValue(current.value).asImageBitmap(), null, Modifier.fillMaxSize())
                    if (current.value == "reduced-motion") {
                        MaterialReducedMotionSurface(MaterialTurnConfiguration(true), false, 0, Color(0xff98846a))
                    } else SlidePageOverlay(slide)
                }
            }
        }
        for (name in names) {
            compose.runOnIdle {
                slide.clearImmediately()
                current.value = name
                if (name != "reduced-motion") {
                    assertTrue(slide.begin(source))
                    val sign = if (name == "rtl" || name == "persian") 1f else -1f
                    slide.updateDrag(when (name) {
                        "idle" -> 0f
                        "completed-transition" -> 412f * sign
                        else -> 160f * sign
                    })
                }
            }
            compose.waitForIdle()
            val image = compose.onRoot().captureToImage().asAndroidBitmap()
            val file = File("build/outputs/material-page-review/${if (name == "reduced-motion") "paper" else "slide"}-$name.png")
            file.parentFile!!.mkdirs()
            file.outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        }
        compose.runOnIdle { slide.dispose() }
        // Fixtures are kept alive until Compose disposes its final Image at test teardown.
    }
}
