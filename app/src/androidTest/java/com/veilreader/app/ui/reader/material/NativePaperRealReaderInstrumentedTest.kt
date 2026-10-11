package com.veilreader.app.ui.reader.material

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.Fragment
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.veilreader.app.debug.NativePaperReaderActivity
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi

/** Uses the production importer + Readium + ReaderScreen, never a fake page counter. */
@OptIn(ExperimentalReadiumApi::class)
@RunWith(AndroidJUnit4::class)
class NativePaperRealReaderInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val automation = instrumentation.uiAutomation

    @Test fun realEpub_bendsDuringDrag_cancelRestores_releaseChangesReadiumLocator() {
        verifyRealBook(rtl = false)
    }

    @Test fun persianRtlEpub_bendsFromLeft_cancelRestores_releaseChangesReadiumLocator() {
        verifyRealBook(rtl = true)
    }

    private fun verifyRealBook(rtl: Boolean) {
        val prefix = if (rtl) "rtl" else "ltr"
        val launch = Intent(instrumentation.targetContext, NativePaperReaderActivity::class.java)
            .putExtra(NativePaperReaderActivity.EXTRA_RTL_BOOK, rtl)
        ActivityScenario.launch<NativePaperReaderActivity>(launch).use { scenario ->
            var activity: NativePaperReaderActivity? = null
            scenario.onActivity { activity = it }
            val host = requireNotNull(activity)
            fun native(): NativeMaterialPageCurlView? = views(host.window.decorView)
                .filterIsInstance<NativeMaterialPageCurlView>().firstOrNull()
            fun navigator(): EpubNavigatorFragment? = fragments(host.supportFragmentManager.fragments)
                .filterIsInstance<EpubNavigatorFragment>().firstOrNull()
            fun locator(): String? {
                var value: String? = null
                instrumentation.runOnMainSync { value = navigator()?.currentLocator?.value?.toJSON()?.toString() }
                return value
            }
            waitUntil("Native GL + real EPUB navigator ready") {
                var ready = false
                instrumentation.runOnMainSync {
                    ready = native()?.renderingReady == true && navigator()?.currentLocator?.value != null &&
                        views(host.window.decorView).filterIsInstance<WebView>().any {
                            it.isShown && it.isAttachedToWindow && it.width > 0 && it.height > 0
                        }
                }
                ready
            }
            SystemClock.sleep(400)
            instrumentation.runOnMainSync {
                assertEquals("The publication must drive physical turn direction",
                    if (rtl) ReadingProgression.RTL else ReadingProgression.LTR,
                    navigator()?.overflow?.value?.readingProgression)
            }
            val original = requireNotNull(locator())
            val bounds = Rect()
            instrumentation.runOnMainSync {
                val web = views(host.window.decorView).filterIsInstance<WebView>()
                    .first { it.isShown && it.width > 0 && it.height > 0 }
                assertTrue(web.getGlobalVisibleRect(bounds))
            }
            val baseline = screenshot("$prefix-01-origin.png")
            val direction = if (rtl) 1f else -1f
            val x = bounds.left + bounds.width() * (if (rtl) 0.05f else 0.95f)
            val y = bounds.top + bounds.height() * 0.72f
            var down = SystemClock.uptimeMillis()
            inject(MotionEvent.ACTION_DOWN, x, y, down)
            for (step in 1..14) {
                inject(MotionEvent.ACTION_MOVE, x + direction * bounds.width() * 0.57f * step / 14, y - step * 1.5f, down)
                SystemClock.sleep(22)
            }
            waitUntil("Actual source buffer acquired") {
                var acquired = false
                instrumentation.runOnMainSync { acquired = (native()?.lastAcquiredSheetEpoch ?: 0L) > 0L }
                acquired
            }
            assertEquals("A held fold must never navigate", original, locator())
            val held = screenshot("$prefix-02-held-real-epub.png")
            assertTrue("Real page pixels must visibly deform before release",
                changedFraction(baseline, held, bounds) > 0.04)
            baseline.recycle(); held.recycle()
            // Deliberately reverse the pull before releasing: same locator, no commit.
            for (step in 13 downTo 0) {
                inject(MotionEvent.ACTION_MOVE, x + direction * bounds.width() * 0.57f * step / 14, y - step * 1.5f, down)
                SystemClock.sleep(22)
            }
            inject(MotionEvent.ACTION_UP, x, y, down)
            SystemClock.sleep(900)
            assertEquals("Reversal must restore the exact location", original, locator())
            screenshot("$prefix-03-cancelled.png").recycle()

            down = SystemClock.uptimeMillis()
            inject(MotionEvent.ACTION_DOWN, x, y, down)
            for (step in 1..16) {
                inject(MotionEvent.ACTION_MOVE, x + direction * bounds.width() * 0.83f * step / 16, y, down)
                SystemClock.sleep(20)
            }
            inject(MotionEvent.ACTION_UP, x + direction * bounds.width() * 0.83f, y, down)
            waitUntil("Released fold changes the actual Readium location") { locator() != null && locator() != original }
            SystemClock.sleep(900)
            val committed = locator()
            assertNotEquals(original, committed)
            SystemClock.sleep(300)
            assertEquals("A single release must settle once", committed, locator())
            screenshot("$prefix-04-committed.png").recycle()

            // Exercise the opposite physical edge in the same real publication.
            var previousEpoch = 0L
            instrumentation.runOnMainSync { previousEpoch = native()?.lastAcquiredSheetEpoch ?: 0L }
            val backX = bounds.left + bounds.width() * (if (rtl) 0.95f else 0.05f)
            val backY = bounds.top + bounds.height() * 0.32f
            down = SystemClock.uptimeMillis()
            inject(MotionEvent.ACTION_DOWN, backX, backY, down)
            for (step in 1..14) {
                inject(MotionEvent.ACTION_MOVE, backX - direction * bounds.width() * 0.57f * step / 14, backY, down)
                SystemClock.sleep(22)
            }
            waitUntil("Opposite-edge source buffer acquired") {
                var acquired = false
                instrumentation.runOnMainSync {
                    acquired = (native()?.lastAcquiredSheetEpoch ?: 0L) > previousEpoch
                }
                acquired
            }
            assertEquals("A held backward fold must never navigate", committed, locator())
            screenshot("$prefix-05-backward-held.png").recycle()
            inject(MotionEvent.ACTION_UP, backX - direction * bounds.width() * 0.57f, backY, down)
            waitUntil("Backward release changes the actual location") { locator() != null && locator() != committed }
            SystemClock.sleep(900)
            val beforeBack = org.json.JSONObject(requireNotNull(committed))
            val afterBack = org.json.JSONObject(requireNotNull(locator()))
            assertEquals(beforeBack.getString("href"), afterBack.getString("href"))
            assertTrue("Backward must reduce the actual book progression",
                afterBack.getJSONObject("locations").getDouble("progression") <
                    beforeBack.getJSONObject("locations").getDouble("progression"))
            screenshot("$prefix-06-backward-committed.png").recycle()
        }
    }

    private fun inject(action: Int, x: Float, y: Float, down: Long) {
        val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0)
        try {
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            assertTrue("Input injection failed", automation.injectInputEvent(event, true))
        } finally { event.recycle() }
    }

    private fun screenshot(name: String): Bitmap {
        val bitmap = requireNotNull(automation.takeScreenshot())
        val directory = File(instrumentation.targetContext.filesDir, "native-paper-evidence").apply { mkdirs() }
        File(directory, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return bitmap
    }

    private fun changedFraction(before: Bitmap, after: Bitmap, bounds: Rect): Double {
        var changed = 0
        var total = 0
        // Ignore chrome/HUD; compare ink and paper in the middle of the real viewport.
        val top = (bounds.top + bounds.height() * 0.2).toInt().coerceAtLeast(0)
        val bottom = (bounds.bottom - bounds.height() * 0.2).toInt().coerceAtMost(before.height)
        for (y in top until bottom step 4) for (x in bounds.left.coerceAtLeast(0) until bounds.right.coerceAtMost(before.width) step 4) {
            val a = before.getPixel(x, y)
            val b = after.getPixel(x, y)
            val difference = kotlin.math.abs(android.graphics.Color.red(a) - android.graphics.Color.red(b)) +
                kotlin.math.abs(android.graphics.Color.green(a) - android.graphics.Color.green(b)) +
                kotlin.math.abs(android.graphics.Color.blue(a) - android.graphics.Color.blue(b))
            if (difference > 45) changed++
            total++
        }
        return changed.toDouble() / total.coerceAtLeast(1)
    }

    private fun views(root: View): List<View> = buildList {
        add(root)
        if (root is ViewGroup) for (i in 0 until root.childCount) addAll(views(root.getChildAt(i)))
    }
    private fun fragments(roots: List<Fragment>): List<Fragment> = roots.flatMap { listOf(it) + fragments(it.childFragmentManager.fragments) }
    private fun waitUntil(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 20_000
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return
            SystemClock.sleep(40)
        }
        fail(message)
    }
}
