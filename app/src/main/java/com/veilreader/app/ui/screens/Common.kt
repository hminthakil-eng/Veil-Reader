package com.veilreader.app.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.ui.theme.VeilMotion
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSpacing
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun VeilReveal(
    delayMillis: Int = 0,
    distance: Dp = 8.dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        revealed = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(
            durationMillis = 180,
            easing = FastOutSlowInEasing
        ),
        label = "veil-reveal-alpha"
    )
    val distancePx = with(LocalDensity.current) { distance.toPx() }
    val translationY by animateFloatAsState(
        targetValue = if (revealed) 0f else distancePx,
        animationSpec = tween(
            durationMillis = 210,
            easing = FastOutSlowInEasing
        ),
        label = "veil-reveal-y"
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translationY
        },
        content = content
    )
}

@Composable
fun BrassRule(
    modifier: Modifier = Modifier,
    strong: Boolean = false
) {
    Box(
        modifier = modifier
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        VeilPalette.Brass.copy(alpha = if (strong) 0.82f else 0.46f),
                        VeilPalette.Brass.copy(alpha = if (strong) 0.82f else 0.46f),
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
fun ArchivePanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .animateContentSize(tween(VeilMotion.STANDARD_MS))
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        VeilPalette.DeepBrass.copy(alpha = 0.34f),
                        colors.surfaceVariant.copy(alpha = 0.68f),
                        colors.surface.copy(alpha = 0.98f)
                    )
                )
            )
            .border(
                BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.46f)),
                shape
            )
    ) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-42).dp)
                .size(180.dp)
                .clip(RoundedCornerShape(90.dp))
                .background(
                    Brush.radialGradient(
                        listOf(
                            VeilPalette.Brass.copy(alpha = 0.11f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            VeilPalette.Brass.copy(alpha = 0.86f),
                            VeilPalette.Brass.copy(alpha = 0.46f),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.TopCenter)
        )

        Box(
            Modifier
                .fillMaxHeight()
                .width(2.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            VeilPalette.Brass.copy(alpha = 0.34f),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.CenterStart)
        )

        Box(
            Modifier
                .size(7.dp)
                .rotate(45f)
                .background(VeilPalette.Brass.copy(alpha = 0.82f))
                .align(Alignment.TopStart)
                .offset(x = 10.dp, y = 10.dp)
        )

        Box(
            Modifier
                .size(7.dp)
                .rotate(45f)
                .background(VeilPalette.Brass.copy(alpha = 0.82f))
                .align(Alignment.BottomEnd)
                .offset(x = (-10).dp, y = (-10).dp)
        )

        Column(
            modifier = Modifier.padding(horizontal = VeilSpacing.lg, vertical = VeilSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs),
            content = content
        )
    }
}

@Composable
fun ScreenHeader(eyebrow: String, title: String, subtitle: String? = null) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
    ) {
        Text(
            eyebrow.uppercase(),
            color = VeilPalette.Brass,
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.75.sp)
        )
        BrassRule(Modifier.width(72.dp))
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

/** Shared archival panel for the world around the book. */
@Composable
fun MysteryCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ArchivePanel(modifier = modifier, content = content)
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
    val palettes = listOf(
        listOf(Color(0xFF26313A), Color(0xFF12181E), Color(0xFF090C10)),
        listOf(Color(0xFF372529), Color(0xFF1B1417), Color(0xFF0C0A0B)),
        listOf(Color(0xFF303126), Color(0xFF191A14), Color(0xFF0B0C09)),
        listOf(Color(0xFF242A33), Color(0xFF141820), Color(0xFF080B0F))
    )
    val palette = palettes[(title.hashCode().ushr(1) % palettes.size)]

    Box(
        Modifier
            .matchParentSize()
            .background(Brush.verticalGradient(palette))
    )

    Box(
        Modifier
            .matchParentSize()
            .padding(8.dp)
            .border(
                BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.34f)),
                RoundedCornerShape(5.dp)
            )
    )

    Box(
        Modifier
            .width(2.dp)
            .fillMaxHeight()
            .background(VeilPalette.Brass.copy(alpha = 0.38f))
            .align(Alignment.CenterStart)
    )

    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 17.dp, end = 14.dp, top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                "GRAYFOG ARCHIVE",
                color = VeilPalette.Brass.copy(alpha = 0.82f),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 7.sp,
                    letterSpacing = 1.45.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            BrassRule(Modifier.width(42.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                title,
                color = VeilPalette.Moon,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    color = VeilPalette.Mist.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
