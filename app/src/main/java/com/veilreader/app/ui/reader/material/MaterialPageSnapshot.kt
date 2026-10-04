package com.veilreader.app.ui.reader.material

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.os.Trace
import android.view.View

/**
 * One explicit capture boundary between Readium's publication View and the Paper renderer.
 *
 * A provider may change (software View.draw today, hardware-composited capture after
 * device A/B evidence) without changing Paper physics, navigation or GL ownership.
 */
internal fun interface MaterialPageSnapshotProvider {
    fun capture(
        view: View,
        target: Bitmap,
        sourceRevision: Long
    ): MaterialPageSnapshotCapture
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

internal object ViewDrawMaterialPageSnapshotProvider : MaterialPageSnapshotProvider {
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
