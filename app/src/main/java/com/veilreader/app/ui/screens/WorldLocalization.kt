package com.veilreader.app.ui.screens

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.veilreader.app.R
import com.veilreader.app.domain.ReadingPath
import com.veilreader.app.domain.ReadingPolicy

@StringRes
private fun pathNameRes(pathId: String): Int? = when (pathId) {
    "oracle" -> R.string.path_oracle_name
    "dreamwalker" -> R.string.path_dreamwalker_name
    "archivist" -> R.string.path_archivist_name
    "vanguard" -> R.string.path_vanguard_name
    "nocturne" -> R.string.path_nocturne_name
    "artificer" -> R.string.path_artificer_name
    else -> null
}

@StringRes
private fun pathEpithetRes(pathId: String): Int? = when (pathId) {
    "oracle" -> R.string.path_oracle_epithet
    "dreamwalker" -> R.string.path_dreamwalker_epithet
    "archivist" -> R.string.path_archivist_epithet
    "vanguard" -> R.string.path_vanguard_epithet
    "nocturne" -> R.string.path_nocturne_epithet
    "artificer" -> R.string.path_artificer_epithet
    else -> null
}

@StringRes
private fun pathDescriptionRes(pathId: String): Int? = when (pathId) {
    "oracle" -> R.string.path_oracle_description
    "dreamwalker" -> R.string.path_dreamwalker_description
    "archivist" -> R.string.path_archivist_description
    "vanguard" -> R.string.path_vanguard_description
    "nocturne" -> R.string.path_nocturne_description
    "artificer" -> R.string.path_artificer_description
    else -> null
}

@StringRes
private fun pathAspectRes(pathId: String): Int = when (pathId) {
    "oracle" -> R.string.path_oracle_aspect
    "dreamwalker" -> R.string.path_dreamwalker_aspect
    "archivist" -> R.string.path_archivist_aspect
    "vanguard" -> R.string.path_vanguard_aspect
    "nocturne" -> R.string.path_nocturne_aspect
    "artificer" -> R.string.path_artificer_aspect
    else -> R.string.path_generic_aspect
}

@StringRes
private fun pathInvocationRes(pathId: String): Int = when (pathId) {
    "oracle" -> R.string.path_oracle_invocation
    "dreamwalker" -> R.string.path_dreamwalker_invocation
    "archivist" -> R.string.path_archivist_invocation
    "vanguard" -> R.string.path_vanguard_invocation
    "nocturne" -> R.string.path_nocturne_invocation
    "artificer" -> R.string.path_artificer_invocation
    else -> R.string.path_generic_invocation
}

private val rankResources = mapOf(
    "oracle" to intArrayOf(
        R.string.path_oracle_rank_0, R.string.path_oracle_rank_1,
        R.string.path_oracle_rank_2, R.string.path_oracle_rank_3,
        R.string.path_oracle_rank_4, R.string.path_oracle_rank_5
    ),
    "dreamwalker" to intArrayOf(
        R.string.path_dreamwalker_rank_0, R.string.path_dreamwalker_rank_1,
        R.string.path_dreamwalker_rank_2, R.string.path_dreamwalker_rank_3,
        R.string.path_dreamwalker_rank_4, R.string.path_dreamwalker_rank_5
    ),
    "archivist" to intArrayOf(
        R.string.path_archivist_rank_0, R.string.path_archivist_rank_1,
        R.string.path_archivist_rank_2, R.string.path_archivist_rank_3,
        R.string.path_archivist_rank_4, R.string.path_archivist_rank_5
    ),
    "vanguard" to intArrayOf(
        R.string.path_vanguard_rank_0, R.string.path_vanguard_rank_1,
        R.string.path_vanguard_rank_2, R.string.path_vanguard_rank_3,
        R.string.path_vanguard_rank_4, R.string.path_vanguard_rank_5
    ),
    "nocturne" to intArrayOf(
        R.string.path_nocturne_rank_0, R.string.path_nocturne_rank_1,
        R.string.path_nocturne_rank_2, R.string.path_nocturne_rank_3,
        R.string.path_nocturne_rank_4, R.string.path_nocturne_rank_5
    ),
    "artificer" to intArrayOf(
        R.string.path_artificer_rank_0, R.string.path_artificer_rank_1,
        R.string.path_artificer_rank_2, R.string.path_artificer_rank_3,
        R.string.path_artificer_rank_4, R.string.path_artificer_rank_5
    )
)

@Composable
internal fun localizedPathName(path: ReadingPath): String =
    pathNameRes(path.id)?.let { stringResource(it) } ?: path.name

@Composable
internal fun localizedPathEpithet(path: ReadingPath): String =
    pathEpithetRes(path.id)?.let { stringResource(it) } ?: path.epithet

@Composable
internal fun localizedPathDescription(path: ReadingPath): String =
    pathDescriptionRes(path.id)?.let { stringResource(it) } ?: path.description

@Composable
internal fun localizedPathAspect(pathId: String): String =
    stringResource(pathAspectRes(pathId))

@Composable
internal fun localizedPathInvocation(pathId: String): String =
    stringResource(pathInvocationRes(pathId))

@Composable
internal fun localizedPathRank(path: ReadingPath, rankIndex: Int): String {
    val resource = rankResources[path.id]?.getOrNull(rankIndex)
    return resource?.let { stringResource(it) }
        ?: path.ranks.getOrNull(rankIndex)
        ?: ""
}

@Composable
internal fun localizedRitualDescription(pathId: String, rankIndex: Int): String {
    val target = ReadingPolicy.ritualTarget(pathId, rankIndex)
    val resource = when (pathId) {
        "dreamwalker" -> R.string.path_ritual_dreamwalker
        "vanguard" -> R.string.path_ritual_vanguard
        "nocturne" -> R.string.path_ritual_nocturne
        "archivist" -> R.string.path_ritual_archivist
        "artificer" -> R.string.path_ritual_artificer
        else -> R.string.path_ritual_oracle
    }
    return stringResource(resource, target)
}
