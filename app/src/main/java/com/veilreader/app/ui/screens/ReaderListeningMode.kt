package com.veilreader.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.domain.Book
import com.veilreader.app.domain.ReaderTtsSettings
import com.veilreader.app.ui.books.bookArtifactState
import com.veilreader.app.ui.reader.tts.ReaderTtsPhase
import com.veilreader.app.ui.reader.tts.ReaderTtsProblem
import com.veilreader.app.ui.reader.tts.ReaderTtsState
import com.veilreader.app.ui.reader.tts.ReaderTtsVoice
import com.veilreader.app.ui.reader.tts.selectOfflineTtsVoice
import com.veilreader.app.ui.theme.VeilMaterials
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.withVeilContentScript
import java.util.Locale

/**
 * First-class listening destination.
 *
 * This surface deliberately keeps Reader progress and listening progress separate. It is a visual
 * controller over the same service-owned TTS session; it never writes a visual Reader locator.
 */
@Composable
internal fun ReaderListeningMode(
    book: Book,
    state: ReaderTtsState,
    activeText: String? = null,
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
    var showAdvanced by remember { mutableStateOf(false) }
    BackHandler {
        if (showAdvanced) showAdvanced = false else onDone()
    }

    val preferredVoiceId = settings.preferredVoiceId(publicationLanguage)
    val currentVoice = remember(voices, preferredVoiceId, publicationLanguage) {
        publicationLanguage?.let { selectOfflineTtsVoice(voices, it, preferredVoiceId) }
    }
    val previousLabel = stringResourceCompat(R.string.tts_previous_segment)
    val nextLabel = stringResourceCompat(R.string.tts_next_segment)
    val playLabel = stringResourceCompat(R.string.tts_play)
    val pauseLabel = stringResourceCompat(R.string.tts_pause)
    val problemLabel = when {
        !supported -> stringResourceCompat(R.string.tts_unsupported)
        startFailed -> stringResourceCompat(R.string.tts_playback_failed)
        state.problem == ReaderTtsProblem.NO_OFFLINE_VOICE ->
            stringResourceCompat(R.string.tts_missing_voice)
        state.problem == ReaderTtsProblem.PREFERRED_VOICE_UNAVAILABLE ->
            stringResourceCompat(R.string.tts_preferred_voice_unavailable)
        state.problem == ReaderTtsProblem.NO_ENGINE ->
            stringResourceCompat(R.string.tts_initialization_failed)
        state.problem != null -> stringResourceCompat(R.string.tts_playback_failed)
        else -> null
    }
    val chapterLabel = state.sourceLocator?.title
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: book.currentChapter.takeUnless { it.isBlank() || it == "Not started" }
        ?: book.title

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to VeilPalette.Archive,
                    0.42f to VeilPalette.Ink,
                    1f to VeilPalette.Ink
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VeilSpacing.lg, vertical = VeilSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = onDone,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(stringResourceCompat(R.string.tts_listening_mode_close))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResourceCompat(R.string.tts_listening_mode_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = VeilPalette.Moon
                    )
                    Text(
                        stringResourceCompat(R.string.tts_listening_mode_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = VeilMaterials.TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(VeilSpacing.xs))

            BookCover(
                title = book.title,
                subtitle = book.author,
                imagePath = book.coverCachePath,
                artifact = remember(book) { bookArtifactState(book) },
                focusArtifact = true,
                modifier = Modifier
                    .width(184.dp)
                    .aspectRatio(0.69f)
            )

            Text(
                book.title,
                style = MaterialTheme.typography.headlineSmall.withVeilContentScript(book.title),
                color = VeilPalette.Moon,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (book.author.isNotBlank()) {
                Text(
                    book.author,
                    style = MaterialTheme.typography.bodyMedium.withVeilContentScript(book.author),
                    color = VeilMaterials.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth(),
                color = VeilPalette.Archive.copy(alpha = 0.72f),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 0.dp
            ) {
                Column(
                    Modifier.padding(VeilSpacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
                ) {
                    Text(
                        stringResourceCompat(R.string.tts_listening_mode_position),
                        style = MaterialTheme.typography.labelMedium,
                        color = VeilPalette.Brass
                    )
                    Text(
                        chapterLabel,
                        style = MaterialTheme.typography.titleMedium.withVeilContentScript(chapterLabel),
                        color = VeilPalette.Moon,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    activeText
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { spoken ->
                            HorizontalDivider(
                                color = VeilPalette.Brass.copy(alpha = 0.22f)
                            )
                            Text(
                                spoken,
                                style = MaterialTheme.typography.bodyLarge.withVeilContentScript(spoken),
                                color = VeilPalette.Moon.copy(alpha = 0.92f),
                                textAlign = TextAlign.Start,
                                maxLines = 7,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    if (state.phase == ReaderTtsPhase.PREPARING || startPending) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = VeilPalette.Brass,
                            trackColor = VeilPalette.Brass.copy(alpha = 0.14f)
                        )
                    }
                }
            }

            Row(
                Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = state.phase !in setOf(
                        ReaderTtsPhase.STOPPED,
                        ReaderTtsPhase.CLOSED,
                        ReaderTtsPhase.FAILED
                    ),
                    modifier = Modifier
                        .size(64.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = previousLabel
                        },
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("‹", style = MaterialTheme.typography.headlineLarge)
                }

                FilledTonalButton(
                    onClick = {
                        when (state.phase) {
                            ReaderTtsPhase.PLAYING,
                            ReaderTtsPhase.PREPARING -> onPause()
                            ReaderTtsPhase.PAUSED -> onResume()
                            ReaderTtsPhase.STOPPED,
                            ReaderTtsPhase.ENDED,
                            ReaderTtsPhase.FAILED,
                            ReaderTtsPhase.CLOSED -> onStart()
                        }
                    },
                    enabled = supported && !startPending && state.phase != ReaderTtsPhase.CLOSED,
                    modifier = Modifier
                        .size(84.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription =
                                if (state.phase == ReaderTtsPhase.PLAYING ||
                                    state.phase == ReaderTtsPhase.PREPARING
                                ) pauseLabel else playLabel
                        },
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        if (
                            state.phase == ReaderTtsPhase.PLAYING ||
                            state.phase == ReaderTtsPhase.PREPARING
                        ) "Ⅱ" else "▶",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                OutlinedButton(
                    onClick = onNext,
                    enabled = state.phase !in setOf(
                        ReaderTtsPhase.STOPPED,
                        ReaderTtsPhase.CLOSED,
                        ReaderTtsPhase.FAILED
                    ),
                    modifier = Modifier
                        .size(64.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = nextLabel
                        },
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("›", style = MaterialTheme.typography.headlineLarge)
                }
            }

            val number = rememberVeilNumberFormatter()
            val speedPresets = listOf(0.8, 1.0, 1.25, 1.5, 2.0, 3.0)
            Row(
                Modifier
                    .widthIn(max = 620.dp)
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(VeilSpacing.xs)
            ) {
                speedPresets.forEach { speed ->
                    FilterChip(
                        selected = kotlin.math.abs(settings.speed - speed) < 0.01,
                        onClick = {
                            onSettingsChange(settings.copy(speed = speed))
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                        label = { Text("${number(speed)}×") }
                    )
                }
            }

            Surface(
                onClick = { showAdvanced = true },
                modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth(),
                color = VeilPalette.Archive.copy(alpha = 0.86f),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    Modifier.padding(horizontal = VeilSpacing.md, vertical = VeilSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResourceCompat(R.string.tts_voice_section),
                            style = MaterialTheme.typography.labelMedium,
                            color = VeilPalette.Brass
                        )
                        Text(
                            currentVoice?.let { friendlyVoiceName(it.id) }
                                ?: stringResourceCompat(
                                    if (preferredVoiceId != null)
                                        R.string.tts_preferred_voice_unavailable
                                    else R.string.tts_voice_automatic
                                ),
                            style = MaterialTheme.typography.titleMedium,
                            color = VeilPalette.Moon,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        stringResourceCompat(R.string.tts_listening_mode_settings),
                        style = MaterialTheme.typography.labelLarge,
                        color = VeilPalette.Brass
                    )
                }
            }

            if (sleepDeadlineEpochMs != null && sleepDeadlineEpochMs > System.currentTimeMillis()) {
                AssistChip(
                    onClick = { onSetSleepTimer(0) },
                    label = { Text(stringResourceCompat(R.string.tts_sleep_timer_active)) }
                )
            }

            if (listeningPositionAvailable) {
                TextButton(
                    onClick = onSyncListeningPosition,
                    modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text(stringResourceCompat(R.string.tts_go_to_listening_position))
                }
            }

            if (problemLabel != null) {
                Text(
                    problemLabel,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            if (state.phase !in setOf(ReaderTtsPhase.STOPPED, ReaderTtsPhase.CLOSED)) {
                TextButton(
                    onClick = onStop,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(stringResourceCompat(R.string.tts_stop))
                }
            }

            Spacer(Modifier.height(VeilSpacing.lg))
        }

        if (showAdvanced) {
            Dialog(
                onDismissRequest = { showAdvanced = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(VeilPalette.Ink.copy(alpha = 0.88f))
                        .systemBarsPadding()
                        .padding(VeilSpacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.widthIn(max = 620.dp).fillMaxWidth()) {
                        ReaderTtsControls(
                            state = state,
                            supported = supported,
                            settings = settings,
                            startPending = startPending,
                            startFailed = startFailed,
                            publicationLanguage = publicationLanguage,
                            voiceCatalogSupported = voiceCatalogSupported,
                            voices = voices,
                            voiceCatalogLoading = voiceCatalogLoading,
                            voiceCatalogProblem = voiceCatalogProblem,
                            previewProblem = previewProblem,
                            sleepDeadlineEpochMs = sleepDeadlineEpochMs,
                            onSetSleepTimer = onSetSleepTimer,
                            listeningPositionAvailable = listeningPositionAvailable,
                            onSyncListeningPosition = onSyncListeningPosition,
                            onStart = onStart,
                            onResume = onResume,
                            onPause = onPause,
                            onPrevious = onPrevious,
                            onNext = onNext,
                            onStop = onStop,
                            onRefreshVoices = onRefreshVoices,
                            onPreviewVoice = onPreviewVoice,
                            onSettingsChange = onSettingsChange,
                            onDone = { showAdvanced = false }
                        )
                    }
                }
            }
        }
    }
}

private fun friendlyVoiceName(raw: String): String {
    val compact = raw
        .substringAfterLast('#')
        .substringAfterLast('/')
        .replace('_', ' ')
        .replace('-', ' ')
        .trim()
    if (compact.isBlank()) return raw
    return compact
        .split(Regex("\\s+"))
        .joinToString(" ") { token ->
            token.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.ROOT) else ch.toString()
            }
        }
        .take(48)
}

@Composable
private fun stringResourceCompat(id: Int): String =
    androidx.compose.ui.res.stringResource(id)
