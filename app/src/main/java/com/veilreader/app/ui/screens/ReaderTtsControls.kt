package com.veilreader.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.domain.ReaderTtsSettings
import com.veilreader.app.ui.reader.ReaderTtsError
import com.veilreader.app.ui.reader.ReaderTtsUiState
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSpacing

@Composable
internal fun ReaderTtsControls(
    state: ReaderTtsUiState,
    settings: ReaderTtsSettings,
    onStart: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
    onSettingsChange: (ReaderTtsSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val formatNumber = rememberVeilNumberFormatter()
    var speedDraft by remember { mutableFloatStateOf(settings.speed.toFloat()) }
    var pitchDraft by remember { mutableFloatStateOf(settings.pitch.toFloat()) }

    LaunchedEffect(settings) {
        speedDraft = settings.speed.toFloat()
        pitchDraft = settings.pitch.toFloat()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)) {
            Text(
                stringResource(R.string.tts_title),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                stringResource(R.string.tts_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when {
            !state.supported -> {
                TtsNotice(stringResource(R.string.tts_unsupported))
            }

            state.error != null -> {
                TtsNotice(
                    when (state.error) {
                        ReaderTtsError.UNSUPPORTED_PUBLICATION ->
                            stringResource(R.string.tts_unsupported)
                        ReaderTtsError.INITIALIZATION ->
                            stringResource(R.string.tts_initialization_failed)
                        ReaderTtsError.PLAYBACK ->
                            stringResource(R.string.tts_playback_failed)
                    }
                )
            }
        }

        if (state.starting) {
            Text(
                stringResource(R.string.tts_preparing),
                color = VeilPalette.Brass,
                style = MaterialTheme.typography.labelLarge
            )
        }

        if (!state.active) {
            Button(
                onClick = onStart,
                enabled = state.supported && !state.starting,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VeilPalette.Brass,
                    contentColor = VeilPalette.Ink
                )
            ) {
                Text(stringResource(R.string.tts_start))
            }
        } else {
            if (state.utterance.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = VeilPalette.Archive.copy(alpha = 0.54f),
                    border = BorderStroke(
                        1.dp,
                        VeilPalette.Brass.copy(alpha = 0.26f)
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            stringResource(R.string.tts_current),
                            style = MaterialTheme.typography.labelSmall,
                            color = VeilPalette.Brass
                        )
                        Text(
                            state.utterance,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(
                    onClick = onPrevious,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text("‹")
                }
                Button(
                    onClick = onPlayPause,
                    modifier = Modifier.weight(1.6f).heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VeilPalette.Brass,
                        contentColor = VeilPalette.Ink
                    )
                ) {
                    Text(
                        stringResource(
                            if (state.playing) R.string.tts_pause
                            else R.string.tts_play
                        )
                    )
                }
                TextButton(
                    onClick = onNext,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text("›")
                }
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.weight(1.2f).heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.tts_stop))
                }
            }
        }

        Text(
            stringResource(R.string.tts_speed),
            style = MaterialTheme.typography.labelLarge
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0.5×", style = MaterialTheme.typography.labelSmall)
            Text("${formatNumber(speedDraft)}×", color = VeilPalette.Brass)
            Text("2.0×", style = MaterialTheme.typography.labelSmall)
        }
        Slider(
            value = speedDraft,
            onValueChange = { speedDraft = it },
            onValueChangeFinished = {
                onSettingsChange(
                    settings.copy(speed = speedDraft.toDouble()).normalized()
                )
            },
            valueRange = 0.50f..2.00f
        )

        Text(
            stringResource(R.string.tts_pitch),
            style = MaterialTheme.typography.labelLarge
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0.6×", style = MaterialTheme.typography.labelSmall)
            Text("${formatNumber(pitchDraft)}×", color = VeilPalette.Brass)
            Text("1.4×", style = MaterialTheme.typography.labelSmall)
        }
        Slider(
            value = pitchDraft,
            onValueChange = { pitchDraft = it },
            onValueChangeFinished = {
                onSettingsChange(
                    settings.copy(pitch = pitchDraft.toDouble()).normalized()
                )
            },
            valueRange = 0.60f..1.40f
        )

        Text(
            stringResource(R.string.tts_follow_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TtsNotice(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        color = VeilPalette.Ink.copy(alpha = 0.24f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            text,
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
