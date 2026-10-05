package com.veilreader.app.ui.screens

import com.veilreader.app.ui.theme.VeilSanctuary
import org.readium.r2.navigator.preferences.ReadingProgression

internal data class ReaderPageStackDepth(val leftDp: Float, val rightDp: Float)

/** Static book-edge atmosphere, independent of the GPU sheet renderer. */
internal fun readerPageStackDepth(progress: Float, progression: ReadingProgression): ReaderPageStackDepth {
    val p = progress.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    val minimum = VeilSanctuary.minimumPageStackDp
    val range = VeilSanctuary.maximumPageStackDp - minimum
    val consumed = minimum + range * p
    val remaining = minimum + range * (1f - p)
    return if (progression == ReadingProgression.RTL) {
        ReaderPageStackDepth(remaining, consumed)
    } else {
        ReaderPageStackDepth(consumed, remaining)
    }
}
