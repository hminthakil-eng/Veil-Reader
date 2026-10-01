package com.veilreader.app.ui.screens

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import com.veilreader.app.R
import androidx.compose.ui.unit.dp
import com.github.barteksc.pdfviewer.PDFView
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderNavigationMode
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSpacing
import kotlinx.coroutines.delay
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.shared.ExperimentalReadiumApi

@Composable
internal fun PdfZoomControls(
    navigator: Navigator?,
    appearance: ReaderAppearance,
    onAppearanceChange: (ReaderAppearance) -> Unit,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier,
    onDone: () -> Unit
) {
    var pdfView by remember(navigator) {
        mutableStateOf(navigator.findPdfView())
    }
    LaunchedEffect(navigator) {
        while (pdfView == null) {
            pdfView = navigator.findPdfView()
            if (pdfView == null) delay(100)
        }
    }

    val scrollLabel = stringResource(R.string.pdf_scroll)
    val pageLabel = stringResource(R.string.pdf_page)
    val scrollSemantics = stringResource(R.string.pdf_continuous_scroll)
    val pageSemantics = stringResource(R.string.pdf_paginated_layout)
    val zoomSemantics = stringResource(R.string.pdf_zoom)
    val zoomOutSemantics = stringResource(R.string.pdf_zoom_out)
    val zoomInSemantics = stringResource(R.string.pdf_zoom_in)
    val zoomResetSemantics = stringResource(R.string.pdf_zoom_reset)
    val formatPercent = rememberVeilPercentFormatter()

    val view = pdfView
    var zoomMirror by remember(view) { mutableFloatStateOf(view?.zoom ?: 1f) }

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
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                stringResource(R.string.pdf_controls_eyebrow),
                style = MaterialTheme.typography.labelSmall,
                color = VeilPalette.Brass
            )
            BrassRule(Modifier.fillMaxWidth())
            Text(
                stringResource(R.string.pdf_controls_title),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                stringResource(R.string.pdf_controls_intro),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Text(
            stringResource(R.string.pdf_layout),
            style = MaterialTheme.typography.labelSmall,
            color = VeilPalette.Brass
        )

        Row(
            Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                false to pageLabel,
                true to scrollLabel
            ).forEach { (scrollMode, label) ->
                val selected = appearance.scroll == scrollMode
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 72.dp)
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton
                        ) {
                            onAppearanceChange(appearance.copy(scroll = scrollMode))
                        }
                        .semantics {
                            contentDescription =
                                if (scrollMode) scrollSemantics else pageSemantics
                        },
                    shape = MaterialTheme.shapes.extraSmall,
                    color = if (selected) {
                        VeilPalette.DeepBrass.copy(alpha = 0.76f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.46f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (selected) VeilPalette.Brass.copy(alpha = 0.82f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f)
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ReaderMotionPreview(
                            mode = if (scrollMode) {
                                ReaderNavigationMode.SCROLL
                            } else {
                                ReaderNavigationMode.PAGED
                            },
                            active = selected,
                            modifier = Modifier
                                .width(48.dp)
                                .height(28.dp)
                        )
                        VeilMicroLabel(
                            text = label,
                            color = if (selected) {
                                VeilPalette.Moon
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            strong = true
                        )
                    }
                }
            }
        }

        Text(
            if (appearance.scroll) {
                stringResource(R.string.pdf_scroll_description)
            } else {
                stringResource(R.string.pdf_page_description)
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )

        BrassRule(Modifier.fillMaxWidth())

        if (view == null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.46f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(VeilSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
                ) {
                    Text(
                        stringResource(R.string.pdf_preparing),
                        style = MaterialTheme.typography.labelSmall,
                        color = VeilPalette.Brass
                    )
                    Text(
                        stringResource(R.string.pdf_renderer_connecting),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            val minZoom = view.minZoom.coerceAtLeast(0.5f)
            val maxZoom = view.maxZoom.coerceAtLeast(minZoom + 0.5f)
            val displayedZoom = normalizedPdfZoom(zoomMirror, minZoom, maxZoom)

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.pdf_zoom),
                    style = MaterialTheme.typography.labelSmall,
                    color = VeilPalette.Brass,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    formatPercent(displayedZoom),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Slider(
                value = displayedZoom,
                onValueChange = {
                    val requested = normalizedPdfZoom(it, minZoom, maxZoom)
                    view.zoomTo(requested)
                    zoomMirror = normalizedPdfZoom(view.zoom, minZoom, maxZoom)
                },
                valueRange = minZoom..maxZoom,
                modifier = Modifier.semantics {
                    contentDescription = zoomSemantics
                    stateDescription = formatPercent(displayedZoom)
                }
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                        if (shouldAnimatePdfZoom(reducedMotion)) {
                            view.zoomWithAnimation(requested)
                        } else {
                            view.zoomTo(requested)
                            zoomMirror = normalizedPdfZoom(view.zoom, minZoom, maxZoom)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = zoomOutSemantics },
                    shape = MaterialTheme.shapes.extraSmall,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.56f)
                    )
                ) {
                    Text("−", style = MaterialTheme.typography.titleLarge)
                }

                OutlinedButton(
                    onClick = {
                        if (shouldAnimatePdfZoom(reducedMotion)) {
                            view.resetZoomWithAnimation()
                        } else {
                            view.zoomTo(normalizedPdfZoom(1f, minZoom, maxZoom))
                            zoomMirror = normalizedPdfZoom(view.zoom, minZoom, maxZoom)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = zoomResetSemantics },
                    shape = MaterialTheme.shapes.extraSmall,
                    border = BorderStroke(
                        1.dp,
                        VeilPalette.Brass.copy(alpha = 0.44f)
                    )
                ) {
                    Text(formatPercent(1f))
                }

                OutlinedButton(
                    onClick = {
                        val requested = nextPdfZoom(
                            current = normalizedPdfZoom(view.zoom, minZoom, maxZoom),
                            min = minZoom,
                            max = maxZoom,
                            factor = 1.25f
                        )
                        if (shouldAnimatePdfZoom(reducedMotion)) {
                            view.zoomWithAnimation(requested)
                        } else {
                            view.zoomTo(requested)
                            zoomMirror = normalizedPdfZoom(view.zoom, minZoom, maxZoom)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = zoomInSemantics },
                    shape = MaterialTheme.shapes.extraSmall,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.56f)
                    )
                ) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }

            Button(
                onClick = {
                    view.fitToWidth(view.currentPage)
                    zoomMirror = normalizedPdfZoom(view.zoom, minZoom, maxZoom)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = MaterialTheme.shapes.extraSmall,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VeilPalette.DeepBrass.copy(alpha = 0.78f),
                    contentColor = VeilPalette.Moon
                )
            ) {
                Text(stringResource(R.string.pdf_fit_width))
            }
        }

        BrassRule(Modifier.fillMaxWidth())

        ReaderBrightnessControls(
            appearance = appearance,
            onChange = onAppearanceChange
        )

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            shape = MaterialTheme.shapes.extraSmall,
            colors = ButtonDefaults.buttonColors(
                containerColor = VeilPalette.Brass,
                contentColor = Color(0xFF17120A)
            )
        ) {
            Text(stringResource(R.string.reader_back_to_reading))
        }
    }
}

internal fun shouldAnimatePdfZoom(reducedMotion: Boolean): Boolean =
    !reducedMotion

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
