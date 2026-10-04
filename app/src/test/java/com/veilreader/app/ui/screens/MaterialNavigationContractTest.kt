package com.veilreader.app.ui.screens

import android.graphics.PointF
import android.view.View
import androidx.compose.runtime.MonotonicFrameClock
import com.veilreader.app.domain.PageMaterial
import com.veilreader.app.ui.reader.material.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.input.DragEvent
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalReadiumApi::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "mdpi")
class MaterialNavigationContractTest {
    @Test fun resizeRejectsStaleSnapshotWithoutMutatingNavigation() = runTest {
        val session = Session(CoroutineScope(coroutineContext + FrameClock()))
        session.drag(DragEvent.Type.Start, 0f)
        session.drag(DragEvent.Type.Move, 60f)
        advanceUntilIdle()
        assertTrue(session.state.matchesCapturedViewport(400f, 800f))
        assertFalse(session.state.matchesCapturedViewport(800f, 400f))
        assertFalse(session.state.matchesCapturedViewport(400f, 600f))
        assertFalse(session.state.matchesCapturedViewport(Float.NaN, 800f))
        assertEquals(0, session.commits)
        session.listener.forceCancelPendingTurn()
        assertEquals(session.navigator.source, session.navigator.currentLocator.value)
        session.state.dispose()
    }

    @Test fun cancellationCueArrivesAtReleaseBeforeSettling() = runTest {
        val session = Session(CoroutineScope(coroutineContext + FrameClock()))
        session.drag(DragEvent.Type.Start, 0f)
        session.drag(DragEvent.Type.Move, 60f)
        advanceUntilIdle()
        session.drag(DragEvent.Type.End, 60f)
        runCurrent()
        assertTrue(session.state.active)
        assertEquals(1, session.cues.count { it.moment == MaterialSensoryMoment.CANCEL })
        assertEquals(session.navigator.source, session.navigator.currentLocator.value)
        advanceUntilIdle()
        assertEquals(1, session.cues.count { it.moment == MaterialSensoryMoment.CANCEL })
        assertEquals(0, session.commits)
        session.state.dispose()
    }

    @Test fun reducedMotionFreezesMaterialEvenWithoutSnapshot() = runTest {
        val session = Session(CoroutineScope(coroutineContext + FrameClock()), reduced = true)
        session.drag(DragEvent.Type.Start, 0f)
        session.drag(DragEvent.Type.Move, 220f)
        runCurrent()
        session.state.materialConfiguration = MaterialTurnConfiguration(true, PageMaterials.glossy)
        session.drag(DragEvent.Type.End, 220f)
        advanceUntilIdle()
        assertEquals(1, session.commits)
        assertTrue(session.cues.all { it.material == PageMaterial.PARCHMENT })
        assertEquals(1, session.cues.count { it.moment == MaterialSensoryMoment.COMPLETE })
        session.state.dispose()
    }
    private class FrameClock : MonotonicFrameClock {
        private var nanos = 0L
        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
            delay(16); nanos += 16_000_000L
            return onFrame(nanos)
        }
    }
    private class Navigator(private val rtl: Boolean = false, zeroView: Boolean = false) : OverflowableNavigator {
        val source = Locator(requireNotNull(Url("chapter.xhtml")), MediaType.XHTML,
            locations = Locator.Locations(progression = .314, position = 4),
            text = Locator.Text(highlight = "Exact reading position"))
        val destination = source.copy(locations = Locator.Locations(progression = .415, position = 5))
        override val currentLocator = MutableStateFlow(source)
        override val publicationView = object : View(RuntimeEnvironment.getApplication()) {
            override fun draw(canvas: android.graphics.Canvas) {
                if (zeroView) error("Capture unavailable") else super.draw(canvas)
            }
        }.apply { layout(0, 0, 400, 800) }
        override val overflow = MutableStateFlow<OverflowableNavigator.Overflow>(object : OverflowableNavigator.Overflow {
            override val readingProgression = if (rtl) ReadingProgression.RTL else ReadingProgression.LTR
            override val scroll = false
            override val axis = Axis.HORIZONTAL
        })
        var forward = 0
        var backward = 0
        var restores = 0
        var boundary = false
        override fun goForward(animated: Boolean): Boolean {
            assertFalse(animated); forward++
            if (boundary) return false
            currentLocator.value = destination; return true
        }
        override fun goBackward(animated: Boolean): Boolean {
            assertFalse(animated); backward++
            currentLocator.value = destination; return true
        }
        override fun go(locator: Locator, animated: Boolean): Boolean {
            assertFalse(animated); assertEquals(source, locator)
            restores++; currentLocator.value = locator; return true
        }
        override fun go(link: Link, animated: Boolean) = false
        override fun addInputListener(listener: InputListener) {}
        override fun removeInputListener(listener: InputListener) {}
    }
    private class Session(scope: CoroutineScope, rtl: Boolean = false, reduced: Boolean = false, missing: Boolean = false) {
        val navigator = Navigator(rtl, missing)
        val state = PaperCurlState().apply {
            materialConfiguration = MaterialTurnConfiguration(true, PageMaterials.forId(PageMaterial.PARCHMENT))
        }
        var commits = 0
        var boundaries = 0
        var enabled = true
        val cues = mutableListOf<MaterialSensoryCue>()
        val listener = PaperCurlInputListener(navigator, state, { enabled }, scope,
            isReducedMotion = { reduced }, onInteraction = {}, onCommittedTurn = { commits++ },
            onBoundaryHit = { boundaries++ }, onMaterialFeedback = { cues += it })
        val origin = if (rtl) 2f else 398f
        val sign = if (rtl) 1f else -1f
        fun drag(type: DragEvent.Type, distance: Float) = listener.onDrag(
            DragEvent(type, PointF(origin, 720f), PointF(sign * distance, 0f)))
    }

    @Test fun previewDoesNotPersistAndCancelledTurnRestoresExactLocator() = runTest {
        val session = Session(CoroutineScope(coroutineContext + FrameClock()))
        assertTrue(session.drag(DragEvent.Type.Start, 0f))
        session.drag(DragEvent.Type.Move, 60f)
        advanceUntilIdle()
        assertEquals(session.navigator.destination, session.navigator.currentLocator.value)
        assertEquals(0, session.commits)
        session.drag(DragEvent.Type.End, 60f)
        advanceUntilIdle()
        assertEquals(session.navigator.source, session.navigator.currentLocator.value)
        assertEquals(1, session.navigator.restores)
        assertEquals(0, session.commits)
        assertFalse(session.state.active)
        session.state.dispose()
    }

    @Test fun allThreeRendererPathsRemainAvailableAndCancelWithoutPersistence() = runTest {
        try {
            for (backend in listOf("legacy", "strip", "mesh")) {
                MaterialPageEngineRollout.setDebugOverride(backend == "strip")
                val session = Session(CoroutineScope(coroutineContext + FrameClock()))
                if (backend != "mesh") session.state.materialConfiguration = MaterialTurnConfiguration()
                session.drag(DragEvent.Type.Start, 0f)
                session.drag(DragEvent.Type.Move, 80f)
                advanceUntilIdle()
                assertTrue(session.state.active)
                assertEquals(backend == "strip", session.state.usingMaterialEngine())
                assertEquals(backend == "mesh", session.state.capturedMaterial.enabled)
                assertEquals(0, session.commits)
                assertTrue(session.listener.forceCancelPendingTurn())
                assertEquals(session.navigator.source, session.navigator.currentLocator.value)
                assertEquals(1, session.navigator.restores)
                assertEquals(0, session.commits)
                assertFalse(session.state.active)
                session.state.dispose()
            }
        } finally { MaterialPageEngineRollout.setDebugOverride(null) }
    }

    @Test fun completedMaterialTurnCommitsOnceInLtrAndRtl() = runTest {
        for (rtl in listOf(false, true)) {
            val session = Session(CoroutineScope(coroutineContext + FrameClock()), rtl)
            session.drag(DragEvent.Type.Start, 0f)
            session.drag(DragEvent.Type.Move, 220f)
            advanceUntilIdle()
            assertEquals(0, session.commits)
            session.drag(DragEvent.Type.End, 220f)
            advanceUntilIdle()
            assertEquals(1, session.navigator.forward)
            assertEquals(0, session.navigator.backward)
            assertEquals(1, session.commits)
            assertEquals(0, session.navigator.restores)
            assertEquals(1, session.cues.count { it.moment == MaterialSensoryMoment.COMPLETE })
            assertEquals(1, session.cues.count { it.moment == MaterialSensoryMoment.THRESHOLD })
            assertFalse(session.state.active)
            session.state.dispose()
        }
    }

    @Test fun closeWaitsForPreviewRestorationWithoutCountingOrCompletionFeedback() = runTest {
        val session = Session(CoroutineScope(coroutineContext + FrameClock()))
        session.drag(DragEvent.Type.Start, 0f)
        session.drag(DragEvent.Type.Move, 220f)
        advanceUntilIdle()
        assertTrue(session.listener.cancelPendingTurnAndAwait())
        assertEquals(session.navigator.source, session.navigator.currentLocator.value)
        assertEquals(0, session.commits)
        assertEquals(0, session.cues.count { it.moment == MaterialSensoryMoment.COMPLETE })
        assertFalse(session.state.active)
        session.state.dispose()
    }

    @Test fun lifecycleDisposalAndModeHandoffRestoreUncommittedPreviewSynchronously() = runTest {
        val session = Session(CoroutineScope(coroutineContext + FrameClock()))
        session.drag(DragEvent.Type.Start, 0f)
        session.drag(DragEvent.Type.Move, 220f)
        advanceUntilIdle()
        session.enabled = false
        assertTrue(session.listener.forceCancelPendingTurn())
        assertEquals(session.navigator.source, session.navigator.currentLocator.value)
        assertEquals(0, session.commits)
        assertFalse(session.state.active)
        session.state.dispose()
    }

    @Test fun reducedMotionAndMissingCaptureNavigateOnlyAtRelease() = runTest {
        for (reduced in listOf(false, true)) {
            val session = Session(CoroutineScope(coroutineContext + FrameClock()), reduced = reduced, missing = !reduced)
            session.drag(DragEvent.Type.Start, 0f)
            session.drag(DragEvent.Type.Move, 220f)
            advanceUntilIdle()
            assertEquals(0, session.navigator.forward)
            assertNull(session.state.snapshot)
            session.drag(DragEvent.Type.End, 220f)
            advanceUntilIdle()
            assertEquals(1, session.commits)
            assertEquals(1, session.navigator.forward)
            assertEquals(session.navigator.destination, session.navigator.currentLocator.value)
            session.state.dispose()
        }
    }

    @Test fun liftedSheetFreezesPresetAndFeatureGateUntilTransactionEnds() = runTest {
        val session = Session(CoroutineScope(coroutineContext + FrameClock()))
        session.drag(DragEvent.Type.Start, 0f)
        session.drag(DragEvent.Type.Move, 220f)
        advanceUntilIdle()
        session.state.materialConfiguration = MaterialTurnConfiguration(false, PageMaterials.glossy)
        assertTrue(session.state.capturedMaterial.enabled)
        assertEquals(PageMaterial.PARCHMENT, session.state.capturedMaterial.material.id)
        session.drag(DragEvent.Type.End, 220f)
        advanceUntilIdle()
        assertEquals(1, session.commits)
        assertEquals(PageMaterial.PARCHMENT, session.cues.last().material)
        session.state.dispose()
    }

    @Test fun failedBoundaryNavigationNeverCommitsEvenWithReducedMotion() = runTest {
        val session = Session(CoroutineScope(coroutineContext + FrameClock()), reduced = true)
        session.navigator.boundary = true
        session.drag(DragEvent.Type.Start, 0f)
        session.drag(DragEvent.Type.Move, 220f)
        session.drag(DragEvent.Type.End, 220f)
        advanceUntilIdle()
        assertEquals(0, session.commits)
        assertEquals(1, session.boundaries)
        assertEquals(session.navigator.source, session.navigator.currentLocator.value)
        session.state.dispose()
    }
}
