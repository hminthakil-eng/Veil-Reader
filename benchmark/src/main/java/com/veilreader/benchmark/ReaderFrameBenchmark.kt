package com.veilreader.benchmark

import androidx.benchmark.macro.BaselineProfileMode
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

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMetricApi::class)
class ReaderFrameBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun pageTurns() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(
            FrameTimingMetric(),
            TraceSectionMetric(
                sectionName = "paper.gpu.draw",
                mode = TraceSectionMetric.Mode.Max,
                label = "paperGpuDraw"
            ),
            TraceSectionMetric(
                sectionName = "paper.gpu.texture_upload",
                mode = TraceSectionMetric.Mode.Max,
                label = "paperTextureUpload"
            )
        ),
        compilationMode = CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.UseIfAvailable
        ),
        iterations = 6,
        setupBlock = {
            pressHome()
            startActivityAndWait(readerIntent())
            awaitReaderSurface()
        }
    ) {
        turnReaderPages(turns = 8)
    }
}
