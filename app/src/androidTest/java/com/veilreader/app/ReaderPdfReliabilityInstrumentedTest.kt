package com.veilreader.app

import android.app.UiAutomation
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
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

        instrumentation.startActivitySync(
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

        uiAutomation.setRotation(UiAutomation.ROTATION_FREEZE_90)
        try {
            waitForPackage(target.packageName)
            SystemClock.sleep(1_000)
            revealReaderChrome()
            waitForText("Zoom")
        } finally {
            uiAutomation.setRotation(UiAutomation.ROTATION_UNFREEZE)
        }
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
