package com.veilreader.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.domain.ReaderTtsSettings
import com.veilreader.app.ui.reader.tts.ReaderTtsPhase
import com.veilreader.app.ui.reader.tts.ReaderTtsProblem
import com.veilreader.app.ui.reader.tts.ReaderTtsState
import com.veilreader.app.ui.theme.VeilSpacing

/** Foreground controls over the sole owned speech session; no visual locator/progress writer. */
@Composable
internal fun ReaderTtsControls(
    state: ReaderTtsState,
    supported: Boolean,
    settings: ReaderTtsSettings,
    startPending: Boolean,
    startFailed: Boolean,
    onStart: () -> Unit,
    onResume: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onSettingsChange: (ReaderTtsSettings) -> Unit,
    onDone: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.extraSmall) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(VeilSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
        ) {
            Text(stringResource(R.string.tts_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.tts_intro), style = MaterialTheme.typography.bodyMedium)
            val problem = when {
                !supported -> R.string.tts_unsupported
                startFailed -> R.string.tts_playback_failed
                state.problem == ReaderTtsProblem.NO_OFFLINE_VOICE -> R.string.tts_missing_voice
                state.problem == ReaderTtsProblem.NO_ENGINE -> R.string.tts_initialization_failed
                state.problem != null -> R.string.tts_playback_failed
                else -> null
            }
            problem?.let {
                Text(stringResource(it), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            if (startPending || state.phase == ReaderTtsPhase.PREPARING) {
                Text(stringResource(R.string.tts_preparing), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            TextButton(onClick = onStart, enabled = supported && !startPending && state.phase != ReaderTtsPhase.CLOSED,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.tts_start))
            }
            if (state.phase == ReaderTtsPhase.PLAYING || state.phase == ReaderTtsPhase.PREPARING) {
                TextButton(onClick = onPause, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.tts_pause))
                }
            } else if (state.phase == ReaderTtsPhase.PAUSED) {
                TextButton(onClick = onResume, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.tts_play))
                }
            }
            TextButton(onClick = onStop, enabled = startPending || state.phase !in setOf(ReaderTtsPhase.STOPPED, ReaderTtsPhase.CLOSED),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.tts_stop))
            }
            val number = rememberVeilNumberFormatter()
            val speedLabel = stringResource(R.string.tts_speed)
            Text("$speedLabel: ${number(settings.speed)}", style = MaterialTheme.typography.labelLarge)
            Slider(value = settings.speed.toFloat(), valueRange = 0.5f..2f,
                onValueChange = { onSettingsChange(settings.copy(speed = it.toDouble())) },
                modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = speedLabel })
            val pitchLabel = stringResource(R.string.tts_pitch)
            Text("$pitchLabel: ${number(settings.pitch)}", style = MaterialTheme.typography.labelLarge)
            Slider(value = settings.pitch.toFloat(), valueRange = 0.6f..1.4f,
                onValueChange = { onSettingsChange(settings.copy(pitch = it.toDouble())) },
                modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = pitchLabel })
            TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.reader_image_viewer_close))
            }
        }
    }
}
