package com.veilreader.app.ui.screens

import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.PageTurnStyle
import org.readium.r2.navigator.input.DragEvent
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.KeyEvent
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.ExperimentalReadiumApi

internal enum class ReaderTapOwner {
    IMAGE,
    PAPER,
    SLIDE,
    DIRECTIONAL,
    CHROME,
    RENDERER,
    BLOCKED
}

internal enum class ReaderInteractionMode {
    NAVIGATION,
    CHROME_PRIORITY,
    RENDERER_SELECTION,
    RENDERER_ACCESSIBILITY,
    BLOCKED
}

internal enum class ReaderKeyRoute {
    NAVIGATION,
    RENDERER,
    BLOCKED
}

internal fun readerKeyRoute(mode: ReaderInteractionMode): ReaderKeyRoute =
    when (mode) {
        ReaderInteractionMode.RENDERER_SELECTION,
        ReaderInteractionMode.RENDERER_ACCESSIBILITY -> ReaderKeyRoute.RENDERER
        ReaderInteractionMode.BLOCKED -> ReaderKeyRoute.BLOCKED
        ReaderInteractionMode.NAVIGATION,
        ReaderInteractionMode.CHROME_PRIORITY -> ReaderKeyRoute.NAVIGATION
    }

internal fun readerInteractionMode(
    selectionModeActive: Boolean,
    overlayVisible: Boolean,
    closeInFlight: Boolean,
    controlsVisible: Boolean,
    touchExplorationEnabled: Boolean = false
): ReaderInteractionMode = when {
    overlayVisible || closeInFlight -> ReaderInteractionMode.BLOCKED
    selectionModeActive -> ReaderInteractionMode.RENDERER_SELECTION
    touchExplorationEnabled -> ReaderInteractionMode.RENDERER_ACCESSIBILITY
    controlsVisible -> ReaderInteractionMode.CHROME_PRIORITY
    else -> ReaderInteractionMode.NAVIGATION
}

internal fun pageTurnTapZonePx(
    width: Float,
    density: Float,
    preferredFraction: Float = 0.22f
): Float {
    if (width <= 0f) return 0f
    val safeDensity = density.coerceAtLeast(0.1f)
    val minComfortableZone = 56f * safeDensity
    val maxComfortableZone = 112f * safeDensity
    val preferred = width * preferredFraction.coerceIn(0.14f, 0.26f)
    val upperBound = minOf(maxComfortableZone, width * 0.28f)
    return maxOf(minComfortableZone, preferred)
        .coerceAtMost(upperBound)
}

internal fun shouldUseDirectionalTapNavigation(
    format: BookFormat,
    scroll: Boolean,
    pageTurnStyle: PageTurnStyle
): Boolean =
    format == BookFormat.EPUB &&
        !scroll &&
        pageTurnStyle == PageTurnStyle.NONE

/**
 * Page-turn style is an EPUB-only preference.
 *
 * PDF navigation must not change when the hidden EPUB page-turn preference changes.
 * Keep PDF directional-key navigation deterministic and unanimated here; native PDF
 * swipe/fling/zoom behavior remains owned by the renderer.
 */
internal fun shouldAnimateDirectionalNavigation(
    format: BookFormat,
    scroll: Boolean,
    pageTurnStyle: PageTurnStyle
): Boolean =
    format == BookFormat.EPUB &&
        !scroll &&
        pageTurnStyle == PageTurnStyle.SLIDE

/**
 * One Veil input listener is registered with Readium.
 *
 * Internal delegate order is product policy, not an incidental registration order:
 * paper curl -> Veil slide -> static paged navigation -> directional keys -> Veil chrome -> renderer fallback.
 */
@OptIn(ExperimentalReadiumApi::class)
internal class ReaderInputArbiter(
    private val contentTarget: InputListener?,
    private val paper: InputListener?,
    private val slide: InputListener?,
    private val staticPaged: InputListener?,
    private val directional: InputListener,
    private val chromeTap: (TapEvent) -> Boolean,
    private val interactionMode: () -> ReaderInteractionMode = {
        ReaderInteractionMode.NAVIGATION
    },
    private val onTapOwner: (ReaderTapOwner) -> Unit = {}
) : InputListener {

    override fun onTap(event: TapEvent): Boolean {
        when (interactionMode()) {
            ReaderInteractionMode.RENDERER_SELECTION,
            ReaderInteractionMode.RENDERER_ACCESSIBILITY -> {
                onTapOwner(ReaderTapOwner.RENDERER)
                return false
            }
            ReaderInteractionMode.BLOCKED -> {
                onTapOwner(ReaderTapOwner.BLOCKED)
                return true
            }
            ReaderInteractionMode.CHROME_PRIORITY -> {
                if (chromeTap(event)) {
                    onTapOwner(ReaderTapOwner.CHROME)
                    return true
                }
                onTapOwner(ReaderTapOwner.BLOCKED)
                return true
            }
            ReaderInteractionMode.NAVIGATION -> Unit
        }

        if (contentTarget?.onTap(event) == true) {
            onTapOwner(ReaderTapOwner.IMAGE)
            return true
        }

        if (paper?.onTap(event) == true) {
            onTapOwner(ReaderTapOwner.PAPER)
            return true
        }

        if (slide?.onTap(event) == true) {
            onTapOwner(ReaderTapOwner.SLIDE)
            return true
        }

        if (directional.onTap(event)) {
            onTapOwner(ReaderTapOwner.DIRECTIONAL)
            return true
        }

        if (chromeTap(event)) {
            onTapOwner(ReaderTapOwner.CHROME)
            return true
        }

        onTapOwner(ReaderTapOwner.RENDERER)
        return false
    }

    override fun onDrag(event: DragEvent): Boolean {
        when (interactionMode()) {
            ReaderInteractionMode.RENDERER_SELECTION,
            ReaderInteractionMode.RENDERER_ACCESSIBILITY -> return false
            ReaderInteractionMode.BLOCKED -> return true
            ReaderInteractionMode.NAVIGATION,
            ReaderInteractionMode.CHROME_PRIORITY -> Unit
        }

        if (paper?.onDrag(event) == true) return true
        if (slide?.onDrag(event) == true) return true
        if (staticPaged?.onDrag(event) == true) return true
        return false
    }

    override fun onKey(event: KeyEvent): Boolean {
        when (readerKeyRoute(interactionMode())) {
            ReaderKeyRoute.RENDERER -> return false
            ReaderKeyRoute.BLOCKED -> return true
            ReaderKeyRoute.NAVIGATION -> Unit
        }

        if (paper?.onKey(event) == true) return true
        if (slide?.onKey(event) == true) return true
        return directional.onKey(event)
    }
}
