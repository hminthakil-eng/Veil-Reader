package com.veilreader.app

import android.app.Activity
import android.app.UiAutomation
import android.content.Intent
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.github.barteksc.pdfviewer.PDFView
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReaderPdfReliabilityInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val uiAutomation: UiAutomation
        get() = instrumentation.uiAutomation

    @Test
    fun importedPdf_exposesFitLayoutAndSurvivesRotation() {
        seedPdfFixture()

        val target = instrumentation.targetContext
        val launchIntent = target.packageManager
            .getLaunchIntentForPackage(target.packageName)
        assertNotNull("Launch intent missing", launchIntent)

        val activity = instrumentation.startActivitySync(
            launchIntent!!.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        )
        instrumentation.waitForIdleSync()
        uiAutomation.waitForIdle(500, 5_000)

        clickText("Library")
        clickFirstText("Import", "Import a book")
        waitForPackage("com.google.android.documentsui")

        clickDescription("Show roots")
        clickText("Downloads")
        clickText("VeilReaderQa.pdf")

        waitForPackage(target.packageName)
        SystemClock.sleep(2_000)

        val pdfView = waitForPdfView(activity)
        exerciseNativePdfGestures(pdfView)

        revealReaderChrome()
        clickText("Zoom")
        waitForText("PDF zoom")
        waitForText("Fit page width")

        val before = currentPdfLayoutLabel()
        clickDescription("PDF continuous scroll")
        val after = waitForPdfLayoutLabel(excluding = before)
        assertNotEquals("PDF layout toggle did not change mode", before, after)

        // Exercise the renderer's manual fit path while the real PDFView is attached.
        clickText("Fit page width")

        // Restore the original layout so this test does not leak reader preference state.
        clickDescription("PDF continuous scroll")
        waitForPdfLayoutLabel(excluding = after)
        pressAndroidBack()

        instrumentation.runOnMainSync { pdfView.jumpTo(1, false) }
        waitForPdfPage(pdfView, 1)
        SystemClock.sleep(1_000)

        uiAutomation.setRotation(UiAutomation.ROTATION_FREEZE_90)
        try {
            waitForPackage(target.packageName)
            val rotatedActivity = waitForResumedActivity(excluding = activity)
            val rotatedPdfView = waitForPdfView(rotatedActivity)
            waitForPdfPage(rotatedPdfView, 1)
            revealReaderChrome()
            waitForText("Zoom")
        } finally {
            uiAutomation.setRotation(UiAutomation.ROTATION_UNFREEZE)
        }
    }

    private fun waitForPdfView(activity: Activity): PDFView {
        val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            var found: PDFView? = null
            instrumentation.runOnMainSync {
                found = activity.window.decorView.findPdfView()
            }
            found?.let { view ->
                if (readPageCount(view) > 0) return view
            }
            SystemClock.sleep(POLL_MS)
        }
        error("Timed out waiting for rendered PDFView")
    }

    private fun waitForResumedActivity(excluding: Activity): Activity {
        val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            val resumed = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .firstOrNull { it !== excluding }
            if (resumed != null) return resumed
            SystemClock.sleep(POLL_MS)
        }
        error("Timed out waiting for recreated reader Activity")
    }

    private fun waitForPdfPage(view: PDFView, expected: Int) {
        val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            if (readCurrentPage(view) == expected) return
            SystemClock.sleep(POLL_MS)
        }
        error("Timed out waiting for PDF page " + expected + "; current=" + readCurrentPage(view))
    }

    private fun exerciseNativePdfGestures(view: PDFView) {
        val initialZoom = readZoom(view)
        dispatchDoubleTap(view)
        val doubleTapZoom = waitForZoomAbove(view, initialZoom)
        assertTrue("Double-tap did not increase PDF zoom", doubleTapZoom > initialZoom)

        instrumentation.runOnMainSync { view.resetZoom() }
        waitForZoomNear(view, initialZoom)

        dispatchPinchOut(view)
        val pinchZoom = waitForZoomAbove(view, initialZoom)
        assertTrue("Pinch did not increase PDF zoom", pinchZoom > initialZoom)

        val (beforeX, beforeY) = readOffsets(view)
        dispatchPan(view)
        val deadline = SystemClock.elapsedRealtime() + 3_000
        var moved = false
        while (SystemClock.elapsedRealtime() < deadline) {
            val (afterX, afterY) = readOffsets(view)
            if (abs(afterX - beforeX) > 1f || abs(afterY - beforeY) > 1f) {
                moved = true
                break
            }
            SystemClock.sleep(100)
        }
        assertTrue("Pan did not move the zoomed PDF viewport", moved)

        instrumentation.runOnMainSync { view.resetZoom() }
        waitForZoomNear(view, initialZoom)
    }

    private fun dispatchDoubleTap(view: PDFView) {
        instrumentation.runOnMainSync {
            val x = view.width / 2f
            val y = view.height / 2f
            val base = SystemClock.uptimeMillis()
            dispatchSinglePointer(view, MotionEvent.ACTION_DOWN, x, y, base, base)
            dispatchSinglePointer(view, MotionEvent.ACTION_UP, x, y, base, base + 40)
            dispatchSinglePointer(view, MotionEvent.ACTION_DOWN, x, y, base + 120, base + 120)
            dispatchSinglePointer(view, MotionEvent.ACTION_UP, x, y, base + 120, base + 160)
        }
    }

    private fun dispatchPan(view: PDFView) {
        instrumentation.runOnMainSync {
            val y = view.height / 2f
            val startX = view.width * 0.72f
            val endX = view.width * 0.28f
            val base = SystemClock.uptimeMillis()
            dispatchSinglePointer(view, MotionEvent.ACTION_DOWN, startX, y, base, base)
            for (step in 1..5) {
                val fraction = step / 5f
                val x = startX + (endX - startX) * fraction
                dispatchSinglePointer(view, MotionEvent.ACTION_MOVE, x, y, base, base + step * 30L)
            }
            dispatchSinglePointer(view, MotionEvent.ACTION_UP, endX, y, base, base + 190)
        }
    }

    private fun dispatchPinchOut(view: PDFView) {
        instrumentation.runOnMainSync {
            val centerX = view.width / 2f
            val centerY = view.height / 2f
            val base = SystemClock.uptimeMillis()
            val properties = arrayOf(
                MotionEvent.PointerProperties().apply {
                    id = 0
                    toolType = MotionEvent.TOOL_TYPE_FINGER
                },
                MotionEvent.PointerProperties().apply {
                    id = 1
                    toolType = MotionEvent.TOOL_TYPE_FINGER
                }
            )

            fun coords(distance: Float): Array<MotionEvent.PointerCoords> = arrayOf(
                MotionEvent.PointerCoords().apply {
                    x = centerX - distance
                    y = centerY
                    pressure = 1f
                    size = 1f
                },
                MotionEvent.PointerCoords().apply {
                    x = centerX + distance
                    y = centerY
                    pressure = 1f
                    size = 1f
                }
            )

            dispatchMultiPointer(view, MotionEvent.ACTION_DOWN, 1, properties, coords(70f), base, base)
            dispatchMultiPointer(
                view,
                MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                2,
                properties,
                coords(70f),
                base,
                base + 30
            )
            for (step in 1..6) {
                dispatchMultiPointer(
                    view,
                    MotionEvent.ACTION_MOVE,
                    2,
                    properties,
                    coords(70f + step * 35f),
                    base,
                    base + 30L + step * 25L
                )
            }
            dispatchMultiPointer(
                view,
                MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                2,
                properties,
                coords(280f),
                base,
                base + 210
            )
            dispatchMultiPointer(view, MotionEvent.ACTION_UP, 1, properties, coords(280f), base, base + 240)
        }
    }

    private fun dispatchSinglePointer(
        view: View,
        action: Int,
        x: Float,
        y: Float,
        downTime: Long,
        eventTime: Long
    ) {
        val event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0).apply {
            source = InputDevice.SOURCE_TOUCHSCREEN
        }
        try {
            view.dispatchTouchEvent(event)
        } finally {
            event.recycle()
        }
    }

    private fun dispatchMultiPointer(
        view: View,
        action: Int,
        pointerCount: Int,
        properties: Array<MotionEvent.PointerProperties>,
        coords: Array<MotionEvent.PointerCoords>,
        downTime: Long,
        eventTime: Long
    ) {
        val event = MotionEvent.obtain(
            downTime,
            eventTime,
            action,
            pointerCount,
            properties,
            coords,
            0,
            0,
            1f,
            1f,
            0,
            0,
            InputDevice.SOURCE_TOUCHSCREEN,
            0
        )
        try {
            view.dispatchTouchEvent(event)
        } finally {
            event.recycle()
        }
    }

    private fun waitForZoomAbove(view: PDFView, baseline: Float): Float {
        val deadline = SystemClock.elapsedRealtime() + 3_000
        var value = readZoom(view)
        while (SystemClock.elapsedRealtime() < deadline) {
            value = readZoom(view)
            if (value > baseline + 0.05f) return value
            SystemClock.sleep(100)
        }
        return value
    }

    private fun waitForZoomNear(view: PDFView, expected: Float) {
        val deadline = SystemClock.elapsedRealtime() + 3_000
        while (SystemClock.elapsedRealtime() < deadline) {
            if (abs(readZoom(view) - expected) < 0.05f) return
            SystemClock.sleep(100)
        }
        assertEquals(expected, readZoom(view), 0.05f)
    }

    private fun readZoom(view: PDFView): Float {
        var value = 1f
        instrumentation.runOnMainSync { value = view.zoom }
        return value
    }

    private fun readOffsets(view: PDFView): Pair<Float, Float> {
        var value = 0f to 0f
        instrumentation.runOnMainSync {
            value = view.currentXOffset to view.currentYOffset
        }
        return value
    }

    private fun readCurrentPage(view: PDFView): Int {
        var value = -1
        instrumentation.runOnMainSync { value = view.currentPage }
        return value
    }

    private fun readPageCount(view: PDFView): Int {
        var value = 0
        instrumentation.runOnMainSync { value = view.pageCount }
        return value
    }

    private fun View.findPdfView(): PDFView? {
        if (this is PDFView) return this
        if (this !is ViewGroup) return null
        for (index in 0 until childCount) {
            getChildAt(index).findPdfView()?.let { return it }
        }
        return null
    }

    private fun seedPdfFixture() {
        val command =
            "sh -c \"printf '%s' '$PDF_BASE64' | base64 -d > /sdcard/Download/VeilReaderQa.pdf\""
        uiAutomation.executeShellCommand(command).close()
        SystemClock.sleep(500)
    }

    private fun pressAndroidBack() {
        uiAutomation.executeShellCommand("input keyevent KEYCODE_BACK").close()
        SystemClock.sleep(750)
    }

    private fun revealReaderChrome() {
        val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            if (findClickableNode { it.text?.toString() == "Zoom" } != null) return
            tapReaderCenter()
            SystemClock.sleep(750)
        }
        error("Timed out revealing PDF reader chrome")
    }

    private fun tapReaderCenter() {
        val metrics = instrumentation.targetContext.resources.displayMetrics
        uiAutomation.executeShellCommand(
            "input tap ${metrics.widthPixels / 2} ${metrics.heightPixels / 2}"
        ).close()
        SystemClock.sleep(500)
    }

    private fun currentPdfLayoutLabel(): String =
        waitForNode("PDF layout label") {
            val text = it.text?.toString().orEmpty()
            text.startsWith("Paginated") || text.startsWith("Vertical flow")
        }.text.toString()

    private fun waitForPdfLayoutLabel(excluding: String): String =
        waitForNode("changed PDF layout label") {
            val text = it.text?.toString().orEmpty()
            (text.startsWith("Paginated") || text.startsWith("Vertical flow")) &&
                text != excluding
        }.text.toString()

    private fun clickFirstText(vararg candidates: String) {
        val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            candidates.forEach { candidate ->
                findClickableNode { it.text?.toString() == candidate }?.let {
                    clickNode(it)
                    return
                }
            }
            SystemClock.sleep(POLL_MS)
        }
        error("None of the text targets appeared: ${candidates.joinToString()}")
    }

    private fun clickText(text: String) {
        val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            findClickableNode { it.text?.toString() == text }?.let {
                clickNode(it)
                return
            }
            SystemClock.sleep(POLL_MS)
        }
        error("Timed out waiting for clickable text=$text")
    }

    private fun waitForText(text: String) {
        waitForNode("text=$text") { it.text?.toString() == text }
    }

    private fun clickDescription(description: String) {
        waitForNode("description=$description") {
            it.contentDescription?.toString() == description
        }.also(::clickNode)
    }

    private fun waitForPackage(packageName: String) {
        waitForNode("package=$packageName") { it.packageName?.toString() == packageName }
    }

    private fun waitForNode(
        label: String,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo {
        val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            findNode(predicate)?.let { return it }
            SystemClock.sleep(POLL_MS)
        }
        error("Timed out waiting for $label")
    }

    private fun findClickableNode(
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        val root = uiAutomation.rootInActiveWindow ?: return null
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (predicate(node) && clickableAncestor(node) != null) return node
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(queue::add)
            }
        }
        return null
    }

    private fun clickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        while (current != null && !current.isClickable) current = current.parent
        return current
    }

    private fun findNode(
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        val root = uiAutomation.rootInActiveWindow ?: return null
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (predicate(node)) return node
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(queue::add)
            }
        }
        return null
    }

    private fun clickNode(node: AccessibilityNodeInfo) {
        val current = checkNotNull(clickableAncestor(node)) {
            "No clickable ancestor for accessibility node"
        }
        check(current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            "Accessibility click failed"
        }
        SystemClock.sleep(750)
    }

    private companion object {
        const val TIMEOUT_MS = 20_000L
        const val POLL_MS = 250L

        // Three-page, text-only PDF 1.4 fixture. Kept inline so the instrumentation gate is
        // hermetic and does not depend on host-side files or network access.
        const val PDF_BASE64 =
            "JVBERi0xLjQKJeLjz9MKMSAwIG9iago8PCAvVHlwZSAvQ2F0YWxvZyAvUGFnZXMgMiAwIFIgPj4KZW5kb2JqCjIgMCBvYmoKPDwgL1R5cGUgL1BhZ2VzIC9LaWRzIFszIDAgUiA1IDAgUiA3IDAgUl0gL0NvdW50IDMgPj4KZW5kb2JqCjMgMCBvYmoKPDwgL1R5cGUgL1BhZ2UgL1BhcmVudCAyIDAgUiAvTWVkaWFCb3ggWzAgMCA2MTIgNzkyXSAvUmVzb3VyY2VzIDw8IC9Gb250IDw8IC9GMSA5IDAgUiA+PiA+PiAvQ29udGVudHMgNCAwIFIgPj4KZW5kb2JqCjQgMCBvYmoKPDwgL0xlbmd0aCAxMzcgPj4Kc3RyZWFtCkJUIC9GMSAyNCBUZiA3MiA3MDAgVGQgKFZlaWwgUmVhZGVyIFBERiBTbW9rZSBQYWdlIDEpIFRqIDAgLTQwIFRkIC9GMSAxNCBUZiAoUGluY2ggem9vbSwgZml0LCBzY3JvbGwsIG9yaWVudGF0aW9uIGFuZCByZXN1bWUgdGVzdC4pIFRqIEVUCmVuZHN0cmVhbQplbmRvYmoKNSAwIG9iago8PCAvVHlwZSAvUGFnZSAvUGFyZW50IDIgMCBSIC9NZWRpYUJveCBbMCAwIDYxMiA3OTJdIC9SZXNvdXJjZXMgPDwgL0ZvbnQgPDwgL0YxIDkgMCBSID4+ID4+IC9Db250ZW50cyA2IDAgUiA+PgplbmRvYmoKNiAwIG9iago8PCAvTGVuZ3RoIDEzNyA+PgpzdHJlYW0KQlQgL0YxIDI0IFRmIDcyIDcwMCBUZCAoVmVpbCBSZWFkZXIgUERGIFNtb2tlIFBhZ2UgMikgVGogMCAtNDAgVGQgL0YxIDE0IFRmIChQaW5jaCB6b29tLCBmaXQsIHNjcm9sbCwgb3JpZW50YXRpb24gYW5kIHJlc3VtZSB0ZXN0LikgVGogRVQKZW5kc3RyZWFtCmVuZG9iago3IDAgb2JqCjw8IC9UeXBlIC9QYWdlIC9QYXJlbnQgMiAwIFIgL01lZGlhQm94IFswIDAgNjEyIDc5Ml0gL1Jlc291cmNlcyA8PCAvRm9udCA8PCAvRjEgOSAwIFIgPj4gPj4gL0NvbnRlbnRzIDggMCBSID4+CmVuZG9iago4IDAgb2JqCjw8IC9MZW5ndGggMTM3ID4+CnN0cmVhbQpCVCAvRjEgMjQgVGYgNzIgNzAwIFRkIChWZWlsIFJlYWRlciBQREYgU21va2UgUGFnZSAzKSBUaiAwIC00MCBUZCAvRjEgMTQgVGYgKFBpbmNoIHpvb20sIGZpdCwgc2Nyb2xsLCBvcmllbnRhdGlvbiBhbmQgcmVzdW1lIHRlc3QuKSBUaiBFVAplbmRzdHJlYW0KZW5kb2JqCjkgMCBvYmoKPDwgL1R5cGUgL0ZvbnQgL1N1YnR5cGUgL1R5cGUxIC9CYXNlRm9udCAvSGVsdmV0aWNhID4+CmVuZG9iagp4cmVmCjAgMTAKMDAwMDAwMDAwMCA2NTUzNSBmIAowMDAwMDAwMDE1IDAwMDAwIG4gCjAwMDAwMDAwNjQgMDAwMDAgbiAKMDAwMDAwMDEzMyAwMDAwMCBuIAowMDAwMDAwMjU5IDAwMDAwIG4gCjAwMDAwMDA0NDcgMDAwMDAgbiAKMDAwMDAwMDU3MyAwMDAwMCBuIAowMDAwMDAwNzYxIDAwMDAwIG4gCjAwMDAwMDA4ODcgMDAwMDAgbiAKMDAwMDAwMTA3NSAwMDAwMCBuIAp0cmFpbGVyCjw8IC9TaXplIDEwIC9Sb290IDEgMCBSID4+CnN0YXJ0eHJlZgoxMTQ1CiUlRU9GCg=="
    }
}
