package com.veilreader.app.ui.reader

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
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

internal fun readerHardwareAccessibilityActive(manager: AccessibilityManager?): Boolean =
    manager?.isTouchExplorationEnabled == true ||
        manager?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN)
            ?.isNotEmpty() == true

/** Activity-scoped press ownership survives Reader handler disposal/replacement. */
internal class ReaderHardwareKeyDispatcher : ReaderHardwareKeyHost {
    private var ownerId: String? = null
    private var handler: ((ReaderHardwareButtonEvent) -> Boolean)? = null
    private val consumedPresses = mutableMapOf<ReaderHardwareButton, String>()

    override fun installReaderHardwareKeyHandler(
        ownerId: String,
        handler: (ReaderHardwareButtonEvent) -> Boolean
    ) {
        this.ownerId = ownerId
        this.handler = handler
    }

    override fun clearReaderHardwareKeyHandler(ownerId: String) {
        if (this.ownerId != ownerId) return
        this.ownerId = null
        handler = null
    }

    fun handle(event: ReaderHardwareButtonEvent): Boolean {
        if (event.phase == ReaderHardwareButtonPhase.DOWN && event.repeatCount == 0) {
            // Android can route a previous key-up to a dialog. A fresh physical
            // down starts a new press even if that key-up never reached us.
            consumedPresses.remove(event.button)
        }
        val pressOwner = consumedPresses[event.button]
        // Do not deliver the tail of an old session's press to a new Reader.
        val handled = if (pressOwner == null || pressOwner == ownerId) {
            handler?.invoke(event) == true
        } else {
            false
        }
        if (event.phase == ReaderHardwareButtonPhase.UP) {
            consumedPresses.remove(event.button)
        } else if (handled) {
            ownerId?.let { consumedPresses[event.button] = it }
        }
        return handled || pressOwner != null
    }
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

        if (event.repeatCount == 0) activeConsumedButtons.remove(event.button)

        val action = actionFor(event.button)
        val alreadyConsumed = event.button in activeConsumedButtons

        // A press delegated to Android cannot become a Reader press halfway through.
        if (event.repeatCount > 0 && !alreadyConsumed) return false

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
        // A failed repeat (for example while a page turn is in flight or at the
        // publication boundary) cannot transfer an already-owned press to Android.
        return handled || alreadyConsumed
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
