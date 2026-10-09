package com.veilreader.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingGfxInfoMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * GitHub-hosted emulator smoke lane.
 *
 * The API 35 lavapipe emulator used in CI does not reliably emit the Perfetto frame-timeline
 * expect/actual slices required by FrameTimingMetric. This lane uses AndroidX's gfxinfo-backed
 * frame metric to keep a real catastrophic-regression guardrail without manufacturing missing
 * Perfetto samples. ReaderFrameBenchmark remains the canonical trace-based physical-device gate.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMetricApi::class)
class ReaderFrameSmokeBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coverTapControlsAndEdgeAdvance() = exerciseCover(svgCover = false)

    @Test
    fun svgCoverTapControlsAndEdgeAdvance() = exerciseCover(svgCover = true)

    private fun exerciseCover(svgCover: Boolean) = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingGfxInfoMetric()),
        compilationMode = CompilationMode.None(),
        iterations = 1,
        setupBlock = {
            pressHome()
            startActivityAndWait(readerIntent().putExtra("benchmark_svg_cover", svgCover))
            awaitReaderSurface()
        }
    ) {
        val origin = readerLocator()
        check(origin.contains("cover.xhtml")) { "Fixture did not open on its image cover: $origin" }
        val resources = InstrumentationRegistry.getInstrumentation().context.packageManager
            .getResourcesForApplication(TARGET_PACKAGE)
        val closeLabel = resources.getString(resources.getIdentifier("reader_close", "string", TARGET_PACKAGE))
        device.click(device.displayWidth / 2, device.displayHeight * 4 / 5)
        check(device.wait(Until.hasObject(By.desc(closeLabel)), 3_000)) {
            "Center tap on cover did not open Reader chrome"
        }
        device.click(device.displayWidth / 2, device.displayHeight * 4 / 5)
        check(device.wait(Until.gone(By.desc(closeLabel)), 3_000)) {
            "Center tap did not dismiss Reader chrome"
        }
        // The lower edge is outside the image, so this exercises navigation without image viewing.
        device.click(device.displayWidth * 19 / 20, device.displayHeight * 4 / 5)
        awaitReaderLocatorDeparture(origin)
        Thread.sleep(650)
        check(readerLocator().contains("c1.xhtml")) {
            "Edge tap did not commit cover-to-chapter navigation: ${readerLocator()}"
        }
    }

    @Test
    fun pageTurns() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingGfxInfoMetric()),
        compilationMode = CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.UseIfAvailable
        ),
        iterations = 6,
        setupBlock = {
            pressHome()
            startActivityAndWait(readerIntent())
            awaitReaderSurface()

            // Keep startup/first-composition work out of the steady-state page-turn smoke sample.
            // Two unmeasured turns also make the gfxinfo percentile less sensitive to a single
            // lavapipe outlier on the ~30-frame CI sample. Budgets remain unchanged.
            turnReaderPages(turns = 2)
            device.waitForIdle()
            Thread.sleep(650)
        }
    ) {
        turnReaderPages(turns = 12)
    }
}
