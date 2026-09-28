package com.veilreader.app.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.SilentNamesChoice
import com.veilreader.app.domain.SilentNamesEncounter
import com.veilreader.app.domain.SilentNamesMode
import com.veilreader.app.domain.SilentNamesOutcome
import com.veilreader.app.domain.SilentNamesReceipt
import com.veilreader.app.ui.VeilEyebrowText
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilRealm
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.grayfogAtmosphere
import com.veilreader.app.ui.theme.narrativeArchitectureField
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SilentNamesScreen(
    profile: ReaderProfile,
    receipt: SilentNamesReceipt?,
    storageBlocked: Boolean = false,
    onSeal: suspend (SilentNamesChoice, SilentNamesMode) -> Result<SilentNamesReceipt>,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedMode by remember { mutableStateOf(SilentNamesMode.STORY) }
    var localReceipt by remember(receipt) { mutableStateOf(receipt) }
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VeilPalette.Ink)
            .grayfogAtmosphere(
                realm = VeilRealm.CASTLE,
                seed = 731,
                intensity = 0.90f
            )
            .narrativeArchitectureField(
                realm = VeilRealm.CASTLE,
                seed = 731,
                intensity = 0.70f
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(390.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            VeilPalette.DeepBrass.copy(alpha = 0.12f),
                            VeilPalette.Ink.copy(alpha = 0.16f),
                            VeilPalette.Ink
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VeilSpacing.lg, vertical = VeilSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
        ) {
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.heightIn(min = 48.dp),
                border = BorderStroke(1.dp, VeilPalette.BorderDark)
            ) {
                Text(stringResource(R.string.silent_names_back_hall))
            }

            SilentNamesWindowSigil(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(152.dp)
            )

            VeilEyebrowText(
                text = stringResource(R.string.silent_names_hall_eyebrow),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = VeilPalette.Brass
            )

            Text(
                text = stringResource(R.string.silent_names_title),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineLarge,
                color = VeilPalette.Moon,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.silent_names_intro),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                color = VeilPalette.Mist,
                textAlign = TextAlign.Center
            )

            SilentNamesPathResonance(profile)

            val sealed = localReceipt
            if (sealed == null && storageBlocked) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = VeilPalette.RaisedIron.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.52f)),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        stringResource(R.string.silent_names_record_unavailable),
                        modifier = Modifier.padding(VeilSpacing.lg),
                        style = MaterialTheme.typography.bodyLarge,
                        color = VeilPalette.Moon
                    )
                }
                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VeilPalette.DeepBrass,
                        contentColor = VeilPalette.Moon
                    )
                ) {
                    Text(stringResource(R.string.silent_names_back_hall))
                }
            } else if (sealed == null) {
                SilentNamesModeSelector(
                    selected = selectedMode,
                    enabled = !saving,
                    onSelected = {
                        saveFailed = false
                        selectedMode = it
                    }
                )

                SilentNamesChoices(
                    profile = profile,
                    mode = selectedMode,
                    enabled = !saving,
                    onChoice = { choice ->
                        if (saving) return@SilentNamesChoices
                        saving = true
                        saveFailed = false
                        scope.launch {
                            try {
                                val result = onSeal(choice, selectedMode)
                                result.fold(
                                    onSuccess = { committed ->
                                        localReceipt = committed
                                    },
                                    onFailure = {
                                        saveFailed = true
                                    }
                                )
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Throwable) {
                                saveFailed = true
                            } finally {
                                saving = false
                            }
                        }
                    }
                )

                if (saving) {
                    Text(
                        stringResource(R.string.silent_names_saving),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = VeilPalette.Brass,
                        textAlign = TextAlign.Center
                    )
                }
                if (saveFailed) {
                    Surface(
                        color = VeilPalette.RaisedIron.copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.42f)),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            stringResource(R.string.silent_names_save_failed),
                            modifier = Modifier.padding(VeilSpacing.md),
                            style = MaterialTheme.typography.bodyMedium,
                            color = VeilPalette.Moon
                        )
                    }
                }
            } else {
                SilentNamesSealedResult(sealed)

                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VeilPalette.DeepBrass,
                        contentColor = VeilPalette.Moon
                    )
                ) {
                    Text(stringResource(R.string.silent_names_back_hall))
                }
            }

            Spacer(Modifier.height(VeilSpacing.xl))
        }
    }
}

@Composable
private fun SilentNamesPathResonance(profile: ReaderProfile) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = VeilPalette.RaisedIron.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.22f)),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(VeilSpacing.md),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            VeilEyebrowText(
                text = stringResource(R.string.silent_names_path_resonance),
                color = VeilPalette.Brass.copy(alpha = 0.88f)
            )
            Text(
                text = profile.path.name,
                style = MaterialTheme.typography.titleMedium,
                color = VeilPalette.Moon
            )
            Text(
                text = stringResource(pathResonanceRes(profile.path.id)),
                style = MaterialTheme.typography.bodyMedium,
                color = VeilPalette.Mist
            )
        }
    }
}

@Composable
private fun SilentNamesModeSelector(
    selected: SilentNamesMode,
    enabled: Boolean,
    onSelected: (SilentNamesMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
        Text(
            stringResource(R.string.silent_names_choose_mode),
            style = MaterialTheme.typography.titleMedium,
            color = VeilPalette.Moon
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
        ) {
            SilentNamesModeButton(
                label = stringResource(R.string.silent_names_story_mode),
                selected = selected == SilentNamesMode.STORY,
                enabled = enabled,
                onClick = { onSelected(SilentNamesMode.STORY) },
                modifier = Modifier.weight(1f)
            )
            SilentNamesModeButton(
                label = stringResource(R.string.silent_names_dice_mode),
                selected = selected == SilentNamesMode.DICE,
                enabled = enabled,
                onClick = { onSelected(SilentNamesMode.DICE) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SilentNamesModeButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = 52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VeilPalette.DeepBrass,
                contentColor = VeilPalette.Moon
            )
        ) {
            Text(label, textAlign = TextAlign.Center)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = 52.dp),
            border = BorderStroke(1.dp, VeilPalette.BorderDark)
        ) {
            Text(label, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SilentNamesChoices(
    profile: ReaderProfile,
    mode: SilentNamesMode,
    enabled: Boolean,
    onChoice: (SilentNamesChoice) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)) {
        SilentNamesChoice.entries.forEach { choice ->
            val probability = (
                SilentNamesEncounter.successProbability(
                    pathId = profile.path.id,
                    choice = choice,
                    advantage = false
                ) * 100.0
                ).roundToInt()

            OutlinedButton(
                onClick = { onChoice(choice) },
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp),
                border = BorderStroke(
                    1.dp,
                    if (SilentNamesEncounter.modifier(profile.path.id, choice) == 3) {
                        VeilPalette.Brass.copy(alpha = 0.72f)
                    } else {
                        VeilPalette.BorderDark
                    }
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(choice.labelRes()),
                        style = MaterialTheme.typography.titleMedium,
                        color = VeilPalette.Moon
                    )
                    if (mode == SilentNamesMode.DICE) {
                        Text(
                            text = stringResource(R.string.silent_names_odds, probability),
                            style = MaterialTheme.typography.bodySmall,
                            color = VeilPalette.Mist
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SilentNamesSealedResult(receipt: SilentNamesReceipt) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = VeilPalette.RaisedIron.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.52f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(VeilSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VeilSpacing.sm)
        ) {
            Text(
                stringResource(R.string.silent_names_saved),
                style = MaterialTheme.typography.labelLarge,
                color = VeilPalette.Brass
            )

            receipt.dice?.let { dice ->
                Text(
                    stringResource(
                        R.string.silent_names_roll_result,
                        dice.selected,
                        SilentNamesEncounter.modifier(receipt.pathId, receipt.choice),
                        SilentNamesEncounter.DIFFICULTY
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = VeilPalette.Mist
                )
            }

            Text(
                stringResource(receipt.outcome.outcomeRes()),
                style = MaterialTheme.typography.bodyLarge,
                color = VeilPalette.Moon
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = VeilPalette.Ink.copy(alpha = 0.38f),
                border = BorderStroke(1.dp, VeilPalette.Brass.copy(alpha = 0.24f)),
                shape = MaterialTheme.shapes.small
            ) {
                Column(
                    modifier = Modifier.padding(VeilSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VeilEyebrowText(
                        text = stringResource(R.string.silent_names_hall_echo),
                        color = VeilPalette.Brass.copy(alpha = 0.86f)
                    )
                    Text(
                        stringResource(pathEchoRes(receipt.pathId)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = VeilPalette.Mist
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = VeilSpacing.sm)
                    .border(
                        1.dp,
                        VeilPalette.Brass.copy(alpha = 0.35f),
                        MaterialTheme.shapes.small
                    )
                    .padding(VeilSpacing.md)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    VeilEyebrowText(
                        text = stringResource(R.string.silent_names_reward_title),
                        color = VeilPalette.Brass
                    )
                    Text(
                        stringResource(R.string.silent_names_reward_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = VeilPalette.Mist
                    )
                }
            }
        }
    }
}

@Composable
private fun SilentNamesWindowSigil(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val brass = VeilPalette.Brass
        val faint = brass.copy(alpha = 0.28f)
        val strong = 1.25.dp.toPx()
        val thin = 0.8.dp.toPx()

        drawCircle(faint, size.minDimension * 0.46f, center, style = Stroke(thin))
        drawCircle(brass.copy(alpha = 0.72f), size.minDimension * 0.34f, center, style = Stroke(strong))
        drawLine(
            brass.copy(alpha = 0.56f),
            Offset(center.x, size.height * 0.17f),
            Offset(center.x, size.height * 0.83f),
            strong
        )
        drawLine(
            faint,
            Offset(size.width * 0.23f, center.y),
            Offset(size.width * 0.77f, center.y),
            thin
        )
        drawArc(
            brass,
            startAngle = 205f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = Offset(size.width * 0.27f, size.height * 0.26f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.46f, size.height * 0.48f),
            style = Stroke(strong)
        )
        drawCircle(brass, 2.2.dp.toPx(), Offset(center.x, size.height * 0.69f))
    }
}

@StringRes
private fun pathResonanceRes(pathId: String): Int = when (pathId) {
    "oracle" -> R.string.silent_names_path_oracle
    "dreamwalker" -> R.string.silent_names_path_dreamwalker
    "archivist" -> R.string.silent_names_path_archivist
    "vanguard" -> R.string.silent_names_path_vanguard
    "nocturne" -> R.string.silent_names_path_nocturne
    "artificer" -> R.string.silent_names_path_artificer
    else -> R.string.silent_names_path_neutral
}

@StringRes
private fun pathEchoRes(pathId: String): Int = when (pathId) {
    "oracle" -> R.string.silent_names_echo_oracle
    "dreamwalker" -> R.string.silent_names_echo_dreamwalker
    "archivist" -> R.string.silent_names_echo_archivist
    "vanguard" -> R.string.silent_names_echo_vanguard
    "nocturne" -> R.string.silent_names_echo_nocturne
    "artificer" -> R.string.silent_names_echo_artificer
    else -> R.string.silent_names_echo_neutral
}

@StringRes
private fun SilentNamesChoice.labelRes(): Int =
    when (this) {
        SilentNamesChoice.EXAMINE_SEAL -> R.string.silent_names_examine_seal
        SilentNamesChoice.FOLLOW_LIGHT -> R.string.silent_names_follow_light
        SilentNamesChoice.SPEAK_TO_KEEPER -> R.string.silent_names_speak_to_keeper
    }

@StringRes
private fun SilentNamesOutcome.outcomeRes(): Int =
    when (this) {
        SilentNamesOutcome.RESTORED_INSCRIPTION -> R.string.silent_names_restored_inscription
        SilentNamesOutcome.WORKSHOP_TRAIL -> R.string.silent_names_workshop_trail
        SilentNamesOutcome.LANTERN_BRIDGE -> R.string.silent_names_lantern_bridge
        SilentNamesOutcome.LOWER_PASSAGE -> R.string.silent_names_lower_passage
        SilentNamesOutcome.KEEPER_TESTIMONY -> R.string.silent_names_keeper_testimony
        SilentNamesOutcome.KEEPER_REQUEST -> R.string.silent_names_keeper_request
    }
