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

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = TARGET_PACKAGE
    ) {
        startActivityAndWait()
        pressHome()

        startActivityAndWait(readerIntent())
        device.waitForIdle()
        Thread.sleep(1_000)
        turnReaderPages()
    }
}
