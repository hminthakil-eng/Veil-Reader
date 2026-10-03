package com.veilreader.app.ui.screens

import android.graphics.PointF
import android.view.View
import com.veilreader.app.domain.ReaderTapAction
import com.veilreader.app.domain.ReaderTapGrid
import com.veilreader.app.domain.ReaderTapZone
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.ExperimentalReadiumApi
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalReadiumApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderTapOwnershipTest {
    private val event = TapEvent(PointF(10f, 10f))

    @Test
    fun `renderer matrix zones bypass every Veil turn and chrome delegate`() {
        var calls = 0
        val delegate = object : InputListener {
            override fun onTap(event: TapEvent): Boolean { calls += 1; return true }
        }
        val arbiter = ReaderInputArbiter(
            contentTarget = null,
            tapZones = zones(ReaderTapAction.RENDERER),
            paper = delegate, slide = delegate, staticPaged = delegate,
            directional = delegate, chromeTap = { calls += 1; true }
        )
        assertFalse(arbiter.onTap(event))
        assertEquals(0, calls)
    }

    @Test
    fun `default matrix defers to existing page turn ownership`() {
        var calls = 0
        val arbiter = ReaderInputArbiter(
            contentTarget = null, tapZones = zones(ReaderTapAction.VEIL_DEFAULT),
            paper = object : InputListener {
                override fun onTap(event: TapEvent): Boolean { calls += 1; return true }
            },
            slide = null, staticPaged = null, directional = object : InputListener {},
            chromeTap = { error("Paper should already own this tap") }
        )
        assertTrue(arbiter.onTap(event))
        assertEquals(1, calls)
    }

    @Test
    fun `selection and accessibility bypass matrix and page turns`() {
        for (mode in listOf(ReaderInteractionMode.RENDERER_SELECTION, ReaderInteractionMode.RENDERER_ACCESSIBILITY)) {
            val delegate = object : InputListener {
                override fun onTap(event: TapEvent): Boolean = error("Renderer owns this tap")
            }
            val arbiter = ReaderInputArbiter(
                contentTarget = delegate, tapZones = zones(ReaderTapAction.NEXT_PAGE),
                paper = delegate, slide = delegate, staticPaged = delegate, directional = delegate,
                chromeTap = { error("Renderer owns this tap") }, interactionMode = { mode }
            )
            assertFalse(arbiter.onTap(event))
        }
    }

    @Test
    fun `semantic next and previous invoke their own callbacks`() {
        var next = 0
        var previous = 0
        for (action in listOf(ReaderTapAction.NEXT_PAGE, ReaderTapAction.PREVIOUS_PAGE)) {
            val listener = zones(action, onNext = { next += 1; true }, onPrevious = { previous += 1; true })
            assertEquals(ReaderTapZoneDisposition.CONSUMED, listener.routeTap(event))
        }
        assertEquals(1, next)
        assertEquals(1, previous)
    }

    private fun zones(
        action: ReaderTapAction,
        onNext: () -> Boolean = { error("Unexpected next") },
        onPrevious: () -> Boolean = { error("Unexpected previous") }
    ): ReaderTapZoneInputListener {
        val view = View(RuntimeEnvironment.getApplication()).apply { layout(0, 0, 900, 1200) }
        val navigator = Proxy.newProxyInstance(
            OverflowableNavigator::class.java.classLoader, arrayOf(OverflowableNavigator::class.java)
        ) { _, method, _ ->
            if (method.name == "getPublicationView") view else error("Unexpected navigator call: ${method.name}")
        } as OverflowableNavigator
        return ReaderTapZoneInputListener(
            navigator, { ReaderTapGrid().withAction(ReaderTapZone.TOP_LEFT, action) },
            { true }, { true }, onPrevious, onNext, { error("Unexpected chrome") }
        )
    }
}
