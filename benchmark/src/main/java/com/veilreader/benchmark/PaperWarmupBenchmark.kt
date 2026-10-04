package com.veilreader.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures the Reader-open window that prepares the first canonical Paper snapshot.
 *
 * This deliberately includes the idle warm-capture phase: the product contract is
 * that expensive source capture moves before the user's first gesture without
 * creating visible startup jank.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMetricApi::class)
class PaperWarmupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun firstPaperSnapshotPreparation() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(
            FrameTimingMetric(),
            TraceSectionMetric(
                sectionName = "paper.capture.prepare",
                mode = TraceSectionMetric.Mode.Max,
                label = "paperPrepareCaptureMax"
            ),
            TraceSectionMetric(
                sectionName = "paper.capture.view_draw",
                mode = TraceSectionMetric.Mode.Max,
                label = "paperViewDrawCaptureMax"
            )
        ),
        compilationMode = CompilationMode.None(),
        iterations = 6,
        setupBlock = {
            pressHome()
        }
    ) {
        startActivityAndWait(readerIntent())
        awaitReaderSurface()
        Thread.sleep(500)
    }
}
