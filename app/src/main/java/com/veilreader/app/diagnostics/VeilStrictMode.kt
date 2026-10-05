package com.veilreader.app.diagnostics

import android.os.Build
import android.os.StrictMode
import com.veilreader.app.BuildConfig

/**
 * Debug-only runtime diagnostics for accidental main-thread I/O and leaked Android resources.
 *
 * Penalties intentionally log instead of crashing so exploratory Reader/device QA stays usable.
 * Release builds never install these policies.
 */
object VeilStrictMode {
    fun installForDebug() {
        if (!BuildConfig.DEBUG) return

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .detectCustomSlowCalls()
                .detectResourceMismatches()
                .penaltyLog()
                .build()
        )

        val vmPolicy = StrictMode.VmPolicy.Builder()
            .detectActivityLeaks()
            .detectLeakedClosableObjects()
            .detectLeakedSqlLiteObjects()
            .detectLeakedRegistrationObjects()
            .detectFileUriExposure()
            .detectCleartextNetwork()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vmPolicy.detectUnsafeIntentLaunch()
        }

        StrictMode.setVmPolicy(
            vmPolicy
                .penaltyLog()
                .build()
        )
    }
}
