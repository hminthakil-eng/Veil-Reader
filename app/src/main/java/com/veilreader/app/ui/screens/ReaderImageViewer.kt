package com.veilreader.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import com.veilreader.app.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.veilreader.app.ui.theme.VeilPalette
import kotlin.math.max
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.services.content.Content

internal data class ReaderImageContent(
    val bitmap: Bitmap,
    val caption: String?
)

@OptIn(ExperimentalReadiumApi::class)
internal class ReaderImageTapInputListener(
    private val onImageTapped: (Content.ImageElement) -> Unit
) : InputListener {
    override fun onTap(event: TapEvent): Boolean {
        val image = event.targetElement?.content as? Content.ImageElement
            ?: return false
        onImageTapped(image)
        return true
    }
}

@Composable
internal fun ReaderImageViewer(
    content: ReaderImageContent,
    title: String,
    closeLabel: String,
    zoomHint: String,
    onDismiss: () -> Unit
) {
    var scale by remember(content.bitmap) { mutableFloatStateOf(1f) }
    var pan by remember(content.bitmap) { mutableStateOf(Offset.Zero) }
    var viewport by remember(content.bitmap) { mutableStateOf(IntSize.Zero) }
    val imageSize = remember(content.bitmap) {
        IntSize(content.bitmap.width, content.bitmap.height)
    }
    val formatPercent = rememberVeilPercentFormatter()
    val zoomLabel = stringResource(R.string.reader_zoom)

    fun setZoom(requested: Float) {
        scale = (if (requested.isFinite()) requested else 1f).coerceIn(1f, 5f)
        pan = clampReaderImagePan(pan, scale, viewport, imageSize)
    }

    LaunchedEffect(viewport, imageSize) {
        pan = clampReaderImagePan(pan, scale, viewport, imageSize)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = VeilPalette.Ink,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.labelSmall,
                        color = VeilPalette.Brass
                    )
                    content.caption?.takeIf { it.isNotBlank() }?.let { caption ->
                        Text(
                            caption,
                            style = MaterialTheme.typography.bodySmall,
                            color = VeilPalette.Moon.copy(alpha = 0.76f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier.weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .clipToBounds()
                        .onSizeChanged { viewport = it }
                        .pointerInput(content.bitmap) {
                            detectTransformGestures { centroid, gesturePan, zoom, _ ->
                                val requestedScale = scale * zoom
                                val nextScale = if (requestedScale.isFinite()) {
                                    requestedScale.coerceIn(1f, 5f)
                                } else {
                                    scale
                                }
                                pan = readerImagePanAfterZoom(
                                    pan = pan,
                                    currentScale = scale,
                                    nextScale = nextScale,
                                    centroid = centroid,
                                    gesturePan = gesturePan,
                                    viewport = viewport,
                                    imageSize = imageSize
                                )
                                scale = nextScale
                            }
                        }
                ) {
                    Image(
                        bitmap = content.bitmap.asImageBitmap(),
                        contentDescription = content.caption ?: title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = pan.x
                            translationY = pan.y
                        }
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        zoomHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = VeilPalette.Moon.copy(alpha = 0.76f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            zoomLabel,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            color = VeilPalette.Moon
                        )
                        Text(
                            formatPercent(scale),
                            style = MaterialTheme.typography.labelMedium,
                            color = VeilPalette.Brass
                        )
                        TextButton(
                            onClick = { setZoom(1f) },
                            enabled = scale > 1f,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text(stringResource(R.string.common_reset)) }
                    }
                    Slider(
                        value = scale,
                        onValueChange = ::setZoom,
                        valueRange = 1f..5f,
                        modifier = Modifier.semantics {
                            contentDescription = zoomLabel
                            stateDescription = formatPercent(scale)
                        }
                    )
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = MaterialTheme.shapes.extraSmall,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VeilPalette.Brass,
                            contentColor = VeilPalette.Ink
                        )
                    ) { Text(closeLabel) }
                }
            }
        }
    }
}

/**
 * Clamp against the fitted illustration, not the enclosing letterboxed viewport.
 * A short or narrow illustration cannot be dragged into an empty margin.
 */
internal fun clampReaderImagePan(
    requested: Offset,
    scale: Float,
    viewport: IntSize,
    imageSize: IntSize = viewport
): Offset {
    if (!scale.isFinite() || scale <= 1f ||
        viewport.width <= 0 || viewport.height <= 0 ||
        imageSize.width <= 0 || imageSize.height <= 0
    ) return Offset.Zero

    val safeScale = scale.coerceIn(1f, 5f)
    val fit = minOf(
        viewport.width.toFloat() / imageSize.width,
        viewport.height.toFloat() / imageSize.height
    )
    val maxX = ((imageSize.width * fit * safeScale - viewport.width) * 0.5f)
        .coerceAtLeast(0f)
    val maxY = ((imageSize.height * fit * safeScale - viewport.height) * 0.5f)
        .coerceAtLeast(0f)
    return Offset(
        x = (if (requested.x.isFinite()) requested.x else 0f).coerceIn(-maxX, maxX),
        y = (if (requested.y.isFinite()) requested.y else 0f).coerceIn(-maxY, maxY)
    )
}

/** Keep the image point under the pinch centroid fixed until an actual image edge is reached. */
internal fun readerImagePanAfterZoom(
    pan: Offset,
    currentScale: Float,
    nextScale: Float,
    centroid: Offset,
    gesturePan: Offset,
    viewport: IntSize,
    imageSize: IntSize
): Offset {
    if (!currentScale.isFinite() || currentScale <= 0f || !nextScale.isFinite()) {
        return Offset.Zero
    }
    val ratio = nextScale.coerceIn(1f, 5f) / currentScale
    val focus = centroid - Offset(viewport.width * 0.5f, viewport.height * 0.5f)
    val anchoredPan = pan * ratio + focus * (1f - ratio) + gesturePan
    return clampReaderImagePan(anchoredPan, nextScale, viewport, imageSize)
}

internal fun decodeReaderImage(
    bytes: ByteArray,
    maxDimensionPx: Int = 4096
): Bitmap? {
    if (bytes.isEmpty()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val sample = readerImageSampleSize(
        width = bounds.outWidth,
        height = bounds.outHeight,
        maxDimensionPx = maxDimensionPx
    )

    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return runCatching {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }.getOrNull()
}


internal fun readerImageSampleSize(
    width: Int,
    height: Int,
    maxDimensionPx: Int = 4096
): Int {
    if (width <= 0 || height <= 0) return 1
    val safeMax = max(512, maxDimensionPx)
    var sample = 1
    while (
        width / sample > safeMax ||
        height / sample > safeMax
    ) {
        sample *= 2
    }
    return sample
}

