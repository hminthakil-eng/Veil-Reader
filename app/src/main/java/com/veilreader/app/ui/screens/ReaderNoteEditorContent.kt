package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.domain.Highlight
import com.veilreader.app.ui.theme.withVeilContentScript

/** The parent owns awaited persistence; editing can scroll without moving Save and Cancel. */
@Composable
internal fun ReaderNoteEditorContent(
    highlight: Highlight,
    note: String,
    onNoteChange: (String) -> Unit,
    saving: Boolean,
    errorRes: Int?,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(modifier.fillMaxSize().padding(16.dp)) {
      val shortWindow = maxHeight < 260.dp
      val shortWideWindow = shortWindow && maxWidth >= 448.dp
      Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!shortWindow) Text(stringResource(R.string.reader_notebook_passage_note),
            color = colors.onSurface, style = MaterialTheme.typography.titleLarge)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val quote = highlight.quote.trim()
            if (!shortWindow && quote.isNotBlank()) Surface(color = colors.surfaceVariant,
                shape = MaterialTheme.shapes.small, border = BorderStroke(1.dp, colors.outlineVariant)) {
                Text("“$quote”", Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium.withVeilContentScript(quote),
                    color = colors.onSurface, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
            OutlinedTextField(value = note, onValueChange = onNoteChange,
                label = { Text(stringResource(R.string.reader_notebook_your_thoughts)) },
                minLines = 4, maxLines = 8, enabled = !saving,
                modifier = Modifier.fillMaxWidth())
            errorRes?.let { Text(stringResource(it),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
        }
        val cancelAction: @Composable (Modifier) -> Unit = { actionModifier ->
            OutlinedButton(onClick = onCancel, enabled = !saving,
                modifier = actionModifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.common_cancel))
            }
        }
        val saveAction: @Composable (Modifier) -> Unit = { actionModifier ->
            Button(onClick = onSave, enabled = !saving,
                modifier = actionModifier.heightIn(min = 48.dp)) {
                Text(stringResource(if (saving) R.string.reader_notebook_saving else R.string.common_save))
            }
        }
        if (shortWideWindow) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            cancelAction(Modifier.weight(1f))
            saveAction(Modifier.weight(1f))
        } else VeilAdaptiveDialogActions(spacing = 8.dp, first = cancelAction, second = saveAction)
      }
    }
}
