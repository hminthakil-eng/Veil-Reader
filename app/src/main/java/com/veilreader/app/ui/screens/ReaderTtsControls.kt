package com.veilreader.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
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
import com.veilreader.app.ui.reader.tts.ReaderTtsVoice
import com.veilreader.app.ui.theme.VeilSpacing
import java.util.Locale

/** Controls over the sole owned speech session; no visual locator/progress writer. */
@Composable
internal fun ReaderTtsControls(
    state: ReaderTtsState,
    supported: Boolean,
    settings: ReaderTtsSettings,
    startPending: Boolean,
    startFailed: Boolean,
    publicationLanguage: String? = null,
    voiceCatalogSupported: Boolean = false,
    voices: List<ReaderTtsVoice> = emptyList(),
    voiceCatalogLoading: Boolean = false,
    voiceCatalogProblem: ReaderTtsProblem? = null,
    previewProblem: ReaderTtsProblem? = null,
    sleepDeadlineEpochMs: Long? = null,
    onSetSleepTimer: (Int) -> Unit = {},
    listeningPositionAvailable: Boolean = false,
    onSyncListeningPosition: () -> Unit = {},
    onStart: () -> Unit,
    onResume: () -> Unit,
    onPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
    onRefreshVoices: () -> Unit = {},
    onPreviewVoice: (languageTag: String, voiceId: String, sample: String) -> Unit =
        { _, _, _ -> },
    onSettingsChange: (ReaderTtsSettings) -> Unit,
    onDone: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.extraSmall) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(VeilSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
        ) {
            Text(stringResource(R.string.tts_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.tts_intro), style = MaterialTheme.typography.bodyMedium)

            val problem = when {
                !supported -> R.string.tts_unsupported
                startFailed -> R.string.tts_playback_failed
                state.problem == ReaderTtsProblem.NO_OFFLINE_VOICE -> R.string.tts_missing_voice
                state.problem == ReaderTtsProblem.PREFERRED_VOICE_UNAVAILABLE ->
                    R.string.tts_preferred_voice_unavailable
                state.problem == ReaderTtsProblem.NO_ENGINE -> R.string.tts_initialization_failed
                state.problem != null -> R.string.tts_playback_failed
                else -> null
            }
            problem?.let {
                Text(
                    stringResource(it),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }

            if (startPending || state.phase == ReaderTtsPhase.PREPARING) {
                Text(
                    stringResource(R.string.tts_preparing),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }

            if (listeningPositionAvailable) {
                Text(
                    stringResource(R.string.tts_listening_position_hint),
                    style = MaterialTheme.typography.bodySmall
                )
                TextButton(
                    onClick = onSyncListeningPosition,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.tts_go_to_listening_position))
                }
            }

            if (voiceCatalogSupported) {
                if (voiceCatalogLoading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(
                        stringResource(R.string.tts_loading_voices),
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    ReaderTtsVoicePicker(
                        settings = settings,
                        publicationLanguage = publicationLanguage,
                        voices = voices,
                        voiceCatalogProblem = voiceCatalogProblem,
                        previewProblem = previewProblem,
                        onRefreshVoices = onRefreshVoices,
                        onPreviewVoice = onPreviewVoice,
                        onSettingsChange = onSettingsChange
                    )
                }
                HorizontalDivider()
            }

            TextButton(
                onClick = onStart,
                enabled = supported && !startPending && state.phase != ReaderTtsPhase.CLOSED,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.tts_start))
            }

            if (state.phase == ReaderTtsPhase.PLAYING || state.phase == ReaderTtsPhase.PREPARING) {
                TextButton(
                    onClick = onPause,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.tts_pause))
                }
            } else if (state.phase == ReaderTtsPhase.PAUSED) {
                TextButton(
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.tts_play))
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
            ) {
                TextButton(
                    onClick = onPrevious,
                    enabled = state.phase !in setOf(
                        ReaderTtsPhase.STOPPED,
                        ReaderTtsPhase.CLOSED,
                        ReaderTtsPhase.FAILED
                    ),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.tts_previous_segment))
                }
                TextButton(
                    onClick = onNext,
                    enabled = state.phase !in setOf(
                        ReaderTtsPhase.STOPPED,
                        ReaderTtsPhase.CLOSED,
                        ReaderTtsPhase.FAILED
                    ),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.tts_next_segment))
                }
            }

            TextButton(
                onClick = onStop,
                enabled = startPending || state.phase !in setOf(
                    ReaderTtsPhase.STOPPED,
                    ReaderTtsPhase.CLOSED
                ),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.tts_stop))
            }

            var sleepMenu by remember { mutableStateOf(false) }
            Text(
                stringResource(R.string.tts_sleep_timer),
                style = MaterialTheme.typography.labelLarge
            )
            Box(Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = { sleepMenu = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text(
                        if (
                            sleepDeadlineEpochMs != null &&
                            sleepDeadlineEpochMs > System.currentTimeMillis()
                        ) {
                            stringResource(R.string.tts_sleep_timer_active)
                        } else {
                            stringResource(R.string.tts_sleep_timer_off)
                        }
                    )
                }
                DropdownMenu(
                    expanded = sleepMenu,
                    onDismissRequest = { sleepMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.tts_sleep_timer_off)) },
                        onClick = {
                            onSetSleepTimer(0)
                            sleepMenu = false
                        }
                    )
                    listOf(15, 30, 45, 60).forEach { minutes ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        R.string.tts_sleep_timer_minutes,
                                        minutes
                                    )
                                )
                            },
                            onClick = {
                                onSetSleepTimer(minutes)
                                sleepMenu = false
                            }
                        )
                    }
                }
            }

            val number = rememberVeilNumberFormatter()
            val speedLabel = stringResource(R.string.tts_speed)
            Text(
                "$speedLabel: ${number(settings.speed)}",
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = settings.speed.toFloat(),
                valueRange = 0.5f..3f,
                onValueChange = {
                    onSettingsChange(settings.copy(speed = it.toDouble()))
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = speedLabel }
            )

            val pitchLabel = stringResource(R.string.tts_pitch)
            Text(
                "$pitchLabel: ${number(settings.pitch)}",
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = settings.pitch.toFloat(),
                valueRange = 0.6f..1.4f,
                onValueChange = {
                    onSettingsChange(settings.copy(pitch = it.toDouble()))
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = pitchLabel }
            )

            TextButton(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.reader_image_viewer_close))
            }
        }
    }
}

@Composable
private fun ReaderTtsVoicePicker(
    settings: ReaderTtsSettings,
    publicationLanguage: String?,
    voices: List<ReaderTtsVoice>,
    voiceCatalogProblem: ReaderTtsProblem?,
    previewProblem: ReaderTtsProblem?,
    onRefreshVoices: () -> Unit,
    onPreviewVoice: (languageTag: String, voiceId: String, sample: String) -> Unit,
    onSettingsChange: (ReaderTtsSettings) -> Unit
) {
    val offlineVoices = remember(voices) {
        voices.filter { it.installed && !it.requiresNetwork }
    }
    val languages = remember(offlineVoices) {
        offlineVoices
            .map { it.languageTag }
            .distinct()
            .sorted()
    }
    val preferredInitial = remember(publicationLanguage, languages) {
        val requested = publicationLanguage
            ?.let(Locale::forLanguageTag)
            ?.language
            ?.takeIf { it.isNotBlank() }
        languages.firstOrNull {
            Locale.forLanguageTag(it).language == requested
        } ?: languages.firstOrNull()
    }
    var selectedLanguage by remember(preferredInitial) {
        mutableStateOf(preferredInitial)
    }
    var languageMenu by remember { mutableStateOf(false) }
    var voiceMenu by remember { mutableStateOf(false) }

    Text(
        stringResource(R.string.tts_voice_section),
        style = MaterialTheme.typography.titleMedium
    )

    if (voices.isEmpty()) {
        Text(
            when (voiceCatalogProblem) {
                ReaderTtsProblem.NO_ENGINE -> stringResource(R.string.tts_initialization_failed)
                else -> stringResource(R.string.tts_voice_catalog_empty)
            },
            style = MaterialTheme.typography.bodySmall
        )
        TextButton(
            onClick = onRefreshVoices,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Text(stringResource(R.string.tts_refresh_voices))
        }
        return
    }

    val selected = selectedLanguage
    if (selected == null) {
        Text(
            stringResource(R.string.tts_missing_voice),
            style = MaterialTheme.typography.bodySmall
        )
        return
    }

    val currentLocale = Locale.forLanguageTag(selected)
    val currentVoices = offlineVoices.filter {
        Locale.forLanguageTag(it.languageTag).language == currentLocale.language
    }
    val preferredVoice = settings.preferredVoiceId(selected)
    val selectedVoice = currentVoices.firstOrNull { it.id == preferredVoice }

    Text(
        stringResource(R.string.tts_language),
        style = MaterialTheme.typography.labelLarge
    )
    Box(Modifier.fillMaxWidth()) {
        TextButton(
            onClick = { languageMenu = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Text(displayLanguage(selected))
        }
        DropdownMenu(
            expanded = languageMenu,
            onDismissRequest = { languageMenu = false }
        ) {
            languages.forEach { tag ->
                DropdownMenuItem(
                    text = { Text(displayLanguage(tag)) },
                    onClick = {
                        selectedLanguage = tag
                        languageMenu = false
                    }
                )
            }
        }
    }

    Text(
        stringResource(R.string.tts_voice),
        style = MaterialTheme.typography.labelLarge
    )
    Box(Modifier.fillMaxWidth()) {
        TextButton(
            onClick = { voiceMenu = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Text(
                selectedVoice?.id ?: stringResource(R.string.tts_voice_automatic),
                maxLines = 2
            )
        }
        DropdownMenu(
            expanded = voiceMenu,
            onDismissRequest = { voiceMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.tts_voice_automatic)) },
                onClick = {
                    onSettingsChange(settings.withPreferredVoice(selected, null))
                    voiceMenu = false
                }
            )
            currentVoices.forEach { voice ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(voice.id)
                            Text(
                                voice.languageTag,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    onClick = {
                        onSettingsChange(
                            settings.withPreferredVoice(selected, voice.id)
                        )
                        voiceMenu = false
                    }
                )
            }
        }
    }

    val previewVoice = selectedVoice ?: currentVoices.firstOrNull()
    if (previewVoice != null) {
        val sample = when (currentLocale.language) {
            "fa" -> stringResource(R.string.tts_preview_sample_fa)
            "ar" -> stringResource(R.string.tts_preview_sample_ar)
            else -> stringResource(R.string.tts_preview_sample_default)
        }
        TextButton(
            onClick = {
                onPreviewVoice(selected, previewVoice.id, sample)
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Text(stringResource(R.string.tts_preview_voice))
        }
    }

    if (preferredVoice != null && selectedVoice == null) {
        Text(
            stringResource(R.string.tts_preferred_voice_unavailable),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodySmall
        )
    }
    if (previewProblem != null) {
        Text(
            stringResource(R.string.tts_preview_failed),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun displayLanguage(languageTag: String): String {
    val locale = Locale.forLanguageTag(languageTag)
    val display = locale.getDisplayName(LocalLocale.current.platformLocale)
    return display.takeIf { it.isNotBlank() } ?: languageTag
}
