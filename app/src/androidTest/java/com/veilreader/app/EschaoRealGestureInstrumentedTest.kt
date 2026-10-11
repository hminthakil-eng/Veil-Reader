package com.veilreader.app

import android.app.Activity
import android.content.Intent
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.opengl.GLSurfaceView
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Red test for the real user complaint: Page Curl looks like Slide or never
 * changes a page. Unlike a view-tree check, this injects a complete edge drag
 * through Android's input dispatcher into eschao's native OpenGL page mesh.
 *
 * A pass proves one synthetic page transition in emulator ONLY, not that it
 * looks or feels like a physical page on a real 60/120Hz phone.
 */
@RunWith(AndroidJUnit4::class)
class EschaoRealGestureInstrumentedTest {

    @Test
    fun fullEdgeDragActuallyTurnsOneMaterialPageNotJustShowsSurface() {
        val instr = InstrumentationRegistry.getInstrumentation()
        val target = instr.targetContext
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setClassName(target.packageName, "com.veilreader.app.debug.EschaoPageFlipArenaActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val activity: Activity = instr.startActivitySync(intent)
        try {
            assertEquals("com.veilreader.app.debug.EschaoPageFlipArenaActivity", activity.javaClass.name)
            val surface = arrayOfNulls<GLSurfaceView>(1)
            val caption = arrayOfNulls<TextView>(1)
            val attachedDeadline = SystemClock.elapsedRealtime() + 8_000L
            while (SystemClock.elapsedRealtime() < attachedDeadline && surface[0] == null) {
                instr.runOnMainSync {
                    val nodes = ArrayDeque<View>()
                    nodes.add(activity.window.decorView)
                    while (nodes.isNotEmpty()) {
                        val view = nodes.removeFirst()
                        if (view is GLSurfaceView &&
                            view.isAttachedToWindow && view.isShown &&
                            view.width > 0 && view.height > 0) surface[0] = view
                        if (view is TextView && view.text.contains("VEIL PAPER ARENA")) caption[0] = view
                        if (view is ViewGroup) for (i in 0 until view.childCount) {
                            nodes.addLast(view.getChildAt(i))
                        }
                    }
                }
                if (surface[0] == null) SystemClock.sleep(80)
            }
            assertNotNull("Native GL fold view is missing", surface[0])
            assertNotNull("A/B page transition HUD is missing", caption[0])
            // Let the first texture upload finish before starting a deliberate
            // slow swipe: starting before the surface is ready is a different gate.
            instr.waitForIdleSync()
            SystemClock.sleep(400)
            val bounds = IntArray(2)
            val size = IntArray(2)
            instr.runOnMainSync {
                surface[0]!!.getLocationOnScreen(bounds)
                size[0] = surface[0]!!.width
                size[1] = surface[0]!!.height
            }
            val startX = bounds[0] + size[0] * 0.90f
            val endX = bounds[0] + size[0] * 0.12f
            val atY = bounds[1] + size[1] * 0.60f
            val time = SystemClock.uptimeMillis()
            fun inject(action: Int, x: Float, at: Long) {
                val e = MotionEvent.obtain(time, at, action, x, atY, 0).apply {
                    source = InputDevice.SOURCE_TOUCHSCREEN
                }
                try { instr.sendPointerSync(e) } finally { e.recycle() }
            }
            inject(MotionEvent.ACTION_DOWN, startX, time)
            for (step in 1..16) {
                val progress = step / 16f
                val x = startX + (endX - startX) * progress
                inject(MotionEvent.ACTION_MOVE, x, time + 35L * step)
            }
            inject(MotionEvent.ACTION_UP, endX, time + 35L * 17L)
            val changedDeadline = SystemClock.elapsedRealtime() + 5_000L
            var advanced = false
            while (SystemClock.elapsedRealtime() < changedDeadline && !advanced) {
                instr.runOnMainSync { advanced = caption[0]!!.text.contains("Sheet 2 / 8") }
                if (!advanced) SystemClock.sleep(80)
            }
            assertTrue(
                "A real edge drag must complete eschao forward fold and advance to sheet 2; " +
                    "a merely visible GLSurfaceView is NOT enough",
                advanced
            )
        } finally {
            instr.runOnMainSync { activity.finish() }
            instr.waitForIdleSync()
        }
    }
}
