package com.veilreader.app

import android.app.UiAutomation
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReaderSafImportInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val uiAutomation: UiAutomation
        get() = instrumentation.uiAutomation

    @Test
    fun importedEpub_opensReaderThroughAndroidSaf() {
        uiAutomation.executeShellCommand("pm clear com.google.android.documentsui").close()
        SystemClock.sleep(500)

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
        clickText("VeilReaderQa.epub")

        waitForPackage(target.packageName)
        waitForViewId(
            "com.veilreader.app:id/resourcePager",
            "com.veilreader.app:id/webView"
        )
    }

    private fun clickFirstText(vararg candidates: String) {
        val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            candidates.forEach { candidate ->
                findNode { it.text?.toString() == candidate }?.let {
                    clickNode(it)
                    return
                }
            }
            SystemClock.sleep(POLL_MS)
        }
        error("None of the text targets appeared: ${candidates.joinToString()}")
    }

    private fun clickText(text: String) {
        waitForNode("text=$text") { it.text?.toString() == text }.also(::clickNode)
    }

    private fun clickDescription(description: String) {
        waitForNode("description=$description") {
            it.contentDescription?.toString() == description
        }.also(::clickNode)
    }

    private fun waitForPackage(packageName: String) {
        waitForNode("package=$packageName") { it.packageName?.toString() == packageName }
    }

    private fun waitForViewId(vararg ids: String) {
        waitForNode("viewId=${ids.joinToString()}") {
            it.viewIdResourceName in ids
        }
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
        var current: AccessibilityNodeInfo? = node
        while (current != null && !current.isClickable) {
            current = current.parent
        }
        checkNotNull(current) { "No clickable ancestor for accessibility node" }
        check(current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            "Accessibility click failed"
        }
        SystemClock.sleep(750)
    }

    private companion object {
        const val TIMEOUT_MS = 15_000L
        const val POLL_MS = 250L
    }
}
