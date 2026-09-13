package com.veilreader.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.veilreader.app.domain.EstateState
import com.veilreader.app.domain.ReaderAppearance
import com.veilreader.app.domain.ReaderTheme
import com.veilreader.app.ui.VeilWorldBackdrop

/** Optional, local setup. No account, permissions carousel, or synthetic book is required. */
@Composable
fun WelcomeScreen(
    gameVisible: Boolean,
    onStart: (Boolean) -> Unit,
    onSkip: () -> Unit,
    onRestore: () -> Unit
) {
    var world by rememberSaveable { mutableStateOf(gameVisible) }
    BackHandler(onBack = onSkip)
    VeilWorldBackdrop {
        Column(Modifier.fillMaxHeight().widthIn(max = 660.dp).fillMaxWidth().systemBarsPadding()
            .align(Alignment.TopCenter).verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("VEIL READER", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = onSkip, modifier = Modifier.heightIn(min = 48.dp)) { Text("Skip setup") }
            }
            Text("A little reading.\nA world of your own.", style = MaterialTheme.typography.headlineLarge)
            Text("A quiet home for your EPUBs and PDFs. Read beautifully, save the words that matter, and come back to your place.",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (world) {
                EstateScene(EstateState(), Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge))
            } else {
                ReaderAppearancePreview(ReaderAppearance(theme = ReaderTheme.SEPIA))
            }
            Text("Choose your atmosphere", style = MaterialTheme.typography.titleLarge)
            WelcomeChoice("Reading & a world", "Begin in a wayside shack. Reading earns the materials to build your castle and discover the Lantern Road.", world) { world = true }
            WelcomeChoice("Quiet reading", "Keep the focus on your books. Hide the game screens and bring them back whenever you like.", !world) { world = false }
            Button(onClick = { onStart(world) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text("Open my library")
            }
            Text("Import a book from Android Files. Inside a book, tap the center for controls, then Appearance to choose Slide, 3D curl, Instant, or Scroll for EPUB.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Restore an existing backup") }
            Text("Private by default · No account needed · Change any choice in Settings", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WelcomeChoice(title: String, detail: String, chosen: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().semantics { role = Role.RadioButton; selected = chosen },
        color = if (chosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.large) {
        Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = chosen, onClick = null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
