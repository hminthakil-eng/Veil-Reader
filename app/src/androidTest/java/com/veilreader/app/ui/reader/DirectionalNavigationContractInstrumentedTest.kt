package com.veilreader.app.ui.reader

import android.content.Context
import android.graphics.PointF
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.navigator.util.DirectionalNavigationAdapter
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalReadiumApi::class)
class DirectionalNavigationContractInstrumentedTest {

    @Test
    fun ltr_rightEdge_movesForward_and_leftEdge_movesBackward() {
        val navigator = FakeNavigator(
            context = ApplicationProvider.getApplicationContext(),
            progression = ReadingProgression.LTR
        )
        val adapter = DirectionalNavigationAdapter(
            navigator = navigator,
            animatedTransition = true
        )

        assertTrue(adapter.onTap(TapEvent(PointF(950f, 800f))))
        assertEquals(1, navigator.forwardCalls)
        assertEquals(0, navigator.backwardCalls)
        assertTrue(navigator.lastAnimated)

        navigator.reset()

        assertTrue(adapter.onTap(TapEvent(PointF(50f, 800f))))
        assertEquals(0, navigator.forwardCalls)
        assertEquals(1, navigator.backwardCalls)
        assertTrue(navigator.lastAnimated)
    }

    @Test
    fun rtl_rightEdge_movesBackward_and_leftEdge_movesForward() {
        val navigator = FakeNavigator(
            context = ApplicationProvider.getApplicationContext(),
            progression = ReadingProgression.RTL
        )
        val adapter = DirectionalNavigationAdapter(
            navigator = navigator,
            animatedTransition = true
        )

        assertTrue(adapter.onTap(TapEvent(PointF(950f, 800f))))
        assertEquals(0, navigator.forwardCalls)
        assertEquals(1, navigator.backwardCalls)
        assertTrue(navigator.lastAnimated)

        navigator.reset()

        assertTrue(adapter.onTap(TapEvent(PointF(50f, 800f))))
        assertEquals(1, navigator.forwardCalls)
        assertEquals(0, navigator.backwardCalls)
        assertTrue(navigator.lastAnimated)
    }

    @Test
    fun centerTap_isNotConsumedByDirectionalNavigation() {
        val navigator = FakeNavigator(
            context = ApplicationProvider.getApplicationContext(),
            progression = ReadingProgression.LTR
        )
        val adapter = DirectionalNavigationAdapter(navigator = navigator)

        assertFalse(adapter.onTap(TapEvent(PointF(500f, 800f))))
        assertEquals(0, navigator.forwardCalls)
        assertEquals(0, navigator.backwardCalls)
    }

    @Test
    fun continuousScroll_doesNotConsumeHorizontalEdgeTaps_byDefault() {
        val navigator = FakeNavigator(
            context = ApplicationProvider.getApplicationContext(),
            progression = ReadingProgression.RTL,
            scroll = true
        )
        val adapter = DirectionalNavigationAdapter(navigator = navigator)

        assertFalse(adapter.onTap(TapEvent(PointF(950f, 800f))))
        assertEquals(0, navigator.forwardCalls)
        assertEquals(0, navigator.backwardCalls)
    }

    @Test
    fun reducedMotionContract_passesAnimatedFalseToReadium() {
        val navigator = FakeNavigator(
            context = ApplicationProvider.getApplicationContext(),
            progression = ReadingProgression.LTR
        )
        val adapter = DirectionalNavigationAdapter(
            navigator = navigator,
            animatedTransition = false
        )

        assertTrue(adapter.onTap(TapEvent(PointF(950f, 800f))))
        assertEquals(1, navigator.forwardCalls)
        assertFalse(navigator.lastAnimated)
    }

    private class FakeNavigator(
        context: Context,
        progression: ReadingProgression,
        scroll: Boolean = false
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
            return true
        }

        override fun goBackward(animated: Boolean): Boolean {
            backwardCalls += 1
            lastAnimated = animated
            return true
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
