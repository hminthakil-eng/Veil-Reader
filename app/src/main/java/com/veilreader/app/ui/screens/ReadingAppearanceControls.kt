package com.veilreader.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veilreader.app.domain.PageTurnStyle
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderTheme

/**
 * Shared persistent EPUB appearance controls.
 *
 * Both the in-reader bottom sheet and app Settings use this component and the same ReaderAppearance
 * storage. No second preference model or database is introduced.
 */
@Composable
internal fun ReadingAppearanceControls(
    appearance: ReaderAppearance,
    onChange: (ReaderAppearance) -> Unit,
    modifier: Modifier = Modifier,
    onDone: (() -> Unit)? = null
) {
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Reading appearance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "These defaults stay on this device and are used the next time you open an EPUB.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Text("Presets", style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppearancePreset("Book", appearance.theme == ReaderTheme.PAPER) {
                onChange(
                    appearance.copy(
                        theme = ReaderTheme.PAPER,
                        fontScale = 1.0,
                        lineHeight = 1.45,
                        pageMargins = 1.0,
                        scroll = false,
                        publisherStyles = true
                    )
                )
            }
            AppearancePreset("Comfort", appearance.theme == ReaderTheme.SEPIA) {
                onChange(
                    appearance.copy(
                        theme = ReaderTheme.SEPIA,
                        fontScale = 1.08,
                        lineHeight = 1.6,
                        pageMargins = 1.15,
                        scroll = false,
                        publisherStyles = false
                    )
                )
            }
            AppearancePreset("Night", appearance.theme == ReaderTheme.DUSK) {
                onChange(
                    appearance.copy(
                        theme = ReaderTheme.DUSK,
                        fontScale = 1.05,
                        lineHeight = 1.55,
                        pageMargins = 1.1,
                        publisherStyles = false
                    )
                )
            }
            AppearancePreset("OLED", appearance.theme == ReaderTheme.OLED) {
                onChange(
                    appearance.copy(
                        theme = ReaderTheme.OLED,
                        fontScale = 1.05,
                        lineHeight = 1.55,
                        pageMargins = 1.1,
                        publisherStyles = false
                    )
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Text("Text size · ${(appearance.fontScale * 100).toInt()}%", fontWeight = FontWeight.SemiBold)
        Slider(
            value = appearance.fontScale.toFloat(),
            onValueChange = { onChange(appearance.copy(fontScale = it.toDouble(), publisherStyles = false)) },
            valueRange = .75f..1.8f
        )

        Text("Line height · ${"%.2f".format(appearance.lineHeight)}", fontWeight = FontWeight.SemiBold)
        Slider(
            value = appearance.lineHeight.toFloat(),
            onValueChange = { onChange(appearance.copy(lineHeight = it.toDouble(), publisherStyles = false)) },
            valueRange = 1.1f..2.0f
        )

        Text("Page margins · ${"%.2f".format(appearance.pageMargins)}", fontWeight = FontWeight.SemiBold)
        Slider(
            value = appearance.pageMargins.toFloat(),
            onValueChange = { onChange(appearance.copy(pageMargins = it.toDouble(), publisherStyles = false)) },
            valueRange = .5f..2.0f
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Continuous scroll", fontWeight = FontWeight.SemiBold)
                Text(
                    "Turn this off for paginated reading with page-turn gestures.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = appearance.scroll,
                onCheckedChange = { onChange(appearance.copy(scroll = it)) },
                modifier = Modifier.semantics { contentDescription = "Continuous scroll" }
            )
        }

        if (!appearance.scroll) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Page turn", fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = appearance.pageTurnStyle == PageTurnStyle.PAPER,
                        onClick = {
                            onChange(appearance.copy(pageTurnStyle = PageTurnStyle.PAPER))
                        },
                        label = { Text("Paper curl") },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    )
                    FilterChip(
                        selected = appearance.pageTurnStyle == PageTurnStyle.SLIDE,
                        onClick = {
                            onChange(appearance.copy(pageTurnStyle = PageTurnStyle.SLIDE))
                        },
                        label = { Text("Simple slide") },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    )
                }
                Text(
                    "Paper curl follows the page edge. Simple slide is a lighter compatibility option.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Publisher styling", fontWeight = FontWeight.SemiBold)
                Text(
                    "Keep the book's original typography and layout when possible.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = appearance.publisherStyles,
                onCheckedChange = { onChange(appearance.copy(publisherStyles = it)) },
                modifier = Modifier.semantics { contentDescription = "Publisher styling" }
            )
        }

        onDone?.let { done ->
            Button(onClick = done, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Back to reading")
            }
        }
    }
}

@Composable
private fun AppearancePreset(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.heightIn(min = 48.dp)
    )
}
