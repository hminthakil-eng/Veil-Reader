package com.veilreader.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
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
    val continuousScrollLabel = stringResource(R.string.reading_continuous_scroll)
    val publisherStylingLabel = stringResource(R.string.reading_publisher_styling)
    val textSizeLabel = stringResource(R.string.reading_text_size)
    val lineHeightLabel = stringResource(R.string.reading_line_height)
    val pageMarginsLabel = stringResource(R.string.reading_page_margins)

    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.reading_appearance_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.reading_appearance_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Text(
            stringResource(R.string.reading_page_color),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            stringResource(R.string.reading_page_color_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        ReaderTheme.entries.chunked(2).forEach { themes ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                themes.forEach { theme ->
                    PageColorChoice(
                        label = stringResource(
                            when (theme) {
                                ReaderTheme.PAPER -> R.string.reader_theme_paper
                                ReaderTheme.SEPIA -> R.string.reader_theme_sepia
                                ReaderTheme.DUSK -> R.string.reader_theme_dusk
                                ReaderTheme.OLED -> R.string.reader_theme_oled
                            }
                        ),
                        selected = !appearance.publisherStyles && appearance.theme == theme,
                        modifier = Modifier.weight(1f),
                        onClick = { onChange(appearance.withPageTheme(theme)) }
                    )
                }
            }
        }
        if (appearance.publisherStyles) {
            Text(
                stringResource(R.string.reading_page_color_publisher_active),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        Text(
            stringResource(
                R.string.reading_text_size_value,
                (appearance.fontScale * 100).toInt()
            ),
            fontWeight = FontWeight.SemiBold
        )
        Slider(
            modifier = Modifier.semantics { contentDescription = textSizeLabel },
            value = appearance.fontScale.toFloat(),
            onValueChange = {
                onChange(
                    appearance.copy(
                        fontScale = it.toDouble(),
                        publisherStyles = false
                    )
                )
            },
            valueRange = .75f..1.8f
        )

        Text(
            stringResource(R.string.reading_line_height_value, appearance.lineHeight),
            fontWeight = FontWeight.SemiBold
        )
        Slider(
            modifier = Modifier.semantics { contentDescription = lineHeightLabel },
            value = appearance.lineHeight.toFloat(),
            onValueChange = {
                onChange(
                    appearance.copy(
                        lineHeight = it.toDouble(),
                        publisherStyles = false
                    )
                )
            },
            valueRange = 1.1f..2.0f
        )

        Text(
            stringResource(R.string.reading_page_margins_value, appearance.pageMargins),
            fontWeight = FontWeight.SemiBold
        )
        Slider(
            modifier = Modifier.semantics { contentDescription = pageMarginsLabel },
            value = appearance.pageMargins.toFloat(),
            onValueChange = {
                onChange(
                    appearance.copy(
                        pageMargins = it.toDouble(),
                        publisherStyles = false
                    )
                )
            },
            valueRange = .5f..2.0f
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    continuousScrollLabel,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.reading_continuous_scroll_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = appearance.scroll,
                onCheckedChange = { onChange(appearance.copy(scroll = it)) },
                modifier = Modifier.semantics {
                    contentDescription = continuousScrollLabel
                }
            )
        }

        if (!appearance.scroll) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.reading_page_turn),
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = appearance.pageTurnStyle == PageTurnStyle.PAPER,
                        onClick = {
                            onChange(
                                appearance.copy(pageTurnStyle = PageTurnStyle.PAPER)
                            )
                        },
                        label = {
                            Text(stringResource(R.string.reading_page_turn_paper_curl))
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    )
                    FilterChip(
                        selected = appearance.pageTurnStyle == PageTurnStyle.SLIDE,
                        onClick = {
                            onChange(
                                appearance.copy(pageTurnStyle = PageTurnStyle.SLIDE)
                            )
                        },
                        label = {
                            Text(stringResource(R.string.reading_page_turn_simple_slide))
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    )
                }
                Text(
                    stringResource(R.string.reading_page_turn_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    publisherStylingLabel,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.reading_publisher_styling_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = appearance.publisherStyles,
                onCheckedChange = {
                    onChange(appearance.copy(publisherStyles = it))
                },
                modifier = Modifier.semantics {
                    contentDescription = publisherStylingLabel
                }
            )
        }

        onDone?.let { done ->
            Button(
                onClick = done,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) {
                Text(stringResource(R.string.reading_back_to_reading))
            }
        }
    }
}

@Composable
private fun PageColorChoice(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier.heightIn(min = 48.dp)
    )
}
