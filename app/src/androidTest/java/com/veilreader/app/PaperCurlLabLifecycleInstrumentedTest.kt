package com.veilreader.app

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.irurueta.android.gl.curl.CurlTextureView
import com.veilreader.app.debug.ExternalCurlArenaActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Native GLTextureView is NOT auto-paused/resumed by the Activity lifecycle.
 * Upstream requires the owner to forward onPause()/onResume() explicitly.
 * Characterise pause -> resume without a lost GL view or Activity crash.
 *
 * This does not prove GPU mesh geometry or physical-device frame pacing.
 */
@RunWith(AndroidJUnit4::class)
class PaperCurlLabLifecycleInstrumentedTest {
    @Test
    fun nativeCurlSurfaceRemainsUsableAfterBackgroundForeground() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(Intent.ACTION_VIEW)
            .setClassName(context.packageName, ExternalCurlArenaActivity::class.java.name)
        ActivityScenario.launch<ExternalCurlArenaActivity>(intent).use { scenario ->
            repeat(2) {
                scenario.moveToState(Lifecycle.State.STARTED)
                scenario.moveToState(Lifecycle.State.RESUMED)
                scenario.onActivity { activity ->
                    assertEquals(ExternalCurlArenaActivity::class.java, activity.javaClass)
                    val nodes = ArrayDeque<View>()
                    nodes.add(activity.window.decorView)
                    var foundVisibleGl = false
                    while (nodes.isNotEmpty()) {
                        val view = nodes.removeFirst()
                        if (view is CurlTextureView && view.isAttachedToWindow && view.isShown &&
                            view.width > 0 && view.height > 0) {
                            foundVisibleGl = true
                            break
                        }
                        if (view is ViewGroup) {
                            for (index in 0 until view.childCount) {
                                nodes.addLast(view.getChildAt(index))
                            }
                        }
                    }
                    assertTrue("Native CurlTextureView must remain visible after Activity resume", foundVisibleGl)
                }
            }
        }
    }
}
