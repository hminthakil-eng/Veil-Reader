package com.veilreader.app.ui.reader

import com.veilreader.app.domain.ReaderHardwareKeyAction
import com.veilreader.app.domain.ReaderHardwareKeyMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderHardwareKeyControllerTest {
    @Test
    fun `system mapping never consumes hardware volume events`() {
        val controller = controller(
            mapping = ReaderHardwareKeyMap()
        )

        assertFalse(controller.handle(down(ReaderHardwareButton.VOLUME_UP, 1000L)))
        assertFalse(controller.handle(up(ReaderHardwareButton.VOLUME_UP, 1020L)))
    }

    @Test
    fun `mapped key consumes down and matching up`() {
        var nextCalls = 0
        val controller = controller(
            mapping = ReaderHardwareKeyMap(
                volumeUp = ReaderHardwareKeyAction.NEXT_PAGE
            ),
            onNext = { nextCalls += 1; true }
        )

        assertTrue(controller.handle(down(ReaderHardwareButton.VOLUME_UP, 1000L)))
        assertEquals(1, nextCalls)
        assertTrue(controller.handle(up(ReaderHardwareButton.VOLUME_UP, 1030L)))
    }

    @Test
    fun `held key repeats are throttled but separate presses are not`() {
        var nextCalls = 0
        val controller = controller(
            mapping = ReaderHardwareKeyMap(
                volumeDown = ReaderHardwareKeyAction.NEXT_PAGE
            ),
            onNext = { nextCalls += 1; true }
        )

        assertTrue(controller.handle(down(ReaderHardwareButton.VOLUME_DOWN, 1000L)))
        assertTrue(
            controller.handle(
                down(
                    button = ReaderHardwareButton.VOLUME_DOWN,
                    timeMs = 1080L,
                    repeatCount = 1
                )
            )
        )
        assertEquals(1, nextCalls)

        assertTrue(
            controller.handle(
                down(
                    button = ReaderHardwareButton.VOLUME_DOWN,
                    timeMs = 1200L,
                    repeatCount = 2
                )
            )
        )
        assertEquals(2, nextCalls)
        assertTrue(controller.handle(up(ReaderHardwareButton.VOLUME_DOWN, 1210L)))

        // A physically separate quick press is allowed immediately after key-up.
        assertTrue(controller.handle(down(ReaderHardwareButton.VOLUME_DOWN, 1250L)))
        assertEquals(3, nextCalls)
    }

    @Test
    fun `repeat throttling is isolated per physical volume key`() {
        var nextCalls = 0
        var previousCalls = 0
        val controller = ReaderHardwareKeyController(
            mapping = {
                ReaderHardwareKeyMap(
                    volumeUp = ReaderHardwareKeyAction.NEXT_PAGE,
                    volumeDown = ReaderHardwareKeyAction.PREVIOUS_PAGE
                )
            },
            isEnabled = { true },
            onPreviousPage = { previousCalls += 1; true },
            onNextPage = { nextCalls += 1; true },
            onToggleControls = { false }
        )

        assertTrue(controller.handle(down(ReaderHardwareButton.VOLUME_UP, 1000L)))
        assertTrue(controller.handle(down(ReaderHardwareButton.VOLUME_DOWN, 1050L)))
        assertEquals(1, nextCalls)
        assertEquals(1, previousCalls)
    }

    @Test
    fun `disabled reader mapping falls back to Android system volume`() {
        var calls = 0
        val controller = ReaderHardwareKeyController(
            mapping = {
                ReaderHardwareKeyMap(
                    volumeUp = ReaderHardwareKeyAction.PREVIOUS_PAGE
                )
            },
            isEnabled = { false },
            onPreviousPage = { calls += 1; true },
            onNextPage = { false },
            onToggleControls = { false }
        )

        assertFalse(controller.handle(down(ReaderHardwareButton.VOLUME_UP, 1000L)))
        assertEquals(0, calls)
    }

    @Test
    fun `consumed key-up stays consumed if mode changes mid press`() {
        var enabled = true
        val controller = ReaderHardwareKeyController(
            mapping = {
                ReaderHardwareKeyMap(
                    volumeUp = ReaderHardwareKeyAction.TOGGLE_CONTROLS
                )
            },
            isEnabled = { enabled },
            onPreviousPage = { false },
            onNextPage = { false },
            onToggleControls = { true }
        )

        assertTrue(controller.handle(down(ReaderHardwareButton.VOLUME_UP, 1000L)))
        enabled = false
        assertTrue(
            controller.handle(
                down(
                    button = ReaderHardwareButton.VOLUME_UP,
                    timeMs = 1080L,
                    repeatCount = 1
                )
            )
        )
        assertTrue(controller.handle(up(ReaderHardwareButton.VOLUME_UP, 1100L)))
    }

    @Test
    fun `repeat guard tolerates clock rollback`() {
        assertTrue(shouldHandleReaderHardwareRepeat(900L, 1000L))
        assertFalse(shouldHandleReaderHardwareRepeat(1100L, 1000L))
        assertTrue(shouldHandleReaderHardwareRepeat(1180L, 1000L))
    }

    private fun controller(
        mapping: ReaderHardwareKeyMap,
        onNext: () -> Boolean = { false }
    ): ReaderHardwareKeyController =
        ReaderHardwareKeyController(
            mapping = { mapping },
            isEnabled = { true },
            onPreviousPage = { false },
            onNextPage = onNext,
            onToggleControls = { false }
        )

    private fun down(
        button: ReaderHardwareButton,
        timeMs: Long,
        repeatCount: Int = 0
    ): ReaderHardwareButtonEvent =
        ReaderHardwareButtonEvent(
            button = button,
            phase = ReaderHardwareButtonPhase.DOWN,
            eventTimeMs = timeMs,
            repeatCount = repeatCount
        )

    private fun up(
        button: ReaderHardwareButton,
        timeMs: Long
    ): ReaderHardwareButtonEvent =
        ReaderHardwareButtonEvent(
            button = button,
            phase = ReaderHardwareButtonPhase.UP,
            eventTimeMs = timeMs
        )
}
