package com.veilreader.app.ui.reader.material

import android.content.Intent
import android.animation.ValueAnimator
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
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
    private var capturePrefix = "reader"

    @Test fun realEpub_bendsDuringDrag_cancelRestores_releaseChangesReadiumLocator() {
        verifyRealBook(rtl = false)
    }

    @Test fun persianRtlEpub_bendsFromLeft_cancelRestores_releaseChangesReadiumLocator() {
        verifyRealBook(rtl = true)
    }

    private fun verifyRealBook(rtl: Boolean) {
        // General storage tests disable Android animators. Veil correctly treats
        // that as Reduced Motion; a real curl test must explicitly enable them.
        val previousScale = shell("settings get global animator_duration_scale").trim()
        require(previousScale == "null" || previousScale.matches(Regex("[0-9]+(?:\\.[0-9]+)?")))
        try {
            shell("settings put global animator_duration_scale 1")
            waitUntil("System animation policy allows an actual paper fold") {
                ValueAnimator.areAnimatorsEnabled()
            }
            verifyAnimatedBook(rtl)
        } finally {
            if (previousScale == "null") shell("settings delete global animator_duration_scale")
            else shell("settings put global animator_duration_scale $previousScale")
        }
    }

    private fun verifyAnimatedBook(rtl: Boolean) {
        val prefix = if (rtl) "rtl" else "ltr"
        capturePrefix = prefix
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
            fun sourceWeb(): WebView? = views(navigator()?.publicationView ?: host.window.decorView)
                .filterIsInstance<WebView>().firstOrNull {
                    val visible = Rect()
                    it.isShown && it.isAttachedToWindow && it.width > 0 && it.height > 0 &&
                        it.getGlobalVisibleRect(visible) &&
                        visible.width() > it.width * 0.75f && visible.height() > it.height * 0.75f
                }
            fun locator(): String? {
                var value: String? = null
                instrumentation.runOnMainSync { value = navigator()?.currentLocator?.value?.toJSON()?.toString() }
                return value
            }
            var paintRequested: WebView? = null
            var painted: WebView? = null
            var visiblePaperSince = 0L
            waitUntil("Native GL and actual publication pixels ready") {
                var ready = false
                val visibleBounds = Rect()
                instrumentation.runOnMainSync {
                    val web = sourceWeb()
                    val locationReady = navigator()?.currentLocator?.value?.locations?.progression != null
                    if (web != null && locationReady && paintRequested !== web) {
                        web.evaluateJavascript("document.readyState === 'complete' && document.body.innerText.length > 100") { result ->
                            if (result == "true" && paintRequested !== web) {
                                paintRequested = web
                                web.postVisualStateCallback(1L, object : WebView.VisualStateCallback() {
                                    override fun onComplete(requestId: Long) { painted = web }
                                })
                            }
                        }
                    }
                    ready = native()?.renderingReady == true && locationReady && web != null && painted === web
                    if (ready) web?.getGlobalVisibleRect(visibleBounds)
                }
                // DOM/accessibility readiness can precede the modal's actual fade-out.
                // Require this owned white-paper fixture's ink on the acquired screen,
                // stable across frames, before injecting a real touch into Readium.
                if (ready && fixtureInkVisible(visibleBounds)) {
                    if (visiblePaperSince == 0L) visiblePaperSince = SystemClock.elapsedRealtime()
                    SystemClock.elapsedRealtime() - visiblePaperSince >= 200L
                } else {
                    visiblePaperSince = 0L
                    false
                }
            }
            instrumentation.runOnMainSync {
                assertEquals("The publication must drive physical turn direction",
                    if (rtl) ReadingProgression.RTL else ReadingProgression.LTR,
                    navigator()?.overflow?.value?.readingProgression)
            }
            val original = requireNotNull(locator())
            val bounds = Rect()
            instrumentation.runOnMainSync {
                val web = requireNotNull(sourceWeb())
                assertTrue(web.getGlobalVisibleRect(bounds))
            }
            val inputWeb = requireNotNull(sourceWeb())
            evaluate(inputWeb, """(() => {
                window.__veilFixtureTouches = {touchstart:0,touchmove:0,touchend:0,touchcancel:0};
                for (const type of Object.keys(window.__veilFixtureTouches)) {
                    document.addEventListener(type, () => window.__veilFixtureTouches[type]++, {capture:true, passive:true});
                }
                return true;
            })()""")
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
            android.util.Log.i("VeilNativePaperProbe", "bounds=$bounds down=${host.receivedTouchDowns} dom=" +
                evaluate(inputWeb, "JSON.stringify({touches:window.__veilFixtureTouches, bridge:typeof Android, drag:typeof Android === 'undefined' ? 'missing' : typeof Android.onDragStart, selection:window.getSelection().isCollapsed})"))
            waitUntil("Actual source buffer acquired") {
                var acquired = false
                instrumentation.runOnMainSync { acquired = (native()?.currentAcquiredSheetEpoch ?: 0L) > 0L }
                acquired
            }
            assertEquals("A held fold must never navigate", original, locator())
            var sourceCopy: Bitmap? = null
            instrumentation.runOnMainSync {
                sourceCopy = requireNotNull(native()?.currentSourceSnapshot).copy(Bitmap.Config.ARGB_8888, false)
            }
            requireNotNull(sourceCopy).let { source ->
                saveCapture("$prefix-00-source-ink.png", source)
                try {
                    assertTrue("The uploaded Readium sheet must contain this fixture's actual ink",
                        sourceInkFraction(source) > 0.01)
                } finally { source.recycle() }
            }
            val held = screenshot("$prefix-02-held-real-epub.png")
            assertTrue("Real page pixels must visibly deform before release",
                changedFraction(baseline, held, bounds) > 0.04)
            assertTrue("The flat front must preserve book ink, not sample a blank texture corner",
                retainedFrontInk(baseline, held, bounds, rtl) > 0.75)
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
                    acquired = (native()?.currentAcquiredSheetEpoch ?: 0L) > previousEpoch
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
        val pointer = MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_FINGER
        }
        val coordinates = MotionEvent.PointerCoords().apply {
            this.x = x; this.y = y
            pressure = if (action == MotionEvent.ACTION_UP) 0f else 1f
            size = 1f
        }
        val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, 1,
            arrayOf(pointer), arrayOf(coordinates), 0, 0, 1f, 1f, 0, 0,
            InputDevice.SOURCE_TOUCHSCREEN, 0)
        try {
            assertTrue("Input injection failed", automation.injectInputEvent(event, true))
        } finally { event.recycle() }
    }

    private fun evaluate(web: WebView, script: String): String {
        val completed = CountDownLatch(1)
        var result = "callback-timeout"
        instrumentation.runOnMainSync {
            web.evaluateJavascript(script) { value -> result = value; completed.countDown() }
        }
        completed.await(2, TimeUnit.SECONDS)
        return result
    }

    private fun shell(command: String): String = android.os.ParcelFileDescriptor.AutoCloseInputStream(
        automation.executeShellCommand(command)
    ).bufferedReader().use { it.readText() }

    private fun screenshot(name: String): Bitmap {
        val bitmap = requireNotNull(automation.takeScreenshot())
        saveCapture(name, bitmap)
        return bitmap
    }

    private fun saveCapture(name: String, bitmap: Bitmap) {
        val directory = File(instrumentation.targetContext.filesDir, "native-paper-evidence").apply { mkdirs() }
        val file = File(directory, name)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        exportCapture(file)
    }

    /** UTP uninstalls the app; export owned fixture captures while it is still alive. */
    private fun exportCapture(file: File) {
        require(file.name.matches(Regex("[A-Za-z0-9_-]+\\.png")))
        val packageName = instrumentation.targetContext.packageName
        require(packageName.matches(Regex("[A-Za-z0-9_.]+")))
        val command = "mkdir -p /data/local/tmp/veil-native-paper-evidence && " +
            "run-as $packageName cat files/native-paper-evidence/${file.name} " +
            "> /data/local/tmp/veil-native-paper-evidence/${file.name} && echo VEIL_CAPTURE_EXPORTED"
        val script = File(requireNotNull(instrumentation.targetContext.externalCacheDir), "native-paper-export.sh")
        require(script.absolutePath.matches(Regex("[A-Za-z0-9_./-]+")))
        script.writeText("#!/system/bin/sh\n$command\n")
        try {
            val result = android.os.ParcelFileDescriptor.AutoCloseInputStream(
                automation.executeShellCommand("sh ${script.absolutePath}")
            ).bufferedReader().use { it.readText() }
            check(result.trim() == "VEIL_CAPTURE_EXPORTED") { "Fixture capture export failed: $result" }
        } finally { script.delete() }
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

    private fun sourceInkFraction(bitmap: Bitmap): Double {
        var ink = 0
        var total = 0
        for (y in bitmap.height / 4 until bitmap.height * 3 / 4 step 8) {
            for (x in bitmap.width / 10 until bitmap.width * 9 / 10 step 8) {
                val pixel = bitmap.getPixel(x, y)
                if (android.graphics.Color.alpha(pixel) > 200 && luminance(pixel) < 80) ink++
                total++
            }
        }
        return ink.toDouble() / total.coerceAtLeast(1)
    }

    private fun retainedFrontInk(before: Bitmap, after: Bitmap, bounds: Rect, rtl: Boolean): Double {
        val left = (bounds.left + bounds.width() * (if (rtl) 0.7 else 0.1)).toInt().coerceAtLeast(1)
        val right = (bounds.left + bounds.width() * (if (rtl) 0.9 else 0.3)).toInt().coerceAtMost(before.width - 1)
        val top = (bounds.top + bounds.height() * 0.25).toInt().coerceAtLeast(1)
        val bottom = (bounds.top + bounds.height() * 0.65).toInt().coerceAtMost(before.height - 1)
        var ink = 0
        var retained = 0
        for (y in top until bottom step 4) for (x in left until right step 4) {
            if (luminance(before.getPixel(x, y)) < 80) {
                ink++
                // Permit one pixel of hardware/software text rasterization difference.
                if ((y - 1..y + 1).any { yy -> (x - 1..x + 1).any { xx ->
                        luminance(after.getPixel(xx, yy)) < 150
                    } }) retained++
            }
        }
        assertTrue("The fixture must provide positive ink evidence", ink > 50)
        return retained.toDouble() / ink
    }

    private fun luminance(pixel: Int): Int = (android.graphics.Color.red(pixel) +
        android.graphics.Color.green(pixel) + android.graphics.Color.blue(pixel)) / 3

    private fun views(root: View): List<View> = buildList {
        add(root)
        if (root is ViewGroup) for (i in 0 until root.childCount) addAll(views(root.getChildAt(i)))
    }

    private fun fixtureInkVisible(bounds: Rect): Boolean {
        if (bounds.isEmpty) return false
        val bitmap = automation.takeScreenshot() ?: return false
        try {
            var paper = 0
            var ink = 0
            var total = 0
            val left = (bounds.left + bounds.width() * 0.1).toInt().coerceAtLeast(0)
            val right = (bounds.right - bounds.width() * 0.1).toInt().coerceAtMost(bitmap.width)
            val top = (bounds.top + bounds.height() * 0.25).toInt().coerceAtLeast(0)
            val bottom = (bounds.top + bounds.height() * 0.7).toInt().coerceAtMost(bitmap.height)
            for (y in top until bottom step 8) for (x in left until right step 8) {
                val pixel = bitmap.getPixel(x, y)
                val light = (android.graphics.Color.red(pixel) + android.graphics.Color.green(pixel) +
                    android.graphics.Color.blue(pixel)) / 3
                if (light > 245) paper++
                if (light < 80) ink++
                total++
            }
            return total > 0 && paper.toDouble() / total > 0.7 && ink.toDouble() / total > 0.01
        } finally { bitmap.recycle() }
    }
    private fun fragments(roots: List<Fragment>): List<Fragment> = roots.flatMap { listOf(it) + fragments(it.childFragmentManager.fragments) }
    private fun waitUntil(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 20_000
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return
            SystemClock.sleep(40)
        }
        screenshot("$capturePrefix-99-timeout.png").recycle()
        fail(message)
    }
}
