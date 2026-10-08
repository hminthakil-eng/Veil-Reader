package com.veilreader.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderNavigationMode
import com.veilreader.app.domain.ReaderReadingMode
import com.veilreader.app.ui.reader.material.MaterialPageEngineRollout
import com.veilreader.app.ui.theme.ReaderVisualGeometry
import com.veilreader.app.ui.theme.ReaderVisualOpacity
import com.veilreader.app.ui.theme.VeilMaterials
import com.veilreader.app.ui.theme.VeilPalette

/**
 * Presentation-only appearance controls for Reader Sanctuary.
 *
 * Navigation ownership, persistence and effective-appearance policy remain outside this file.
 */
@Composable
internal fun ReaderAppearanceChoice(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val foreground = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(
            alpha = ReaderVisualOpacity.Disabled
        )
        selected -> VeilPalette.Moon
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier
            .heightIn(min = ReaderVisualGeometry.TouchTarget)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            ),
        shape = RoundedCornerShape(ReaderVisualGeometry.CompactControlRadius),
        color = if (selected) {
            VeilMaterials.ElevatedSurface
        } else {
            VeilPalette.Archive.copy(alpha = ReaderVisualOpacity.InactiveArchiveSurface)
        },
        contentColor = foreground,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) {
                VeilPalette.Brass.copy(alpha = ReaderVisualOpacity.SelectedBorder)
            } else {
                VeilPalette.Brass.copy(alpha = ReaderVisualOpacity.QuietBorder)
            }
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = foreground,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun localizedReaderMotionSummary(appearance: ReaderAppearance): String {
    if (appearance.readingMode == ReaderReadingMode.SCROLL) {
        return stringResource(R.string.settings_mode_scroll)
    }

    val readingMode = stringResource(R.string.settings_mode_paged)
    val pageTurn = stringResource(
        when (appearance.pageTurnStyle) {
            PageTurnStyle.PAPER -> R.string.settings_mode_curl
            PageTurnStyle.SLIDE -> R.string.settings_mode_slide
            PageTurnStyle.NONE -> R.string.settings_page_turn_none
        }
    )
    return "$readingMode · $pageTurn"
}

@Composable
internal fun localizedPageTurnStyleDescription(style: PageTurnStyle): String =
    stringResource(
        when (style) {
            PageTurnStyle.PAPER -> R.string.settings_mode_curl_description
            PageTurnStyle.SLIDE -> R.string.settings_mode_slide_description
            PageTurnStyle.NONE -> R.string.settings_mode_paged_description
        }
    )

@Composable
internal fun ReaderReadingModeSelector(
    selected: ReaderReadingMode,
    scrollEnabled: Boolean,
    onSelect: (ReaderReadingMode) -> Unit
) {
    val choices = listOf(
        ReaderReadingMode.PAGED to ReaderNavigationMode.PAGED,
        ReaderReadingMode.SCROLL to ReaderNavigationMode.SCROLL
    )

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = shouldStackDenseChoices(
            widthDp = maxWidth.value.toInt(),
            fontScale = LocalDensity.current.fontScale,
            optionCount = choices.size
        )

        if (stacked) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                choices.forEach { (mode, previewMode) ->
                    val active = selected == mode
                    val enabled = mode != ReaderReadingMode.SCROLL || scrollEnabled
                    ReaderModeChoice(
                        label = stringResource(
                            if (mode == ReaderReadingMode.SCROLL) {
                                R.string.settings_mode_scroll
                            } else {
                                R.string.settings_mode_paged
                            }
                        ),
                        previewMode = previewMode,
                        active = active,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelect(mode) }
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                choices.forEach { (mode, previewMode) ->
                    val active = selected == mode
                    val enabled = mode != ReaderReadingMode.SCROLL || scrollEnabled
                    ReaderModeChoice(
                        label = stringResource(
                            if (mode == ReaderReadingMode.SCROLL) {
                                R.string.settings_mode_scroll
                            } else {
                                R.string.settings_mode_paged
                            }
                        ),
                        previewMode = previewMode,
                        active = active,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(mode) }
                    )
                }
            }
        }
    }
}

internal fun readerPageTurnStyleEnabled(
    style: PageTurnStyle,
    selectorEnabled: Boolean,
    materialPageEnabled: Boolean
): Boolean =
    selectorEnabled &&
        (style != PageTurnStyle.PAPER || materialPageEnabled)

@Composable
internal fun ReaderPageTurnSelector(
    selected: PageTurnStyle,
    enabled: Boolean = true,
    paperEnabled: Boolean = MaterialPageEngineRollout.isEnabled(),
    onSelect: (PageTurnStyle) -> Unit
) {
    val choices = listOf(
        Triple(
            PageTurnStyle.PAPER,
            ReaderNavigationMode.PAPER_CURL,
            stringResource(R.string.settings_mode_curl)
        ),
        Triple(
            PageTurnStyle.SLIDE,
            ReaderNavigationMode.SLIDE,
            stringResource(R.string.settings_mode_slide)
        ),
        Triple(
            PageTurnStyle.NONE,
            ReaderNavigationMode.PAGED,
            stringResource(R.string.settings_page_turn_none)
        )
    )

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = shouldStackDenseChoices(
            widthDp = maxWidth.value.toInt(),
            fontScale = LocalDensity.current.fontScale,
            optionCount = choices.size
        )

        if (stacked) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                choices.forEach { (style, previewMode, label) ->
                    val choiceEnabled = readerPageTurnStyleEnabled(
                        style = style,
                        selectorEnabled = enabled,
                        materialPageEnabled = paperEnabled
                    )
                    ReaderModeChoice(
                        label = label,
                        previewMode = previewMode,
                        active = selected == style,
                        enabled = choiceEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { if (choiceEnabled) onSelect(style) }
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                choices.forEach { (style, previewMode, label) ->
                    val choiceEnabled = readerPageTurnStyleEnabled(
                        style = style,
                        selectorEnabled = enabled,
                        materialPageEnabled = paperEnabled
                    )
                    ReaderModeChoice(
                        label = label,
                        previewMode = previewMode,
                        active = selected == style,
                        enabled = choiceEnabled,
                        modifier = Modifier.weight(1f),
                        onClick = { if (choiceEnabled) onSelect(style) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderModeChoice(
    label: String,
    previewMode: ReaderNavigationMode,
    active: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .heightIn(min = 58.dp)
            .selectable(
                selected = active,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            ),
        shape = RoundedCornerShape(ReaderVisualGeometry.CompactControlRadius),
        color = if (active) {
            VeilMaterials.ElevatedSurface
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = ReaderVisualOpacity.InactiveSurface)
        },
        border = BorderStroke(
            1.dp,
            if (active) {
                VeilPalette.Brass.copy(alpha = ReaderVisualOpacity.SelectedBorder)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.46f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            ReaderMotionPreview(
                mode = previewMode,
                active = active,
                modifier = Modifier
                    .width(44.dp)
                    .height(28.dp)
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    !enabled ->
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    active -> VeilPalette.Moon
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun ReaderMotionPreview(
    mode: ReaderNavigationMode,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val lineColor = if (active) {
        VeilPalette.Brass
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
    }
    val faint = lineColor.copy(alpha = 0.38f)

    Canvas(modifier) {
        val stroke = 1.15.dp.toPx()
        val radius = 1.5.dp.toPx()
        val inset = 1.5.dp.toPx()
        val pageTop = size.height * 0.08f
        val pageHeight = size.height * 0.84f

        when (mode) {
            ReaderNavigationMode.PAPER_CURL -> {
                drawRoundRect(
                    color = faint,
                    topLeft = Offset(inset, pageTop),
                    size = Size(size.width - inset * 2f, pageHeight),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(stroke)
                )
                val fold = Path().apply {
                    moveTo(size.width * 0.67f, pageTop)
                    lineTo(size.width * 0.60f, size.height * 0.48f)
                    lineTo(size.width * 0.78f, size.height * 0.92f)
                }
                drawPath(
                    path = fold,
                    color = lineColor,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
                drawLine(
                    color = lineColor.copy(alpha = 0.54f),
                    start = Offset(size.width * 0.67f, pageTop),
                    end = Offset(size.width * 0.92f, size.height * 0.25f),
                    strokeWidth = stroke
                )
            }

            ReaderNavigationMode.SLIDE -> {
                val pageWidth = size.width * 0.46f
                drawRoundRect(
                    color = faint,
                    topLeft = Offset(-pageWidth * 0.28f, pageTop),
                    size = Size(pageWidth, pageHeight),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(stroke)
                )
                drawRoundRect(
                    color = lineColor,
                    topLeft = Offset(size.width * 0.42f, pageTop),
                    size = Size(pageWidth, pageHeight),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(stroke)
                )
                drawLine(
                    color = lineColor.copy(alpha = 0.55f),
                    start = Offset(size.width * 0.35f, size.height * 0.50f),
                    end = Offset(size.width * 0.55f, size.height * 0.50f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }

            ReaderNavigationMode.PAGED -> {
                drawRoundRect(
                    color = lineColor,
                    topLeft = Offset(size.width * 0.18f, pageTop),
                    size = Size(size.width * 0.64f, pageHeight),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(stroke)
                )
                repeat(3) { index ->
                    val y = size.height * (0.34f + index * 0.15f)
                    drawLine(
                        color = faint,
                        start = Offset(size.width * 0.31f, y),
                        end = Offset(size.width * 0.69f, y),
                        strokeWidth = stroke * 0.75f,
                        cap = StrokeCap.Round
                    )
                }
            }

            ReaderNavigationMode.SCROLL -> {
                drawRoundRect(
                    color = faint,
                    topLeft = Offset(size.width * 0.20f, pageTop),
                    size = Size(size.width * 0.60f, pageHeight),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(stroke)
                )
                repeat(4) { index ->
                    val y = size.height * (0.24f + index * 0.16f)
                    val shift = if (index % 2 == 0) 0f else size.width * 0.06f
                    drawLine(
                        color = if (index == 2) lineColor else faint,
                        start = Offset(size.width * 0.30f + shift, y),
                        end = Offset(size.width * 0.70f, y),
                        strokeWidth = stroke * 0.8f,
                        cap = StrokeCap.Round
                    )
                }
                drawLine(
                    color = lineColor,
                    start = Offset(size.width * 0.82f, size.height * 0.30f),
                    end = Offset(size.width * 0.82f, size.height * 0.70f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = lineColor,
                    start = Offset(size.width * 0.76f, size.height * 0.64f),
                    end = Offset(size.width * 0.82f, size.height * 0.70f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = lineColor,
                    start = Offset(size.width * 0.88f, size.height * 0.64f),
                    end = Offset(size.width * 0.82f, size.height * 0.70f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
