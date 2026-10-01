package com.veilreader.app.ui.screens

import android.content.Context
import android.graphics.PointF
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.Key
import org.readium.r2.navigator.input.KeyEvent
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalReadiumApi::class)
class VeilDirectionalNavigationContractInstrumentedTest {

    @Test
    fun ltr_rightEdge_movesForward_and_leftEdge_movesBackward() {
        val navigator = fakeNavigator(ReadingProgression.LTR)
        val listener = listener(navigator)

        assertTrue(listener.onTap(TapEvent(PointF(950f, 800f))))
        assertEquals(1, navigator.forwardCalls)
        assertEquals(0, navigator.backwardCalls)
        assertTrue(navigator.lastAnimated)

        navigator.reset()

        assertTrue(listener.onTap(TapEvent(PointF(50f, 800f))))
        assertEquals(0, navigator.forwardCalls)
        assertEquals(1, navigator.backwardCalls)
        assertTrue(navigator.lastAnimated)
    }

    @Test
    fun rtl_rightEdge_movesBackward_and_leftEdge_movesForward() {
        val navigator = fakeNavigator(ReadingProgression.RTL)
        val listener = listener(navigator)

        assertTrue(listener.onTap(TapEvent(PointF(950f, 800f))))
        assertEquals(0, navigator.forwardCalls)
        assertEquals(1, navigator.backwardCalls)

        navigator.reset()

        assertTrue(listener.onTap(TapEvent(PointF(50f, 800f))))
        assertEquals(1, navigator.forwardCalls)
        assertEquals(0, navigator.backwardCalls)
    }

    @Test
    fun publicationBoundary_isConsumed_withoutLeakingIntoChromeFallback() {
        val navigator = fakeNavigator(
            progression = ReadingProgression.LTR,
            navigationSucceeds = false
        )
        var boundaryHits = 0
        val listener = VeilDirectionalNavigationInputListener(
            navigator = navigator,
            isAnimated = { true },
            isTapNavigationEnabled = { true },
            onBoundaryHit = { boundaryHits += 1 }
        )

        assertTrue(listener.onTap(TapEvent(PointF(950f, 800f))))
        assertEquals(1, navigator.forwardCalls)
        assertEquals(1, boundaryHits)
    }

    @Test
    fun paperKeyboard_ownsArrowAndSpace_andCommitsExactlyOnce() {
        val navigator = fakeNavigator(ReadingProgression.LTR)
        var interactions = 0
        var commits = 0
        var boundaries = 0
        val listener = PaperCurlInputListener(
            navigator = navigator,
            state = PaperCurlState(),
            isEnabled = { true },
            scope = CoroutineScope(Dispatchers.Unconfined),
            isReducedMotion = { true },
            onInteraction = { interactions += 1 },
            onCommittedTurn = { commits += 1 },
            onBoundaryHit = { boundaries += 1 }
        )

        assertTrue(
            listener.onKey(
                KeyEvent(
                    type = KeyEvent.Type.Down,
                    key = Key.ArrowRight,
                    modifiers = emptySet(),
                    characters = null
                )
            )
        )
        assertEquals(1, navigator.forwardCalls)
        assertEquals(1, interactions)
        assertEquals(1, commits)
        assertEquals(0, boundaries)

        navigator.reset()

        assertTrue(
            listener.onKey(
                KeyEvent(
                    type = KeyEvent.Type.Down,
                    key = Key.Space,
                    modifiers = emptySet(),
                    characters = " "
                )
            )
        )
        assertEquals(1, navigator.forwardCalls)
        assertEquals(2, interactions)
        assertEquals(2, commits)
        assertEquals(0, boundaries)
    }

    @Test
    fun paperKeyboard_terminalBoundary_isConsumed_withoutFalseCommit() {
        val navigator = fakeNavigator(
            progression = ReadingProgression.LTR,
            navigationSucceeds = false
        )
        var commits = 0
        var boundaries = 0
        val listener = PaperCurlInputListener(
            navigator = navigator,
            state = PaperCurlState(),
            isEnabled = { true },
            scope = CoroutineScope(Dispatchers.Unconfined),
            isReducedMotion = { true },
            onInteraction = {},
            onCommittedTurn = { commits += 1 },
            onBoundaryHit = { boundaries += 1 }
        )

        assertTrue(
            listener.onKey(
                KeyEvent(
                    type = KeyEvent.Type.Down,
                    key = Key.ArrowRight,
                    modifiers = emptySet(),
                    characters = null
                )
            )
        )
        assertEquals(1, navigator.forwardCalls)
        assertEquals(0, commits)
        assertEquals(1, boundaries)
    }

    @Test
    fun centerTap_isNotConsumedByDirectionalNavigation() {
        val navigator = fakeNavigator(ReadingProgression.LTR)
        val listener = listener(navigator)

        assertFalse(listener.onTap(TapEvent(PointF(500f, 800f))))
        assertEquals(0, navigator.forwardCalls)
        assertEquals(0, navigator.backwardCalls)
    }

    @Test
    fun continuousScroll_doesNotConsumeHorizontalEdgeTaps() {
        val navigator = fakeNavigator(
            progression = ReadingProgression.RTL,
            scroll = true
        )
        val listener = listener(navigator)

        assertFalse(listener.onTap(TapEvent(PointF(950f, 800f))))
        assertEquals(0, navigator.forwardCalls)
        assertEquals(0, navigator.backwardCalls)
    }

    @Test
    fun startupGate_preventsDirectionalNavigationUntilReaderIsReady() {
        val navigator = fakeNavigator(ReadingProgression.LTR)
        val listener = VeilDirectionalNavigationInputListener(
            navigator = navigator,
            isAnimated = { true },
            isEnabled = { false },
            isTapNavigationEnabled = { true }
        )

        assertFalse(listener.onTap(TapEvent(PointF(950f, 800f))))
        assertEquals(0, navigator.forwardCalls)
        assertEquals(0, navigator.backwardCalls)
    }

    @Test
    fun explicitTapGate_preventsNavigation_evenWhenPaginated() {
        val navigator = fakeNavigator(ReadingProgression.LTR)
        val listener = VeilDirectionalNavigationInputListener(
            navigator = navigator,
            isAnimated = { true },
            isTapNavigationEnabled = { false }
        )

        assertFalse(listener.onTap(TapEvent(PointF(950f, 800f))))
        assertEquals(0, navigator.forwardCalls)
        assertEquals(0, navigator.backwardCalls)
    }

    @Test
    fun reducedMotion_passesAnimatedFalseToNavigator() {
        val navigator = fakeNavigator(ReadingProgression.LTR)
        val listener = VeilDirectionalNavigationInputListener(
            navigator = navigator,
            isAnimated = { false },
            isTapNavigationEnabled = { true }
        )

        assertTrue(listener.onTap(TapEvent(PointF(950f, 800f))))
        assertEquals(1, navigator.forwardCalls)
        assertFalse(navigator.lastAnimated)
    }

    private fun listener(navigator: FakeNavigator) =
        VeilDirectionalNavigationInputListener(
            navigator = navigator,
            isAnimated = { true },
            isTapNavigationEnabled = { true }
        )

    private fun fakeNavigator(
        progression: ReadingProgression,
        scroll: Boolean = false,
        navigationSucceeds: Boolean = true
    ) = FakeNavigator(
        context = ApplicationProvider.getApplicationContext(),
        progression = progression,
        scroll = scroll,
        navigationSucceeds = navigationSucceeds
    )

    private class FakeNavigator(
        context: Context,
        progression: ReadingProgression,
        scroll: Boolean = false,
        private val navigationSucceeds: Boolean = true
    ) : OverflowableNavigator {
        override val publicationView: View = View(context).apply {
            layout(0, 0, 1000, 1600)
        }

        override val overflow: StateFlow<OverflowableNavigator.Overflow> =
            MutableStateFlow(
                FakeOverflow(
                    readingProgression = progression,
                    scroll = scroll
                )
            )

        override val currentLocator: StateFlow<Locator>
            get() = error("currentLocator is not used by this contract test")

        var forwardCalls: Int = 0
            private set
        var backwardCalls: Int = 0
            private set
        var lastAnimated: Boolean = false
            private set

        override fun goForward(animated: Boolean): Boolean {
            forwardCalls += 1
            lastAnimated = animated
            return navigationSucceeds
        }

        override fun goBackward(animated: Boolean): Boolean {
            backwardCalls += 1
            lastAnimated = animated
            return navigationSucceeds
        }

        override fun go(locator: Locator, animated: Boolean): Boolean = false
        override fun go(link: Link, animated: Boolean): Boolean = false
        override fun addInputListener(listener: InputListener) = Unit
        override fun removeInputListener(listener: InputListener) = Unit

        fun reset() {
            forwardCalls = 0
            backwardCalls = 0
            lastAnimated = false
        }
    }

    private data class FakeOverflow(
        override val readingProgression: ReadingProgression,
        override val scroll: Boolean,
        override val axis: Axis = Axis.HORIZONTAL
    ) : OverflowableNavigator.Overflow
}
