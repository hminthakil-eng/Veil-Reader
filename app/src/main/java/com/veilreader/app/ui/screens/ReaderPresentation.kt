package com.veilreader.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.veilreader.app.domain.ReaderNavigationMode
import com.veilreader.app.domain.ReaderTheme
import com.veilreader.app.ui.theme.sanctuaryPageMaterialFor
import com.veilreader.app.ui.theme.sanctuarySurfaceProfileFor
import org.readium.r2.shared.publication.ReadingProgression

internal fun readerCanvasColor(theme: ReaderTheme): Color = when (theme) {
    ReaderTheme.PAPER -> Color(0xFFE9DEC5)
    ReaderTheme.SEPIA -> Color(0xFFE2D0AA)
    ReaderTheme.DUSK -> Color(0xFF18151D)
    ReaderTheme.OLED -> Color.Black
}

@Composable
internal fun ReaderBoundaryPulse(
    side: PaperCurlSide,
    theme: ReaderTheme,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    if (alpha <= 0.001f) return
    val pulseColor = when (theme) {
        ReaderTheme.PAPER -> Color(0xFF74542D)
        ReaderTheme.SEPIA -> Color(0xFF684721)
        ReaderTheme.DUSK -> Color(0xFFD9C7A5)
        ReaderTheme.OLED -> Color(0xFFD8D8D8)
    }

    Canvas(modifier) {
        val pulseWidth = 54.dp.toPx().coerceAtMost(size.width * 0.12f)
        if (pulseWidth <= 0f) return@Canvas
        val left = side == PaperCurlSide.LEFT
        val startX = if (left) 0f else size.width - pulseWidth
        val endX = if (left) pulseWidth else size.width
        drawRect(
            brush = Brush.horizontalGradient(
                colorStops = if (left) {
                    arrayOf(
                        0f to pulseColor.copy(alpha = 0.16f * alpha),
                        1f to Color.Transparent
                    )
                } else {
                    arrayOf(
                        0f to Color.Transparent,
                        1f to pulseColor.copy(alpha = 0.16f * alpha)
                    )
                },
                startX = startX,
                endX = endX
            ),
            topLeft = Offset(startX, 0f),
            size = Size(pulseWidth, size.height)
        )
    }
}

internal fun isRenderableReaderViewport(
    width: Float,
    height: Float
): Boolean =
    width.isFinite() &&
        height.isFinite() &&
        width > 0f &&
        height > 0f

@Composable
internal fun ReaderPageAtmosphere(
    theme: ReaderTheme,
    navigationMode: ReaderNavigationMode,
    paperPatina: Float,
    progress: Float,
    progression: ReadingProgression,
    modifier: Modifier = Modifier
) {
    val dark = theme == ReaderTheme.DUSK || theme == ReaderTheme.OLED
    val material = sanctuaryPageMaterialFor(navigationMode)
    val surface = sanctuarySurfaceProfileFor(theme, paperPatina)
    val stack = readerPageStackDepth(progress, progression)
    val patina = surface.patina

    Canvas(modifier) {
        if (!isRenderableReaderViewport(size.width, size.height)) {
            return@Canvas
        }
        val agedTone = when (theme) {
            ReaderTheme.PAPER -> Color(0xFF73562F)
            ReaderTheme.SEPIA -> Color(0xFF65431F)
            ReaderTheme.DUSK -> Color(0xFF09070B)
            ReaderTheme.OLED -> Color.Black
        }
        val edge = if (dark) {
            Color.Black.copy(alpha = 0.20f)
        } else {
            agedTone.copy(alpha = surface.stackEdgeAlpha)
        }
        val highlight = if (dark) {
            Color.White.copy(alpha = 0.020f)
        } else {
            Color.White.copy(alpha = 0.11f + 0.035f * (1f - patina))
        }
        val leftStackWidth = stack.leftDp.dp.toPx()
        val rightStackWidth = stack.rightDp.dp.toPx()

        if (material.showPhysicalPageStack) {
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(edge, Color.Transparent),
                    startX = 0f,
                    endX = leftStackWidth
                ),
                size = Size(leftStackWidth, size.height)
            )
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, edge),
                    startX = size.width - rightStackWidth,
                    endX = size.width
                ),
                topLeft = Offset(size.width - rightStackWidth, 0f),
                size = Size(rightStackWidth, size.height)
            )

            val sheetLine = if (dark) {
                Color.White.copy(alpha = 0.018f)
            } else {
                agedTone.copy(alpha = surface.sheetLineAlpha)
            }
            repeat(3) { index ->
                val fraction = (index + 1) / 4f
                drawLine(
                    color = sheetLine,
                    start = Offset(leftStackWidth * fraction, 0f),
                    end = Offset(leftStackWidth * fraction, size.height),
                    strokeWidth = 0.45.dp.toPx()
                )
                drawLine(
                    color = sheetLine,
                    start = Offset(size.width - rightStackWidth * fraction, 0f),
                    end = Offset(size.width - rightStackWidth * fraction, size.height),
                    strokeWidth = 0.45.dp.toPx()
                )
            }
        }

        val falloff = if (dark) {
            Color.Black.copy(alpha = 0.075f)
        } else {
            agedTone.copy(alpha = surface.pageShadeAlpha)
        }
        val band = 28.dp.toPx()
        if (material.showEdgeFalloff) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(falloff, Color.Transparent),
                    startY = 0f,
                    endY = band
                ),
                size = Size(size.width, band)
            )
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, falloff),
                    startY = size.height - band,
                    endY = size.height
                ),
                topLeft = Offset(0f, size.height - band),
                size = Size(size.width, band)
            )
        }

        if (!dark && patina > 0.04f) {
            val mottleAlpha = surface.mottleAlpha
            val radius = size.minDimension * 0.46f
            listOf(
                Offset(size.width * 0.10f, size.height * 0.16f),
                Offset(size.width * 0.86f, size.height * 0.34f),
                Offset(size.width * 0.24f, size.height * 0.80f)
            ).forEachIndexed { index, center ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            agedTone.copy(alpha = mottleAlpha * if (index == 1) 0.72f else 1f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    ),
                    center = center,
                    radius = radius
                )
            }

            val oxidation = agedTone.copy(alpha = surface.edgeOxidationAlpha)
            val sideBand = 34.dp.toPx()
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(oxidation, Color.Transparent),
                    startX = 0f,
                    endX = sideBand
                ),
                size = Size(sideBand, size.height)
            )
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, oxidation),
                    startX = size.width - sideBand,
                    endX = size.width
                ),
                topLeft = Offset(size.width - sideBand, 0f),
                size = Size(sideBand, size.height)
            )
        }

        if (!dark && material.showMicroFibres) {
            val fibreAlpha = surface.fibreAlpha
            val fibre = agedTone.copy(alpha = fibreAlpha)
            val fibreCount = surface.fibreCount
            repeat(fibreCount) { index ->
                val y = ((index * 71f + 29f) % size.height)
                val x = ((index * 43f + 17f) % (size.width * 0.72f))
                val length = 20.dp.toPx() + (index % 5) * 10.dp.toPx()
                val tilt = ((index % 5) - 2) * 0.42.dp.toPx()
                drawLine(
                    color = fibre.copy(alpha = fibreAlpha * (0.62f + (index % 4) * 0.10f)),
                    start = Offset(x, y),
                    end = Offset(
                        (x + length).coerceAtMost(size.width),
                        (y + tilt).coerceIn(0f, size.height)
                    ),
                    strokeWidth = if (index % 7 == 0) 0.72.dp.toPx() else 0.48.dp.toPx()
                )
            }

            val speckAlpha = surface.speckAlpha
            val speckCount = surface.speckCount
            repeat(speckCount) { index ->
                val x = ((index * 97f + 31f) % size.width)
                val y = ((index * 137f + 47f) % size.height)
                drawCircle(
                    color = agedTone.copy(alpha = speckAlpha * (0.58f + (index % 3) * 0.14f)),
                    radius = when {
                        index % 11 == 0 -> 0.95.dp.toPx()
                        index % 5 == 0 -> 0.68.dp.toPx()
                        else -> 0.42.dp.toPx()
                    },
                    center = Offset(x, y)
                )
            }
        }

        drawLine(
            color = highlight,
            start = Offset(0f, 1.dp.toPx()),
            end = Offset(size.width, 1.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
    }
}
