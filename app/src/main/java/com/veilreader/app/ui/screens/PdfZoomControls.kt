package com.veilreader.app.ui.screens

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.barteksc.pdfviewer.PDFView
import kotlinx.coroutines.delay
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.shared.ExperimentalReadiumApi

@Composable
internal fun PdfZoomControls(
    navigator: Navigator?,
    modifier: Modifier = Modifier,
    onDone: () -> Unit
) {
    var pdfView by remember(navigator) {
        mutableStateOf(navigator.findPdfView())
    }
    LaunchedEffect(navigator) {
        repeat(12) {
            if (pdfView != null) return@LaunchedEffect
            delay(100)
            pdfView = navigator.findPdfView()
        }
    }

    val view = pdfView
    var zoomMirror by remember(view) { mutableFloatStateOf(view?.zoom ?: 1f) }

    // AndroidPdfViewer 3.2.8 exposes zoom getters/mutators but no zoom-change callback.
    // Keep PDFView authoritative and mirror its current zoom only while this sheet is composed.
    LaunchedEffect(view) {
        val target = view ?: return@LaunchedEffect
        while (true) {
            val minZoom = target.minZoom.coerceAtLeast(0.5f)
            val maxZoom = target.maxZoom.coerceAtLeast(minZoom + 0.5f)
            zoomMirror = normalizedPdfZoom(target.zoom, minZoom, maxZoom)
            delay(PDF_ZOOM_MIRROR_INTERVAL_MS)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "PDF zoom",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            "Pinch or double-tap the page at any time. These controls give you a reliable manual fallback.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )

        if (view == null) {
            Text(
                "Zoom controls are still connecting to the PDF renderer.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val minZoom = view.minZoom.coerceAtLeast(0.5f)
            val maxZoom = view.maxZoom.coerceAtLeast(minZoom + 0.5f)
            val displayedZoom = normalizedPdfZoom(zoomMirror, minZoom, maxZoom)

            Text(
                "Zoom · ${(displayedZoom * 100).toInt()}%",
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = displayedZoom,
                onValueChange = {
                    val requested = normalizedPdfZoom(it, minZoom, maxZoom)
                    view.zoomTo(requested)
                    zoomMirror = normalizedPdfZoom(view.zoom, minZoom, maxZoom)
                },
                valueRange = minZoom..maxZoom
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val requested = nextPdfZoom(
                            current = normalizedPdfZoom(view.zoom, minZoom, maxZoom),
                            min = minZoom,
                            max = maxZoom,
                            factor = 0.8f
                        )
                        view.zoomWithAnimation(requested)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("−") }
                OutlinedButton(
                    onClick = {
                        view.resetZoomWithAnimation()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Reset") }
                OutlinedButton(
                    onClick = {
                        val requested = nextPdfZoom(
                            current = normalizedPdfZoom(view.zoom, minZoom, maxZoom),
                            min = minZoom,
                            max = maxZoom,
                            factor = 1.25f
                        )
                        view.zoomWithAnimation(requested)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("+") }
            }

            OutlinedButton(
                onClick = {
                    view.fitToWidth(view.currentPage)
                    zoomMirror = normalizedPdfZoom(view.zoom, minZoom, maxZoom)
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("Fit page width") }
        }

        HorizontalDivider()
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
        ) { Text("Back to reading") }
    }
}

internal fun normalizedPdfZoom(
    current: Float,
    min: Float,
    max: Float
): Float {
    val safeMin = if (min.isFinite()) min.coerceAtLeast(0.01f) else 1f
    val safeMax = if (max.isFinite()) max.coerceAtLeast(safeMin) else safeMin
    val safeCurrent = if (current.isFinite()) current else safeMin
    return safeCurrent.coerceIn(safeMin, safeMax)
}

internal fun nextPdfZoom(
    current: Float,
    min: Float,
    max: Float,
    factor: Float
): Float {
    val safeCurrent = normalizedPdfZoom(current, min, max)
    val safeFactor = if (factor.isFinite() && factor > 0f) factor else 1f
    return normalizedPdfZoom(safeCurrent * safeFactor, min, max)
}

private const val PDF_ZOOM_MIRROR_INTERVAL_MS = 80L

@OptIn(ExperimentalReadiumApi::class)
private fun Navigator?.findPdfView(): PDFView? {
    val root = (this as? OverflowableNavigator)?.publicationView ?: return null
    return root.findPdfView()
}

private fun View.findPdfView(): PDFView? {
    if (this is PDFView) return this
    if (this !is ViewGroup) return null
    for (index in 0 until childCount) {
        getChildAt(index).findPdfView()?.let { return it }
    }
    return null
}