package com.veilreader.app.ui.screens

import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.Key
import org.readium.r2.navigator.input.KeyEvent
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Veil-owned directional fallback so animation policy can change at runtime.
 *
 * Paper mode can be intercepted by Veil's curl layer. Slide mode falls through
 * here and uses Readium's native animated navigation. Keyboard navigation
 * stays consistent with the currently selected page-turn style.
 */
@OptIn(ExperimentalReadiumApi::class)
internal class VeilDirectionalNavigationInputListener(
    private val navigator: OverflowableNavigator,
    private val isAnimated: () -> Boolean
) : InputListener {

    override fun onTap(event: TapEvent): Boolean {
        if (navigator.overflow.value.scroll) return false

        val width = navigator.publicationView.width.toDouble()
        if (width <= 0.0) return false

        val edge = maxOf(MIN_EDGE_PX, width * EDGE_FRACTION)
        return when {
            event.point.x <= edge -> goLeft()
            event.point.x >= width - edge -> goRight()
            else -> false
        }
    }

    override fun onKey(event: KeyEvent): Boolean {
        if (event.type != KeyEvent.Type.Down || event.modifiers.isNotEmpty()) {
            return false
        }

        return when (event.key) {
            Key.ArrowUp -> navigator.goBackward(animated = isAnimated())
            Key.ArrowDown, Key.Space -> navigator.goForward(animated = isAnimated())
            Key.ArrowLeft -> goLeft()
            Key.ArrowRight -> goRight()
            else -> false
        }
    }

    private fun goLeft(): Boolean =
        when (navigator.overflow.value.readingProgression) {
            ReadingProgression.LTR ->
                navigator.goBackward(animated = isAnimated())
            ReadingProgression.RTL ->
                navigator.goForward(animated = isAnimated())
        }

    private fun goRight(): Boolean =
        when (navigator.overflow.value.readingProgression) {
            ReadingProgression.LTR ->
                navigator.goForward(animated = isAnimated())
            ReadingProgression.RTL ->
                navigator.goBackward(animated = isAnimated())
        }

    private companion object {
        const val MIN_EDGE_PX = 80.0
        const val EDGE_FRACTION = 0.30
    }
}
