package com.veilreader.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.veilreader.app.domain.Book
import com.veilreader.app.ui.books.BookPatina
import com.veilreader.app.ui.books.bookArtifactState as canonicalBookArtifactState
import com.veilreader.app.ui.theme.VeilPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class BookPresence {
    PRISTINE,
    OPENED,
    READING,
    COMPLETED
}

enum class BookArchiveAge {
    NEW,
    SETTLED,
    AGED,
    ARCHIVAL
}

data class BookArtifactState(
    val presence: BookPresence,
    val archiveAge: BookArchiveAge,
    val progress: Float,
    val leftStack: Float,
    val rightStack: Float,
    val patina: Float,
    val recentlyOpened: Boolean,
    val favorite: Boolean,
    val completed: Boolean,
    val auraIndex: Int
)

/**
 * Builds a visual history only from fields Veil already persists.
 *
 * Rereads, annotations and semantic relationships are intentionally absent until the
 * domain model owns durable counters for them.
 */
fun bookArtifactState(
    book: Book,
    nowEpochMs: Long = System.currentTimeMillis()
): BookArtifactState {
    // Archive age, completion and recent return have one contract across every shelf.
    val canonical = canonicalBookArtifactState(book, nowEpochMs)
    val progress = canonical.progress
    val archiveAge = when (canonical.patina) {
        BookPatina.FRESH -> BookArchiveAge.NEW
        BookPatina.SETTLED -> BookArchiveAge.SETTLED
        BookPatina.AGED -> BookArchiveAge.AGED
        BookPatina.ARCHIVAL -> BookArchiveAge.ARCHIVAL
    }
    val presence = when {
        canonical.finished -> BookPresence.COMPLETED
        progress > 0f -> BookPresence.READING
        book.lastOpenedAtEpochMs > 0L -> BookPresence.OPENED
        else -> BookPresence.PRISTINE
    }

    val agePatina = when (archiveAge) {
        BookArchiveAge.NEW -> 0.03f
        BookArchiveAge.SETTLED -> 0.11f
        BookArchiveAge.AGED -> 0.22f
        BookArchiveAge.ARCHIVAL -> 0.34f
    }
    val completionPatina = if (canonical.finished) 0.12f else 0f
    val favoritePatina = if (canonical.favorite) 0.04f else 0f
    val engagementPatina = progress * 0.14f + completionPatina + favoritePatina

    val auraHash = (book.title + "|" + book.author)
        .fold(17) { acc, char -> acc * 31 + char.code }

    return BookArtifactState(
        presence = presence,
        archiveAge = archiveAge,
        progress = progress,
        leftStack = progress,
        rightStack = 1f - progress,
        patina = (agePatina + engagementPatina).coerceIn(0f, 0.56f),
        recentlyOpened = canonical.recentlyOpened,
        favorite = canonical.favorite,
        completed = canonical.finished,
        auraIndex = (auraHash and Int.MAX_VALUE) % 4
    )
}

fun fallbackBookAura(state: BookArtifactState): Color =
    when (state.auraIndex) {
        0 -> VeilPalette.Brass
        1 -> VeilPalette.Spirit
        2 -> Color(0xFF8894AD)
        else -> Color(0xFF9A6F76)
    }

/**
 * Veil-owned artifact layers. The publication cover itself is never modified.
 *
 * Page-stack edges move with reading progress; patina grows from real stored history;
 * completion, favorite and recent-return marks remain secondary to the original cover.
 */
@Composable
fun BookArtifactOverlay(
    state: BookArtifactState,
    aura: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val paper = VeilPalette.ReaderPaper
        val patina = Color(0xFF6F5132)

        if (state.patina > 0.01f) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to patina.copy(alpha = state.patina * 0.34f),
                    0.16f to Color.Transparent,
                    0.78f to Color.Transparent,
                    1f to patina.copy(alpha = state.patina * 0.46f)
                ),
                size = size
            )
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Black.copy(alpha = state.patina * 0.34f),
                        Color.Transparent,
                        Color.Transparent,
                        patina.copy(alpha = state.patina * 0.26f)
                    )
                ),
                size = size
            )
        }

        val leftLayers = (1 + state.leftStack * 3f).toInt().coerceIn(1, 4)
        repeat(leftLayers) { index ->
            val x = (1.0f + index * 0.9f).dp.toPx()
            drawLine(
                color = paper.copy(alpha = 0.045f + state.leftStack * 0.055f),
                start = Offset(x, h * 0.12f),
                end = Offset(x, h * 0.90f),
                strokeWidth = 0.65.dp.toPx()
            )
        }

        val rightLayers = (1 + state.rightStack * 4f).toInt().coerceIn(1, 5)
        repeat(rightLayers) { index ->
            val x = w - (1.2f + index * 0.95f).dp.toPx()
            drawLine(
                color = paper.copy(alpha = 0.055f + state.rightStack * 0.075f),
                start = Offset(x, h * 0.10f),
                end = Offset(x, h * 0.92f),
                strokeWidth = 0.70.dp.toPx()
            )
        }

        if (state.progress > 0f) {
            drawLine(
                color = aura.copy(alpha = 0.58f),
                start = Offset(0f, h - 1.4.dp.toPx()),
                end = Offset(w * state.progress, h - 1.4.dp.toPx()),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        if (state.recentlyOpened) {
            val center = w * 0.5f
            drawLine(
                color = aura.copy(alpha = 0.72f),
                start = Offset(center - 12.dp.toPx(), 1.3.dp.toPx()),
                end = Offset(center + 12.dp.toPx(), 1.3.dp.toPx()),
                strokeWidth = 1.1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        if (state.favorite) {
            val center = Offset(w - 11.dp.toPx(), 12.dp.toPx())
            val long = 5.4.dp.toPx()
            val short = 2.6.dp.toPx()
            drawLine(
                aura.copy(alpha = 0.92f),
                Offset(center.x - long, center.y),
                Offset(center.x + long, center.y),
                1.dp.toPx(),
                StrokeCap.Round
            )
            drawLine(
                aura.copy(alpha = 0.92f),
                Offset(center.x, center.y - long),
                Offset(center.x, center.y + long),
                1.dp.toPx(),
                StrokeCap.Round
            )
            drawLine(
                aura.copy(alpha = 0.66f),
                Offset(center.x - short, center.y - short),
                Offset(center.x + short, center.y + short),
                0.8.dp.toPx(),
                StrokeCap.Round
            )
            drawLine(
                aura.copy(alpha = 0.66f),
                Offset(center.x + short, center.y - short),
                Offset(center.x - short, center.y + short),
                0.8.dp.toPx(),
                StrokeCap.Round
            )
        }

        if (state.completed) {
            val center = Offset(w - 14.dp.toPx(), h - 15.dp.toPx())
            val radius = 8.2.dp.toPx()
            drawCircle(
                color = VeilPalette.Brass.copy(alpha = 0.66f),
                radius = radius,
                center = center,
                style = Stroke(0.9.dp.toPx())
            )
            val diamond = Path().apply {
                moveTo(center.x, center.y - 4.dp.toPx())
                lineTo(center.x + 4.dp.toPx(), center.y)
                lineTo(center.x, center.y + 4.dp.toPx())
                lineTo(center.x - 4.dp.toPx(), center.y)
                close()
            }
            drawPath(
                path = diamond,
                color = VeilPalette.Brass.copy(alpha = 0.78f),
                style = Stroke(0.9.dp.toPx())
            )
            drawCircle(
                color = VeilPalette.Brass.copy(alpha = 0.86f),
                radius = 1.15.dp.toPx(),
                center = center
            )
        }

        if (state.archiveAge == BookArchiveAge.ARCHIVAL) {
            val center = Offset(12.dp.toPx(), h - 13.dp.toPx())
            repeat(3) { index ->
                val angle = (index * 120.0 - 90.0) * PI / 180.0
                val end = Offset(
                    center.x + cos(angle).toFloat() * 5.dp.toPx(),
                    center.y + sin(angle).toFloat() * 5.dp.toPx()
                )
                drawLine(
                    color = patina.copy(alpha = 0.42f),
                    start = center,
                    end = end,
                    strokeWidth = 0.75.dp.toPx()
                )
            }
        }
    }
}
