package com.veilreader.benchmark

import android.content.ComponentName
import android.content.Intent
import androidx.benchmark.macro.MacrobenchmarkScope

internal const val TARGET_PACKAGE = "com.veilreader.app"
internal const val READER_ACTIVITY = "com.veilreader.app.benchmark.BenchmarkReaderActivity"

internal fun readerIntent(): Intent = Intent().apply {
    component = ComponentName(TARGET_PACKAGE, READER_ACTIVITY)
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
