package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veilreader.app.domain.*

@Composable
internal fun ReaderAppearancePanel(
    appearance: ReaderAppearance,
    onChange: (ReaderAppearance) -> Unit,
    onDone: () -> Unit,
    doneLabel: String = "Back to reading",
    reflowable: Boolean = true
) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (reflowable) "Make the page yours" else "Reading comfort", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Tune the page once, then get back to the book. These choices stay on your device.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (reflowable) {
            ReaderAppearancePreview(appearance)
            Text("Reading mode", style = MaterialTheme.typography.titleLarge)
            ReaderModeChoices(appearance, onChange)

            Text("Presets", fontWeight = FontWeight.SemiBold)
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
                            publisherStyles = true,
                            font = ReaderFont.ORIGINAL,
                            justified = false
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
                            publisherStyles = false,
                            font = ReaderFont.SERIF,
                            justified = false
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
                            publisherStyles = false,
                            font = ReaderFont.SERIF,
                            justified = false
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
                            publisherStyles = false,
                            font = ReaderFont.SERIF,
                            justified = false
                        )
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Text("Text size · ${(appearance.fontScale * 100).toInt()}%", fontWeight = FontWeight.SemiBold)
            Slider(
                modifier = Modifier.semantics { contentDescription = "Text size" },
                value = appearance.fontScale.toFloat(),
                onValueChange = { onChange(appearance.copy(fontScale = it.toDouble(), publisherStyles = false)) },
                valueRange = .75f..1.8f
            )

            Text("Line height · ${"%.2f".format(appearance.lineHeight)}", fontWeight = FontWeight.SemiBold)
            Slider(
                modifier = Modifier.semantics { contentDescription = "Line height" },
                value = appearance.lineHeight.toFloat(),
                onValueChange = { onChange(appearance.copy(lineHeight = it.toDouble(), publisherStyles = false)) },
                valueRange = 1.1f..2.0f
            )

            Text("Page margins · ${"%.2f".format(appearance.pageMargins)}", fontWeight = FontWeight.SemiBold)
            Slider(
                modifier = Modifier.semantics { contentDescription = "Page margins" },
                value = appearance.pageMargins.toFloat(),
                onValueChange = { onChange(appearance.copy(pageMargins = it.toDouble(), publisherStyles = false)) },
                valueRange = .5f..2.0f
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Text("Typeface", style = MaterialTheme.typography.titleLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReaderFont.entries.forEach { font ->
                    FilterChip(selected = appearance.font == font,
                        onClick = { onChange(appearance.copy(font = font, publisherStyles = font == ReaderFont.ORIGINAL)) },
                        label = { Text(font.label) }, modifier = Modifier.heightIn(min = 48.dp))
                }
            }
            ReaderOption("Justified text", "Align both edges, where the book supports it.", appearance.justified) {
                onChange(appearance.copy(justified = it, publisherStyles = false))
            }
            ReaderOption("Publisher styling", "Keep the book's original typography and layout when possible.", appearance.publisherStyles) {
                onChange(appearance.copy(publisherStyles = it, font = if (it) ReaderFont.ORIGINAL else appearance.font))
            }
        } else {
            Text("PDF pages keep their original layout. Font, paper, and page-turn styles are available for EPUB books.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ReaderOption("Keep the screen awake", "Only while a book is open.", appearance.keepScreenOn) {
            onChange(appearance.copy(keepScreenOn = it))
        }
        ReaderOption("Reduce motion", "Remove decorative transitions and EPUB page animations. Android’s animation setting is also respected.", appearance.reduceMotion) {
            onChange(appearance.copy(reduceMotion = it))
        }

        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(doneLabel)
        }
    }
}

@Composable
private fun AppearancePreset(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
internal fun ReaderAppearancePreview(appearance: ReaderAppearance) {
    val paper = when (appearance.theme) {
        ReaderTheme.PAPER -> Color(0xFFFFFCF6)
        ReaderTheme.SEPIA -> Color(0xFFF1E5CC)
        ReaderTheme.DUSK -> Color(0xFF18151D)
        ReaderTheme.OLED -> Color.Black
    }
    val ink = if (appearance.theme in listOf(ReaderTheme.DUSK, ReaderTheme.OLED)) Color(0xFFE5DECF) else Color(0xFF332C26)
    val typeface = when (appearance.font) {
        ReaderFont.ORIGINAL, ReaderFont.SERIF -> FontFamily.Serif
        ReaderFont.SANS -> FontFamily.SansSerif
        ReaderFont.MONO -> FontFamily.Monospace
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(color = paper, contentColor = ink, shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, ink.copy(alpha = .18f)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = (20 * appearance.pageMargins).dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("THE LANTERN ROAD", style = MaterialTheme.typography.labelSmall, color = ink.copy(alpha = .75f))
                Text("A room of your own", fontFamily = typeface, fontSize = (23 * appearance.fontScale).sp,
                    lineHeight = (28 * appearance.fontScale).sp)
                Text("The lamp was already lit when you arrived. Beyond the window, the valley settled into evening. You opened a book, and the little room became a world.",
                    fontFamily = typeface, fontSize = (17 * appearance.fontScale).sp,
                    lineHeight = (17 * appearance.fontScale * appearance.lineHeight).sp,
                    textAlign = if (appearance.justified) TextAlign.Justify else TextAlign.Start)
            }
        }
        Text("Sample preview · A book’s own styling may look different.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
