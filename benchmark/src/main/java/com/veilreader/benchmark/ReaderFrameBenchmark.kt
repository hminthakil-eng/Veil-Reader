package com.veilreader.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ReaderFrameBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun pageTurns() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.UseIfAvailable
        ),
        iterations = 6,
        setupBlock = {
            pressHome()
            startActivityAndWait(readerIntent())
            val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
            val surfaceLabelId = targetContext.resources.getIdentifier(
                "reader_surface_label",
                "string",
                TARGET_PACKAGE
            )
            check(surfaceLabelId != 0) {
                "Reader surface accessibility resource is missing from benchmark target"
            }
            val surfaceLabel = targetContext.getString(surfaceLabelId)
            check(device.wait(Until.hasObject(By.desc(surfaceLabel)), 20_000)) {
                "Reader surface did not become ready for frame benchmark"
            }
            device.waitForIdle()
        }
    ) {
        turnReaderPages(turns = 8)
    }
}
