package com.veilreader.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    /**
     * The Baseline Profile Gradle plugin generates this against nonMinifiedRelease.
     * Keep this journey on production-visible entry points only. The deterministic
     * reader fixture is benchmark-build-only and is exercised by ReaderFrameBenchmark.
     */
    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = TARGET_PACKAGE
    ) {
        startActivityAndWait()
        device.waitForIdle()
    }
}
