package com.veilreader.app.ui.reader.material

import android.app.Activity
import android.view.View
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class MaterialPagePreparationLifecycleTest {
    @Test fun hiddenSourceDoesNotCaptureEvenWhenViewRemainsAttached() = runTest {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().visible()
        val view = View(activity.get())
        activity.get().setContentView(view)
        view.layout(0, 0, 32, 48)
        var captures = 0
        val engine = MaterialPageEngineState(snapshotProvider = MaterialPageImmediateSnapshotProvider { _, target, revision ->
            captures++
            MaterialPageSnapshotCapture.Ready(target, revision, "test", 0L)
        })
        try {
            assertTrue(view.isAttachedToWindow)
            assertFalse(engine.prepareSnapshot(view) { false })
            assertEquals(0, captures)
            assertTrue(engine.prepareSnapshot(view) { true })
            assertEquals(1, captures)
        } finally {
            engine.dispose()
            activity.pause().stop().destroy()
        }
    }

    @Test fun sourceBecomingHiddenAtVisualReadinessCannotStartCapture() = runTest {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().visible()
        val view = View(activity.get())
        activity.get().setContentView(view)
        view.layout(0, 0, 32, 48)
        var checks = 0
        var captures = 0
        val engine = MaterialPageEngineState(snapshotProvider = MaterialPageImmediateSnapshotProvider { _, target, revision ->
            captures++
            MaterialPageSnapshotCapture.Ready(target, revision, "test", 0L)
        })
        try {
            assertFalse(engine.prepareSnapshot(view) { ++checks == 1 })
            assertEquals(2, checks)
            assertEquals(0, captures)
        } finally {
            engine.dispose()
            activity.pause().stop().destroy()
        }
    }

    @Test fun captureCannotPublishPreparedPixelsAfterSourceOwnerChanges() = runTest {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().visible()
        val view = View(activity.get())
        activity.get().setContentView(view)
        view.layout(0, 0, 32, 48)
        var current = true
        val engine = MaterialPageEngineState(snapshotProvider = MaterialPageImmediateSnapshotProvider { _, target, revision ->
            current = false
            MaterialPageSnapshotCapture.Ready(target, revision, "test", 0L)
        })
        try {
            assertFalse(engine.prepareSnapshot(view) { current })
        } finally {
            engine.dispose()
            activity.pause().stop().destroy()
        }
    }

    @Test fun warmCaptureUsesPreparedProviderWithoutTouchingImmediateProvider() = runTest {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().visible()
        val view = View(activity.get())
        activity.get().setContentView(view)
        view.layout(0, 0, 32, 48)
        var immediateCaptures = 0
        var preparedCaptures = 0
        val engine = MaterialPageEngineState(
            snapshotProvider = MaterialPageImmediateSnapshotProvider { _, target, revision ->
                immediateCaptures++
                MaterialPageSnapshotCapture.Ready(target, revision, "immediate", 0L)
            },
            preparedSnapshotProvider = MaterialPagePreparedSnapshotProvider { _, target, revision ->
                preparedCaptures++
                MaterialPageSnapshotCapture.Ready(target, revision, "prepared", 0L)
            }
        )
        try {
            assertTrue(engine.prepareSnapshot(view))
            assertEquals(1, preparedCaptures)
            assertEquals(0, immediateCaptures)
            assertTrue(engine.begin(view, MaterialPageSide.RIGHT))
            assertEquals(0, immediateCaptures)
            engine.clearImmediately()
        } finally {
            engine.dispose()
            activity.pause().stop().destroy()
        }
    }

    @Test fun hardwarePreparedProviderFallsBackForNonWebViewContent() = runTest {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().visible()
        val view = View(activity.get())
        activity.get().setContentView(view)
        view.layout(0, 0, 32, 48)
        val target = android.graphics.Bitmap.createBitmap(
            32,
            48,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        var fallbackCaptures = 0
        val provider = HardwareCompositedPreparedMaterialPageSnapshotProvider(
            fallback = MaterialPageImmediateSnapshotProvider { _, bitmap, revision ->
                fallbackCaptures++
                MaterialPageSnapshotCapture.Ready(bitmap, revision, "fallback", 0L)
            }
        )
        try {
            val capture = provider.capture(view, target, 9L)
            assertTrue(capture is MaterialPageSnapshotCapture.Ready)
            assertEquals(
                "fallback",
                (capture as MaterialPageSnapshotCapture.Ready).provider
            )
            assertEquals(1, fallbackCaptures)
        } finally {
            target.recycle()
            activity.pause().stop().destroy()
        }
    }

}
