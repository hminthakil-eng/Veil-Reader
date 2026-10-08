package com.veilreader.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.veilreader.app.R
import com.veilreader.app.ui.theme.VeilPalette
import kotlin.math.max
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.services.content.Content

internal data class ReaderImageContent(
    val bitmap: Bitmap,
    val caption: String?
) : AutoCloseable {
    override fun close() {
        if (!bitmap.isRecycled) bitmap.recycle()
    }
}

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
    var transform by remember(content.bitmap) { mutableStateOf(ReaderImageTransform()) }
    var viewport by remember(content.bitmap) { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(content.bitmap, viewport) {
        transform = clampReaderImageTransform(
            transform, viewport.width, viewport.height, content.bitmap.width, content.bitmap.height
        )
    }
    fun zoomBy(factor: Float) {
        transform = transformReaderImage(
            transform, viewport.width, viewport.height, content.bitmap.width, content.bitmap.height,
            viewport.width / 2f, viewport.height / 2f, factor
        )
    }
    val imageDescription = content.caption?.takeIf { it.isNotBlank() } ?: title

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
            color = Color(0xFF07090D),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(),
                    color = VeilPalette.Ink.copy(alpha = 0.92f),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        VeilMicroLabel(
                            text = title,
                            strong = true
                        )
                        content.caption?.takeIf { it.isNotBlank() }?.let { caption ->
                            Text(
                                caption,
                                style = MaterialTheme.typography.bodySmall,
                                color = VeilPalette.Moon.copy(alpha = 0.76f),
                                maxLines = 2
                            )
                        }
                    }
                }

                Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                    Image(
                        bitmap = content.bitmap.asImageBitmap(),
                        contentDescription = imageDescription,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp)
                            .onSizeChanged { viewport = it }
                            .pointerInput(content.bitmap) {
                                detectTransformGestures { centroid, gesturePan, zoom, _ ->
                                    transform = transformReaderImage(
                                        transform, viewport.width, viewport.height,
                                        content.bitmap.width, content.bitmap.height,
                                        centroid.x, centroid.y, zoom, gesturePan.x, gesturePan.y
                                    )
                                }
                            }
                            .graphicsLayer {
                                scaleX = transform.scale
                                scaleY = transform.scale
                                translationX = transform.panX
                                translationY = transform.panY
                            }
                    )
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth(),
                    color = VeilPalette.Ink.copy(alpha = 0.92f),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val zoomOutLabel = stringResource(R.string.pdf_zoom_out)
                        val fitLabel = stringResource(R.string.reader_image_zoom_reset)
                        val zoomInLabel = stringResource(R.string.pdf_zoom_in)
                        val zoomColors = IconButtonDefaults.iconButtonColors(
                            contentColor = VeilPalette.Moon,
                            disabledContentColor = VeilPalette.Moon.copy(alpha = 0.38f)
                        )
                        Row(horizontalArrangement = Arrangement.Center) {
                            IconButton(
                                onClick = { zoomBy(0.8f) },
                                enabled = transform.scale > 1f,
                                modifier = Modifier.size(48.dp).semantics { contentDescription = zoomOutLabel },
                                colors = zoomColors
                            ) {
                                Text("−", Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.titleLarge)
                            }
                            IconButton(
                                onClick = { transform = ReaderImageTransform() },
                                modifier = Modifier.size(48.dp).semantics { contentDescription = fitLabel },
                                colors = zoomColors
                            ) {
                                Text("↺", Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.titleLarge)
                            }
                            IconButton(
                                onClick = { zoomBy(1.25f) },
                                enabled = transform.scale < 5f && viewport.width > 0 && viewport.height > 0,
                                modifier = Modifier.size(48.dp).semantics { contentDescription = zoomInLabel },
                                colors = zoomColors
                            ) {
                                Text("+", Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                        Text(
                            zoomHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = VeilPalette.Moon.copy(alpha = 0.58f)
                        )
                        Spacer(Modifier.heightIn(min = 4.dp))
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = MaterialTheme.shapes.extraSmall,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VeilPalette.Brass,
                                contentColor = Color(0xFF17120A)
                            )
                        ) {
                            Text(closeLabel)
                        }
                    }
                }
            }
        }
    }
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
