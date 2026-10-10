package com.veilreader.app.ui.reader.material

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.webkit.WebView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class MaterialPagePreparationOwnershipTest {
    private fun immediateProvider() = MaterialPageImmediateSnapshotProvider { _, target, revision ->
        target.eraseColor(Color.BLUE)
        MaterialPageSnapshotCapture.Ready(target, revision, "immediate", 0L)
    }

    @Test fun turnCannotReuseBufferOfSuspendedWarmCapture() = runTest {
        val activity = Robolectric.buildActivity(Activity::class.java).setup()
        val view = WebView(activity.get())
        activity.get().setContentView(view)
        activity.visible()
        // WebView's provider-backed setFrame leaves layout() at 0x0 in
        // Robolectric. Set View bounds for this pixel-ownership fixture.
        view.left = 0
        view.top = 0
        view.right = 32
        view.bottom = 48
        assertTrue("Fixture must enter capture: attached=${view.isAttachedToWindow}, " +
            "shown=${view.isShown}, size=${view.width}x${view.height}, visibility=${view.visibility}",
            materialPageVisibleWebView(view) === view)
        val finishCapture = CompletableDeferred<Unit>()
        var capturedTarget: Bitmap? = null
        val engine = MaterialPageEngineState(
            snapshotProvider = immediateProvider(),
            preparedSnapshotProvider = MaterialPagePreparedSnapshotProvider { _, target, revision ->
                capturedTarget = target
                finishCapture.await()
                target.eraseColor(Color.RED)
                MaterialPageSnapshotCapture.Ready(target, revision, "delayed", 0L)
            },
            awaitSourceVisualReady = { _, _ -> true }
        )
        val preparation = async(start = CoroutineStart.UNDISPATCHED) { engine.prepareSnapshot(view) }
        try {
            assertFalse(preparation.isCompleted)
            assertTrue(engine.begin(view, MaterialPageSide.RIGHT))
            assertNotSame(capturedTarget, engine.snapshot)
            finishCapture.complete(Unit)
            assertFalse(preparation.await())
            assertEquals(Color.BLUE, engine.snapshot!!.getPixel(0, 0))
        } finally {
            preparation.cancel()
            engine.dispose()
            activity.pause().stop().destroy()
        }
    }

    @Test fun cancellationReleasesOneOfTwoBusyCaptureSlots() = runTest {
        val activity = Robolectric.buildActivity(Activity::class.java).setup()
        val view = WebView(activity.get())
        activity.get().setContentView(view)
        activity.visible()
        // WebView's provider-backed setFrame leaves layout() at 0x0 in
        // Robolectric. Set View bounds for this pixel-ownership fixture.
        view.left = 0
        view.top = 0
        view.right = 32
        view.bottom = 48
        assertTrue("Fixture must enter capture: attached=${view.isAttachedToWindow}, " +
            "shown=${view.isShown}, size=${view.width}x${view.height}, visibility=${view.visibility}",
            materialPageVisibleWebView(view) === view)
        val neverFinishes = CompletableDeferred<Unit>()
        val targets = mutableListOf<Bitmap>()
        val engine = MaterialPageEngineState(
            snapshotProvider = immediateProvider(),
            preparedSnapshotProvider = MaterialPagePreparedSnapshotProvider { _, target, revision ->
                targets += target
                neverFinishes.await()
                MaterialPageSnapshotCapture.Ready(target, revision, "delayed", 0L)
            },
            awaitSourceVisualReady = { _, _ -> true }
        )
        val first = async(start = CoroutineStart.UNDISPATCHED) { engine.prepareSnapshot(view) }
        val second = async(start = CoroutineStart.UNDISPATCHED) { engine.prepareSnapshot(view) }
        try {
            assertEquals(2, targets.size)
            assertNotSame(targets[0], targets[1])
            assertFalse(engine.begin(view, MaterialPageSide.RIGHT))
            first.cancel()
            first.join()
            assertTrue(engine.begin(view, MaterialPageSide.RIGHT))
            assertNotSame(targets[1], engine.snapshot)
        } finally {
            first.cancel()
            second.cancel()
            first.join()
            second.join()
            engine.dispose()
            activity.pause().stop().destroy()
        }
    }

    @Test fun idleReleaseRejectsLateCaptureWithoutSoftwareFallback() = runTest {
        val activity = Robolectric.buildActivity(Activity::class.java).setup()
        val view = WebView(activity.get())
        activity.get().setContentView(view)
        activity.visible()
        // WebView's provider-backed setFrame leaves layout() at 0x0 in
        // Robolectric. Set View bounds for this pixel-ownership fixture.
        view.left = 0
        view.top = 0
        view.right = 32
        view.bottom = 48
        assertTrue("Fixture must enter capture: attached=${view.isAttachedToWindow}, " +
            "shown=${view.isShown}, size=${view.width}x${view.height}, visibility=${view.visibility}",
            materialPageVisibleWebView(view) === view)
        val finishCapture = CompletableDeferred<Unit>()
        var immediateCaptures = 0
        val engine = MaterialPageEngineState(
            snapshotProvider = MaterialPageImmediateSnapshotProvider { _, target, revision ->
                immediateCaptures++
                MaterialPageSnapshotCapture.Ready(target, revision, "immediate", 0L)
            },
            preparedSnapshotProvider = MaterialPagePreparedSnapshotProvider { _, _, revision ->
                finishCapture.await()
                MaterialPageSnapshotCapture.Failed(
                    revision, MaterialPageSnapshotFailureReason.DRAW_FAILED, "delayed failure"
                )
            },
            awaitSourceVisualReady = { _, _ -> true }
        )
        val preparation = async(start = CoroutineStart.UNDISPATCHED) { engine.prepareSnapshot(view) }
        try {
            assertFalse(preparation.isCompleted)
            engine.releaseBufferIfIdle()
            finishCapture.complete(Unit)
            assertFalse(preparation.await())
            assertEquals(0, immediateCaptures)
            assertTrue(engine.begin(view, MaterialPageSide.RIGHT))
            assertEquals(1, immediateCaptures)
        } finally {
            preparation.cancel()
            engine.dispose()
            activity.pause().stop().destroy()
        }
    }

    @Test fun refreshingPreparedPixelsCannotBeConsumedByTurn() = runTest {
        val activity = Robolectric.buildActivity(Activity::class.java).setup()
        val view = WebView(activity.get())
        activity.get().setContentView(view)
        activity.visible()
        // WebView's provider-backed setFrame leaves layout() at 0x0 in
        // Robolectric. Set View bounds for this pixel-ownership fixture.
        view.left = 0
        view.top = 0
        view.right = 32
        view.bottom = 48
        assertTrue("Fixture must enter capture: attached=${view.isAttachedToWindow}, " +
            "shown=${view.isShown}, size=${view.width}x${view.height}, visibility=${view.visibility}",
            materialPageVisibleWebView(view) === view)
        val finishCapture = CompletableDeferred<Unit>()
        var requests = 0
        var refreshingTarget: Bitmap? = null
        val engine = MaterialPageEngineState(
            snapshotProvider = immediateProvider(),
            preparedSnapshotProvider = MaterialPagePreparedSnapshotProvider { _, target, revision ->
                if (++requests > 1) {
                    refreshingTarget = target
                    finishCapture.await()
                }
                target.eraseColor(Color.RED)
                MaterialPageSnapshotCapture.Ready(target, revision, "prepared", 0L)
            },
            awaitSourceVisualReady = { _, _ -> true }
        )
        assertTrue(engine.prepareSnapshot(view))
        val preparation = async(start = CoroutineStart.UNDISPATCHED) { engine.prepareSnapshot(view) }
        try {
            assertFalse(preparation.isCompleted)
            assertTrue(engine.begin(view, MaterialPageSide.RIGHT))
            assertNotSame(refreshingTarget, engine.snapshot)
            finishCapture.complete(Unit)
            assertFalse(preparation.await())
            assertEquals(Color.BLUE, engine.snapshot!!.getPixel(0, 0))
        } finally {
            preparation.cancel()
            engine.dispose()
            activity.pause().stop().destroy()
        }
    }
}
