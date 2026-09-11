package com.veilreader.app.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ScreenHeader(eyebrow: String, title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            eyebrow.uppercase(),
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 11.sp,
            letterSpacing = 1.8.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        subtitle?.let {
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun MysteryCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), content = content)
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

    val shape = RoundedCornerShape(18.dp)
    if (cachedBitmap != null) {
        Image(
            bitmap = requireNotNull(cachedBitmap),
            contentDescription = "Cover of $title",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        return
    }

    val hue = ((title.hashCode().ushr(1) % 300) + 18).toFloat()
    val accent = Color.hsv(hue, 0.48f, 0.60f)
    val mid = Color.hsv((hue + 16f) % 360f, 0.58f, 0.34f)
    val deep = Color.hsv((hue + 28f) % 360f, 0.62f, 0.13f)
    val gradient = Brush.linearGradient(listOf(accent, mid, deep))

    Box(
        modifier = modifier
            .clip(shape)
            .background(gradient)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .width(7.dp)
                .background(Color.Black.copy(alpha = 0.18f))
                .align(Alignment.CenterStart)
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.16f))
                .align(Alignment.TopCenter)
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 18.dp, end = 14.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "VEIL LIBRARY",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 8.sp,
                letterSpacing = 1.6.sp,
                fontWeight = FontWeight.SemiBold
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 17.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        color = Color.White.copy(alpha = 0.74f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
