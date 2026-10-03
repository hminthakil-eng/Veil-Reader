package com.veilreader.app

import android.app.Activity
import android.app.UiAutomation
import android.content.ContentValues
import android.content.Intent
import android.os.Environment
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
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
import org.junit.After
import org.junit.Before
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

    @Before
    fun forcePortraitStart() {
        uiAutomation.setRotation(UiAutomation.ROTATION_FREEZE_0)
        SystemClock.sleep(500)
    }

    @After
    fun releaseRotation() {
        uiAutomation.setRotation(UiAutomation.ROTATION_UNFREEZE)
    }

    @Test
    fun importedPdf_exposesFitLayoutAndSurvivesRotation() {
        uiAutomation.executeShellCommand("pm clear com.google.android.documentsui").close()
        SystemClock.sleep(500)
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

        // Reveal chrome before synthetic native PDF gestures. Multi-pointer dispatch is renderer-level
        // coverage and must not be allowed to poison the Readium tap-arbiter state used by this gate.
        revealReaderChrome(pdfView)
        exerciseNativePdfGestures(pdfView)
        clickText("Zoom")
        waitForText("PDF zoom")
        waitForText("Fit page width")

        // These are radio choices, not toggles. Establish the starting state explicitly.
        clickDescription(target.getString(R.string.pdf_paginated_layout))
        waitForText(target.getString(R.string.pdf_page_description))
        val before = currentPdfLayoutLabel()
        clickDescription(target.getString(R.string.pdf_continuous_scroll))
        val after = waitForPdfLayoutLabel(excluding = before)
        assertNotEquals("PDF layout toggle did not change mode", before, after)

        // Exercise the renderer's manual fit path while the real PDFView is attached.
        clickText("Fit page width")

        // Restore the original layout so this test does not leak reader preference state.
        clickDescription(target.getString(R.string.pdf_paginated_layout))
        waitForPdfLayoutLabel(excluding = after)
        pressAndroidBack()

        exerciseNativeInternalLinkAndReturn(pdfView)
        instrumentation.runOnMainSync { pdfView.jumpTo(1, false) }
        waitForPdfPage(pdfView, 1)
        SystemClock.sleep(1_000)

        uiAutomation.setRotation(UiAutomation.ROTATION_FREEZE_90)
        try {
            waitForPackage(target.packageName)
            val rotatedActivity = waitForResumedActivity(excluding = activity)
            val rotatedPdfView = waitForPdfView(rotatedActivity)
            waitForPdfPage(rotatedPdfView, 1)
            revealReaderChrome(rotatedPdfView)
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
            var resumed: Activity? = null
            instrumentation.runOnMainSync {
                resumed = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .firstOrNull { it !== excluding }
            }
            resumed?.let { return it }
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
        val doubleTapTarget = readDoubleTapTarget(view, initialZoom)
        dispatchDoubleTap(view)

        // AndroidPdfViewer animates a double tap toward mid/max/min zoom. Waiting only until
        // zoom rises above the baseline races that animation: a subsequent resetZoom() can be
        // overwritten by the still-running animator. Wait for the actual animation target first.
        waitForZoomNear(view, doubleTapTarget)
        val doubleTapZoom = readZoom(view)
        assertTrue("Double-tap did not increase PDF zoom", doubleTapZoom > initialZoom)

        val baselineZoom = readMinZoom(view)
        instrumentation.runOnMainSync { view.resetZoom() }
        waitForZoomNear(view, baselineZoom)

        dispatchPinchOut(view)
        val pinchZoom = waitForZoomAbove(view, baselineZoom)
        assertTrue("Pinch did not increase PDF zoom", pinchZoom > baselineZoom)

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
        waitForZoomNear(view, baselineZoom)
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

    private fun readMinZoom(view: PDFView): Float {
        var value = 1f
        instrumentation.runOnMainSync { value = view.minZoom }
        return value
    }

    private fun readDoubleTapTarget(view: PDFView, currentZoom: Float): Float {
        var target = currentZoom
        instrumentation.runOnMainSync {
            target = when {
                currentZoom < view.midZoom -> view.midZoom
                currentZoom < view.maxZoom -> view.maxZoom
                else -> view.minZoom
            }
        }
        return target
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
        val resolver = instrumentation.targetContext.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val displayName = "VeilReaderQa.pdf"

        resolver.delete(
            collection,
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
            arrayOf(displayName),
        )
        uiAutomation.executeShellCommand("rm -f /sdcard/Download/$displayName").close()

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(collection, values)) {
            "Unable to create PDF fixture in MediaStore Downloads"
        }
        resolver.openOutputStream(uri, "w")!!.use { output ->
            instrumentation.context.assets.open("pdf/veil-links-annotations.pdf")
                .use { it.copyTo(output) }
        }
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        SystemClock.sleep(1_000)
    }

    private fun pressAndroidBack() {
        uiAutomation.executeShellCommand("input keyevent KEYCODE_BACK").close()
        SystemClock.sleep(750)
    }

    private fun revealReaderChrome(view: PDFView) {
        if (hasNode { it.text?.toString() == "Zoom" }) return

        val readerSurface = waitForNode("Reader surface") {
            it.contentDescription?.toString() == "Reader surface"
        }

        try {
            check(readerSurface.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                "Reader surface rejected ACTION_CLICK; actions=" +
                    readerSurface.actionList.joinToString { it.label?.toString() ?: it.id.toString() }
            }
        } finally {
            readerSurface.recycleCompat()
        }

        uiAutomation.waitForIdle(250, 2_000)
        val deadline = SystemClock.elapsedRealtime() + 5_000L
        while (SystemClock.elapsedRealtime() < deadline) {
            if (hasNode { it.text?.toString() == "Zoom" }) return
            SystemClock.sleep(POLL_MS)
        }

        error(
            "Reader surface accepted ACTION_CLICK but chrome did not appear; page=" +
                readCurrentPage(view) +
                ", zoom=" + readZoom(view) +
                ", attached=" + view.isAttachedToWindow +
                ", shown=" + view.isShown +
                ", windowFocus=" + view.hasWindowFocus() +
                ", size=" + view.width + "x" + view.height
        )
    }

    private fun tapViewCenter(view: View) {
        val location = IntArray(2)
        instrumentation.runOnMainSync { view.getLocationOnScreen(location) }
        val x = location[0] + view.width / 2
        val y = location[1] + view.height / 2
        uiAutomation.executeShellCommand("input tap $x $y").close()
        SystemClock.sleep(500)
    }

    private fun currentPdfLayoutLabel(): String =
        waitForNode("PDF layout label", ::isPdfLayoutLabel).readTextAndRecycle()

    private fun isPdfLayoutLabel(node: AccessibilityNodeInfo): Boolean {
        val target = instrumentation.targetContext
        val text = node.text?.toString()
        return text == target.getString(R.string.pdf_page_description) ||
            text == target.getString(R.string.pdf_scroll_description)
    }

    private fun waitForPdfLayoutLabel(excluding: String): String =
        waitForNode("changed PDF layout label") {
            isPdfLayoutLabel(it) && it.text?.toString() != excluding
        }.readTextAndRecycle()

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
        waitForNode("text=$text") { it.text?.toString() == text }.recycleCompat()
    }

    private fun clickDescription(description: String) {
        waitForNode("description=$description") {
            it.contentDescription?.toString() == description
        }.also(::clickNode)
    }

    private fun waitForPackage(packageName: String) {
        waitForNode("package=$packageName") { it.packageName?.toString() == packageName }.recycleCompat()
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

    private fun findClickableNode(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? =
        findNode(predicate)

    private fun hasNode(predicate: (AccessibilityNodeInfo) -> Boolean): Boolean =
        findNode(predicate)?.let { it.recycleCompat(); true } ?: false

    private fun clickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        while (current != null && !current.isClickable) {
            val parent = current.parent
            if (current !== node) current.recycleCompat()
            current = parent
        }
        return current
    }

    private fun findNode(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        val root = uiAutomation.rootInActiveWindow ?: return null
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        try {
            while (queue.isNotEmpty()) {
                val node = queue.removeFirst()
                var matched = false
                try {
                    matched = predicate(node)
                    if (matched) return node
                    for (index in 0 until node.childCount) {
                        node.getChild(index)?.let(queue::add)
                    }
                } finally {
                    if (!matched) node.recycleCompat()
                }
            }
            return null
        } finally {
            queue.forEach { it.recycleCompat() }
        }
    }

    private fun clickNode(node: AccessibilityNodeInfo) {
        val current = clickableAncestor(node)
        try {
            val clicked = current?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
            if (!clicked) {
                val bounds = android.graphics.Rect()
                (current ?: node).getBoundsInScreen(bounds)
                check(!bounds.isEmpty) { "Accessibility node has no tappable screen bounds" }
                uiAutomation.executeShellCommand(
                    "input tap ${bounds.centerX()} ${bounds.centerY()}"
                ).close()
            }
        } finally {
            if (current !== node) current?.recycleCompat()
            node.recycleCompat()
        }
        SystemClock.sleep(750)
    }

    private fun AccessibilityNodeInfo.readTextAndRecycle(): String =
        try { text.toString() } finally { recycleCompat() }

    @Suppress("DEPRECATION") // Pooling exists on supported API 26–32; API 33+ recycle is a no-op.
    private fun AccessibilityNodeInfo.recycleCompat() {
        if (Build.VERSION.SDK_INT < 33) recycle()
    }

    private fun exerciseNativeInternalLinkAndReturn(view: PDFView) {
        instrumentation.runOnMainSync { view.jumpTo(0, false) }
        waitForPdfPage(view, 0)
        SystemClock.sleep(750) // Let Readium's debounced source locator become the return origin.
        val tap = IntArray(2)
        instrumentation.runOnMainSync {
            val link = view.getLinks(0).single()
            assertEquals(2, link.destPageIdx)
            val pageSize = view.getPageSize(0)
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            // This fixture has equal, unrotated 612x792 pages. No production geometry shortcut.
            tap[0] = (location[0] + view.currentXOffset +
                link.bounds.centerX() / 612f * pageSize.width * view.zoom).toInt()
            tap[1] = (location[1] + view.currentYOffset +
                (792f - link.bounds.centerY()) / 792f * pageSize.height * view.zoom).toInt()
        }
        uiAutomation.executeShellCommand("input tap ${tap[0]} ${tap[1]}").close()
        waitForPdfPage(view, 2)
        revealReaderChrome(view)
        val returnLabel = instrumentation.targetContext.getString(R.string.reader_previous_location)
        waitForText(returnLabel)
        clickText(returnLabel)
        waitForPdfPage(view, 0)
        SystemClock.sleep(750)
    }

    private companion object {
        const val TIMEOUT_MS = 20_000L
        const val POLL_MS = 250L

    }
}
