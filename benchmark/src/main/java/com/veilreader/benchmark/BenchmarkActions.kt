package com.veilreader.benchmark

import android.content.ComponentName
import android.content.Intent
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

internal const val TARGET_PACKAGE = "com.veilreader.app"
internal const val READER_ACTIVITY = "com.veilreader.app.benchmark.BenchmarkReaderActivity"

internal fun readerIntent(): Intent = Intent().apply {
    component = ComponentName(TARGET_PACKAGE, READER_ACTIVITY)
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
}

internal fun MacrobenchmarkScope.awaitReaderSurface(timeoutMs: Long = 20_000L) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val appResources = instrumentation.context.packageManager
        .getResourcesForApplication(TARGET_PACKAGE)
    val surfaceLabelId = appResources.getIdentifier(
        "reader_surface_label",
        "string",
        TARGET_PACKAGE
    )
    check(surfaceLabelId != 0) {
        "Reader surface accessibility resource is missing from $TARGET_PACKAGE"
    }
    val surfaceLabel = appResources.getString(surfaceLabelId)
    check(device.wait(Until.hasObject(By.desc(surfaceLabel)), timeoutMs)) {
        "Reader surface did not become ready for benchmark"
    }
    device.waitForIdle()
}

internal fun MacrobenchmarkScope.turnReaderPages(turns: Int = 6) {
    val width = device.displayWidth
    val height = device.displayHeight
    repeat(turns) {
        device.swipe(
            width * 4 / 5,
            height / 2,
            width / 5,
            height / 2,
            18
        )
        Thread.sleep(180)
    }
}
