package com.veilreader.app.ui.reader.material

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Native Skia rendering of the production mesh, with deterministic publication text fixtures.
 * This checks visual coherence and produces review assets; it is not phone acceptance.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MaterialCloudRenderTest {
    @Test fun occludedFrontInkNeverLeaksIntoBackFaceInEitherDirection() {
        val renderer = MaterialPageRenderer()
        val source = Bitmap.createBitmap(400, 800, Bitmap.Config.ARGB_8888)
        val output = Bitmap.createBitmap(400, 800, Bitmap.Config.ARGB_8888)
        val paint = android.graphics.Paint().apply { color = Color.BLUE }
        try {
            for (mirror in listOf(false, true)) {
                source.eraseColor(Color.RED)
                Canvas(source).drawRect(if (mirror) 0f else 225f, 0f,
                    if (mirror) 175f else 400f, 800f, paint)
                output.eraseColor(Color.GREEN)
                renderer.draw(Canvas(output), source, 400f, 800f, .5f, .5f, 0f, mirror,
                    MaterialTurnConfiguration(true, PageMaterials.matte.copy(translucency = 1f, grain = 0f)),
                    Color.WHITE, Color.WHITE, 1f)
                val pixel = output.getPixel(if (mirror) 250 else 150, 400)
                assertTrue("Occluded front leaked through the backside: mirror=$mirror pixel=$pixel",
                    Color.blue(pixel) > Color.red(pixel) + 100)
            }
        } finally { renderer.dispose(); source.recycle(); output.recycle() }
    }

    @Test fun exactIdleAndCompleteFramesPreserveTheirPublicationAndInvalidMetricsAreInert() {
        val renderer = MaterialPageRenderer()
        val source = Bitmap.createBitmap(400, 800, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val output = Bitmap.createBitmap(400, 800, Bitmap.Config.ARGB_8888)
        try {
            for (mirror in listOf(false, true)) {
                output.eraseColor(Color.GREEN)
                renderer.draw(Canvas(output), source, 400f, 800f, 0f, .8f, .2f, mirror,
                    MaterialTurnConfiguration(true, PageMaterials.glossy), Color.WHITE, Color.WHITE, 1f)
                assertEquals(Color.RED, output.getPixel(200, 400))
                output.eraseColor(Color.GREEN)
                renderer.draw(Canvas(output), source, 400f, 800f, 1f, .8f, .2f, mirror,
                    MaterialTurnConfiguration(true, PageMaterials.glossy), Color.WHITE, Color.WHITE, 1f)
                assertEquals(Color.GREEN, output.getPixel(200, 400))
            }
            renderer.draw(Canvas(output), source, Float.NaN, 800f, .5f, .5f, 0f, false,
                MaterialTurnConfiguration(true), Color.WHITE, Color.WHITE, Float.NaN)
            assertEquals(Color.GREEN, output.getPixel(200, 400))
        } finally { renderer.dispose(); source.recycle(); output.recycle() }
    }

    @Test fun renderProductionMaterialReviewMatrix() {
        val renderer = MaterialPageRenderer()
        try {
            for (state in MaterialReviewStates.paper.filter { !it.reducedMotion }) {
                val page = publicationFixture(state)
                val image = publicationFixture(state, next = true)
                val canvas = Canvas(image)
                renderer.draw(canvas, page, 412f, 900f, state.sampledProgress(), state.originY,
                    state.tilt, state.rtl, MaterialTurnConfiguration(true, PageMaterials.forId(state.material), state.age),
                    if (state.dark) Color.rgb(30, 28, 34) else if (state.sepia) Color.rgb(221, 204, 174) else Color.rgb(239, 232, 215),
                    if (state.dark) Color.rgb(150, 143, 159) else Color.rgb(255, 248, 232), 1f)
                assertTrue(image.width == 412 && image.height == 900)
                // Substantive text survives; a frame cannot accidentally become a blank sheet.
                var contrastPixels = 0
                for (y in 80..800 step 4) for (x in 20..390 step 4) {
                    val pixel = image.getPixel(x, y)
                    if (state.dark) { if (Color.red(pixel) > 100) contrastPixels++ }
                    else if (Color.red(pixel) < 150) contrastPixels++
                }
                assertTrue("Publication contrast lost in ${state.name}", contrastPixels > 30)
                save(image, "paper-${state.name}")
                page.recycle(); image.recycle()
            }
        } finally { renderer.dispose() }
    }

    @Test fun renderProductionReleaseSequences() {
        val renderer = MaterialPageRenderer()
        try {
            for (material in com.veilreader.app.domain.PageMaterial.entries) {
                val state = MaterialReviewState("release", material = material)
                val source = publicationFixture(state)
                val backdrop = publicationFixture(state, next = true)
                val frame = Bitmap.createBitmap(412, 900, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(frame)
                val profile = PageMaterials.forId(material)
                for (target in listOf(0f, 1f)) {
                    val release = MaterialRelease(.4f, target, if (target == 1f) 1.2f else -.5f, profile)
                    val name = "motion/${material.name.lowercase()}-${if (target == 1f) "complete" else "cancel"}"
                    for (i in 0..24) {
                        canvas.drawBitmap(backdrop, 0f, 0f, null)
                        renderer.draw(canvas, source, 412f, 900f, release.position(i / 40f), .85f,
                            .03f, false, MaterialTurnConfiguration(true, profile, .15f),
                            Color.rgb(239, 232, 215), Color.rgb(255, 248, 232), 1f)
                        save(frame, "$name/${i.toString().padStart(3, '0')}")
                    }
                }
                source.recycle(); backdrop.recycle(); frame.recycle()
            }
        } finally { renderer.dispose() }
    }

    private fun save(bitmap: Bitmap, name: String) {
        val file = File("build/outputs/material-page-review/$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }
}

internal fun publicationFixture(state: MaterialReviewState, next: Boolean = false, large: Boolean = false): Bitmap {
    val page = Bitmap.createBitmap(412, 900, Bitmap.Config.ARGB_8888)
    page.eraseColor(if (state.dark) Color.rgb(22, 20, 26) else if (state.sepia) Color.rgb(241, 225, 198) else Color.rgb(250, 247, 239))
    val paint = TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = if (state.dark) Color.rgb(221, 217, 225) else Color.rgb(53, 49, 43)
        textSize = if (large) 28f else 19f
        typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.NORMAL)
    }
    val text = if (state.persian) {
        "فصل دوم\n\nدر سکوت کتابخانه، نور آرام بر صفحه‌ها می‌نشست. هر واژه راهی به جهان تازه بود و هر برگ نشانی از سفر خواننده.\n\n".repeat(5)
    } else {
        (if (next) "THE NEXT PASSAGE\n\n" else "THE QUIET READER\n\n") +
            "The light moved slowly across the room. Beyond the window, the leaves turned in the evening air. She rested her hand upon the book and continued reading.\n\n".repeat(8)
    }
    val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, 344)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(5f, 1.05f)
        .setTextDirection(if (state.persian) android.text.TextDirectionHeuristics.RTL else android.text.TextDirectionHeuristics.LTR)
        .build()
    val canvas = Canvas(page)
    canvas.translate(34f, 70f)
    layout.draw(canvas)
    return page
}
