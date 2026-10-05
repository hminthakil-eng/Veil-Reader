package com.veilreader.app.ui.screens

import android.annotation.SuppressLint

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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.R
import com.veilreader.app.domain.BookFormat
import com.veilreader.app.ui.books.BookArtifactLayer
import com.veilreader.app.ui.books.BookArtifactState
import com.veilreader.app.ui.theme.withVeilContentScript
import com.veilreader.app.ui.theme.GrayfogOrnamentFrame
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.VeilMeasure
import com.veilreader.app.ui.theme.VeilMotion
import com.veilreader.app.ui.theme.VeilMaterials
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilRealm
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.usesArabicScript
import com.veilreader.app.ui.theme.withVeilTracking
import java.io.File
import java.text.NumberFormat
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal data class VeilWindowSizeDp(
    val width: Float,
    val height: Int
)

internal fun resolveVeilWindowSizeDp(
    windowWidthDp: Float,
    windowHeightDp: Float,
    fallbackWidthDp: Int,
    fallbackHeightDp: Int
): VeilWindowSizeDp {
    val fallbackWidth = fallbackWidthDp.coerceAtLeast(0).toFloat()
    val fallbackHeight = fallbackHeightDp.coerceAtLeast(0).toFloat()
    val width = windowWidthDp.takeIf { it.isFinite() && it > 0f } ?: fallbackWidth
    val height = windowHeightDp.takeIf { it.isFinite() && it > 0f } ?: fallbackHeight
    return VeilWindowSizeDp(
        width = width.coerceAtLeast(0f),
        height = height.toInt().coerceAtLeast(0)
    )
}

/**
 * Current app-window dimensions in dp.
 *
 * The live Compose window container is the structural authority for split-screen,
 * desktop resizing and fold/unfold changes. Configuration is only a bootstrap/test
 * fallback while the window host has not reported a positive container size yet.
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
internal fun currentVeilWindowSizeDp(): VeilWindowSizeDp {
    val window = LocalWindowInfo.current.containerDpSize
    val configuration = LocalConfiguration.current
    return resolveVeilWindowSizeDp(
        windowWidthDp = window.width.value,
        windowHeightDp = window.height.value,
        fallbackWidthDp = configuration.screenWidthDp,
        fallbackHeightDp = configuration.screenHeightDp
    )
}

/** A display fallback only; never changes metadata, source identity or stored history. */
@Composable
internal fun bookDisplayTitle(title: String): String =
    title.trim().ifBlank { stringResource(R.string.common_untitled_book) }

@Composable
internal fun localizedBookFormatLabel(format: BookFormat): String =
    stringResource(
        when (format) {
            BookFormat.EPUB -> R.string.book_format_epub
            BookFormat.PDF -> R.string.book_format_pdf
            BookFormat.AUDIO -> R.string.book_format_audio
            BookFormat.COMIC -> R.string.book_format_comic
        }
    )

@Composable
internal fun rememberVeilPercentFormatter(): (Float) -> String {
    val locale = LocalConfiguration.current.locales[0]
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
internal fun rememberVeilIntegerFormatter(minimumDigits: Int = 1): (Number) -> String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale, minimumDigits) {
        NumberFormat.getIntegerInstance(locale).apply { minimumIntegerDigits = minimumDigits.coerceAtLeast(1) }
    }
    return remember(formatter) {
        { value -> formatter.format(value) }
    }
}
@Composable
internal fun rememberVeilNumberFormatter(maximumFractionDigits: Int = 2): (Number) -> String {
    val locale = LocalConfiguration.current.locales[0]
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



internal fun veilRevealDurationFor(realm: VeilRealm?): Int =
    when (realm) {
        VeilRealm.SANCTUARY -> VeilMotion.FUNCTIONAL_ENTER_MS
        VeilRealm.THRESHOLD,
        VeilRealm.ARCHIVE -> VeilMotion.FUNCTIONAL_MS
        VeilRealm.CASTLE,
        VeilRealm.WORLD,
        VeilRealm.RITUAL,
        VeilRealm.SANCTUM,
        null -> VeilMotion.SPATIAL_MS
    }

@Composable
fun VeilReveal(
    delayMillis: Int = 0,
    distance: Dp = 14.dp,
    realm: VeilRealm? = null,
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

    val revealDuration = veilRevealDurationFor(realm)

    val alpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = if (reducedMotion) {
            tween(VeilMotion.REDUCED_MOTION_FADE_MS)
        } else {
            tween(
                durationMillis = revealDuration,
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
                durationMillis = revealDuration,
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
    val shape = MaterialTheme.shapes.extraSmall
    val highContrast = com.veilreader.app.ui.theme.LocalVeilHighContrast.current
    val frameColor = if (highContrast) VeilPalette.Brass else VeilMaterials.Frame
    Box(
        modifier = modifier
            .animateContentSize(if (LocalVeilReducedMotion.current) snap() else tween(VeilMotion.STANDARD_MS))
            .clip(shape)
            .background(VeilMaterials.Surface)
            .border(
                BorderStroke(
                    if (highContrast) com.veilreader.app.ui.theme.VeilStroke.Emphasis else com.veilreader.app.ui.theme.VeilStroke.Hairline,
                    frameColor.copy(alpha = if (highContrast) 0.85f else com.veilreader.app.ui.theme.VeilFrame.StructuralAlpha)
                ),
                shape
            )
    ) {
        // Registration belongs to the archive object; it does not illuminate every utility panel.
        GrayfogOrnamentFrame(
            modifier = Modifier.matchParentSize(),
            strength = com.veilreader.app.ui.theme.VeilFrame.OrnamentStrength
        )
        Column(
            modifier = Modifier.padding(horizontal = VeilSpacing.md, vertical = VeilSpacing.md),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs),
            content = content
        )
    }
}

internal fun shouldStackDenseChoices(
    widthDp: Int,
    fontScale: Float,
    optionCount: Int
): Boolean {
    val safeWidth = widthDp.coerceAtLeast(0)
    val safeScale = if (fontScale.isFinite() && fontScale > 0f) fontScale else 1f
    val safeCount = optionCount.coerceAtLeast(1)

    return when {
        safeScale >= 1.75f -> true
        safeCount >= 3 && (safeScale >= 1.35f || safeWidth < 360) -> true
        safeCount == 2 && (safeScale >= 1.60f || safeWidth < 320) -> true
        else -> false
    }
}

/**
 * Shared two-action layout for dialogs and constrained panels.
 *
 * Buttons stay side-by-side at normal density, then become full-width stacked actions when
 * available width or font scale makes equal-width labels unsafe. Callers own button styling and
 * enabled state; this component owns only responsive layout.
 */
@Composable
internal fun VeilAdaptiveDialogActions(
    modifier: Modifier = Modifier,
    spacing: Dp = VeilSpacing.xs,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val stacked = shouldStackDenseChoices(
            widthDp = maxWidth.value.toInt(),
            fontScale = LocalDensity.current.fontScale,
            optionCount = 2
        )
        if (stacked) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                first(Modifier.fillMaxWidth())
                second(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                first(Modifier.weight(1f))
                second(Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun VeilMicroLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = VeilPalette.Brass,
    strong: Boolean = false
) {
    val arabicScript = usesArabicScript(text)
    Text(
        text = if (arabicScript) text else text.uppercase(),
        modifier = modifier,
        color = color,
        style = (if (strong) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall)
            .withVeilTracking(text, if (strong) 0.75.sp else 0.65.sp)
    )
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
                MaterialTheme.typography.labelMedium.withVeilContentScript(eyebrow)
            } else {
                MaterialTheme.typography.labelMedium.withVeilTracking(eyebrow, 0.75.sp)
            }
        )
        BrassRule(Modifier.width(72.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge.withVeilContentScript(title),
            modifier = Modifier.semantics { heading() },
            color = MaterialTheme.colorScheme.onBackground
        )
        subtitle?.takeIf(String::isNotBlank)?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium.withVeilContentScript(it),
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

/** Render-review readiness only; decorative cover artwork stays unannounced by TalkBack. */
internal val BookCoverArtworkReady = SemanticsPropertyKey<Boolean>("BookCoverArtworkReady")

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
    artifact: BookArtifactState? = null,
    focusArtifact: Boolean = false
) {
    var coverSize by remember { mutableStateOf(IntSize.Zero) }
    val cachedCover by produceState<CachedCoverVisual?>(
        initialValue = null,
        key1 = imagePath,
        key2 = coverSize
    ) {
        value = null
        if (coverSize.width <= 0 || coverSize.height <= 0) return@produceState
        val decoded = withBookCoverDecodeLease {
            imagePath
                ?.takeIf { it.isNotBlank() }
                ?.let(::File)
                ?.takeIf { it.isFile && it.length() > 0L }
                ?.let { file -> decodeBookCover(file, coverSize) }
        }
        currentCoroutineContext().ensureActive()
        value = decoded
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

    val displayTitle = bookDisplayTitle(title)
    val aura = cachedCover?.aura
        ?: fallbackBookAura(displayTitle)
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
                elevation = when {
                    focusArtifact -> 18.dp
                    artifact?.recentlyOpened == true -> 9.dp
                    else -> 7.dp
                },
                shape = shape,
                ambientColor = aura.copy(
                    alpha = if (focusArtifact) {
                        (auraStrength + 0.18f).coerceAtMost(0.58f)
                    } else {
                        auraStrength
                    }
                ),
                spotColor = Color.Black.copy(alpha = if (focusArtifact) 0.48f else 0.32f)
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                BorderStroke(
                    1.dp,
                    aura.copy(
                        alpha = when {
                            focusArtifact -> 0.82f
                            artifact == null -> 0.48f
                            else -> 0.56f
                        }
                    )
                ),
                shape
            )
            // Every current cover placement already presents the book title beside the artwork.
            // Keep the image layers decorative so TalkBack does not announce the same title twice.
            .clearAndSetSemantics {
                this[BookCoverArtworkReady] = imagePath.isNullOrBlank() || cachedCover != null
            }
    ) {
        GeneratedBookCover(title = displayTitle, subtitle = subtitle)
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
internal fun BoxScope.GeneratedBookCover(title: String, subtitle: String?) {
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

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val captionFits = com.veilreader.app.ui.theme.artifactCaptionFits(
            maxWidth.value, maxHeight.value, LocalDensity.current.fontScale)
        val compactCover = maxWidth.value < com.veilreader.app.ui.theme.VeilComposition.CompactArtifactCaptionWidthDp
        if (captionFits) {
            Column(
                Modifier.fillMaxSize().padding(start = 17.dp, end = 14.dp, top = 16.dp, bottom = 16.dp)
            ) {
                BrassRule(Modifier.width(42.dp))
                // Registration occupies only spare space, never the book's identity field.
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (maxHeight >= com.veilreader.app.ui.theme.VeilArtifact.RegistrationSize) {
                        GeneratedCoverRegistration()
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        title,
                        color = VeilPalette.Moon,
                        style = (if (compactCover) MaterialTheme.typography.titleSmall
                            else MaterialTheme.typography.titleMedium).withVeilContentScript(title),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    subtitle?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            color = VeilPalette.Mist.copy(alpha = 0.82f),
                            style = MaterialTheme.typography.labelSmall.withVeilContentScript(it),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        } else {
            GeneratedCoverMonogram(title)
        }
    }
}

@Composable
private fun GeneratedCoverMonogram(title: String) {
    val initial = remember(title) {
        title.codePoints().filter { Character.isLetter(it) }.findFirst().let { letter ->
            if (letter.isPresent) String(Character.toChars(Character.toUpperCase(letter.asInt))) else null
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (initial == null || maxWidth < 48.dp || maxHeight < 64.dp) {
            GeneratedCoverRegistration()
        } else {
            // This is a decorative edition mark, sized like artwork. The readable title next
            // to the cover still follows user font scale; it remains the accessible identity.
            val markSize = (maxWidth.value * 0.42f / LocalDensity.current.fontScale).sp
            Text(
                initial,
                color = VeilPalette.Moon.copy(alpha = 0.72f),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = markSize, lineHeight = markSize * 1.25f
                ).withVeilContentScript(initial)
            )
        }
    }
}

@Composable
private fun GeneratedCoverRegistration() {
    Box(
        modifier = Modifier
            .size(com.veilreader.app.ui.theme.VeilArtifact.RegistrationSize),
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

}

/** A map and its record become adjacent rooms on wide windows; large text restores reading order. */
@Composable
internal fun VeilArchitecturalPair(
    modifier: Modifier = Modifier,
    primaryFraction: Float = com.veilreader.app.ui.theme.VeilProportion.WorldPrimary,
    minimumSecondaryReadableWidth: Dp = 0.dp,
    spacing: Dp = VeilSpacing.lg,
    primary: @Composable () -> Unit,
    secondary: @Composable () -> Unit
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val paired = com.veilreader.app.ui.theme.useArchitecturalPair(
            widthDp = maxWidth.value,
            fontScale = LocalDensity.current.fontScale
        )
        if (paired) {
            val workingWidth = (maxWidth - VeilSpacing.xl).value
            val recordWidth = minimumSecondaryReadableWidth.value * LocalDensity.current.fontScale.coerceAtLeast(1f)
            val mapFraction = primaryFraction.coerceAtMost(1f - recordWidth / workingWidth).coerceIn(0.1f, 0.9f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xl),
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(mapFraction)) { primary() }
                Column(Modifier.weight(1f - mapFraction)) { secondary() }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                primary()
                secondary()
            }
        }
    }
}

/** Bottom datum for source records; it adds no hit surface or navigation ownership. */
internal fun Modifier.veilLedgerRule(): Modifier = drawWithContent {
    drawContent()
    drawLine(
        color = VeilMaterials.Divider,
        start = androidx.compose.ui.geometry.Offset(0f, size.height),
        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
        strokeWidth = com.veilreader.app.ui.theme.ArenaGeometry.Hairline.toPx()
    )
}

/** One shared shelf register crosses the gaps between objects; covers remain untouched. */
internal fun Modifier.veilShelfDatum(coverHeight: Dp): Modifier = drawBehind {
    val registerY = coverHeight.toPx() + 10.dp.toPx()
    drawRect(VeilMaterials.Depth, topLeft = androidx.compose.ui.geometry.Offset(0f, registerY),
        size = androidx.compose.ui.geometry.Size(size.width, 6.dp.toPx()))
    drawLine(VeilMaterials.Frame, androidx.compose.ui.geometry.Offset(0f, registerY),
        androidx.compose.ui.geometry.Offset(size.width, registerY),
        com.veilreader.app.ui.theme.ArenaGeometry.Hairline.toPx())
}
