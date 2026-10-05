package com.veilreader.app

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/** Export only inert QA images before UTP uninstalls the debug application. */
internal fun exportGrayfogCapture(file: File) {
    require(file.parentFile?.name == "grayfog-review")
    require(file.name.matches(Regex("[A-Za-z0-9_-]+\\.png")))
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val packageName = instrumentation.targetContext.packageName
    require(packageName.matches(Regex("[A-Za-z0-9_.]+")))
    // UiAutomation runs as shell. The private file is read through debug-only run-as;
    // the shell owns the exported copy, so uninstall cannot erase review evidence.
    val command = "mkdir -p /data/local/tmp/veil-grayfog-review && " +
        "run-as $packageName cat files/grayfog-review/${file.name} " +
        "> /data/local/tmp/veil-grayfog-review/${file.name} && echo VEIL_CAPTURE_EXPORTED"
    val result = android.os.ParcelFileDescriptor.AutoCloseInputStream(
        instrumentation.uiAutomation.executeShellCommand(command)
    ).bufferedReader().use { it.readText() }
    check(result.trim() == "VEIL_CAPTURE_EXPORTED") { "QA capture export failed: $result" }
}
