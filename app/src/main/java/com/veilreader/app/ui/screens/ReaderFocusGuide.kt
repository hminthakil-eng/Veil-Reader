package com.veilreader.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.veilreader.app.domain.FocusGuideStyle
import com.veilreader.app.domain.ReaderAppearance

/**
 * Veil-owned focus aid.
 *
 * Renderer-agnostic by design: this overlay never mutates EPUB CSS, PDF pages,
 * selection state, locators, or touch ownership. It can be disabled instantly
 * without altering publication state.
 */
@Composable
internal fun ReaderFocusGuideOverlay(
    appearance: ReaderAppearance,
    modifier: Modifier = Modifier
) {
    if (appearance.focusGuideStyle == FocusGuideStyle.OFF) return

    val accent = MaterialTheme.colorScheme.secondary
    val surface = MaterialTheme.colorScheme.background
    val strength = appearance.focusGuideStrength.coerceIn(0.15, 0.80).toFloat()
    val heightFraction = appearance.focusGuideHeight.coerceIn(0.08, 0.30).toFloat()

    Canvas(modifier.clearAndSetSemantics { }) {
        if (size.height <= 0f || size.width <= 0f) return@Canvas

        val minGuideHeight = 52.dp.toPx()
        val guideHeight = (size.height * heightFraction)
            .coerceAtLeast(minGuideHeight)
            .coerceAtMost(size.height * 0.42f)
        val guideTop = (size.height - guideHeight) / 2f
        val guideBottom = guideTop + guideHeight
        val veilColor = Color.Black.copy(alpha = (strength * 0.72f).coerceIn(0.08f, 0.58f))
        val edgeColor = accent.copy(alpha = (0.24f + strength * 0.46f).coerceAtMost(0.62f))

        when (appearance.focusGuideStyle) {
            FocusGuideStyle.OFF -> Unit

            FocusGuideStyle.WINDOW -> {
                drawRect(
                    color = veilColor,
                    topLeft = Offset.Zero,
                    size = Size(size.width, guideTop)
                )
                drawRect(
                    color = veilColor,
                    topLeft = Offset(0f, guideBottom),
                    size = Size(size.width, size.height - guideBottom)
                )
                drawLine(
                    color = edgeColor,
                    start = Offset(0f, guideTop),
                    end = Offset(size.width, guideTop),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = edgeColor,
                    start = Offset(0f, guideBottom),
                    end = Offset(size.width, guideBottom),
                    strokeWidth = 1.dp.toPx()
                )
            }

            FocusGuideStyle.RULER -> {
                val outerAlpha = (strength * 0.42f).coerceIn(0.06f, 0.34f)
                drawRect(
                    color = Color.Black.copy(alpha = outerAlpha),
                    topLeft = Offset.Zero,
                    size = Size(size.width, guideTop)
                )
                drawRect(
                    color = Color.Black.copy(alpha = outerAlpha),
                    topLeft = Offset(0f, guideBottom),
                    size = Size(size.width, size.height - guideBottom)
                )
                val inset = 12.dp.toPx()
                val rulerWidth = (size.width - inset * 2f).coerceAtLeast(0f)
                drawRoundRect(
                    color = surface.copy(alpha = 0.08f + strength * 0.08f),
                    topLeft = Offset(inset, guideTop),
                    size = Size(rulerWidth, guideHeight),
                    cornerRadius = CornerRadius(10.dp.toPx())
                )
                drawRoundRect(
                    color = edgeColor,
                    topLeft = Offset(inset, guideTop),
                    size = Size(rulerWidth, guideHeight),
                    cornerRadius = CornerRadius(10.dp.toPx()),
                    style = Stroke(width = 1.25.dp.toPx())
                )
            }

            FocusGuideStyle.LINE -> {
                val centerY = size.height / 2f
                val shadow = Color.Black.copy(alpha = (strength * 0.22f).coerceIn(0.04f, 0.18f))
                drawRect(color = shadow, topLeft = Offset.Zero, size = size)
                drawRect(
                    color = surface.copy(alpha = 0.72f),
                    topLeft = Offset(0f, centerY - guideHeight / 2f),
                    size = Size(size.width, guideHeight)
                )
                drawLine(
                    color = edgeColor,
                    start = Offset(16.dp.toPx(), centerY),
                    end = Offset(size.width - 16.dp.toPx(), centerY),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}
