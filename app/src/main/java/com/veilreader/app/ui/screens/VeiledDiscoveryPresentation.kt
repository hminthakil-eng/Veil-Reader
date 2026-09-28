package com.veilreader.app.ui.screens

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.veilreader.app.R
import com.veilreader.app.domain.VeiledDiscoveryPolicy

internal data class VeiledDiscoveryPresentation(
    val id: String,
    val symbol: String,
    @StringRes val titleRes: Int,
    @StringRes val clueRes: Int,
    @StringRes val loreRes: Int,
    val fragmentRes: List<Int>
)

internal val veiledDiscoveryPresentations = listOf(
    VeiledDiscoveryPresentation(
        id = VeiledDiscoveryPolicy.PATIENT_FLAME,
        symbol = "◈",
        titleRes = R.string.discovery_patient_flame_title,
        clueRes = R.string.discovery_patient_flame_clue,
        loreRes = R.string.discovery_patient_flame_lore,
        fragmentRes = listOf(
            R.string.discovery_patient_flame_fragment_0,
            R.string.discovery_patient_flame_fragment_1,
            R.string.discovery_patient_flame_fragment_2
        )
    ),
    VeiledDiscoveryPresentation(
        id = VeiledDiscoveryPolicy.MARGINALIA_GATE,
        symbol = "✧",
        titleRes = R.string.discovery_marginalia_gate_title,
        clueRes = R.string.discovery_marginalia_gate_clue,
        loreRes = R.string.discovery_marginalia_gate_lore,
        fragmentRes = listOf(
            R.string.discovery_marginalia_gate_fragment_0,
            R.string.discovery_marginalia_gate_fragment_1,
            R.string.discovery_marginalia_gate_fragment_2
        )
    ),
    VeiledDiscoveryPresentation(
        id = VeiledDiscoveryPolicy.DEEP_SHELF,
        symbol = "▥",
        titleRes = R.string.discovery_deep_shelf_title,
        clueRes = R.string.discovery_deep_shelf_clue,
        loreRes = R.string.discovery_deep_shelf_lore,
        fragmentRes = listOf(
            R.string.discovery_deep_shelf_fragment_0,
            R.string.discovery_deep_shelf_fragment_1,
            R.string.discovery_deep_shelf_fragment_2
        )
    ),
    VeiledDiscoveryPresentation(
        id = VeiledDiscoveryPolicy.LONG_WATCH,
        symbol = "◐",
        titleRes = R.string.discovery_long_watch_title,
        clueRes = R.string.discovery_long_watch_clue,
        loreRes = R.string.discovery_long_watch_lore,
        fragmentRes = listOf(
            R.string.discovery_long_watch_fragment_0,
            R.string.discovery_long_watch_fragment_1,
            R.string.discovery_long_watch_fragment_2
        )
    ),
    VeiledDiscoveryPresentation(
        id = VeiledDiscoveryPolicy.VEIL_THINS,
        symbol = "⌁",
        titleRes = R.string.discovery_veil_thins_title,
        clueRes = R.string.discovery_veil_thins_clue,
        loreRes = R.string.discovery_veil_thins_lore,
        fragmentRes = listOf(
            R.string.discovery_veil_thins_fragment_0,
            R.string.discovery_veil_thins_fragment_1,
            R.string.discovery_veil_thins_fragment_2
        )
    ),
    VeiledDiscoveryPresentation(
        id = VeiledDiscoveryPolicy.UNNAMED_CHAMBER,
        symbol = "⬡",
        titleRes = R.string.discovery_unnamed_chamber_title,
        clueRes = R.string.discovery_unnamed_chamber_clue,
        loreRes = R.string.discovery_unnamed_chamber_lore,
        fragmentRes = listOf(
            R.string.discovery_unnamed_chamber_fragment_0,
            R.string.discovery_unnamed_chamber_fragment_1,
            R.string.discovery_unnamed_chamber_fragment_2
        )
    )
)

internal fun discoveryPresentationFor(id: String): VeiledDiscoveryPresentation? =
    veiledDiscoveryPresentations.firstOrNull { it.id == id }

@Composable
internal fun localizedDiscoveryFragment(
    presentation: VeiledDiscoveryPresentation,
    fragmentIndex: Int,
    fallback: String
): String {
    val res = presentation.fragmentRes.getOrNull(fragmentIndex)
    return if (res != null) stringResource(res) else fallback
}
