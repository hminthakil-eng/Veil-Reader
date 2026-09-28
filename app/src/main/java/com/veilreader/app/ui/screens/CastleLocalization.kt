package com.veilreader.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.veilreader.app.R

internal data class LocalizedGreatHallArtifactCopy(
    val title: String,
    val subtitle: String
)

@Composable
internal fun localizedGreatHallArtifactCopy(
    artifact: GreatHallArtifact
): LocalizedGreatHallArtifactCopy =
    when (artifact.kind) {
        GreatHallArtifactKind.MIRROR -> LocalizedGreatHallArtifactCopy(
            title = stringResource(R.string.castle_artifact_mirror_title),
            subtitle = if (artifact.awakened) {
                stringResource(
                    R.string.castle_artifact_mirror_awake,
                    artifact.evidenceCount.coerceAtLeast(1)
                )
            } else {
                stringResource(R.string.castle_artifact_mirror_dormant)
            }
        )
        GreatHallArtifactKind.ASTROLABE -> LocalizedGreatHallArtifactCopy(
            title = stringResource(R.string.castle_artifact_astrolabe_title),
            subtitle = stringResource(R.string.castle_artifact_astrolabe_subtitle)
        )
        GreatHallArtifactKind.ARCHIVE_GATE -> LocalizedGreatHallArtifactCopy(
            title = stringResource(R.string.castle_artifact_archive_title),
            subtitle = stringResource(R.string.castle_artifact_archive_subtitle)
        )
        GreatHallArtifactKind.LEDGER -> LocalizedGreatHallArtifactCopy(
            title = stringResource(R.string.castle_artifact_ledger_title),
            subtitle = stringResource(R.string.castle_artifact_ledger_subtitle)
        )
        GreatHallArtifactKind.RITUAL_SEAL -> LocalizedGreatHallArtifactCopy(
            title = stringResource(R.string.castle_artifact_ritual_title),
            subtitle = stringResource(
                if (artifact.awakened) {
                    R.string.castle_artifact_ritual_ready
                } else {
                    R.string.castle_artifact_ritual_waiting
                }
            )
        )
        GreatHallArtifactKind.RELIQUARY -> LocalizedGreatHallArtifactCopy(
            title = stringResource(R.string.castle_artifact_reliquary_title),
            subtitle = stringResource(R.string.castle_artifact_reliquary_subtitle)
        )
        GreatHallArtifactKind.VEILED_DOOR -> LocalizedGreatHallArtifactCopy(
            title = stringResource(R.string.castle_artifact_door_title),
            subtitle = stringResource(R.string.castle_artifact_door_subtitle)
        )
        GreatHallArtifactKind.READING_SEAT -> LocalizedGreatHallArtifactCopy(
            title = stringResource(R.string.castle_artifact_reading_title),
            subtitle = stringResource(R.string.castle_artifact_reading_subtitle)
        )
    }
