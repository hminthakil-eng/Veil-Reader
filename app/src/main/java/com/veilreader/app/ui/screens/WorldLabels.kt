package com.veilreader.app.ui.screens

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.veilreader.app.R
import com.veilreader.app.domain.ReadingPath
import com.veilreader.app.domain.WorldAwakeningStage
import com.veilreader.app.domain.WorldInscriptionKind
import com.veilreader.app.domain.WorldMutationKind
import com.veilreader.app.domain.CastleMutationSignal
import com.veilreader.app.domain.CastleMemoryNarrative

@Composable
internal fun localizedPathName(path: ReadingPath): String =
    pathNameRes(path.id)?.let { stringResource(it) } ?: path.name

@Composable
internal fun localizedRankName(
    pathId: String,
    rankIndex: Int,
    fallback: String
): String =
    rankNameRes(pathId, rankIndex)?.let { stringResource(it) } ?: fallback

@Composable
internal fun localizedCastleRoomName(
    roomId: String,
    fallback: String
): String =
    roomNameRes(roomId)?.let { stringResource(it) } ?: fallback

@Composable
internal fun localizedCastleRoomPurpose(
    roomId: String,
    fallback: String
): String =
    roomPurposeRes(roomId)?.let { stringResource(it) } ?: fallback

@Composable
internal fun localizedWorldStage(stage: WorldAwakeningStage): String =
    stringResource(
        when (stage) {
            WorldAwakeningStage.DORMANT -> R.string.castle_stage_dormant
            WorldAwakeningStage.KINDLED -> R.string.castle_stage_kindled
            WorldAwakeningStage.ATTUNED -> R.string.castle_stage_attuned
            WorldAwakeningStage.RESONANT -> R.string.castle_stage_resonant
            WorldAwakeningStage.ASCENDANT -> R.string.castle_stage_ascendant
        }
    )

@Composable
internal fun localizedWorldInscription(kind: WorldInscriptionKind): String =
    stringResource(
        when (kind) {
            WorldInscriptionKind.ADVANCEMENT_READY -> R.string.castle_world_advancement_ready
            WorldInscriptionKind.ASCENDANT -> R.string.castle_world_ascendant
            WorldInscriptionKind.RETURN_AWAKENING -> R.string.castle_world_return
            WorldInscriptionKind.RITUAL_CHARGED -> R.string.castle_world_ritual_charged
            WorldInscriptionKind.STREAK_EMBERS -> R.string.castle_world_streak
            WorldInscriptionKind.ARCHIVE_DEEP -> R.string.castle_world_archive_deep
            WorldInscriptionKind.KINDLED -> R.string.castle_world_kindled
            WorldInscriptionKind.DORMANT -> R.string.castle_world_dormant
        }
    )

@Composable
internal fun localizedCastleMemoryNarrative(kind: CastleMemoryNarrative): String =
    stringResource(
        when (kind) {
            CastleMemoryNarrative.EMPTY -> R.string.castle_memory_empty
            CastleMemoryNarrative.VOLUMES_ONLY -> R.string.castle_memory_volumes_only
            CastleMemoryNarrative.FEW_ROOMS -> R.string.castle_memory_few_rooms
            CastleMemoryNarrative.RETAINING_SHAPE -> R.string.castle_memory_retaining_shape
            CastleMemoryNarrative.WARM_ARCHIVE -> R.string.castle_memory_warm_archive
            CastleMemoryNarrative.DENSE_MEMORY -> R.string.castle_memory_dense
        }
    )

@Composable
internal fun localizedCastleMutationSignal(signal: CastleMutationSignal): String =
    stringResource(
        when (signal) {
            CastleMutationSignal.RETURN_AWAKENING -> R.string.castle_mutation_return
            CastleMutationSignal.LONG_SILENCE -> R.string.castle_mutation_silence
            CastleMutationSignal.REREAD_PATINA -> R.string.castle_mutation_reread
            CastleMutationSignal.SCRIPTORIUM_LIGHT -> R.string.castle_mutation_scriptorium
            CastleMutationSignal.COMPLETION_ALCOVES -> R.string.castle_mutation_completion
            CastleMutationSignal.FOUNDATION_WEIGHT -> R.string.castle_mutation_foundation
            CastleMutationSignal.NONE -> R.string.castle_mutation_none
        }
    )

@Composable
internal fun localizedWorldMutationTitle(kind: WorldMutationKind): String =
    stringResource(
        when (kind) {
            WorldMutationKind.FOUNDATION_WEIGHT -> R.string.castle_mutation_title_foundation
            WorldMutationKind.COMPLETION_ALCOVES -> R.string.castle_mutation_title_completion
            WorldMutationKind.SCRIPTORIUM_LIGHT -> R.string.castle_mutation_title_scriptorium
            WorldMutationKind.REREAD_PATINA -> R.string.castle_mutation_title_reread
            WorldMutationKind.CONSTELLATION_WEB -> R.string.castle_mutation_title_constellation
            WorldMutationKind.RETURN_AWAKENING -> R.string.castle_mutation_title_return
            WorldMutationKind.PATH_ASCENSION -> R.string.castle_mutation_title_path
            WorldMutationKind.ADVANCEMENT_SEAL -> R.string.castle_mutation_title_seal
        }
    )

@StringRes
private fun pathNameRes(pathId: String): Int? =
    when (pathId) {
        "oracle" -> R.string.path_name_oracle
        "dreamwalker" -> R.string.path_name_dreamwalker
        "archivist" -> R.string.path_name_archivist
        "vanguard" -> R.string.path_name_vanguard
        "nocturne" -> R.string.path_name_nocturne
        "artificer" -> R.string.path_name_artificer
        else -> null
    }

@StringRes
private fun rankNameRes(pathId: String, rankIndex: Int): Int? =
    when (pathId to rankIndex) {
        "oracle" to 0 -> R.string.rank_oracle_0
        "oracle" to 1 -> R.string.rank_oracle_1
        "oracle" to 2 -> R.string.rank_oracle_2
        "oracle" to 3 -> R.string.rank_oracle_3
        "oracle" to 4 -> R.string.rank_oracle_4
        "oracle" to 5 -> R.string.rank_oracle_5
        "dreamwalker" to 0 -> R.string.rank_dreamwalker_0
        "dreamwalker" to 1 -> R.string.rank_dreamwalker_1
        "dreamwalker" to 2 -> R.string.rank_dreamwalker_2
        "dreamwalker" to 3 -> R.string.rank_dreamwalker_3
        "dreamwalker" to 4 -> R.string.rank_dreamwalker_4
        "dreamwalker" to 5 -> R.string.rank_dreamwalker_5
        "archivist" to 0 -> R.string.rank_archivist_0
        "archivist" to 1 -> R.string.rank_archivist_1
        "archivist" to 2 -> R.string.rank_archivist_2
        "archivist" to 3 -> R.string.rank_archivist_3
        "archivist" to 4 -> R.string.rank_archivist_4
        "archivist" to 5 -> R.string.rank_archivist_5
        "vanguard" to 0 -> R.string.rank_vanguard_0
        "vanguard" to 1 -> R.string.rank_vanguard_1
        "vanguard" to 2 -> R.string.rank_vanguard_2
        "vanguard" to 3 -> R.string.rank_vanguard_3
        "vanguard" to 4 -> R.string.rank_vanguard_4
        "vanguard" to 5 -> R.string.rank_vanguard_5
        "nocturne" to 0 -> R.string.rank_nocturne_0
        "nocturne" to 1 -> R.string.rank_nocturne_1
        "nocturne" to 2 -> R.string.rank_nocturne_2
        "nocturne" to 3 -> R.string.rank_nocturne_3
        "nocturne" to 4 -> R.string.rank_nocturne_4
        "nocturne" to 5 -> R.string.rank_nocturne_5
        "artificer" to 0 -> R.string.rank_artificer_0
        "artificer" to 1 -> R.string.rank_artificer_1
        "artificer" to 2 -> R.string.rank_artificer_2
        "artificer" to 3 -> R.string.rank_artificer_3
        "artificer" to 4 -> R.string.rank_artificer_4
        "artificer" to 5 -> R.string.rank_artificer_5
        else -> null
    }

@StringRes
private fun roomNameRes(roomId: String): Int? =
    when (roomId) {
        "library" -> R.string.room_library_name
        "ritual" -> R.string.room_ritual_name
        "observatory" -> R.string.room_observatory_name
        "archive" -> R.string.room_archive_name
        "treasury" -> R.string.room_treasury_name
        "sanctum" -> R.string.room_sanctum_name
        else -> null
    }

@StringRes
private fun roomPurposeRes(roomId: String): Int? =
    when (roomId) {
        "library" -> R.string.room_library_purpose
        "ritual" -> R.string.room_ritual_purpose
        "observatory" -> R.string.room_observatory_purpose
        "archive" -> R.string.room_archive_purpose
        "treasury" -> R.string.room_treasury_purpose
        "sanctum" -> R.string.room_sanctum_purpose
        else -> null
    }
