package com.veilreader.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.veilreader.app.R

@Composable
internal fun localizedEquippedSigilName(id: String): String =
    stringResource(
        when (id) {
            "first_hour" -> R.string.sigil_quiet_hour_name
            "passage_keeper" -> R.string.sigil_passage_keeper_name
            "seven_days" -> R.string.sigil_seven_day_name
            "ten_tomes" -> R.string.sigil_ten_tomes_name
            "first_threshold" -> R.string.sigil_first_threshold_name
            else -> R.string.profile_unknown_sigil
        }
    )
