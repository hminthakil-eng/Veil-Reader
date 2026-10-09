package com.veilreader.app.ui.reader.material

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

/** Exercise the native GL host independently of Readium, motion settings and locator probes. */
class GpuSheetAcquisitionInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun realAcquiredBuffersAcknowledgeDistinctSheetEpochs() {
        val ready = AtomicBoolean(false)
        val failed = AtomicBoolean(false)
        val acquired = AtomicLong(0)
        val host = AtomicReference<GpuMaterialPageCurlView>()
        var bitmap: Bitmap? = null
        compose.setContent {
            AndroidView(factory = { context ->
                GpuMaterialPageCurlView(context,
                    onRendererReady = ready::set,
                    onRendererFailure = { failed.set(true) },
                    onSheetPresented = acquired::set
                ).also(host::set)
            }, modifier = Modifier.size(240.dp))
        }
        try {
            compose.waitUntil(timeoutMillis = 5_000) { ready.get() || failed.get() }
            assertFalse("GPU initialization failed", failed.get())
            compose.runOnIdle {
                bitmap = Bitmap.createBitmap(host.get().width, host.get().height, Bitmap.Config.ARGB_8888)
                    .apply { eraseColor(Color.BLUE) }
            }
            for (epoch in listOf(7L, 20L)) {
                compose.runOnIdle {
                    host.get().submitFrame(bitmap, active = true,
                        curl = gpuPageCurlFrame(progress = .2f, verticalBias = 0f, pullOriginY = .5f,
                            profile = MaterialPageProfiles.MatteBook, side = MaterialPageSide.RIGHT),
                        profile = MaterialPageProfiles.MatteBook, patina = .35f,
                        tone = MaterialPageTone.LIGHT, visualAlpha = 1f, highContrast = false,
                        sheetEpoch = epoch)
                }
                compose.waitUntil(timeoutMillis = 5_000) { acquired.get() == epoch || failed.get() }
                assertFalse("GPU draw failed", failed.get())
                assertEquals(epoch, acquired.get())
            }
        } finally {
            compose.runOnIdle { host.get()?.pauseRenderer() }
            bitmap?.recycle()
        }
    }
}
