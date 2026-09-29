package com.veilreader.app.ui.screens

import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.Key
import org.readium.r2.navigator.input.KeyEvent
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Directional fallback for modes that do not own the input earlier in ReaderInputArbiter.
 *
 * PAPER_CURL and SLIDE normally consume their own page-turn gestures first. This listener keeps
 * static PAGED and renderer fallbacks deterministic, including RTL/LTR key direction.
 */
@OptIn(ExperimentalReadiumApi::class)
internal class VeilDirectionalNavigationInputListener(
    private val navigator: OverflowableNavigator,
    private val isAnimated: () -> Boolean,
    private val isTapNavigationEnabled: () -> Boolean = { true },
    private val onNavigationCommitted: () -> Unit = {}
) : InputListener {

    override fun onTap(event: TapEvent): Boolean {
        if (!isTapNavigationEnabled()) return false
        if (navigator.overflow.value.scroll) return false

        val width = navigator.publicationView.width.toFloat()
        if (width <= 0f) return false
        val density = navigator.publicationView.resources.displayMetrics.density
        val edge = pageTurnTapZonePx(
            width = width,
            density = density,
            preferredFraction = EDGE_FRACTION
        )
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
            Key.ArrowUp -> navigate { navigator.goBackward(animated = isAnimated()) }
            Key.ArrowDown, Key.Space -> navigate { navigator.goForward(animated = isAnimated()) }
            Key.ArrowLeft -> goLeft()
            Key.ArrowRight -> goRight()
            else -> false
        }
    }

    private fun goLeft(): Boolean =
        when (navigator.overflow.value.readingProgression) {
            ReadingProgression.LTR ->
                navigate { navigator.goBackward(animated = isAnimated()) }
            ReadingProgression.RTL ->
                navigate { navigator.goForward(animated = isAnimated()) }
        }

    private fun goRight(): Boolean =
        when (navigator.overflow.value.readingProgression) {
            ReadingProgression.LTR ->
                navigate { navigator.goForward(animated = isAnimated()) }
            ReadingProgression.RTL ->
                navigate { navigator.goBackward(animated = isAnimated()) }
        }

    private inline fun navigate(block: () -> Boolean): Boolean {
        val committed = block()
        if (committed) onNavigationCommitted()
        return committed
    }

    private companion object {
        const val EDGE_FRACTION = 0.22f
    }
}
