package com.veilreader.benchmark

import android.content.ComponentName
import android.content.Intent
import android.os.SystemClock
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
    check(device.wait(Until.hasObject(By.descStartsWith(LOCATOR_PREFIX)), timeoutMs)) {
        "Actual Readium locator probe did not attach"
    }
    // Surface semantics may appear before startup handoff and first painted WebView frame.
    Thread.sleep(800)
}

internal fun MacrobenchmarkScope.turnReaderPages(turns: Int = 6) {
    val width = device.displayWidth
    val height = device.displayHeight
    repeat(turns) {
        val origin = readerLocator()
        device.swipe(
            width * 4 / 5,
            height / 2,
            width / 5,
            height / 2,
            18
        )
        awaitReaderLocatorDeparture(origin)
        // Locator departure occurs under the curl. Do not race the next gesture with settlement.
        Thread.sleep(650)
    }
}

private const val LOCATOR_PREFIX = "benchmark-reader-locator:"

internal fun MacrobenchmarkScope.readerLocator(): String =
    requireNotNull(device.findObject(By.descStartsWith(LOCATOR_PREFIX))?.contentDescription) {
        "Actual Readium locator is unavailable"
    }

internal fun MacrobenchmarkScope.awaitReaderLocatorDeparture(origin: String) {
    val deadline = SystemClock.uptimeMillis() + 8_000L
    while (SystemClock.uptimeMillis() < deadline) {
        if (readerLocator() != origin) return
        Thread.sleep(50)
    }
    error("Gesture did not change the actual Readium locator: $origin")
}
