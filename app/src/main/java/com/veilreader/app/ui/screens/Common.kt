package com.veilreader.app.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.R
import com.veilreader.app.ui.books.BookArtifactLayer
import com.veilreader.app.ui.books.BookArtifactState
import com.veilreader.app.ui.theme.GrayfogOrnamentFrame
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.VeilMeasure
import com.veilreader.app.ui.theme.VeilMotion
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.usesArabicScript
import java.io.File
import java.text.NumberFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
internal fun rememberVeilPercentFormatter(): (Float) -> String {
    val locale = LocalContext.current.resources.configuration.locales[0]
    val formatter = remember(locale) {
        NumberFormat.getPercentInstance(locale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
    }
    return remember(formatter) {
        { value -> formatter.format(value.toDouble()) }
    }
}
@Composable
internal fun rememberVeilIntegerFormatter(): (Number) -> String {
    val locale = LocalContext.current.resources.configuration.locales[0]
    val formatter = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    return remember(formatter) {
        { value -> formatter.format(value) }
    }
}
@Composable
internal fun rememberVeilNumberFormatter(maximumFractionDigits: Int = 2): (Number) -> String {
    val locale = LocalContext.current.resources.configuration.locales[0]
    val formatter = remember(locale, maximumFractionDigits) {
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 0
            this.maximumFractionDigits = maximumFractionDigits.coerceAtLeast(0)
            isGroupingUsed = false
        }
    }
    return remember(formatter) {
        { value -> formatter.format(value) }
    }
}



@Composable
fun VeilReveal(
    delayMillis: Int = 0,
    distance: Dp = 14.dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val reducedMotion = LocalVeilReducedMotion.current
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(reducedMotion) {
        if (!reducedMotion && delayMillis > 0) {
            delay(delayMillis.toLong())
        }
        revealed = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = if (reducedMotion) {
            tween(VeilMotion.REDUCED_MOTION_FADE_MS)
        } else {
            tween(
                durationMillis = VeilMotion.SPATIAL_MS,
                easing = FastOutSlowInEasing
            )
        },
        label = "veil-reveal-alpha"
    )
    val distancePx = with(LocalDensity.current) {
        if (reducedMotion) 0f else distance.toPx()
    }
    val translationY by animateFloatAsState(
        targetValue = if (revealed) 0f else distancePx,
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            tween(
                durationMillis = VeilMotion.SPATIAL_MS,
                easing = FastOutSlowInEasing
            )
        },
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

internal fun backLabelFor(
    destination: String,
    rtl: Boolean
): String =
    if (rtl) {
        "$destination ›"
    } else {
        "‹ $destination"
    }

@Composable
internal fun VeilBackLabel(destination: String): String =
    backLabelFor(
        destination = destination,
        rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    )

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
    val shape = MaterialTheme.shapes.small

    Box(
        modifier = modifier
            .animateContentSize(if (LocalVeilReducedMotion.current) snap() else tween(VeilMotion.STANDARD_MS))
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        colors.surface.copy(alpha = 0.995f),
                        colors.surfaceVariant.copy(alpha = 0.76f),
                        colors.surface.copy(alpha = 0.995f)
                    )
                )
            )
            .border(
                BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.56f)),
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
                            VeilPalette.Brass.copy(alpha = 0.055f),
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

        GrayfogOrnamentFrame(
            modifier = Modifier.matchParentSize(),
            strength = 0.42f
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
                .background(VeilPalette.Brass.copy(alpha = 0.74f))
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
            modifier = Modifier.padding(horizontal = VeilSpacing.md, vertical = VeilSpacing.md),
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
        val arabicScriptEyebrow = usesArabicScript(eyebrow)
        Text(
            if (arabicScriptEyebrow) eyebrow else eyebrow.uppercase(),
            color = VeilPalette.Brass,
            style = if (arabicScriptEyebrow) {
                MaterialTheme.typography.labelMedium
            } else {
                MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.75.sp)
            }
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
                modifier = Modifier.widthIn(max = VeilMeasure.EditorialText)
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

private data class DecodedBookCover(
    val image: ImageBitmap,
    val aura: Color
)

private fun extractBookAura(bitmap: android.graphics.Bitmap): Color {
    if (bitmap.width <= 0 || bitmap.height <= 0) return VeilPalette.Brass

    var red = 0L
    var green = 0L
    var blue = 0L
    var count = 0L

    for (xStep in 1..5) {
        for (yStep in 1..5) {
            val x = ((bitmap.width - 1) * xStep / 6).coerceIn(0, bitmap.width - 1)
            val y = ((bitmap.height - 1) * yStep / 6).coerceIn(0, bitmap.height - 1)
            val pixel = bitmap.getPixel(x, y)
            red += (pixel shr 16) and 0xFF
            green += (pixel shr 8) and 0xFF
            blue += pixel and 0xFF
            count++
        }
    }

    if (count == 0L) return VeilPalette.Brass
    val r = (red.toFloat() / count / 255f).coerceIn(0.08f, 0.92f)
    val g = (green.toFloat() / count / 255f).coerceIn(0.08f, 0.92f)
    val b = (blue.toFloat() / count / 255f).coerceIn(0.08f, 0.92f)

    // Slightly lift chroma/value for dark covers; aura opacity remains very low.
    val lift = 0.08f
    return Color(
        red = (r + lift).coerceAtMost(1f),
        green = (g + lift).coerceAtMost(1f),
        blue = (b + lift).coerceAtMost(1f),
        alpha = 1f
    )
}

private fun fallbackBookAura(title: String): Color {
    val palette = listOf(
        Color(0xFF6F8FA3),
        Color(0xFF9B6D67),
        Color(0xFF7F8D62),
        Color(0xFF8C78A8),
        Color(0xFFA17B4F)
    )
    val index = (title.hashCode().ushr(1) % palette.size)
    return palette[index]
}

/**
 * Publication cover with a deterministic fallback. Cached Readium covers fade in when decoding
 * finishes so library scrolling does not visually pop. Optional [artifact] state adds only
 * Veil-owned layers around the publication art; it never modifies the source cover.
 */
private data class CachedCoverVisual(
    val bitmap: ImageBitmap,
    val aura: Color
)

/** Decode enough pixels for the displayed cover without retaining a full publication image. */
internal fun bookCoverSampleSize(
    sourceWidth: Int,
    sourceHeight: Int,
    targetWidth: Int,
    targetHeight: Int
): Int {
    if (sourceWidth <= 0 || sourceHeight <= 0 || targetWidth <= 0 || targetHeight <= 0) return 1
    var sample = 1
    val pixelBudget = maxOf(
        4_000_000L,
        targetWidth.toLong() * targetHeight.toLong() * 4L
    ).coerceAtMost(12_000_000L)
    while (sample < (1 shl 29)) {
        val next = sample * 2
        val nextWidth = sourceWidth / next
        val nextHeight = sourceHeight / next
        val enoughForCover = nextWidth >= targetWidth && nextHeight >= targetHeight
        val currentPixels = sourceWidth.toLong() / sample * (sourceHeight.toLong() / sample)
        if (!enoughForCover && currentPixels <= pixelBudget) break
        sample = next
    }
    return sample
}

private fun decodeBookCover(file: File, target: IntSize): CachedCoverVisual? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options = BitmapFactory.Options().apply {
        inSampleSize = bookCoverSampleSize(
            bounds.outWidth, bounds.outHeight, target.width, target.height
        )
    }
    return BitmapFactory.decodeFile(file.absolutePath, options)?.let { bitmap ->
        CachedCoverVisual(bitmap.asImageBitmap(), sampledBookAura(bitmap))
    }
}

private fun sampledBookAura(bitmap: android.graphics.Bitmap): Color {
    if (bitmap.width <= 0 || bitmap.height <= 0) return VeilPalette.Brass

    var red = 0L
    var green = 0L
    var blue = 0L
    var count = 0L
    val columns = 5
    val rows = 7

    repeat(columns) { xIndex ->
        repeat(rows) { yIndex ->
            val x = ((xIndex + 0.5f) / columns * bitmap.width)
                .toInt()
                .coerceIn(0, bitmap.width - 1)
            val y = ((yIndex + 0.5f) / rows * bitmap.height)
                .toInt()
                .coerceIn(0, bitmap.height - 1)
            val pixel = bitmap.getPixel(x, y)
            val alpha = (pixel ushr 24) and 0xFF
            if (alpha >= 128) {
                red += (pixel ushr 16) and 0xFF
                green += (pixel ushr 8) and 0xFF
                blue += pixel and 0xFF
                count++
            }
        }
    }

    if (count == 0L) return VeilPalette.Brass

    var r = (red.toFloat() / count.toFloat()) / 255f
    var g = (green.toFloat() / count.toFloat()) / 255f
    var b = (blue.toFloat() / count.toFloat()) / 255f
    val strongest = maxOf(r, g, b)
    if (strongest < 0.34f && strongest > 0.001f) {
        val lift = 0.34f / strongest
        r = (r * lift).coerceIn(0f, 1f)
        g = (g * lift).coerceIn(0f, 1f)
        b = (b * lift).coerceIn(0f, 1f)
    }

    return Color(red = r, green = g, blue = b, alpha = 1f)
}

/**
 * Publication cover plus Veil-owned artifact layers.
 *
 * The original cover stays untouched. Aura is sampled from the artwork when available, while
 * patina, page-stack depth and seals come only from real persisted reading history.
 */
@Composable
fun BookCover(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    imagePath: String? = null,
    artifact: BookArtifactState? = null
) {
    var coverSize by remember { mutableStateOf(IntSize.Zero) }
    val cachedCover by produceState<CachedCoverVisual?>(
        initialValue = null,
        key1 = imagePath,
        key2 = coverSize
    ) {
        value = null
        if (coverSize.width <= 0 || coverSize.height <= 0) return@produceState
        value = withContext(Dispatchers.IO) {
            imagePath
                ?.takeIf { it.isNotBlank() }
                ?.let(::File)
                ?.takeIf { it.isFile && it.length() > 0L }
                ?.let { file -> decodeBookCover(file, coverSize) }
        }
    }
    val reducedMotion = LocalVeilReducedMotion.current
    val imageAlpha by animateFloatAsState(
        targetValue = if (cachedCover == null) 0f else 1f,
        animationSpec = tween(
            if (reducedMotion) {
                VeilMotion.REDUCED_MOTION_FADE_MS
            } else {
                VeilMotion.SPATIAL_MS
            }
        ),
        label = "cover-fade"
    )

    val aura = cachedCover?.aura
        ?: fallbackBookAura(title)
    val auraStrength = when {
        artifact?.recentlyOpened == true -> 0.40f
        artifact?.favorite == true -> 0.30f
        else -> 0.20f
    }

    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .onSizeChanged { coverSize = it }
            .shadow(
                elevation = if (artifact?.recentlyOpened == true) 9.dp else 7.dp,
                shape = shape,
                ambientColor = aura.copy(alpha = auraStrength),
                spotColor = Color.Black.copy(alpha = 0.32f)
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                BorderStroke(
                    1.dp,
                    aura.copy(alpha = if (artifact == null) 0.48f else 0.56f)
                ),
                shape
            )
            // Every current cover placement already presents the book title beside the artwork.
            // Keep the image layers decorative so TalkBack does not announce the same title twice.
            .clearAndSetSemantics { }
    ) {
        GeneratedBookCover(title = title, subtitle = subtitle)
        cachedCover?.let { cover ->
            Image(
                bitmap = cover.bitmap,
                contentDescription = null,
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

        artifact?.let {
            BookArtifactLayer(
                state = it,
                aura = aura,
                modifier = Modifier.matchParentSize()
            )
        }
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
                RoundedCornerShape(2.dp)
            )
    )

    Box(
        Modifier
            .width(2.dp)
            .fillMaxHeight()
            .background(VeilPalette.Brass.copy(alpha = 0.38f))
            .align(Alignment.CenterStart)
    )

    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .size(42.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(21.dp)
                .rotate(45f)
                .border(
                    BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.34f)),
                    RoundedCornerShape(1.dp)
                )
        )
        Box(
            Modifier
                .width(1.dp)
                .height(34.dp)
                .background(VeilPalette.Brass.copy(alpha = 0.18f))
        )
        Box(
            Modifier
                .width(34.dp)
                .height(1.dp)
                .background(VeilPalette.Brass.copy(alpha = 0.18f))
        )
        Box(
            Modifier
                .size(4.dp)
                .rotate(45f)
                .background(VeilPalette.Brass.copy(alpha = 0.62f))
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 17.dp, end = 14.dp, top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                stringResource(R.string.threshold_grayfog_archive),
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
