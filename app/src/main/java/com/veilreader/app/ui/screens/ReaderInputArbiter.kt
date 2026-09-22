package com.veilreader.app.ui.screens

import com.veilreader.app.domain.BookFormat
import com.veilreader.app.domain.PageTurnStyle
import org.readium.r2.navigator.input.DragEvent
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.KeyEvent
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.ExperimentalReadiumApi

internal enum class ReaderTapOwner {
    PAPER,
    DIRECTIONAL,
    CHROME,
    RENDERER
}

internal fun shouldUseDirectionalTapNavigation(
    format: BookFormat,
    scroll: Boolean,
    pageTurnStyle: PageTurnStyle
): Boolean =
    format == BookFormat.EPUB &&
        !scroll &&
        pageTurnStyle == PageTurnStyle.SLIDE

/**
 * Page-turn style is an EPUB-only preference.
 *
 * PDF navigation must not change when the hidden EPUB page-turn preference changes.
 * Keep PDF directional-key navigation deterministic and unanimated here; native PDF
 * swipe/fling/zoom behavior remains owned by the renderer.
 */
internal fun shouldAnimateDirectionalNavigation(
    format: BookFormat,
    pageTurnStyle: PageTurnStyle
): Boolean =
    format == BookFormat.EPUB &&
        pageTurnStyle == PageTurnStyle.SLIDE

/**
 * One Veil input listener is registered with Readium.
 *
 * Internal delegate order is product policy, not an incidental registration order:
 * paper turn -> directional edge navigation -> Veil chrome -> renderer fallback.
 */
@OptIn(ExperimentalReadiumApi::class)
internal class ReaderInputArbiter(
    private val paper: InputListener?,
    private val directional: InputListener,
    private val chromeTap: (TapEvent) -> Boolean,
    private val onTapOwner: (ReaderTapOwner) -> Unit = {}
) : InputListener {

    override fun onTap(event: TapEvent): Boolean {
        if (paper?.onTap(event) == true) {
            onTapOwner(ReaderTapOwner.PAPER)
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

    override fun onDrag(event: DragEvent): Boolean =
        paper?.onDrag(event) == true

    override fun onKey(event: KeyEvent): Boolean =
        directional.onKey(event)
}
