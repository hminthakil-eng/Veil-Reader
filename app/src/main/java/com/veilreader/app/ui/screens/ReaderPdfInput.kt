package com.veilreader.app.ui.screens

import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.net.toUri
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.DragEvent
import org.readium.r2.navigator.input.KeyEvent
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator

/**
 * Pdfium calls Readium's tap listener before its native link hit test. Defer only the Veil tap
 * action until that synchronous hit test completes; links cancel it. Native pan/zoom/keys stay
 * untouched. The registered Reader owner removes its pending callback when detached.
 */
@OptIn(ExperimentalReadiumApi::class)
internal class ReaderPdfTapArbiter(
    private val delegate: InputListener,
    private val isEnabled: () -> Boolean = { true },
    private val handler: Handler = Handler(Looper.getMainLooper())
) : InputListener by delegate {
    private var pending: Runnable? = null
    private var disposed = false

    override fun onTap(event: TapEvent): Boolean {
        if (disposed || !isEnabled()) return false
        cancelPendingTap()
        val action = Runnable {
            pending = null
            if (!disposed && isEnabled()) delegate.onTap(event)
        }
        pending = action
        handler.post(action)
        // Keep Pdfium's optional scroll handle from reacting to this app chrome tap. Its link
        // hit test still runs even when this callback returns true in AndroidPdfViewer 3.2.8.
        return true
    }

    override fun onDrag(event: DragEvent): Boolean =
        !disposed && isEnabled() && delegate.onDrag(event)

    override fun onKey(event: KeyEvent): Boolean =
        !disposed && isEnabled() && delegate.onKey(event)

    fun cancelPendingTap() {
        pending?.let(handler::removeCallbacks)
        pending = null
    }

    fun dispose() {
        disposed = true
        cancelPendingTap()
    }
}

/** Native destination indices are document indices, never the RTL-reversed PDFView indices. */
internal fun pdfInternalLinkLocator(
    current: Locator,
    documentPageIndex: Int,
    pageCount: Int
): Locator? {
    if (documentPageIndex < 0 || documentPageIndex >= pageCount) return null
    val page = documentPageIndex + 1
    // Replace source fragments, text and progression rather than carrying stale source-page data.
    // Readium consumes the standard one-based page fragment and owns view-index conversion.
    return current.copy(
        title = null,
        locations = Locator.Locations(fragments = listOf("page=$page"), position = page),
        text = Locator.Text()
    )
}

internal fun pdfPageNumber(locator: Locator): Int? =
    locator.locations.fragments.asSequence()
        .flatMap { it.removePrefix("#").split('&').asSequence() }
        .mapNotNull { parameter ->
            val pair = parameter.split('=', limit = 2)
            if (pair.size == 2 && pair[0].trim().equals("page", ignoreCase = true)) {
                pair[1].trim().toIntOrNull()?.takeIf { it > 0 }
            } else null
        }
        .firstOrNull() ?: locator.locations.position?.takeIf { it > 0 }

/** Match the existing EPUB external-link policy; never launch file/content/script intents. */
internal fun safePdfExternalLink(raw: String): Uri? =
    runCatching { raw.toUri() }.getOrNull()?.takeIf { uri ->
        (uri.scheme.equals("https", ignoreCase = true) ||
            uri.scheme.equals("http", ignoreCase = true)) &&
            !uri.host.isNullOrBlank()
    }
