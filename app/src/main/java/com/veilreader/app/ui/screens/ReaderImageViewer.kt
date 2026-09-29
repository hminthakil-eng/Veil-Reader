package com.veilreader.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Image(
                    bitmap = content.bitmap.asImageBitmap(),
                    contentDescription = content.caption,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 58.dp)
                        .onSizeChanged { viewport = it }
                        .pointerInput(content.bitmap) {
                            detectTransformGestures { _, gesturePan, zoom, _ ->
                                val nextScale = (scale * zoom).coerceIn(1f, 5f)
                                scale = nextScale
                                pan = if (nextScale <= 1.001f) {
                                    Offset.Zero
                                } else {
                                    clampReaderImagePan(
                                        requested = pan + gesturePan,
                                        scale = nextScale,
                                        viewport = viewport
                                    )
                                }
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = pan.x
                            translationY = pan.y
                        }
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(),
                    color = VeilPalette.Ink.copy(alpha = 0.92f),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
                                maxLines = 2
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    color = VeilPalette.Ink.copy(alpha = 0.92f),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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

internal fun clampReaderImagePan(
    requested: Offset,
    scale: Float,
    viewport: IntSize
): Offset {
    if (scale <= 1f || viewport.width <= 0 || viewport.height <= 0) {
        return Offset.Zero
    }
    val safeScale = scale.coerceIn(1f, 5f)
    val maxX = viewport.width * (safeScale - 1f) * 0.5f
    val maxY = viewport.height * (safeScale - 1f) * 0.5f
    return Offset(
        x = requested.x.coerceIn(-maxX, maxX),
        y = requested.y.coerceIn(-maxY, maxY)
    )
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
