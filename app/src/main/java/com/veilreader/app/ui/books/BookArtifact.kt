package com.veilreader.app.ui.books

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.BookArtifactMemory
import com.veilreader.app.ui.theme.VeilPalette

private const val DAY_MS = 86_400_000L
private const val RECENT_RETURN_MS = 48L * 60L * 60L * 1000L

enum class BookPatina(val level: Int) {
    FRESH(0),
    SETTLED(1),
    AGED(2),
    ARCHIVAL(3)
}

enum class BookReadingState {
    UNOPENED,
    ACTIVE,
    FINISHED
}

data class BookArtifactState(
    val progress: Float,
    val leftPageStack: Float,
    val rightPageStack: Float,
    val readingState: BookReadingState,
    val patina: BookPatina,
    val favorite: Boolean,
    val recentlyOpened: Boolean,
    val handlingWear: Float = 0f,
    val foreEdgeWear: Float = 0f,
    val marginMemory: Float = 0f,
    val bookmarkRibbons: Int = 0,
    val marginFleckCount: Int = 0,
    val archiveSeed: Int
) {
    val finished: Boolean get() = readingState == BookReadingState.FINISHED
    val hasHistory: Boolean get() = readingState != BookReadingState.UNOPENED
}

fun bookArtifactState(
    book: Book,
    nowEpochMs: Long = System.currentTimeMillis(),
    memory: BookArtifactMemory? = null
): BookArtifactState {
    val normalizedProgress = when {
        book.finished -> 1f
        else -> book.progress.coerceIn(0f, 1f)
    }
    val readingState = when {
        book.finished -> BookReadingState.FINISHED
        normalizedProgress > 0f || book.lastOpenedAtEpochMs > 0L -> BookReadingState.ACTIVE
        else -> BookReadingState.UNOPENED
    }

    val archiveAgeDays = if (book.addedAtEpochMs <= 0L) {
        0L
    } else {
        ((nowEpochMs - book.addedAtEpochMs).coerceAtLeast(0L) / DAY_MS)
    }
    val patina = when {
        archiveAgeDays >= 180L -> BookPatina.ARCHIVAL
        archiveAgeDays >= 45L -> BookPatina.AGED
        archiveAgeDays >= 7L -> BookPatina.SETTLED
        else -> BookPatina.FRESH
    }

    val sinceLastOpen = nowEpochMs - book.lastOpenedAtEpochMs
    val recentlyOpened =
        book.lastOpenedAtEpochMs > 0L &&
            sinceLastOpen in 0L..RECENT_RETURN_MS

    val (leftStack, rightStack) = bookPageStackBalance(normalizedProgress)

    return BookArtifactState(
        progress = normalizedProgress,
        leftPageStack = leftStack,
        rightPageStack = rightStack,
        readingState = readingState,
        patina = patina,
        favorite = book.favorite,
        recentlyOpened = recentlyOpened,
        handlingWear = memory?.handlingWear?.coerceIn(0f, 1f) ?: 0f,
        foreEdgeWear = memory?.foreEdgeWear?.coerceIn(0f, 1f) ?: 0f,
        marginMemory = memory?.marginMemory?.coerceIn(0f, 1f) ?: 0f,
        bookmarkRibbons = memory?.ribbonCount?.coerceIn(0, 3) ?: 0,
        marginFleckCount = memory?.marginFleckCount?.coerceIn(0, 12) ?: 0,
        archiveSeed = book.id.hashCode() xor book.title.hashCode()
    )
}

fun bookArtifactRecordLabel(state: BookArtifactState): String {
    val presence = when (state.readingState) {
        BookReadingState.UNOPENED -> "CATALOGUED"
        BookReadingState.ACTIVE -> "IN PROGRESS"
        BookReadingState.FINISHED -> "COMPLETED"
    }
    val age = when (state.patina) {
        BookPatina.FRESH -> "NEW VOLUME"
        BookPatina.SETTLED -> "SETTLED"
        BookPatina.AGED -> "AGED"
        BookPatina.ARCHIVAL -> "DEEP ARCHIVE"
    }
    return "$presence · $age"
}

fun bookPageStackBalance(progress: Float): Pair<Float, Float> {
    val p = progress.coerceIn(0f, 1f)
    val minimum = 0.14f
    val range = 1f - minimum
    return (minimum + range * p) to (minimum + range * (1f - p))
}

/**
 * The Veil representation never paints over the publication cover with fake metadata.
 * It only adds archive-owned physical cues around the image: page block, patina, seals,
 * favorite mark, and recent-return light.
 */
@Composable
fun BookArtifactLayer(
    state: BookArtifactState,
    aura: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val brass = VeilPalette.Brass
        val paper = VeilPalette.ReaderPaper
        val ink = VeilPalette.Ink
        val patinaStrength = state.patina.level / 3f

        // Cover aura is deliberately weak: it should individualize a volume without recoloring art.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    aura.copy(alpha = if (state.recentlyOpened) 0.18f else 0.095f),
                    aura.copy(alpha = 0.022f),
                    Color.Transparent
                ),
                center = Offset(w * 0.78f, h * 0.18f),
                radius = size.minDimension * 0.82f
            ),
            size = size
        )

        // Archive age affects only Veil's patina layer, never the original cover bitmap.
        if (patinaStrength > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF8B6A3C).copy(alpha = 0.035f * patinaStrength),
                        Color.Transparent,
                        ink.copy(alpha = 0.10f * patinaStrength)
                    )
                ),
                size = size
            )
        }

        // Durable handling history lives on the spine and fore-edge only; the cover art remains legible.
        if (state.handlingWear > 0f) {
            val spineWidth = (2.5f + state.handlingWear * 4.5f).dp.toPx()
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        ink.copy(alpha = 0.10f + state.handlingWear * 0.18f),
                        Color(0xFFB99B66).copy(alpha = state.handlingWear * 0.055f),
                        Color.Transparent
                    ),
                    startX = 0f,
                    endX = spineWidth * 2.4f
                ),
                topLeft = Offset(0f, h * 0.035f),
                size = Size(spineWidth * 2.4f, h * 0.93f)
            )

            repeat(5) { index ->
                val seed = (state.archiveSeed ushr (index * 3)) and 0x1F
                val y = h * (0.15f + index * 0.16f) + seed * 0.08f
                drawLine(
                    color = paper.copy(alpha = 0.025f + state.handlingWear * 0.045f),
                    start = Offset(1.dp.toPx(), y),
                    end = Offset(spineWidth * (1.15f + (seed % 4) * 0.16f), y + 0.8.dp.toPx()),
                    strokeWidth = 0.55.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        if (state.foreEdgeWear > 0f) {
            drawLine(
                color = paper.copy(alpha = 0.06f + state.foreEdgeWear * 0.12f),
                start = Offset(w - 1.2.dp.toPx(), h * 0.10f),
                end = Offset(w - 1.2.dp.toPx(), h * 0.90f),
                strokeWidth = (0.7f + state.foreEdgeWear * 1.2f).dp.toPx(),
                cap = StrokeCap.Round
            )

            repeat(7) { index ->
                val seed = (state.archiveSeed xor (index * 0x45D9F3B)) ushr 4
                val y = h * (0.16f + index * 0.105f) + (seed and 0x07) * 0.5f
                val length = (1.5f + ((seed ushr 3) and 0x07) * 0.32f).dp.toPx()
                drawLine(
                    color = paper.copy(alpha = 0.035f + state.foreEdgeWear * 0.07f),
                    start = Offset(w - 0.6.dp.toPx(), y),
                    end = Offset(w - length, y + 0.4.dp.toPx()),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
        }

        // Preserved passages leave tiny registration flecks at the outer edge, never fake text.
        repeat(state.marginFleckCount) { index ->
            val lane = ((state.archiveSeed ushr (index % 12)) + index * 17) and 0x3F
            val y = h * (0.12f + (lane / 63f) * 0.76f)
            val alpha = 0.07f + state.marginMemory * 0.18f
            drawLine(
                color = if (index % 4 == 0) {
                    brass.copy(alpha = alpha)
                } else {
                    paper.copy(alpha = alpha * 0.72f)
                },
                start = Offset(w - 4.2.dp.toPx(), y),
                end = Offset(w - 0.8.dp.toPx(), y),
                strokeWidth = 0.55.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Saved places become restrained physical ribbons. At most three are ever shown.
        repeat(state.bookmarkRibbons) { index ->
            val x = w * (0.56f + index * 0.095f)
            val ribbonHeight = (15f + index * 4f).dp.toPx()
            val ribbonWidth = 3.2.dp.toPx()
            val ribbonColor = when (index) {
                0 -> VeilPalette.MoonCrimson
                1 -> brass
                else -> VeilPalette.Spirit
            }
            drawRect(
                color = ribbonColor.copy(alpha = 0.62f),
                topLeft = Offset(x, 0f),
                size = Size(ribbonWidth, ribbonHeight)
            )
            val notch = Path().apply {
                moveTo(x, ribbonHeight)
                lineTo(x + ribbonWidth / 2f, ribbonHeight - 2.6.dp.toPx())
                lineTo(x + ribbonWidth, ribbonHeight)
                close()
            }
            drawPath(notch, ink.copy(alpha = 0.76f))
        }

        // Physical book block. Progress transfers apparent page mass from right to left.
        val leftWidth = (1.2f + 2.8f * state.leftPageStack).dp.toPx()
        val rightWidth = (1.2f + 2.8f * state.rightPageStack).dp.toPx()

        drawRect(
            color = paper.copy(alpha = 0.11f + state.leftPageStack * 0.11f),
            topLeft = Offset(0f, h * 0.08f),
            size = Size(leftWidth, h * 0.84f)
        )
        drawRect(
            color = paper.copy(alpha = 0.13f + state.rightPageStack * 0.13f),
            topLeft = Offset(w - rightWidth, h * 0.07f),
            size = Size(rightWidth, h * 0.86f)
        )

        repeat(3) { index ->
            val x = w - rightWidth + rightWidth * ((index + 1f) / 4f)
            drawLine(
                color = Color.White.copy(alpha = 0.045f + state.rightPageStack * 0.025f),
                start = Offset(x, h * 0.08f),
                end = Offset(x, h * 0.92f),
                strokeWidth = 0.55.dp.toPx()
            )
        }

        // A hairline location memory reads as a physical registration mark, not a progress bar.
        if (state.hasHistory) {
            drawLine(
                color = brass.copy(alpha = if (state.finished) 0.90f else 0.68f),
                start = Offset(0f, h - 1.dp.toPx()),
                end = Offset(w * state.progress.coerceIn(0.04f, 1f), h - 1.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        if (state.recentlyOpened) {
            drawLine(
                color = aura.copy(alpha = 0.28f),
                start = Offset(2.dp.toPx(), 1.5.dp.toPx()),
                end = Offset(w - 2.dp.toPx(), 1.5.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Favorite mark: tiny eight-point archive star in the upper corner.
        if (state.favorite) {
            val c = Offset(w - 12.dp.toPx(), 12.dp.toPx())
            val outer = 5.dp.toPx()
            val inner = 2.dp.toPx()
            val star = Path()
            repeat(16) { index ->
                val angle = Math.toRadians((-90.0 + index * 22.5))
                val radius = if (index % 2 == 0) outer else inner
                val point = Offset(
                    c.x + kotlin.math.cos(angle).toFloat() * radius,
                    c.y + kotlin.math.sin(angle).toFloat() * radius
                )
                if (index == 0) star.moveTo(point.x, point.y) else star.lineTo(point.x, point.y)
            }
            star.close()
            drawPath(
                path = star,
                color = brass.copy(alpha = 0.90f)
            )
        }

        // Completion seal lives on Veil's representation, leaving the cover itself untouched.
        if (state.finished) {
            val c = Offset(w - 15.dp.toPx(), h - 16.dp.toPx())
            val radius = 9.dp.toPx()
            drawCircle(
                color = ink.copy(alpha = 0.62f),
                radius = radius + 2.dp.toPx(),
                center = c
            )
            drawCircle(
                color = brass.copy(alpha = 0.92f),
                radius = radius,
                center = c,
                style = Stroke(1.1.dp.toPx())
            )
            drawCircle(
                color = brass.copy(alpha = 0.46f),
                radius = radius * 0.62f,
                center = c,
                style = Stroke(0.8.dp.toPx())
            )
            val diamond = Path().apply {
                moveTo(c.x, c.y - 3.5.dp.toPx())
                lineTo(c.x + 3.5.dp.toPx(), c.y)
                lineTo(c.x, c.y + 3.5.dp.toPx())
                lineTo(c.x - 3.5.dp.toPx(), c.y)
                close()
            }
            drawPath(diamond, brass.copy(alpha = 0.92f))
        }
    }
}
