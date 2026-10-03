package com.veilreader.app.ui.reader

import com.veilreader.app.domain.ReaderHardwareKeyAction
import com.veilreader.app.domain.ReaderHardwareKeyMap

enum class ReaderHardwareButton {
    VOLUME_UP,
    VOLUME_DOWN
}

enum class ReaderHardwareButtonPhase {
    DOWN,
    UP
}

data class ReaderHardwareButtonEvent(
    val button: ReaderHardwareButton,
    val phase: ReaderHardwareButtonPhase,
    val eventTimeMs: Long,
    val repeatCount: Int = 0
)

interface ReaderHardwareKeyHost {
    fun installReaderHardwareKeyHandler(
        ownerId: String,
        handler: (ReaderHardwareButtonEvent) -> Boolean
    )

    fun clearReaderHardwareKeyHandler(ownerId: String)
}

/**
 * Platform-independent policy for optional reader hardware-key actions.
 *
 * The host Activity owns Android key consumption. This controller only decides whether
 * a mapped key should trigger a Reader action and whether the corresponding key-up event
 * should remain consumed. SYSTEM is always delegated to Android.
 */
internal class ReaderHardwareKeyController(
    private val mapping: () -> ReaderHardwareKeyMap,
    private val isEnabled: () -> Boolean,
    private val onPreviousPage: () -> Boolean,
    private val onNextPage: () -> Boolean,
    private val onToggleControls: () -> Boolean,
    private val minimumRepeatIntervalMs: Long = 180L
) {
    private val activeConsumedButtons = mutableSetOf<ReaderHardwareButton>()
    private val lastHandledAtMs = mutableMapOf<ReaderHardwareButton, Long>()

    fun handle(event: ReaderHardwareButtonEvent): Boolean {
        if (event.phase == ReaderHardwareButtonPhase.UP) {
            // Consume the matching key-up whenever its key-down was consumed, even if
            // settings or interaction mode changed while the button was held.
            return activeConsumedButtons.remove(event.button)
        }

        val action = actionFor(event.button)
        val alreadyConsumed = event.button in activeConsumedButtons

        // Once a physical press starts as a Reader-owned action, keep the entire press
        // consumed until key-up. A dialog, TalkBack transition, or settings change may
        // disable new Reader actions mid-press, but must not leak repeat events to volume.
        if (
            alreadyConsumed &&
            (action == ReaderHardwareKeyAction.SYSTEM || !isEnabled())
        ) {
            return true
        }

        if (action == ReaderHardwareKeyAction.SYSTEM || !isEnabled()) {
            return false
        }

        val repeatingSamePress = event.repeatCount > 0 || alreadyConsumed
        if (
            repeatingSamePress &&
            !shouldHandleReaderHardwareRepeat(
                nowElapsedMs = event.eventTimeMs,
                lastHandledAtMs = lastHandledAtMs[event.button] ?: Long.MIN_VALUE,
                minimumIntervalMs = minimumRepeatIntervalMs
            )
        ) {
            return alreadyConsumed
        }

        val handled = when (action) {
            ReaderHardwareKeyAction.SYSTEM -> false
            ReaderHardwareKeyAction.PREVIOUS_PAGE -> onPreviousPage()
            ReaderHardwareKeyAction.NEXT_PAGE -> onNextPage()
            ReaderHardwareKeyAction.TOGGLE_CONTROLS -> onToggleControls()
        }

        if (handled) {
            activeConsumedButtons += event.button
            lastHandledAtMs[event.button] = event.eventTimeMs
        }
        return handled
    }

    private fun actionFor(button: ReaderHardwareButton): ReaderHardwareKeyAction =
        when (button) {
            ReaderHardwareButton.VOLUME_UP -> mapping().volumeUp
            ReaderHardwareButton.VOLUME_DOWN -> mapping().volumeDown
        }
}

internal fun shouldHandleReaderHardwareRepeat(
    nowElapsedMs: Long,
    lastHandledAtMs: Long,
    minimumIntervalMs: Long = 180L
): Boolean {
    if (lastHandledAtMs == Long.MIN_VALUE) return true
    if (nowElapsedMs < lastHandledAtMs) return true
    return nowElapsedMs - lastHandledAtMs >= minimumIntervalMs.coerceAtLeast(0L)
}
