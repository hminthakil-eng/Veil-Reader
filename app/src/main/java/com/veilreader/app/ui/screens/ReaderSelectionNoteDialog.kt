package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.veilreader.app.ui.theme.VeilSpacing

/** Reader owns the draft and awaited write; this native surface owns only presentation. */
@Composable
internal fun ReaderSelectionNoteDialog(
    quote: String,
    note: String,
    onNoteChange: (String) -> Unit,
    saving: Boolean,
    saveEnabled: Boolean,
    errorRes: Int?,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!saving) onCancel() },
        properties = DialogProperties(
            dismissOnBackPress = !saving,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .padding(VeilSpacing.lg),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()
                    .heightIn(max = 640.dp).fillMaxHeight(.98f),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                ReaderNoteEditorContent(
                    quote = quote,
                    note = note,
                    onNoteChange = onNoteChange,
                    saving = saving,
                    saveEnabled = saveEnabled,
                    errorRes = errorRes,
                    onCancel = onCancel,
                    onSave = onSave
                )
            }
        }
    }
}
