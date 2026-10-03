package com.veilreader.app.ui.screens

import android.os.SystemClock
import com.veilreader.app.domain.ReaderHardwareKeyAction
import com.veilreader.app.domain.ReaderHardwareKeyMap
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.Key
import org.readium.r2.navigator.input.KeyEvent
import org.readium.r2.shared.ExperimentalReadiumApi

@OptIn(ExperimentalReadiumApi::class)
internal class ReaderHardwareKeyInputListener(
    private val mapping: () -> ReaderHardwareKeyMap,
    private val isEnabled: () -> Boolean,
    private val onPreviousPage: () -> Boolean,
    private val onNextPage: () -> Boolean,
    private val onToggleControls: () -> Boolean,
    private val nowElapsedMs: () -> Long = SystemClock::elapsedRealtime
) : InputListener {
    private var lastHandledAtMs: Long = Long.MIN_VALUE

    override fun onKey(event: KeyEvent): Boolean {
        if (!isEnabled()) return false
        if (event.type != KeyEvent.Type.Down || event.modifiers.isNotEmpty()) return false

        val action = when (event.key) {
            Key.AudioVolumeUp -> mapping().volumeUp
            Key.AudioVolumeDown -> mapping().volumeDown
            else -> return false
        }
        if (action == ReaderHardwareKeyAction.SYSTEM) return false

        val now = nowElapsedMs()
        if (!shouldHandleReaderHardwareRepeat(now, lastHandledAtMs)) {
            return true
        }

        val handled = when (action) {
            ReaderHardwareKeyAction.SYSTEM -> false
            ReaderHardwareKeyAction.PREVIOUS_PAGE -> onPreviousPage()
            ReaderHardwareKeyAction.NEXT_PAGE -> onNextPage()
            ReaderHardwareKeyAction.TOGGLE_CONTROLS -> onToggleControls()
        }
        if (handled) lastHandledAtMs = now
        return handled
    }
}

internal fun isReaderVolumeKey(event: KeyEvent): Boolean =
    event.key == Key.AudioVolumeUp ||
        event.key == Key.AudioVolumeDown ||
        event.key == Key.AudioVolumeMute

internal fun shouldHandleReaderHardwareRepeat(
    nowElapsedMs: Long,
    lastHandledAtMs: Long,
    minimumIntervalMs: Long = 180L
): Boolean {
    if (lastHandledAtMs == Long.MIN_VALUE) return true
    if (nowElapsedMs < lastHandledAtMs) return true
    return nowElapsedMs - lastHandledAtMs >= minimumIntervalMs.coerceAtLeast(0L)
}
