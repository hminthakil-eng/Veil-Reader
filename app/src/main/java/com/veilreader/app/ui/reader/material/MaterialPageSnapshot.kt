package com.veilreader.app.ui.reader.material

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.os.Trace
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import com.veilreader.app.diagnostics.ReaderPerformanceMetrics
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * One explicit capture boundary between Readium's publication View and the Paper renderer.
 *
 * A provider may change (software View.draw today, hardware-composited capture after
 * device A/B evidence) without changing Paper physics, navigation or GL ownership.
 */
internal fun interface MaterialPageImmediateSnapshotProvider {
    fun capture(
        view: View,
        target: Bitmap,
        sourceRevision: Long
    ): MaterialPageSnapshotCapture
}

/**
 * Async/warm capture seam. PixelCopy belongs here, never inside the synchronous
 * input path. A prepared result may only be consumed while its revision and
 * viewport still match the live Readium source.
 */
internal fun interface MaterialPagePreparedSnapshotProvider {
    suspend fun capture(
        view: View,
        target: Bitmap,
        sourceRevision: Long
    ): MaterialPageSnapshotCapture
}

internal fun materialPageVisibleWebView(root: View): WebView? {
    if (
        root is WebView &&
        root.visibility == View.VISIBLE &&
        root.isShown &&
        root.isAttachedToWindow &&
        root.width > 0 &&
        root.height > 0
    ) {
        return root
    }
    val group = root as? ViewGroup ?: return null
    for (index in 0 until group.childCount) {
        materialPageVisibleWebView(group.getChildAt(index))?.let { return it }
    }
    return null
}

internal suspend fun awaitMaterialPageSourceVisualReady(
    root: View,
    requestId: Long,
    timeoutMillis: Long = 220L
): Boolean {
    if (!root.isAttachedToWindow || root.width <= 0 || root.height <= 0) {
        return false
    }
    val webView = materialPageVisibleWebView(root) ?: return true
    return withTimeoutOrNull(timeoutMillis.coerceAtLeast(1L)) {
        suspendCancellableCoroutine { continuation ->
            webView.post {
                if (
                    !continuation.isActive ||
                    !webView.isAttachedToWindow ||
                    webView.width <= 0 ||
                    webView.height <= 0
                ) {
                    if (continuation.isActive) continuation.resume(false)
                    return@post
                }
                webView.postVisualStateCallback(
                    requestId.coerceAtLeast(0L),
                    object : WebView.VisualStateCallback() {
                        override fun onComplete(requestId: Long) {
                            if (continuation.isActive) {
                                continuation.resume(true)
                            }
                        }
                    }
                )
            }
        }
    } ?: false
}

internal data class MaterialPagePreparedSnapshot(
    val bitmap: Bitmap,
    val sourceRevision: Long,
    val width: Int,
    val height: Int,
    val capturedAtElapsedNanos: Long,
    val provider: String
)

internal const val MATERIAL_PAGE_PREPARED_SNAPSHOT_MAX_AGE_NANOS =
    3_000_000_000L

internal fun materialPagePreparedSnapshotIsCurrent(
    prepared: MaterialPagePreparedSnapshot?,
    expectedRevision: Long,
    expectedWidth: Int,
    expectedHeight: Int,
    nowElapsedNanos: Long = prepared?.capturedAtElapsedNanos ?: 0L,
    maxAgeNanos: Long = MATERIAL_PAGE_PREPARED_SNAPSHOT_MAX_AGE_NANOS
): Boolean {
    if (
        prepared == null ||
        prepared.bitmap.isRecycled ||
        prepared.sourceRevision <= 0L ||
        prepared.sourceRevision != expectedRevision ||
        prepared.width != expectedWidth ||
        prepared.height != expectedHeight ||
        prepared.bitmap.width != expectedWidth ||
        prepared.bitmap.height != expectedHeight
    ) {
        return false
    }
    val age =
        nowElapsedNanos - prepared.capturedAtElapsedNanos
    return age >= 0L &&
        age <= maxAgeNanos.coerceAtLeast(0L)
}

internal sealed interface MaterialPageSnapshotCapture {
    data class Ready(
        val bitmap: Bitmap,
        val sourceRevision: Long,
        val provider: String,
        val elapsedNanos: Long
    ) : MaterialPageSnapshotCapture

    data class NotReady(
        val sourceRevision: Long,
        val reason: MaterialPageSnapshotFailureReason
    ) : MaterialPageSnapshotCapture

    data class Failed(
        val sourceRevision: Long,
        val reason: MaterialPageSnapshotFailureReason,
        val errorType: String? = null
    ) : MaterialPageSnapshotCapture
}

internal enum class MaterialPageSnapshotFailureReason {
    VIEW_NOT_READY,
    TARGET_MISMATCH,
    TARGET_RECYCLED,
    STALE_REVISION,
    DRAW_FAILED
}

internal object ViewDrawImmediateMaterialPageSnapshotProvider : MaterialPageImmediateSnapshotProvider {
    override fun capture(
        view: View,
        target: Bitmap,
        sourceRevision: Long
    ): MaterialPageSnapshotCapture {
        if (
            view.width <= 0 ||
            view.height <= 0 ||
            !view.isAttachedToWindow
        ) {
            return MaterialPageSnapshotCapture.NotReady(
                sourceRevision = sourceRevision,
                reason = MaterialPageSnapshotFailureReason.VIEW_NOT_READY
            )
        }
        if (target.isRecycled) {
            return MaterialPageSnapshotCapture.Failed(
                sourceRevision = sourceRevision,
                reason = MaterialPageSnapshotFailureReason.TARGET_RECYCLED
            )
        }
        if (
            target.width != view.width ||
            target.height != view.height ||
            target.config != Bitmap.Config.ARGB_8888
        ) {
            return MaterialPageSnapshotCapture.Failed(
                sourceRevision = sourceRevision,
                reason = MaterialPageSnapshotFailureReason.TARGET_MISMATCH
            )
        }

        val started = SystemClock.elapsedRealtimeNanos()
        Trace.beginSection(TRACE_SECTION)
        return try {
            ReaderPerformanceMetrics.putSingleFrameState(
                root = view,
                key = ReaderPerformanceMetrics.PAPER_WORK_KEY,
                value = "CAPTURE"
            )
            target.eraseColor(android.graphics.Color.TRANSPARENT)
            view.draw(Canvas(target))
            MaterialPageSnapshotCapture.Ready(
                bitmap = target,
                sourceRevision = sourceRevision,
                provider = PROVIDER_NAME,
                elapsedNanos =
                    (SystemClock.elapsedRealtimeNanos() - started)
                        .coerceAtLeast(0L)
            )
        } catch (error: Exception) {
            MaterialPageSnapshotCapture.Failed(
                sourceRevision = sourceRevision,
                reason = MaterialPageSnapshotFailureReason.DRAW_FAILED,
                errorType = error::class.java.simpleName
            )
        } finally {
            Trace.endSection()
        }
    }

    private const val PROVIDER_NAME = "view_draw"
    private const val TRACE_SECTION = "paper.capture.view_draw"
}

internal fun nextMaterialPageSnapshotRevision(current: Long): Long =
    if (current == Long.MAX_VALUE) 1L else current + 1L

internal fun materialPageSnapshotCaptureIsCurrent(
    captureRevision: Long,
    expectedRevision: Long
): Boolean =
    captureRevision > 0L &&
        captureRevision == expectedRevision

/** Reject stretching captured pixels across rotation/reflow while tolerating pixel rounding. */
internal fun materialPageSnapshotScaleIsSafe(
    snapshotWidth: Float,
    snapshotHeight: Float,
    canvasWidth: Float,
    canvasHeight: Float
): Boolean {
    if (!snapshotWidth.isFinite() || snapshotWidth <= 0f ||
        !snapshotHeight.isFinite() || snapshotHeight <= 0f ||
        !canvasWidth.isFinite() || canvasWidth <= 0f ||
        !canvasHeight.isFinite() || canvasHeight <= 0f) {
        return false
    }
    val widthScale = canvasWidth / snapshotWidth
    val heightScale = canvasHeight / snapshotHeight
    return widthScale in 0.96f..1.04f && heightScale in 0.96f..1.04f &&
        kotlin.math.abs(widthScale / heightScale - 1f) <= 0.02f
}
