package com.veilreader.app.ui.reader.material

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.Trace
import android.view.PixelCopy
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

/**
 * Prepared page pixels remain valid primarily by source revision + exact viewport.
 *
 * Readers routinely dwell on a page far longer than a few seconds. A short TTL forced Paper to
 * discard its warm WebView snapshot just before the next human page turn and fall back to a
 * synchronous gesture-time capture. Keep a generous safety ceiling while navigation, relayout,
 * theme/decoration changes and viewport changes continue to invalidate the source revision.
 */
internal const val MATERIAL_PAGE_PREPARED_SNAPSHOT_MAX_AGE_NANOS =
    300_000_000_000L

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


internal fun materialPageActivity(context: Context): Activity? {
    var current: Context? = context
    val visited = HashSet<Context>(4)
    while (current != null && visited.add(current)) {
        if (current is Activity) return current
        current = (current as? ContextWrapper)?.baseContext
    }
    return null
}

internal fun materialPagePixelCopyRect(view: View, activity: Activity): Rect? {
    if (
        !view.isAttachedToWindow ||
        view.width <= 0 ||
        view.height <= 0
    ) {
        return null
    }
    val decor = activity.window.decorView
    if (decor.width <= 0 || decor.height <= 0) return null

    val location = IntArray(2)
    view.getLocationInWindow(location)
    val rect = Rect(
        location[0],
        location[1],
        location[0] + view.width,
        location[1] + view.height
    )
    if (
        rect.left < 0 ||
        rect.top < 0 ||
        rect.right > decor.width ||
        rect.bottom > decor.height ||
        rect.width() != view.width ||
        rect.height() != view.height
    ) {
        return null
    }
    return rect
}

/**
 * Warm hardware-composited capture for Readium's WebView-backed publication surface.
 *
 * PixelCopy writes into a private temporary bitmap so cancellation can never race
 * the engine's reusable snapshot pool. If the window/driver path is unavailable,
 * the existing View.draw provider remains a fail-safe without changing Paper
 * gesture or navigation ownership.
 */
internal class HardwareCompositedPreparedMaterialPageSnapshotProvider(
    private val fallback: MaterialPageImmediateSnapshotProvider =
        ViewDrawImmediateMaterialPageSnapshotProvider
) : MaterialPagePreparedSnapshotProvider {
    override suspend fun capture(
        view: View,
        target: Bitmap,
        sourceRevision: Long
    ): MaterialPageSnapshotCapture {
        if (materialPageVisibleWebView(view) == null) {
            return fallback.capture(view, target, sourceRevision)
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

        val activity = materialPageActivity(view.context)
            ?: return fallback.capture(view, target, sourceRevision)
        val sourceRect = materialPagePixelCopyRect(view, activity)
            ?: return fallback.capture(view, target, sourceRevision)

        val temporary = runCatching {
            Bitmap.createBitmap(
                sourceRect.width(),
                sourceRect.height(),
                Bitmap.Config.ARGB_8888
            )
        }.getOrNull() ?: return fallback.capture(view, target, sourceRevision)

        val started = SystemClock.elapsedRealtimeNanos()
        Trace.beginSection(TRACE_SECTION)
        val result = try {
            ReaderPerformanceMetrics.putSingleFrameState(
                root = view,
                key = ReaderPerformanceMetrics.PAPER_WORK_KEY,
                value = "CAPTURE_HW"
            )
            withTimeoutOrNull(PIXEL_COPY_TIMEOUT_MS) {
                suspendCancellableCoroutine<Int> { continuation ->
                    try {
                        PixelCopy.request(
                            activity.window,
                            sourceRect,
                            temporary,
                            { code ->
                                if (continuation.isActive) {
                                    continuation.resume(code)
                                } else if (!temporary.isRecycled) {
                                    temporary.recycle()
                                }
                            },
                            Handler(Looper.getMainLooper())
                        )
                    } catch (_: Exception) {
                        if (continuation.isActive) {
                            continuation.resume(PIXEL_COPY_REQUEST_EXCEPTION)
                        }
                    }
                }
            }
        } finally {
            Trace.endSection()
        }

        if (result != PixelCopy.SUCCESS) {
            // A timed-out request may still be writing. Its callback owns temporary
            // recycling after cancellation; all completed/error results are safe here.
            if (result != null && !temporary.isRecycled) {
                temporary.recycle()
            }
            return fallback.capture(view, target, sourceRevision)
        }

        return try {
            if (
                !view.isAttachedToWindow ||
                view.width != target.width ||
                view.height != target.height ||
                materialPagePixelCopyRect(view, activity) != sourceRect
            ) {
                MaterialPageSnapshotCapture.NotReady(
                    sourceRevision = sourceRevision,
                    reason = MaterialPageSnapshotFailureReason.VIEW_NOT_READY
                )
            } else {
                target.eraseColor(android.graphics.Color.TRANSPARENT)
                Canvas(target).drawBitmap(temporary, 0f, 0f, null)
                MaterialPageSnapshotCapture.Ready(
                    bitmap = target,
                    sourceRevision = sourceRevision,
                    provider = PROVIDER_NAME,
                    elapsedNanos =
                        (SystemClock.elapsedRealtimeNanos() - started)
                            .coerceAtLeast(0L)
                )
            }
        } finally {
            if (!temporary.isRecycled) temporary.recycle()
        }
    }

    private companion object {
        const val PROVIDER_NAME = "pixel_copy_window"
        const val TRACE_SECTION = "paper.capture.pixel_copy"
        const val PIXEL_COPY_TIMEOUT_MS = 420L
        const val PIXEL_COPY_REQUEST_EXCEPTION = Int.MIN_VALUE
    }
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
