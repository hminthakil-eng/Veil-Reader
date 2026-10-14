package com.veilreader.app.diagnostics

import android.util.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import timber.log.Timber

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DiagnosticLogTest {
    @Test
    fun `durability marker reaches its existing logcat tag before returning`() {
        ShadowLog.clear()
        val forestBefore = Timber.forest()
        val marker = "VEIL_DURABILITY_RESULT scenario=background status=PASS"

        DiagnosticLog.info("VeilDurability", marker)

        val records = ShadowLog.getLogsForTag("VeilDurability")
        assertEquals(1, records.size)
        assertEquals(Log.INFO, records.single().type)
        assertEquals(marker, records.single().msg)
        assertEquals(forestBefore, Timber.forest())
    }

    @Test
    fun `failure diagnostics retain exception detail without enabling third party trees`() {
        ShadowLog.clear()

        DiagnosticLog.error("VeilPaper", "Upload failed", IllegalStateException("missing context"))

        assertTrue(ShadowLog.getLogsForTag("VeilPaper").any {
            it.type == Log.ERROR && it.msg.contains("Upload failed") &&
                it.msg.contains("missing context")
        })
    }
}
