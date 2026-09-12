package com.veilreader.app.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSpacing
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ScreenHeader(eyebrow: String, title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)) {
        Text(
            eyebrow.uppercase(),
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.9.sp)
        )
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        subtitle?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * Shared atmospheric panel for the world surrounding the book.
 *
 * It stays intentionally quiet: low contrast, one hairline edge and no fake elevation. The visual
 * system should feel like layered paper/stone in low light, not a stack of generic Material cards.
 */
@Composable
fun MysteryCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.large
    val colors = MaterialTheme.colorScheme
    val panelBrush = Brush.verticalGradient(
        listOf(
            colors.surfaceVariant.copy(alpha = 0.82f),
            colors.surface.copy(alpha = 0.94f)
        )
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(panelBrush)
            .border(
                BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.72f)),
                shape
            )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = VeilSpacing.lg, vertical = VeilSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs),
            content = content
        )
    }
}

/**
 * Renders the publication's cached Readium cover when available, otherwise a deterministic
 * generated fallback. Decoding stays off the main thread and a missing/corrupt cache never breaks
 * the shelf.
 */
@Composable
fun BookCover(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    imagePath: String? = null
) {
    val cachedBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = imagePath) {
        value = withContext(Dispatchers.IO) {
            imagePath
                ?.takeIf { it.isNotBlank() }
                ?.let(::File)
                ?.takeIf { it.isFile && it.length() > 0L }
                ?.let { file -> BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }
        }
    }

    val shape = MaterialTheme.shapes.medium
    if (cachedBitmap != null) {
        Image(
            bitmap = requireNotNull(cachedBitmap),
            contentDescription = "Cover of $title",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                    shape
                )
        )
        return
    }

    val hue = ((title.hashCode().ushr(1) % 260) + 235).toFloat() % 360f
    val accent = Color.hsv(hue, 0.42f, 0.56f)
    val middle = Color.hsv((hue + 18f) % 360f, 0.48f, 0.30f)
    val deep = Color.hsv((hue + 34f) % 360f, 0.50f, 0.12f)
    val gradient = Brush.linearGradient(listOf(accent, middle, deep))

    Box(
        modifier = modifier
            .clip(shape)
            .background(gradient)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), shape)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .width(6.dp)
                .background(Color.Black.copy(alpha = 0.22f))
                .align(Alignment.CenterStart)
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.12f))
                .align(Alignment.TopCenter)
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 18.dp, end = 14.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "VEIL ARCHIVE",
                color = VeilPalette.Moon.copy(alpha = 0.70f),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 8.sp,
                    letterSpacing = 1.6.sp
                )
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    title,
                    color = Color.White,
                    fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
