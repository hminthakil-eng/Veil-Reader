package com.veilreader.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.veilreader.app.R
import com.veilreader.app.domain.ReaderProfile

@Composable
internal fun localizedCastleTitle(
    canonicalTitle: String,
    profile: ReaderProfile
): String =
    when (canonicalTitle) {
        "Reader of the Veil" -> stringResource(R.string.castle_title_reader)
        "Threshold Walker" -> stringResource(R.string.castle_title_threshold)
        "Keeper of the Quiet Hour" -> stringResource(R.string.castle_title_quiet)
        "Warden of Passages" -> stringResource(R.string.castle_title_passages)
        "Lantern of Seven Nights" -> stringResource(R.string.castle_title_seven)
        "Keeper of Ten Tomes" -> stringResource(R.string.castle_title_ten)
        "Sovereign of the Living Library" -> stringResource(R.string.castle_title_sovereign)
        else -> {
            if (canonicalTitle.startsWith("Veilbound ")) {
                val finalRank = localizedPathPresentation(
                    path = profile.path,
                    rankIndex = profile.path.ranks.lastIndex
                ).rankName
                stringResource(R.string.castle_title_veilbound, finalRank)
            } else {
                canonicalTitle
            }
        }
    }
