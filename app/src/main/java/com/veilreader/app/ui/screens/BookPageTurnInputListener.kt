package com.veilreader.app.ui.screens

import android.animation.TimeInterpolator
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.ExperimentalReadiumApi
import kotlin.coroutines.resume
import kotlin.math.max

@OptIn(ExperimentalReadiumApi::class)
internal class BookPageTurnInputListener(
    private val navigator: OverflowableNavigator,
    private val scope: CoroutineScope,
    private val onInteraction: () -> Unit
) : InputListener {
    private var turning = false

    override fun onTap(event: TapEvent): Boolean {
        val overflow = navigator.overflow.value
        if (overflow.scroll) return false

        val view = navigator.publicationView
        val turn = resolveTurn(event, view, overflow.axis, overflow.readingProgression)
            ?: return false

        if (turning) return true

        onInteraction()
        turning = true
        scope.launch {
            try {
                performBookTurn(view, turn.forward, overflow.readingProgression)
            } finally {
                turning = false
            }
        }
        return true
    }

    private fun resolveTurn(
        event: TapEvent,
        view: View,
        axis: Axis,
        readingProgression: ReadingProgression
    ): PageTurn? {
        val density = view.resources.displayMetrics.density
        return when (axis) {
            Axis.HORIZONTAL -> {
                val width = view.width.toFloat().takeIf { it > 0f } ?: return null
                val edge = max(72f * density, width * 0.26f)
                when {
                    event.point.x <= edge -> PageTurn(
                        forward = readingProgression == ReadingProgression.RTL
                    )
                    event.point.x >= width - edge -> PageTurn(
                        forward = readingProgression != ReadingProgression.RTL
                    )
                    else -> null
                }
            }

            Axis.VERTICAL -> {
                val height = view.height.toFloat().takeIf { it > 0f } ?: return null
                val edge = max(72f * density, height * 0.22f)
                when {
                    event.point.y <= edge -> PageTurn(forward = false)
                    event.point.y >= height - edge -> PageTurn(forward = true)
                    else -> null
                }
            }
        }
    }

    private suspend fun performBookTurn(
        view: View,
        forward: Boolean,
        readingProgression: ReadingProgression
    ) {
        val density = view.resources.displayMetrics.density
        val visualDirection = when {
            readingProgression == ReadingProgression.RTL && forward -> 1f
            readingProgression == ReadingProgression.RTL && !forward -> -1f
            forward -> -1f
            else -> 1f
        }

        view.animate().cancel()
        view.cameraDistance = 12_000f * density
        view.pivotY = view.height / 2f
        view.pivotX = if (visualDirection < 0f) 0f else view.width.toFloat()

        try {
            view.animatePageState(
                rotationY = 78f * visualDirection,
                alpha = 0.72f,
                scaleX = 0.965f,
                durationMs = 125L,
                interpolator = AccelerateInterpolator()
            )

            val moved = if (forward) {
                navigator.goForward(animated = false)
            } else {
                navigator.goBackward(animated = false)
            }

            if (!moved) {
                view.animatePageState(
                    rotationY = 0f,
                    alpha = 1f,
                    scaleX = 1f,
                    durationMs = 120L,
                    interpolator = DecelerateInterpolator()
                )
                return
            }

            delay(24L)
            view.pivotX = if (visualDirection < 0f) {
                view.width.toFloat()
            } else {
                0f
            }
            view.rotationY = -78f * visualDirection
            view.alpha = 0.72f
            view.scaleX = 0.965f

            view.animatePageState(
                rotationY = 0f,
                alpha = 1f,
                scaleX = 1f,
                durationMs = 165L,
                interpolator = DecelerateInterpolator(1.35f)
            )
        } finally {
            view.animate().cancel()
            view.rotationY = 0f
            view.alpha = 1f
            view.scaleX = 1f
            view.scaleY = 1f
            view.pivotX = view.width / 2f
            view.pivotY = view.height / 2f
        }
    }
}

private data class PageTurn(val forward: Boolean)

private suspend fun View.animatePageState(
    rotationY: Float,
    alpha: Float,
    scaleX: Float,
    durationMs: Long,
    interpolator: TimeInterpolator
) {
    suspendCancellableCoroutine { continuation ->
        val animator = animate()
            .rotationY(rotationY)
            .alpha(alpha)
            .scaleX(scaleX)
            .setDuration(durationMs)
            .setInterpolator(interpolator)
            .withEndAction {
                if (continuation.isActive) continuation.resume(Unit)
            }

        continuation.invokeOnCancellation {
            animate().cancel()
        }
        animator.start()
    }
}
