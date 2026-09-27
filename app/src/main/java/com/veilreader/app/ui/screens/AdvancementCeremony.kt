package com.veilreader.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.veilreader.app.ui.theme.LocalVeilReducedMotion
import com.veilreader.app.ui.theme.VeilMotion
import com.veilreader.app.ui.theme.VeilPalette
import com.veilreader.app.ui.theme.VeilRealm
import com.veilreader.app.ui.theme.VeilSpacing
import com.veilreader.app.ui.theme.grayfogAtmosphere
import kotlinx.coroutines.delay

internal data class AdvancementCeremonySnapshot(
    val pathId: String,
    val pathName: String,
    val pathEpithet: String,
    val fromRank: String,
    val toRank: String,
    val rankIndex: Int,
    val ritualDescription: String,
    val invocation: String
)

internal enum class AdvancementCeremonyStage {
    INVOCATION,
    SEALING,
    REVEALED
}

internal data class AdvancementCeremonyTiming(
    val sealMillis: Long,
    val revealHoldMillis: Long
)

internal fun advancementCeremonyTiming(reducedMotion: Boolean): AdvancementCeremonyTiming =
    if (reducedMotion) {
        AdvancementCeremonyTiming(
            sealMillis = VeilMotion.REDUCED_MOTION_FADE_MS.toLong(),
            revealHoldMillis = 260L
        )
    } else {
        AdvancementCeremonyTiming(
            sealMillis = 620L,
            revealHoldMillis = 920L
        )
    }

internal fun canDismissAdvancementCeremony(stage: AdvancementCeremonyStage): Boolean =
    stage == AdvancementCeremonyStage.INVOCATION

@Composable
internal fun AdvancementCeremony(
    snapshot: AdvancementCeremonySnapshot,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val reducedMotion = LocalVeilReducedMotion.current
    val timing = remember(reducedMotion) { advancementCeremonyTiming(reducedMotion) }
    var stage by remember(snapshot) { mutableStateOf(AdvancementCeremonyStage.INVOCATION) }

    LaunchedEffect(stage, timing) {
        when (stage) {
            AdvancementCeremonyStage.INVOCATION -> Unit
            AdvancementCeremonyStage.SEALING -> {
                delay(timing.sealMillis)
                stage = AdvancementCeremonyStage.REVEALED
            }
            AdvancementCeremonyStage.REVEALED -> {
                delay(timing.revealHoldMillis)
                onDismiss()
            }
        }
    }

    val ignition by animateFloatAsState(
        targetValue = when (stage) {
            AdvancementCeremonyStage.INVOCATION -> 0.20f
            AdvancementCeremonyStage.SEALING -> 0.72f
            AdvancementCeremonyStage.REVEALED -> 1f
        },
        animationSpec = if (reducedMotion) snap() else tween(VeilMotion.RITUAL_MS),
        label = "advancement-ignition"
    )
    val sigilScale by animateFloatAsState(
        targetValue = when (stage) {
            AdvancementCeremonyStage.INVOCATION -> 0.92f
            AdvancementCeremonyStage.SEALING -> 1.06f
            AdvancementCeremonyStage.REVEALED -> 1f
        },
        animationSpec = if (reducedMotion) snap() else tween(VeilMotion.RITUAL_MS),
        label = "advancement-sigil-scale"
    )

    Dialog(
        onDismissRequest = {
            if (canDismissAdvancementCeremony(stage)) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = canDismissAdvancementCeremony(stage),
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VeilPalette.Ink)
                .grayfogAtmosphere(
                    realm = VeilRealm.RITUAL,
                    seed = snapshot.pathId.hashCode() xor snapshot.rankIndex,
                    intensity = 1f
                )
                .semantics {
                    contentDescription =
                        "Advancement ritual from ${snapshot.fromRank} to ${snapshot.toRank}"
                }
        ) {
            AdvancementField(
                ignition = ignition,
                modifier = Modifier.matchParentSize()
            )
            PathRitualBackdrop(
                pathId = snapshot.pathId,
                rankIndex = snapshot.rankIndex + 1,
                modifier = Modifier.matchParentSize()
            )

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 620.dp)
                    .fillMaxWidth()
                    .padding(horizontal = VeilSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(VeilSpacing.md)
            ) {
                Text(
                    when (stage) {
                        AdvancementCeremonyStage.INVOCATION -> "THE THRESHOLD OPENS"
                        AdvancementCeremonyStage.SEALING -> "THE SEAL IS TURNING"
                        AdvancementCeremonyStage.REVEALED -> "RANK AWAKENED"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.8.sp),
                    color = VeilPalette.Brass
                )

                Box(
                    modifier = Modifier
                        .size(184.dp)
                        .border(
                            1.dp,
                            VeilPalette.Brass.copy(alpha = 0.18f + ignition * 0.52f),
                            MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.matchParentSize()) {
                        drawCircle(
                            color = VeilPalette.Brass.copy(alpha = 0.04f + ignition * 0.10f),
                            radius = size.minDimension * 0.47f
                        )
                        drawCircle(
                            color = VeilPalette.Brass.copy(alpha = 0.16f + ignition * 0.34f),
                            radius = size.minDimension * 0.39f,
                            style = Stroke(1.dp.toPx())
                        )
                        drawCircle(
                            color = VeilPalette.Spirit.copy(alpha = 0.08f + ignition * 0.20f),
                            radius = size.minDimension * 0.29f,
                            style = Stroke(1.dp.toPx())
                        )
                    }

                    Box(
                        Modifier.size(132.dp * sigilScale)
                    ) {
                        PathSigil(
                            pathId = snapshot.pathId,
                            modifier = Modifier.matchParentSize(),
                            active = true
                        )
                    }
                }

                Text(
                    snapshot.pathName,
                    style = MaterialTheme.typography.labelLarge,
                    color = VeilPalette.Mist
                )

                Text(
                    if (stage == AdvancementCeremonyStage.REVEALED) {
                        snapshot.toRank
                    } else {
                        "${snapshot.fromRank}  →  ${snapshot.toRank}"
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    color = VeilPalette.Moon,
                    textAlign = TextAlign.Center
                )

                Text(
                    when (stage) {
                        AdvancementCeremonyStage.INVOCATION -> snapshot.ritualDescription
                        AdvancementCeremonyStage.SEALING ->
                            "The record is being sealed into ${snapshot.pathEpithet}."
                        AdvancementCeremonyStage.REVEALED -> snapshot.invocation
                    },
                    modifier = Modifier.widthIn(max = 520.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (stage == AdvancementCeremonyStage.REVEALED) {
                        VeilPalette.Brass
                    } else {
                        VeilPalette.Mist
                    },
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(VeilSpacing.sm))

                when (stage) {
                    AdvancementCeremonyStage.INVOCATION -> {
                        Button(
                            onClick = {
                                stage = AdvancementCeremonyStage.SEALING
                                onConfirm()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp),
                            shape = MaterialTheme.shapes.extraSmall,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VeilPalette.Brass,
                                contentColor = Color(0xFF17120A)
                            )
                        ) {
                            Text("Seal advancement")
                        }

                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                "Not yet",
                                color = VeilPalette.Moon.copy(alpha = 0.78f)
                            )
                        }
                    }

                    AdvancementCeremonyStage.SEALING -> {
                        Text(
                            "DO NOT BREAK THE SEAL",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.45.sp),
                            color = VeilPalette.Mist.copy(alpha = 0.68f)
                        )
                    }

                    AdvancementCeremonyStage.REVEALED -> {
                        BrassRule(Modifier.width(148.dp), strong = true)
                        Text(
                            "The Castle will remember this ascent.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VeilPalette.Moon.copy(alpha = 0.78f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancementField(
    ignition: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val center = Offset(size.width * 0.5f, size.height * 0.43f)
        val radius = size.minDimension * 0.72f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    VeilPalette.Brass.copy(alpha = 0.07f * ignition),
                    VeilPalette.Spirit.copy(alpha = 0.025f * ignition),
                    Color.Transparent
                ),
                center = center,
                radius = radius
            ),
            center = center,
            radius = radius
        )

        repeat(7) { index ->
            val y = size.height * (0.18f + index * 0.105f)
            drawLine(
                color = VeilPalette.Brass.copy(
                    alpha = (0.018f + index * 0.003f) * ignition
                ),
                start = Offset(size.width * 0.10f, y),
                end = Offset(size.width * 0.90f, y),
                strokeWidth = 0.7.dp.toPx()
            )
        }

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    VeilPalette.Ink.copy(alpha = 0.22f),
                    VeilPalette.Ink.copy(alpha = 0.86f)
                ),
                startY = size.height * 0.58f,
                endY = size.height
            ),
            topLeft = Offset(0f, size.height * 0.54f),
            size = androidx.compose.ui.geometry.Size(
                size.width,
                size.height * 0.46f
            )
        )
    }
}
