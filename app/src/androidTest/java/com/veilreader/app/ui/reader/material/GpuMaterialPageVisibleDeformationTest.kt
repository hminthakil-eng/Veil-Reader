package com.veilreader.app.ui.reader.material

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Hardware-backed optical contract, deliberately separate from locator and epoch tests.
 *
 * A freshly acquired buffer proves that SurfaceTexture displayed SOMETHING, not
 * that the page bent. This test captures the GLTextureView's real display surface
 * at two different drags and requires substantially different raster positions.
 * A static image, an invisible overlay or a no-op uniform update must fail.
 *
 * This is emulator optical evidence, not a substitute for physical phone QA.
 */
class GpuMaterialPageVisibleDeformationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun dragDeformsVisiblePaperRatherThanOnlyPublishingNewBufferEpochs() {
        val ready = AtomicBoolean(false)
        val failed = AtomicBoolean(false)
        val acquiredEpoch = AtomicLong(0L)
        val view = AtomicReference<GpuMaterialPageCurlView>()
        var source: Bitmap? = null
        val snapshots = mutableListOf<Bitmap>()

        compose.setContent {
            AndroidView(
                factory = { context ->
                    GpuMaterialPageCurlView(
                        context,
                        onRendererReady = ready::set,
                        onRendererFailure = { failed.set(true) },
                        onSheetPresented = acquiredEpoch::set
                    ).also(view::set)
                },
                modifier = Modifier.size(264.dp)
            )
        }

        try {
            compose.waitUntil(timeoutMillis = 5_000) { ready.get() || failed.get() }
            assertFalse("Paper GPU initialization failed", failed.get())

            compose.runOnIdle {
                val gpu = view.get()
                val width = gpu.width
                val height = gpu.height
                assertTrue("Invalid GPU width", width > 0)
                assertTrue("Invalid GPU height", height > 0)
                source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(source!!)
                val paint = Paint().apply { isAntiAlias = false }
                val palette = intArrayOf(
                    Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
                    Color.MAGENTA, Color.CYAN, Color.WHITE, Color.BLACK
                )
                val step = width.toFloat() / palette.size
                for (i in palette.indices) {
                    paint.color = palette[i]
                    canvas.drawRect(i * step, 0f, (i + 1) * step, height.toFloat(), paint)
                }
            }

            for ((index, progress) in listOf(0.04f, 0.75f).withIndex()) {
                val expectedEpoch = 1001L + index
                compose.runOnIdle {
                    view.get().submitFrame(
                        source,
                        active = true,
                        curl = gpuPageCurlFrame(
                            progress = progress,
                            verticalBias = 0.1f,
                            pullOriginY = 0.52f,
                            profile = MaterialPageProfiles.MatteBook,
                            side = MaterialPageSide.RIGHT
                        ),
                        profile = MaterialPageProfiles.MatteBook,
                        patina = 0.35f,
                        tone = MaterialPageTone.LIGHT,
                        visualAlpha = 1f,
                        highContrast = false,
                        sheetEpoch = expectedEpoch
                    )
                }
                compose.waitUntil(timeoutMillis = 5_000) {
                    acquiredEpoch.get() == expectedEpoch || failed.get()
                }
                assertFalse("Renderer failed during curl", failed.get())
                compose.runOnIdle {
                    val picture = view.get().bitmap
                    assertNotNull("Acquired GPU page was not readable from TextureView", picture)
                    snapshots += picture!!
                }
            }

            val first = snapshots[0]
            val second = snapshots[1]
            assertTrue(first.width == second.width && first.height == second.height)
            val total = first.width * first.height
            var changed = 0
            var firstNonTransparent = 0
            var secondNonTransparent = 0
            val pixelsBefore = IntArray(total)
            val pixelsAfter = IntArray(total)
            first.getPixels(pixelsBefore, 0, first.width, 0, 0, first.width, first.height)
            second.getPixels(pixelsAfter, 0, second.width, 0, 0, second.width, second.height)
            for (i in 0 until total) {
                val a = pixelsBefore[i]
                val b = pixelsAfter[i]
                if (Color.alpha(a) > 16) firstNonTransparent++
                if (Color.alpha(b) > 16) secondNonTransparent++
                // Ignore tiny driver/dithering changes. A real changing fold must
                // move a significant portion of the stripe pattern.
                if (kotlin.math.abs(Color.red(a) - Color.red(b)) > 24 ||
                    kotlin.math.abs(Color.green(a) - Color.green(b)) > 24 ||
                    kotlin.math.abs(Color.blue(a) - Color.blue(b)) > 24
                ) changed++
            }
            assertTrue("GPU produced no visible source material", firstNonTransparent > total / 3)
            assertTrue("GPU produced no visible deformed material", secondNonTransparent > total / 3)
            assertTrue(
                "Paper drag published new epochs but did not visibly deform the rendered sheet: changed=$changed pixels of $total",
                changed > total / 20
            )
        } finally {
            compose.runOnIdle { view.get()?.pauseRenderer() }
            snapshots.forEach(Bitmap::recycle)
            source?.recycle()
        }
    }
}
