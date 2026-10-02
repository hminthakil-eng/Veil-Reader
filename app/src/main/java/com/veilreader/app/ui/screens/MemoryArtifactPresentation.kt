package com.veilreader.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.domain.Highlight
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSpacing

internal enum class MemoryArtifactKind {
    PASSAGE,
    NOTE,
    ANNOTATED
}

internal fun memoryArtifactKind(highlight: Highlight): MemoryArtifactKind {
    val hasQuote = highlight.quote.isNotBlank()
    val hasNote = highlight.note.isNotBlank()
    return when {
        hasQuote && hasNote -> MemoryArtifactKind.ANNOTATED
        hasQuote -> MemoryArtifactKind.PASSAGE
        else -> MemoryArtifactKind.NOTE
    }
}

@Composable
internal fun MemoryArtifactContent(
    highlight: Highlight,
    noteEmphasis: Boolean = false,
    modifier: Modifier = Modifier
) {
    val kind = memoryArtifactKind(highlight)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
    ) {
        if (kind != MemoryArtifactKind.NOTE) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier
                        .width(2.dp)
                        .heightIn(min = 54.dp)
                        .background(VeilPalette.Brass.copy(alpha = 0.48f))
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    VeilMicroLabel(
                        text = stringResource(R.string.memory_artifact_passage),
                        color = VeilPalette.Mist.copy(alpha = 0.72f)
                    )
                    Text(
                        "“${highlight.quote.trim()}”",
                        style = if (noteEmphasis) {
                            MaterialTheme.typography.bodyMedium
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        color = VeilPalette.Moon.copy(alpha = 0.92f)
                    )
                }
            }
        }

        if (kind != MemoryArtifactKind.PASSAGE) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
            ) {
                Box(
                    Modifier
                        .width(2.dp)
                        .heightIn(min = 46.dp)
                        .background(VeilPalette.Brass.copy(alpha = 0.76f))
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    VeilMicroLabel(
                        text = stringResource(
                            if (kind == MemoryArtifactKind.NOTE) {
                                R.string.memory_artifact_note
                            } else {
                                R.string.memory_artifact_annotation
                            }
                        )
                    )
                    Text(
                        highlight.note.trim(),
                        style = if (noteEmphasis) {
                            MaterialTheme.typography.bodyLarge
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        color = VeilPalette.Moon
                    )
                }
            }
        }
    }
}
