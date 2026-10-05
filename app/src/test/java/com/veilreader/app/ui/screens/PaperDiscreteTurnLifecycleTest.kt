package com.veilreader.app.ui.screens

import android.view.View
import com.veilreader.app.ui.reader.material.MaterialPageEngineRollout
import java.lang.reflect.Proxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalReadiumApi::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class PaperDiscreteTurnLifecycleTest {
    @Test
    fun `accepted discrete preview restores exact origin when owner is disposed`() = runTest {
        MaterialPageEngineRollout.setDebugOverride(true)
        try {
            listOf(true, false).forEach { paper ->
                val fixture = NavigatorFixture()
                val controls = controls(paper, fixture, this)
                assertTrue(controls.perform())
                runCurrent()
                assertEquals(1, fixture.requests)
                assertTrue(controls.pending())
                assertTrue(controls.cancel())
                advanceUntilIdle()
                assertEquals(listOf(fixture.origin), fixture.restores)
                assertEquals(0, fixture.commits)
                assertFalse(controls.pending())
                assertEquals(1, fixture.requests)
            }
        } finally {
            MaterialPageEngineRollout.setDebugOverride(null)
        }
    }

    @Test
    fun `late canceled preview cannot restore over the next committed turn`() = runTest {
        MaterialPageEngineRollout.setDebugOverride(true)
        try {
            listOf(true, false).forEach { paper ->
                val fixture = NavigatorFixture()
                val controls = controls(paper, fixture, this)
                assertTrue(controls.perform())
                runCurrent()
                assertTrue(controls.cancel())
                fixture.settleImmediately = true
                assertTrue(controls.perform())
                advanceUntilIdle()
                assertEquals(2, fixture.requests)
                assertEquals(1, fixture.commits)
                assertEquals(listOf(fixture.origin), fixture.restores)
                assertEquals(11, fixture.current.value.locations.position)
                assertFalse(controls.pending())
            }
        } finally {
            MaterialPageEngineRollout.setDebugOverride(null)
        }
    }

    @Test
    fun `inline completion accepts twenty repeated turns in either progression`() {
        MaterialPageEngineRollout.setDebugOverride(true)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            listOf(true, false).forEach { paper ->
                listOf(ReadingProgression.LTR, ReadingProgression.RTL).forEach { progression ->
                    val fixture = NavigatorFixture(progression).apply { settleImmediately = true }
                    val controls = controls(paper, fixture, scope)
                    repeat(20) {
                        assertTrue(controls.perform())
                        assertFalse(controls.pending())
                    }
                    assertEquals(20, fixture.requests)
                    assertEquals(20, fixture.commits)
                    assertTrue(fixture.restores.isEmpty())
                }
            }
        } finally {
            scope.cancel()
            MaterialPageEngineRollout.setDebugOverride(null)
        }
    }

    private data class Controls(val perform: () -> Boolean, val pending: () -> Boolean, val cancel: () -> Boolean)

    private fun controls(paper: Boolean, fixture: NavigatorFixture, scope: CoroutineScope): Controls {
        return if (paper) {
            val listener = PaperCurlInputListener(fixture.navigator, PaperCurlState(), { true }, scope,
                isReducedMotion = { true }, onInteraction = {}, onCommittedTurn = { fixture.commits++ })
            Controls({ listener.performDiscreteTurn(PaperTurnDirection.FORWARD) },
                listener::hasPendingTurn, listener::forceCancelPendingTurn)
        } else {
            val listener = SlideNavigationInputListener(fixture.navigator, SlidePageState(), { true }, scope,
                isReducedMotion = { true }, onInteraction = {}, onCommittedTurn = { fixture.commits++ })
            Controls({ listener.performDiscreteTurn(PaperTurnDirection.FORWARD) },
                listener::hasPendingTurn, listener::forceCancelPendingTurn)
        }
    }

    private class NavigatorFixture(progression: ReadingProgression = ReadingProgression.LTR) {
        val origin = Locator(href = Url("chapter.xhtml")!!, mediaType = MediaType.XHTML,
            locations = Locator.Locations(position = 10))
        val current = MutableStateFlow(origin)
        val restores = mutableListOf<Locator>()
        var settleImmediately = false
        var requests = 0
        var commits = 0
        private val view = View(RuntimeEnvironment.getApplication())
        private val overflow = Proxy.newProxyInstance(javaClass.classLoader,
            arrayOf(OverflowableNavigator.Overflow::class.java)) { _, method, _ ->
            when (method.name) {
                "getReadingProgression" -> progression
                "getScroll" -> false
                else -> null
            }
        } as OverflowableNavigator.Overflow
        private val overflowFlow = MutableStateFlow(overflow)
        val navigator = Proxy.newProxyInstance(javaClass.classLoader,
            arrayOf(OverflowableNavigator::class.java)) { _, method, args ->
            when (method.name) {
                "getCurrentLocator" -> current
                "getPublicationView" -> view
                "getOverflow" -> overflowFlow
                "goForward", "goBackward" -> {
                    requests++
                    if (settleImmediately) current.value = current.value.copy(locations =
                        current.value.locations.copy(position = (current.value.locations.position ?: 10) + 1))
                    true
                }
                "go" -> {
                    restores += args!![0] as Locator
                    current.value = args[0] as Locator
                    true
                }
                else -> null
            }
        } as OverflowableNavigator
    }
}
