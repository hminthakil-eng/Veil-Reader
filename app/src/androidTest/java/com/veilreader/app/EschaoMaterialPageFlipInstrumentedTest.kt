package com.veilreader.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A/B gate for the actual eschao Apache-2 native 3D OpenGL page fold.
 * Checks the distinct debug surface can be launched and is visible.
 * Not a physical 60/120 Hz, front/back texture or Readium acceptance.
 */
@RunWith(AndroidJUnit4::class)
class EschaoMaterialPageFlipInstrumentedTest {
    @Test
    fun separateEschaoLabHasExplicitUriAndVisibleNativeGlSurface() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val uriIntent = Intent(Intent.ACTION_VIEW, Uri.parse("veilpaperlab://eschao"))
            .addCategory(Intent.CATEGORY_DEFAULT)
            .setPackage(context.packageName)
        @Suppress("DEPRECATION")
        val targets = context.packageManager.queryIntentActivities(uriIntent, 0)
            .map { it.activityInfo.name }
            .toSet()
        assertEquals(
            "The eschao lab must not resolve to Veil Reader's real MainActivity",
            setOf("com.veilreader.app.debug.EschaoPageFlipArenaActivity"),
            targets
        )

        val launch = Intent(Intent.ACTION_VIEW).apply {
            setClassName(context.packageName, "com.veilreader.app.debug.EschaoPageFlipArenaActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val activity: Activity = instrumentation.startActivitySync(launch)
        try {
            assertEquals("com.veilreader.app.debug.EschaoPageFlipArenaActivity", activity.javaClass.name)
            val deadline = SystemClock.elapsedRealtime() + 8_000L
            var visible = false
            while (SystemClock.elapsedRealtime() < deadline && !visible) {
                instrumentation.runOnMainSync {
                    val queue = ArrayDeque<View>()
                    queue.add(activity.window.decorView)
                    while (queue.isNotEmpty()) {
                        val view = queue.removeFirst()
                        if (view is GLSurfaceView && view.isAttachedToWindow &&
                            view.isShown && view.width > 0 && view.height > 0) {
                            visible = true
                            break
                        }
                        if (view is ViewGroup) for (i in 0 until view.childCount) {
                            queue.addLast(view.getChildAt(i))
                        }
                    }
                }
                if (!visible) SystemClock.sleep(80)
            }
            assertTrue("OpenGL PageFlip arena must show a laid-out native GLSurfaceView", visible)
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
            instrumentation.waitForIdleSync()
        }
    }
}
