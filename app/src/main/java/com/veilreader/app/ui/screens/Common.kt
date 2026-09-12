package com.veilreader.app.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.ui.theme.VeilMotion
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSpacing
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ScreenHeader(eyebrow: String, title: String, subtitle: String? = null) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
    ) {
        Text(
            eyebrow.uppercase(),
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.55.sp)
        )
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        subtitle?.takeIf(String::isNotBlank)?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.widthIn(max = 680.dp)
            )
        }
    }
}

/** Shared quiet panel for the world around the book. */
@Composable
fun MysteryCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.large
    val colors = MaterialTheme.colorScheme
    val panelBrush = Brush.verticalGradient(
        listOf(
            colors.surfaceVariant.copy(alpha = 0.74f),
            colors.surface.copy(alpha = 0.94f)
        )
    )

    Box(
        modifier = modifier
            .animateContentSize(tween(VeilMotion.STANDARD_MS))
            .clip(shape)
            .background(panelBrush)
            .border(
                BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.64f)),
                shape
            )
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.055f))
                .align(Alignment.TopCenter)
        )
        Column(
            modifier = Modifier.padding(horizontal = VeilSpacing.lg, vertical = VeilSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs),
            content = content
        )
    }
}

/**
 * Publication cover with a deterministic fallback. Cached Readium covers fade in when decoding
 * finishes so library scrolling does not visually pop.
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
    val imageAlpha by animateFloatAsState(
        targetValue = if (cachedBitmap == null) 0f else 1f,
        animationSpec = tween(280),
        label = "cover-fade"
    )

    val shape = RoundedCornerShape(11.dp)
    Box(
        modifier = modifier
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.22f),
                spotColor = Color.Black.copy(alpha = 0.30f)
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f)),
                shape
            )
    ) {
        GeneratedBookCover(title = title, subtitle = subtitle)
        cachedBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = "Cover of $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(imageAlpha)
            )
        }

        Box(
            Modifier
                .fillMaxHeight()
                .width(3.dp)
                .background(Color.Black.copy(alpha = 0.16f))
                .align(Alignment.CenterStart)
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.12f))
                .align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun BoxScope.GeneratedBookCover(title: String, subtitle: String?) {
    val hue = ((title.hashCode().ushr(1) % 260) + 235).toFloat() % 360f
    val accent = Color.hsv(hue, 0.38f, 0.58f)
    val middle = Color.hsv((hue + 18f) % 360f, 0.46f, 0.31f)
    val deep = Color.hsv((hue + 34f) % 360f, 0.48f, 0.13f)

    Box(
        Modifier
            .matchParentSize()
            .background(Brush.linearGradient(listOf(accent, middle, deep)))
    )

    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 13.dp, top = 15.dp, bottom = 15.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "VEIL",
            color = VeilPalette.Moon.copy(alpha = 0.72f),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
