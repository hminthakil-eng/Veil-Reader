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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
        ) {
            Box(
                Modifier
                    .width(28.dp)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.78f))
            )
            Text(
                eyebrow.uppercase(),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.25.sp)
            )
        }
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

/**
 * Shared archive plate for the world around the book.
 *
 * Intentionally avoids the old universal "large rounded gradient card" treatment. The hierarchy is
 * carried by restrained material contrast, a brass registration rule and quiet depth.
 */
@Composable
fun MysteryCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.medium
    val colors = MaterialTheme.colorScheme
    val panelBrush = Brush.verticalGradient(
        listOf(
            colors.surfaceVariant.copy(alpha = 0.84f),
            colors.surface.copy(alpha = 0.98f)
        )
    )

    Box(
        modifier = modifier
            .animateContentSize(tween(VeilMotion.STANDARD_MS))
            .clip(shape)
            .background(panelBrush)
            .border(
                BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.72f)),
                shape
            )
    ) {
        Box(
            Modifier
                .width(86.dp)
                .height(1.dp)
                .background(colors.primary.copy(alpha = 0.62f))
                .align(Alignment.TopStart)
        )
        Box(
            Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(colors.primary.copy(alpha = 0.16f))
                .align(Alignment.CenterStart)
        )
        Column(
            modifier = Modifier.padding(
                start = VeilSpacing.lg,
                end = VeilSpacing.lg,
                top = VeilSpacing.lg,
                bottom = VeilSpacing.lg
            ),
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

    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.28f),
                spotColor = Color.Black.copy(alpha = 0.36f)
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
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
                .width(5.dp)
                .background(Color.Black.copy(alpha = 0.26f))
                .align(Alignment.CenterStart)
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.34f))
                .align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun BoxScope.GeneratedBookCover(title: String, subtitle: String?) {
    val archivalTones = listOf(
        Color(0xFF182636),
        Color(0xFF272333),
        Color(0xFF30261E),
        Color(0xFF1B2A2B),
        Color(0xFF242338),
        Color(0xFF2A1E23)
    )
    val index = (title.hashCode() and Int.MAX_VALUE) % archivalTones.size
    val body = archivalTones[index]
    val deep = Color(0xFF080B10)
    val gold = VeilPalette.OldGold

    Box(
        Modifier
            .matchParentSize()
            .background(Brush.linearGradient(listOf(body, deep)))
    )

    Box(
        Modifier
            .matchParentSize()
            .padding(9.dp)
            .border(BorderStroke(1.dp, gold.copy(alpha = 0.34f)), RoundedCornerShape(2.dp))
    )

    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 17.dp, end = 14.dp, top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                "VEIL ARCHIVE",
                color = gold.copy(alpha = 0.88f),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    letterSpacing = 1.45.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Box(
                Modifier
                    .width(34.dp)
                    .height(1.dp)
                    .background(gold.copy(alpha = 0.62f))
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                title,
                color = VeilPalette.Moon,
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    color = VeilPalette.Mist.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
