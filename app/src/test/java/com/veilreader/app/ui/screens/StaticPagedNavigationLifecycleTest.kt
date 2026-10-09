package com.veilreader.app.ui.screens

import android.graphics.PointF
import android.view.View
import java.lang.reflect.Proxy
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.input.DragEvent
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
class StaticPagedNavigationLifecycleTest {
    @Test
    fun `cancelled owner refuses drag without touching navigator`() = runTest {
        val owner = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        owner.cancel()
        val fixture = NavigatorFixture()
        val listener = fixture.listener(owner)
        assertFalse(listener.onDrag(drag(DragEvent.Type.Start)))
        assertFalse(listener.onDrag(drag(DragEvent.Type.End)))
        advanceUntilIdle()
        assertEquals(0, fixture.requests)
        assertEquals(0, fixture.commits)
    }

    @Test
    fun `cancellation before worker starts never moves page`() = runTest {
        val owner = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val fixture = NavigatorFixture()
        turn(fixture.listener(owner))
        owner.cancel()
        advanceUntilIdle()
        assertEquals(0, fixture.requests)
        assertEquals(fixture.origin, fixture.current.value)
        assertEquals(0, fixture.commits)
    }

    @Test
    fun `callback exception releases lock and retains committed destination`() = runTest {
        val errors = mutableListOf<Throwable>()
        val owner = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler) +
            CoroutineExceptionHandler { _, error -> errors += error })
        try {
            val fixture = NavigatorFixture().apply { failCommit = true }
            val listener = fixture.listener(owner)
            turn(listener)
            advanceUntilIdle()
            assertEquals(1, errors.size)
            assertEquals(11, fixture.current.value.locations.position)
            assertEquals(0, fixture.restores)
            fixture.failCommit = false
            turn(listener)
            advanceUntilIdle()
            assertEquals(2, fixture.requests)
            assertEquals(2, fixture.commits)
            assertEquals(12, fixture.current.value.locations.position)
        } finally { owner.cancel() }
    }

    @Test
    fun `settlement read exception releases lock for next turn`() = runTest {
        val errors = mutableListOf<Throwable>()
        val owner = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler) +
            CoroutineExceptionHandler { _, error -> errors += error })
        try {
            val fixture = NavigatorFixture().apply { failSettlement = true }
            val listener = fixture.listener(owner)
            turn(listener)
            advanceUntilIdle()
            assertEquals(1, errors.size)
            assertEquals(0, fixture.commits)
            fixture.failSettlement = false
            fixture.settlementReadFails = false
            turn(listener)
            advanceUntilIdle()
            assertEquals(2, fixture.requests)
            assertEquals(1, fixture.commits)
        } finally { owner.cancel() }
    }

    @Test
    fun `restore exception after bounded no-op settlement releases lock`() = runTest {
        val errors = mutableListOf<Throwable>()
        val owner = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler) +
            CoroutineExceptionHandler { _, error -> errors += error })
        try {
            val fixture = NavigatorFixture().apply { settleImmediately = false; failRestore = true }
            val listener = fixture.listener(owner)
            turn(listener)
            advanceUntilIdle()
            assertEquals(1, errors.size)
            assertEquals(1_500L, testScheduler.currentTime)
            assertEquals(0, fixture.commits)
            fixture.failRestore = false
            fixture.settleImmediately = true
            turn(listener)
            advanceUntilIdle()
            assertEquals(2, fixture.requests)
            assertEquals(1, fixture.commits)
        } finally { owner.cancel() }
    }

    @Test
    fun `mode disabled before worker starts leaves navigator unchanged`() = runTest {
        val owner = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        try {
            val fixture = NavigatorFixture()
            var enabled = true
            val listener = fixture.listener(owner) { enabled }
            turn(listener)
            enabled = false
            advanceUntilIdle()
            assertEquals(0, fixture.requests)
            enabled = true
            turn(listener)
            advanceUntilIdle()
            assertEquals(1, fixture.requests)
            assertEquals(1, fixture.commits)
        } finally { owner.cancel() }
    }

    @Test
    fun `inline completion accepts twenty turns in each progression without stale lock`() {
        listOf(ReadingProgression.LTR, ReadingProgression.RTL).forEach { progression ->
            val owner = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            try {
                val fixture = NavigatorFixture(progression)
                val listener = fixture.listener(owner)
                repeat(20) { turn(listener) }
                assertEquals(20, fixture.requests)
                assertEquals(20, fixture.commits)
                assertEquals(if (progression == ReadingProgression.LTR) 30 else 80,
                    fixture.current.value.locations.position)
            } finally { owner.cancel() }
        }
    }

    private fun turn(listener: StaticPagedNavigationInputListener) {
        assertTrue(listener.onDrag(drag(DragEvent.Type.Start)))
        assertTrue(listener.onDrag(drag(DragEvent.Type.End)))
    }

    private fun drag(type: DragEvent.Type) =
        DragEvent(type, PointF(950f, 400f), PointF(-300f, 0f))

    private class NavigatorFixture(progression: ReadingProgression = ReadingProgression.LTR) {
        val origin = Locator(href = Url("chapter.xhtml")!!, mediaType = MediaType.XHTML,
            locations = Locator.Locations(position = if (progression == ReadingProgression.LTR) 10 else 100))
        val current = MutableStateFlow(origin)
        var requests = 0
        var commits = 0
        var restores = 0
        var settleImmediately = true
        var failCommit = false
        var failRestore = false
        var failSettlement = false
        var settlementReadFails = false
        private val exposedLocator = object : StateFlow<Locator> by current {
            override val value: Locator
                get() {
                    if (settlementReadFails) throw IllegalStateException("settlement failure")
                    return current.value
                }
        }
        private val view = View(RuntimeEnvironment.getApplication()).apply {
            layout(0, 0, 1000, 1600)
        }
        private val overflow = Proxy.newProxyInstance(javaClass.classLoader,
            arrayOf(OverflowableNavigator.Overflow::class.java)) { _, method, _ ->
            when (method.name) {
                "getReadingProgression" -> progression
                "getScroll" -> false
                else -> null
            }
        } as OverflowableNavigator.Overflow
        private val overflowFlow = MutableStateFlow(overflow)
        private val navigator = Proxy.newProxyInstance(javaClass.classLoader,
            arrayOf(OverflowableNavigator::class.java)) { _, method, args ->
            when (method.name) {
                "getCurrentLocator" -> exposedLocator
                "getPublicationView" -> view
                "getOverflow" -> overflowFlow
                "goForward", "goBackward" -> {
                    requests++
                    if (settleImmediately) current.value = current.value.copy(locations =
                        current.value.locations.copy(position = (current.value.locations.position ?: 10) +
                            if (method.name == "goForward") 1 else -1))
                    settlementReadFails = failSettlement
                    true
                }
                "go" -> {
                    restores++
                    if (failRestore) throw IllegalStateException("restore failure")
                    current.value = args!![0] as Locator
                    true
                }
                else -> null
            }
        } as OverflowableNavigator

        fun listener(owner: CoroutineScope, enabled: () -> Boolean = { true }) =
            StaticPagedNavigationInputListener(navigator, enabled, owner, {}, {
                commits++
                if (failCommit) throw IllegalStateException("commit failure")
            })
    }
}
