package com.veilreader.app

import android.app.Activity
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.irurueta.android.gl.curl.CurlTextureView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Characterization of the debug-only native curl fixture.
 *
 * A successful launcher/deep-link resolution is NOT proof the GL view is
 * actually attached and visible. Check a real Activity view tree on emulator.
 * This test does not certify sheet geometry or physical 60/120Hz quality.
 */
@RunWith(AndroidJUnit4::class)
class PaperCurlLabSurfaceInstrumentedTest {

    @Test
    fun explicitlyStartedCurlLabShowsLaidOutNativeCurlSurface() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val component = "com.veilreader.app.debug.ExternalCurlArenaActivity"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setClassName(context.packageName, component)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val activity: Activity = instrumentation.startActivitySync(intent)
        try {
            assertEquals("Only the isolated debug Curl Lab may launch", component, activity.javaClass.name)
            val deadline = SystemClock.elapsedRealtime() + 8_000L
            var found = false
            while (SystemClock.elapsedRealtime() < deadline && !found) {
                instrumentation.runOnMainSync {
                    val root = activity.window.decorView
                    val nodes = ArrayDeque<View>()
                    nodes.add(root)
                    while (nodes.isNotEmpty()) {
                        val node = nodes.removeFirst()
                        if (node is CurlTextureView && node.isAttachedToWindow &&
                            node.isShown && node.width > 0 && node.height > 0) {
                            found = true
                            break
                        }
                        if (node is ViewGroup) {
                            for (index in 0 until node.childCount) {
                                nodes.addLast(node.getChildAt(index))
                            }
                        }
                    }
                }
                if (!found) SystemClock.sleep(80)
            }
            assertTrue(
                "Curl Lab launched but no visible, attached, non-zero native GL surface was found",
                found
            )
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
            instrumentation.waitForIdleSync()
        }
    }
}
