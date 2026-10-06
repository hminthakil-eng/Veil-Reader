package com.veilreader.app.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.veilreader.app.exportGrayfogCapture
import com.veilreader.app.ui.review.GrayfogReviewActivity
import com.veilreader.app.ui.review.GrayfogReviewSurface
import java.io.File
import java.util.Locale
import kotlin.math.max
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Pixel evidence for dialog-backed Grayfog surfaces.
 *
 * These captures intentionally launch the real debug Activity on an emulator. Platform Dialog
 * windows inherit the configured root-view context, so large-text evidence is not faked by a
 * CompositionLocal density override that only affects the parent composition.
 */
@RunWith(Parameterized::class)
class DialogVisualEvidenceTest(
    private val language: String,
    private val highContrast: Boolean
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} contrast={1}")
        fun cases(): List<Array<Any>> =
            listOf("en", "fa").flatMap { language ->
                listOf(false, true).map { contrast ->
                    arrayOf<Any>(language, contrast)
                }
            }
    }

    @Test
    fun dialogSurfacesUseRealWindowFontScaleAndProduceDistinctLargeTextEvidence() {
        for (target in listOf(
            GrayfogReviewSurface.BOOK_DETAIL,
            GrayfogReviewSurface.RITUAL,
            GrayfogReviewSurface.ERROR
        )) {
            val normal = capture(target, 1f)
            val large = capture(target, 2f)
            try {
                val ratio = sampledPixelDifferenceRatio(normal, large)
                assertTrue(
                    "${target.name} ${language} ${if (highContrast) "contrast" else "standard"} " +
                        "100% and 200% captures are unexpectedly equivalent (difference=$ratio)",
                    ratio >= 0.01
                )
            } finally {
                normal.recycle()
                large.recycle()
            }
        }
    }

    private fun capture(target: GrayfogReviewSurface, scale: Float): Bitmap {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(appContext, GrayfogReviewActivity::class.java).apply {
            putExtra(GrayfogReviewActivity.EXTRA_SURFACE, target.name)
            putExtra(GrayfogReviewActivity.EXTRA_LOCALE, language)
            putExtra(GrayfogReviewActivity.EXTRA_FONT_SCALE, scale)
            putExtra(GrayfogReviewActivity.EXTRA_HIGH_CONTRAST, highContrast)
        }

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch<GrayfogReviewActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val content = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
                val root = checkNotNull(content.getChildAt(0))
                assertEquals(scale, root.resources.configuration.fontScale, 0.01f)
                val actualLanguage = root.resources.configuration.locales[0].language
                assertEquals(Locale.forLanguageTag(language).language, actualLanguage)
            }

            instrumentation.waitForIdleSync()
            // Reduced-motion dialogs still use a short fade/reveal. Wait beyond that finite window
            // before taking the shell-owned screenshot.
            Thread.sleep(350)

            val screenshot = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            val cropped = cropSystemBars(screenshot)
            screenshot.recycle()

            val directory = File(appContext.filesDir, "grayfog-review").apply { mkdirs() }
            val name =
                "dialog-${target.name.lowercase()}-$language-${(scale * 100).toInt()}-" +
                    "${if (highContrast) "contrast" else "standard"}.png"
            val file = File(directory, name)
            file.outputStream().use { output ->
                check(cropped.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
            exportGrayfogCapture(file)
            return cropped
        }
    }

    private fun cropSystemBars(source: Bitmap): Bitmap {
        val top = max(0, (source.height * 0.08f).toInt())
        val bottom = max(top + 1, (source.height * 0.92f).toInt())
        return Bitmap.createBitmap(source, 0, top, source.width, bottom - top)
    }

    /**
     * Compare down-sampled pixels so status-bar clocks and PNG metadata cannot satisfy the guard.
     * One percent is deliberately conservative: a genuine 100% -> 200% text/layout change should
     * alter far more than isolated anti-aliasing noise.
     */
    private fun sampledPixelDifferenceRatio(first: Bitmap, second: Bitmap): Double {
        val width = 180
        val height = max(
            1,
            (first.height.toDouble() / first.width.toDouble() * width.toDouble()).toInt()
        )
        val a = Bitmap.createScaledBitmap(first, width, height, true)
        val b = Bitmap.createScaledBitmap(second, width, height, true)
        try {
            val pixelsA = IntArray(width * height)
            val pixelsB = IntArray(width * height)
            a.getPixels(pixelsA, 0, width, 0, 0, width, height)
            b.getPixels(pixelsB, 0, width, 0, 0, width, height)
            var changed = 0
            for (index in pixelsA.indices) {
                if (pixelsA[index] != pixelsB[index]) changed++
            }
            return changed.toDouble() / pixelsA.size.toDouble()
        } finally {
            a.recycle()
            b.recycle()
        }
    }
}
