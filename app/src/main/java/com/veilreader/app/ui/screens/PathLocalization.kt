package com.veilreader.app.ui.screens

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.veilreader.app.R
import com.veilreader.app.domain.PathDoctrine
import com.veilreader.app.domain.ReadingPath
import com.veilreader.app.domain.ReadingPolicy

internal data class LocalizedPathPresentation(
    val name: String,
    val epithet: String,
    val description: String,
    val rankName: String,
    val aspect: String,
    val invocation: String
)

internal data class LocalizedPathDoctrine(
    val maxim: String,
    val embodimentName: String,
    val embodimentDescription: String,
    val insightName: String,
    val insightDescription: String,
    val stabilityName: String,
    val stabilityDescription: String
)

private data class PathIdentityResources(
    @StringRes val nameRes: Int,
    @StringRes val epithetRes: Int,
    @StringRes val descriptionRes: Int,
    val rankRes: List<Int>,
    @StringRes val aspectRes: Int,
    @StringRes val invocationRes: Int
)

private data class PathDoctrineResources(
    @StringRes val maximRes: Int,
    @StringRes val embodimentNameRes: Int,
    @StringRes val embodimentDescriptionRes: Int,
    @StringRes val insightNameRes: Int,
    @StringRes val insightDescriptionRes: Int,
    @StringRes val stabilityNameRes: Int,
    @StringRes val stabilityDescriptionRes: Int,
    @StringRes val ritualRes: Int
)

private val pathIdentityResources = mapOf(
    "oracle" to PathIdentityResources(
        R.string.path_name_oracle,
        R.string.path_epithet_oracle,
        R.string.path_description_oracle,
        listOf(
            R.string.path_rank_oracle_0,
            R.string.path_rank_oracle_1,
            R.string.path_rank_oracle_2,
            R.string.path_rank_oracle_3,
            R.string.path_rank_oracle_4,
            R.string.path_rank_oracle_5
        ),
        R.string.path_aspect_oracle,
        R.string.path_invocation_oracle
    ),
    "dreamwalker" to PathIdentityResources(
        R.string.path_name_dreamwalker,
        R.string.path_epithet_dreamwalker,
        R.string.path_description_dreamwalker,
        listOf(
            R.string.path_rank_dreamwalker_0,
            R.string.path_rank_dreamwalker_1,
            R.string.path_rank_dreamwalker_2,
            R.string.path_rank_dreamwalker_3,
            R.string.path_rank_dreamwalker_4,
            R.string.path_rank_dreamwalker_5
        ),
        R.string.path_aspect_dreamwalker,
        R.string.path_invocation_dreamwalker
    ),
    "archivist" to PathIdentityResources(
        R.string.path_name_archivist,
        R.string.path_epithet_archivist,
        R.string.path_description_archivist,
        listOf(
            R.string.path_rank_archivist_0,
            R.string.path_rank_archivist_1,
            R.string.path_rank_archivist_2,
            R.string.path_rank_archivist_3,
            R.string.path_rank_archivist_4,
            R.string.path_rank_archivist_5
        ),
        R.string.path_aspect_archivist,
        R.string.path_invocation_archivist
    ),
    "vanguard" to PathIdentityResources(
        R.string.path_name_vanguard,
        R.string.path_epithet_vanguard,
        R.string.path_description_vanguard,
        listOf(
            R.string.path_rank_vanguard_0,
            R.string.path_rank_vanguard_1,
            R.string.path_rank_vanguard_2,
            R.string.path_rank_vanguard_3,
            R.string.path_rank_vanguard_4,
            R.string.path_rank_vanguard_5
        ),
        R.string.path_aspect_vanguard,
        R.string.path_invocation_vanguard
    ),
    "nocturne" to PathIdentityResources(
        R.string.path_name_nocturne,
        R.string.path_epithet_nocturne,
        R.string.path_description_nocturne,
        listOf(
            R.string.path_rank_nocturne_0,
            R.string.path_rank_nocturne_1,
            R.string.path_rank_nocturne_2,
            R.string.path_rank_nocturne_3,
            R.string.path_rank_nocturne_4,
            R.string.path_rank_nocturne_5
        ),
        R.string.path_aspect_nocturne,
        R.string.path_invocation_nocturne
    ),
    "artificer" to PathIdentityResources(
        R.string.path_name_artificer,
        R.string.path_epithet_artificer,
        R.string.path_description_artificer,
        listOf(
            R.string.path_rank_artificer_0,
            R.string.path_rank_artificer_1,
            R.string.path_rank_artificer_2,
            R.string.path_rank_artificer_3,
            R.string.path_rank_artificer_4,
            R.string.path_rank_artificer_5
        ),
        R.string.path_aspect_artificer,
        R.string.path_invocation_artificer
    )
)

private val pathDoctrineResources = mapOf(
    "oracle" to PathDoctrineResources(
        R.string.path_doctrine_oracle_maxim,
        R.string.path_doctrine_oracle_embodiment_name,
        R.string.path_doctrine_oracle_embodiment_desc,
        R.string.path_doctrine_oracle_insight_name,
        R.string.path_doctrine_oracle_insight_desc,
        R.string.path_doctrine_oracle_stability_name,
        R.string.path_doctrine_oracle_stability_desc,
        R.string.path_ritual_oracle
    ),
    "dreamwalker" to PathDoctrineResources(
        R.string.path_doctrine_dreamwalker_maxim,
        R.string.path_doctrine_dreamwalker_embodiment_name,
        R.string.path_doctrine_dreamwalker_embodiment_desc,
        R.string.path_doctrine_dreamwalker_insight_name,
        R.string.path_doctrine_dreamwalker_insight_desc,
        R.string.path_doctrine_dreamwalker_stability_name,
        R.string.path_doctrine_dreamwalker_stability_desc,
        R.string.path_ritual_dreamwalker
    ),
    "archivist" to PathDoctrineResources(
        R.string.path_doctrine_archivist_maxim,
        R.string.path_doctrine_archivist_embodiment_name,
        R.string.path_doctrine_archivist_embodiment_desc,
        R.string.path_doctrine_archivist_insight_name,
        R.string.path_doctrine_archivist_insight_desc,
        R.string.path_doctrine_archivist_stability_name,
        R.string.path_doctrine_archivist_stability_desc,
        R.string.path_ritual_archivist
    ),
    "vanguard" to PathDoctrineResources(
        R.string.path_doctrine_vanguard_maxim,
        R.string.path_doctrine_vanguard_embodiment_name,
        R.string.path_doctrine_vanguard_embodiment_desc,
        R.string.path_doctrine_vanguard_insight_name,
        R.string.path_doctrine_vanguard_insight_desc,
        R.string.path_doctrine_vanguard_stability_name,
        R.string.path_doctrine_vanguard_stability_desc,
        R.string.path_ritual_vanguard
    ),
    "nocturne" to PathDoctrineResources(
        R.string.path_doctrine_nocturne_maxim,
        R.string.path_doctrine_nocturne_embodiment_name,
        R.string.path_doctrine_nocturne_embodiment_desc,
        R.string.path_doctrine_nocturne_insight_name,
        R.string.path_doctrine_nocturne_insight_desc,
        R.string.path_doctrine_nocturne_stability_name,
        R.string.path_doctrine_nocturne_stability_desc,
        R.string.path_ritual_nocturne
    ),
    "artificer" to PathDoctrineResources(
        R.string.path_doctrine_artificer_maxim,
        R.string.path_doctrine_artificer_embodiment_name,
        R.string.path_doctrine_artificer_embodiment_desc,
        R.string.path_doctrine_artificer_insight_name,
        R.string.path_doctrine_artificer_insight_desc,
        R.string.path_doctrine_artificer_stability_name,
        R.string.path_doctrine_artificer_stability_desc,
        R.string.path_ritual_artificer
    )
)

@Composable
internal fun localizedPathPresentation(
    path: ReadingPath,
    rankIndex: Int,
    alternateFallbackInvocation: Boolean = false
): LocalizedPathPresentation {
    val resources = pathIdentityResources[path.id]
    if (resources == null) {
        return LocalizedPathPresentation(
            name = path.name,
            epithet = path.epithet,
            description = path.description,
            rankName = path.ranks.getOrNull(rankIndex).orEmpty(),
            aspect = stringResource(R.string.path_aspect_fallback),
            invocation = stringResource(
                if (alternateFallbackInvocation) {
                    R.string.path_invocation_other_fallback
                } else {
                    R.string.path_invocation_fallback
                }
            )
        )
    }

    val safeRank = rankIndex.coerceIn(0, resources.rankRes.lastIndex)
    return LocalizedPathPresentation(
        name = stringResource(resources.nameRes),
        epithet = stringResource(resources.epithetRes),
        description = stringResource(resources.descriptionRes),
        rankName = stringResource(resources.rankRes[safeRank]),
        aspect = stringResource(resources.aspectRes),
        invocation = stringResource(resources.invocationRes)
    )
}

@Composable
internal fun localizedPathDoctrine(
    pathId: String,
    fallback: PathDoctrine
): LocalizedPathDoctrine {
    val resources = pathDoctrineResources[pathId]
        ?: return LocalizedPathDoctrine(
            maxim = fallback.maxim,
            embodimentName = fallback.embodimentName,
            embodimentDescription = fallback.embodimentDescription,
            insightName = fallback.insightName,
            insightDescription = fallback.insightDescription,
            stabilityName = fallback.stabilityName,
            stabilityDescription = fallback.stabilityDescription
        )

    return LocalizedPathDoctrine(
        maxim = stringResource(resources.maximRes),
        embodimentName = stringResource(resources.embodimentNameRes),
        embodimentDescription = stringResource(resources.embodimentDescriptionRes),
        insightName = stringResource(resources.insightNameRes),
        insightDescription = stringResource(resources.insightDescriptionRes),
        stabilityName = stringResource(resources.stabilityNameRes),
        stabilityDescription = stringResource(resources.stabilityDescriptionRes)
    )
}

@Composable
internal fun localizedRitualDescription(
    pathId: String,
    rankIndex: Int
): String {
    val resources = pathDoctrineResources[pathId]
        ?: return ReadingPolicy.ritualDescription(pathId, rankIndex)
    return stringResource(resources.ritualRes, ReadingPolicy.ritualTarget(pathId, rankIndex))
}

internal fun knownLocalizedPathIds(): Set<String> =
    pathIdentityResources.keys.intersect(pathDoctrineResources.keys)
