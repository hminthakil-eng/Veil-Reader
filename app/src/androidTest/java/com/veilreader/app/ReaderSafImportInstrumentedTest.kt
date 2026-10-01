package com.veilreader.app

import android.app.Activity
import android.app.UiAutomation
import android.content.Intent
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReaderSafImportInstrumentedTest {
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
    fun importedEpub_opensReaderThroughAndroidSaf() {
        DOCUMENTS_UI_PACKAGES.forEach { packageName ->
            uiAutomation.executeShellCommand("pm clear $packageName").close()
        }
        SystemClock.sleep(500)
        seedEpubFixture()

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
        waitForDocumentsUi()

        clickDescription("Show roots")
        clickText("Downloads")
        clickText("VeilReaderQa.epub")

        waitForPackage(target.packageName)
        waitForViewId(
            "com.veilreader.app:id/resourcePager",
            "com.veilreader.app:id/webView"
        )

        uiAutomation.setRotation(UiAutomation.ROTATION_FREEZE_90)
        try {
            waitForPackage(target.packageName)
            waitForResumedActivity(excluding = activity)
            waitForViewId(
                "com.veilreader.app:id/resourcePager",
                "com.veilreader.app:id/webView"
            )
        } finally {
            uiAutomation.setRotation(UiAutomation.ROTATION_UNFREEZE)
        }
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
        error("Timed out waiting for recreated EPUB Activity")
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
        waitForNode("package=$packageName") {
            it.packageName?.toString() == packageName
        }.recycleSafely()
    }

    private fun waitForDocumentsUi() {
        waitForNode("DocumentsUI package") {
            it.packageName?.toString() in DOCUMENTS_UI_PACKAGES
        }.recycleSafely()
    }

    private fun waitForViewId(vararg ids: String) {
        waitForNode("viewId=${ids.joinToString()}") {
            it.viewIdResourceName in ids
        }.recycleSafely()
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
            val matches = try {
                predicate(node)
            } catch (error: Throwable) {
                node.recycleSafely()
                recycleQueuedNodes(queue)
                throw error
            }
            if (matches) {
                recycleQueuedNodes(queue)
                return node
            }
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(queue::add)
            }
            node.recycleSafely()
        }
        return null
    }

    private fun clickNode(node: AccessibilityNodeInfo) {
        var current: AccessibilityNodeInfo? = node
        while (current != null && !current.isClickable) {
            val parent = current.parent
            if (current !== node) current.recycleSafely()
            current = parent
        }

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
            if (current != null && current !== node) current.recycleSafely()
            node.recycleSafely()
        }
        SystemClock.sleep(750)
    }

    private fun recycleQueuedNodes(queue: ArrayDeque<AccessibilityNodeInfo>) {
        while (queue.isNotEmpty()) {
            queue.removeFirst().recycleSafely()
        }
    }

    @Suppress("DEPRECATION")
    private fun AccessibilityNodeInfo.recycleSafely() {
        recycle()
    }

    private fun seedEpubFixture() {
        val resolver = instrumentation.targetContext.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val displayName = "VeilReaderQa.epub"
        resolver.delete(
            collection,
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
            arrayOf(displayName),
        )
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/epub+zip")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(collection, values)) {
            "Unable to create EPUB fixture in MediaStore Downloads"
        }
        resolver.openOutputStream(uri, "w")!!.use { output ->
            output.write(Base64.decode(EPUB_BASE64, Base64.DEFAULT))
        }
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        SystemClock.sleep(1_000)
    }

    private companion object {
        val DOCUMENTS_UI_PACKAGES = setOf(
            "com.android.documentsui",
            "com.google.android.documentsui"
        )

        const val TIMEOUT_MS = 15_000L
        const val POLL_MS = 250L
        const val EPUB_BASE64 = "UEsDBBQAAAAAAK9jKl1vYassFAAAABQAAAAIAAAAbWltZXR5cGVhcHBsaWNhdGlvbi9lcHViK3ppcFBLAwQUAAAACACvYypdhxXScZYAAADcAAAAFgAAAE1FVEEtSU5GL2NvbnRhaW5lci54bWxVjsEKwjAQRH8l5Cpt9BqSFATvXvyANd1qMMkuSSr69xaRqreBebwZMzxSFHcsNVC2ctdv5eCMp9wgZCz/jVjYXK2cS9YENVSdIWHVzWtizCP5OWFu+o3pVSKdKURtChHrN4ppjrFjaFcrD8fTXjH4G1ywJ56kSDgG6NqT0UpgjsFDW34owjPX7oNuliGpnFE/erXOuhdQSwMEFAAAAAgAr2MqXSZxqEyIAQAA5QIAABAAAABFUFVCL3BhY2thZ2Uub3BmlZLPbqMwEIdfxfK1AhuHNi0Cqt1zL7uKeujN2EMyKhivMU1y24foE+6TrDH5095aCQnMzPf554Hy8dB35A3ciIOpaJZy+liXVqpXuQUSamas6M57WzC23+9T1LZNB7dlgvM1G2xLr/AqwGQy+GeCBDUYjy2CqyhqWpc9eKmll4uz0OqitZProlIrBh30ARxZlmYsUFoVVxNBvcgmZ4ppQl3cZs26EXmerHLVJHkr8qRpG5WI21Y19+uVXHNVsk+S6PToO6g3OyBP0nhwhjxh46Q7kn9/38kGRk9+DsNrRJfemVIOpB9c/QzYkd8gdYj060dsOpfmtk6a7RSmV4OJtcs6zoBYN1hw/lhRrcLW/Vj0g56jhYMJLu4S/pBkfMN5Ea+Xks1YvdzmCQaPNNiGkHWJHvo4FiPfKNk5aONjetj5vqOkB40y8UcLFZXWdqikD5+KxfLNYW45xUEYFwn7IFXZ2amyrys/GcTFIL5lYNczjhYNLM5gCtpTovNGH1+KyJ4IdvqN6/9QSwMEFAAAAAgAr2MqXVtBy7bKAAAAXQEAAA4AAABFUFVCL25hdi54aHRtbHWPwY7CIBCGX4XgvWP1YNpMSTbu3cTdF6DtKE2AIS1u9e0t5aKbeCH84fuY+dFEZ8XdWT810sQYaoB5not5X/B4hbKqKrgnRmaopnBr38ihD5eV3W23B+AwSYWGdK8wDtGSOrKP5OOEkDNCfm25fyj0+k+kL+v4CNTIyF3SyxdrCchWoR0UamFGujSyK4u8lPo1JE6BvPhmHhH0Z3bT2RtJ9SV+nLZWHJe08pBgSNZ6e1N3r2PO5Mi1NA7+Ks7M7r+9VFnOXAuSpp5QSwMEFAAAAAgAr2MqXcIP3VuSAQAARBoAAA0AAABFUFVCL2MxLnhodG1s7ZlLbsMgEIavMsq6Mk3SVyrHUvpYt1J7ARImBoWAC6SOb98h2RTOMJI3Hn6Y8ScvPtmtTkcL56N1cT3TKQ3PQozj2IzLxodezFerlTjnzKxrNUrVtckki923RvgY0MGb96EV12IrrpGtVxPF53WKKu3QfcoYZY8wbyCvHxAHDBAxgQQrXcLgYIvRKIRE61vvDw28/2KYINDxlN0Gf+p1ziuz32NAl+DnhDEZ725AOgV4iR9l6I0DjZYqLu+BOMgdwt4HitEVRwwNvODkaVPuNhqn/HgDQdLGFCis6GhDww0y6QhyF3yMl2hv6UEa2EA0ltpRLY+Xmx7wethgc7NR04Qw0XgYlJxAyzyNQtW0YvgPZMFASiBLBlICuWMgJZB7BlICeWAgJZBHBlICeWIgJZAVAymBzG+ZSEWEXfVCRC/AqPVsZ0846zbwdZTWwivdkdsvCmDssvUrxDJbE2GbrYmwztZE2GdrIiy0NRE22poIK239aY2VtibCSlsTYWetibCz1kTYWS9ExPUXj8h/hro/UEsDBBQAAAAIAK9jKl0g4kSDmAEAAFIaAAANAAAARVBVQi9jMi54aHRtbO2ZTU7DMBBGrzLqGsW0/BaFSIDYI+ACTj2NrTp2sF1Cbs+43WBL3GCkbBJ/45k8ZfEUtzqNFn5G6+LjSqc0PQgxz3MzXzU+DGK93W7FT86sulajVF2bTLLYfWqEdxxx7DEYN8C792MrzmutOCd7rxaqWv8TpoV26t5kjHJAWDeQYwfECQNETCDBSpcwOOgxGoWQaL33/tDA6zeGBQJ1oWwf/HHQOa/Mfo8BXYKvI8ZkvLsA6RTgKT7KMBgHGi09cbkG4iR3CHsfKEZXnDE08IyLp6LcbTZO+fkCgqTCFCisaGtDw00y6QhyF3yMp+hg6UUaeIJoLLWjZ3m83PSA580mm5vNmiaEhcbDoOQCWuZpFKqmFdNfIBsGUgK5YiAlkGsGUgK5YSAlkFsGUgK5YyAlkHsGUgLZMpASyPqSiVRE2FVPRPQGjHpc7ewRV90TfIzSWnihO3L7TQGMXbb+hFhmayJsszUR1tmaCPtsTYSFtibCRlsTYaWtf62x0tZEWGlrIuysNRF21poIO+uJiDif9Ih8TtT9AlBLAQIUAxQAAAAAAK9jKl1vYassFAAAABQAAAAIAAAAAAAAAAAAAACAAQAAAABtaW1ldHlwZVBLAQIUAxQAAAAIAK9jKl2HFdJxlgAAANwAAAAWAAAAAAAAAAAAAACAAToAAABNRVRBLUlORi9jb250YWluZXIueG1sUEsBAhQDFAAAAAgAr2MqXSZxqEyIAQAA5QIAABAAAAAAAAAAAAAAAIABBAEAAEVQVUIvcGFja2FnZS5vcGZQSwECFAMUAAAACACvYypdW0HLtsoAAABdAQAADgAAAAAAAAAAAAAAgAG6AgAARVBVQi9uYXYueGh0bWxQSwECFAMUAAAACACvYypdwg/dW5IBAABEGgAADQAAAAAAAAAAAAAAgAGwAwAARVBVQi9jMS54aHRtbFBLAQIUAxQAAAAIAK9jKl0g4kSDmAEAAFIaAAANAAAAAAAAAAAAAACAAW0FAABFUFVCL2MyLnhodG1sUEsFBgAAAAAGAAYAagEAADAHAAAAAA=="
    }
}
