package com.veilreader.app

import android.app.Activity
import android.content.Intent
import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Page ownership is transaction-like: ACTION_CANCEL must never commit a
 * partial fold, even if the touch has crossed the release threshold.
 *
 * Also verifies a subsequent deliberate turn works, catching stale GLES
 * front/back texture state after rollback.
 */
@RunWith(AndroidJUnit4::class)
class EschaoCancelGestureInstrumentedTest {
    @Test
    fun canceledCurlDoesNotTurnAndNextDeliberateSwipeStillWorks() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setClassName(context.packageName, "com.veilreader.app.debug.EschaoPageFlipArenaActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val activity: Activity = instrumentation.startActivitySync(intent)
        try {
            val gl = arrayOfNulls<GLSurfaceView>(1)
            val pageLabel = arrayOfNulls<TextView>(1)
            val readyDeadline = SystemClock.elapsedRealtime() + 8_000L
            while (SystemClock.elapsedRealtime() < readyDeadline && gl[0] == null) {
                instrumentation.runOnMainSync {
                    val nodes = ArrayDeque<View>()
                    nodes.add(activity.window.decorView)
                    while (nodes.isNotEmpty()) {
                        val view = nodes.removeFirst()
                        if (view is GLSurfaceView && view.isAttachedToWindow &&
                            view.isShown && view.width > 0 && view.height > 0) gl[0] = view
                        if (view is TextView && view.text.contains("VEIL PAPER ARENA")) pageLabel[0] = view
                        if (view is ViewGroup) for (i in 0 until view.childCount) {
                            nodes.addLast(view.getChildAt(i))
                        }
                    }
                }
                if (gl[0] == null) SystemClock.sleep(80)
            }
            assertNotNull("No EGL surface in new engine", gl[0])
            assertNotNull("No page number to validate cancellation", pageLabel[0])
            instrumentation.waitForIdleSync()
            SystemClock.sleep(400)

            val offset = IntArray(2)
            val dims = IntArray(2)
            instrumentation.runOnMainSync {
                gl[0]!!.getLocationOnScreen(offset)
                dims[0] = gl[0]!!.width
                dims[1] = gl[0]!!.height
            }
            val xStart = offset[0] + dims[0] * 0.90f
            val xEnd = offset[0] + dims[0] * 0.12f
            val y = offset[1] + dims[1] * 0.60f

            fun drag(terminalAction: Int) {
                val start = SystemClock.uptimeMillis()
                fun send(action: Int, x: Float, eventTime: Long) {
                    val e = MotionEvent.obtain(start, eventTime, action, x, y, 0).apply {
                        source = InputDevice.SOURCE_TOUCHSCREEN
                    }
                    try { instrumentation.sendPointerSync(e) } finally { e.recycle() }
                }
                send(MotionEvent.ACTION_DOWN, xStart, start)
                for (step in 1..16) {
                    val fraction = step / 16f
                    send(MotionEvent.ACTION_MOVE, xStart + (xEnd - xStart) * fraction,
                        start + step * 35L)
                }
                send(terminalAction, xEnd, start + 17L * 35L)
            }

            drag(MotionEvent.ACTION_CANCEL)
            // FINISH_MS=420. A canceled gesture must not ever commit to Sheet 2.
            SystemClock.sleep(1_000)
            var canceledStillOnFirst = false
            instrumentation.runOnMainSync {
                canceledStillOnFirst = pageLabel[0]!!.text.contains("Sheet 1 / 8")
            }
            assertTrue("ACTION_CANCEL must keep Sheet 1; no accidental locator commit",
                canceledStillOnFirst)

            drag(MotionEvent.ACTION_UP)
            val deadline = SystemClock.elapsedRealtime() + 5_000L
            var progressed = false
            while (SystemClock.elapsedRealtime() < deadline && !progressed) {
                instrumentation.runOnMainSync {
                    progressed = pageLabel[0]!!.text.contains("Sheet 2 / 8")
                }
                if (!progressed) SystemClock.sleep(80)
            }
            assertTrue(
                "After cancellation, renderer must recover and next real fold must reach Sheet 2",
                progressed
            )
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
            instrumentation.waitForIdleSync()
        }
    }
}
