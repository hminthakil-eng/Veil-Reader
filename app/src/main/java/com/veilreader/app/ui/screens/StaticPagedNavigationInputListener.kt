package com.veilreader.app.ui.screens

import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.veilreader.app.ui.reader.awaitReaderVisualNavigationDeparture
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.input.DragEvent
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Owns drag gestures for Veil's static paginated mode.
 *
 * The renderer must not start its native sliding animation when the user explicitly chose
 * PAGED. We reserve the drag, then perform one unanimated navigation only after a deliberate
 * horizontal release. Short drags simply leave the current page in place.
 */
@OptIn(ExperimentalReadiumApi::class)
internal class StaticPagedNavigationInputListener(
    private val navigator: OverflowableNavigator,
    private val isEnabled: () -> Boolean,
    private val scope: CoroutineScope,
    private val onInteraction: () -> Unit,
    private val onNavigationCommitted: () -> Unit,
    private val onBoundaryHit: (PaperCurlSide) -> Unit = {}
) : InputListener {
    private var reserved = false
    private var navigationJob: Job? = null

    override fun onDrag(event: DragEvent): Boolean {
        if (!isEnabled()) {
            reserved = false
            return false
        }
        if (navigationJob != null) return true

        return when (event.type) {
            DragEvent.Type.Start -> {
                reserved = true
                onInteraction()
                true
            }

            DragEvent.Type.Move -> reserved

            DragEvent.Type.End -> {
                if (!reserved) return false
                reserved = false

                val width = navigator.publicationView.width.toFloat()
                val density = navigator.publicationView.resources.displayMetrics.density
                val direction = staticPagedDragDirection(
                    offsetX = event.offset.x,
                    offsetY = event.offset.y,
                    width = width,
                    density = density,
                    progression = navigator.overflow.value.readingProgression
                ) ?: return true

                val origin = navigator.currentLocator.value
                val accepted = when (direction) {
                    PaperTurnDirection.FORWARD -> navigator.goForward(animated = false)
                    PaperTurnDirection.BACKWARD -> navigator.goBackward(animated = false)
                }
                if (!accepted) {
                    onBoundaryHit(
                        paperTurnSideFor(
                            direction = direction,
                            progression = navigator.overflow.value.readingProgression
                        )
                    )
                    return true
                }

                navigationJob = scope.launch {
                    val moved =
                        awaitReaderVisualNavigationDeparture(
                            currentLocator = navigator.currentLocator,
                            origin = origin
                        )
                    if (moved) {
                        onNavigationCommitted()
                    } else {
                        navigator.go(origin, animated = false)
                        onBoundaryHit(
                            paperTurnSideFor(
                                direction = direction,
                                progression = navigator.overflow.value.readingProgression
                            )
                        )
                    }
                    navigationJob = null
                }
                true
            }
        }
    }
}

internal fun shouldUseStaticPagedDragNavigation(
    format: com.veilreader.app.domain.BookFormat,
    scroll: Boolean,
    pageTurnStyle: com.veilreader.app.domain.PageTurnStyle
): Boolean =
    format == com.veilreader.app.domain.BookFormat.EPUB &&
        !scroll &&
        pageTurnStyle == com.veilreader.app.domain.PageTurnStyle.NONE

internal fun staticPagedDragDirection(
    offsetX: Float,
    offsetY: Float,
    width: Float,
    density: Float,
    progression: ReadingProgression
): PaperTurnDirection? {
    if (width <= 0f) return null
    val horizontal = abs(offsetX)
    val vertical = abs(offsetY)
    if (horizontal < vertical * STATIC_PAGED_HORIZONTAL_BIAS) return null

    val threshold = max(
        STATIC_PAGED_MIN_DISTANCE_DP * density.coerceAtLeast(0.1f),
        width * STATIC_PAGED_WIDTH_FRACTION
    )
    if (horizontal < threshold) return null

    val side = if (offsetX < 0f) {
        PaperCurlSide.RIGHT
    } else {
        PaperCurlSide.LEFT
    }
    return paperTurnDirectionFor(side, progression)
}

private const val STATIC_PAGED_MIN_DISTANCE_DP = 56f
private const val STATIC_PAGED_WIDTH_FRACTION = 0.12f
private const val STATIC_PAGED_HORIZONTAL_BIAS = 1.15f
