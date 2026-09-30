package com.veilreader.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.veilreader.app.R
import com.veilreader.app.domain.EchoDepth
import com.veilreader.app.domain.HighlightMemory

@Composable
internal fun highlightAgeLabel(memory: HighlightMemory): String {
    if (!memory.ageKnown) return stringResource(R.string.archive_mark_date_unknown)
    val days = memory.ageDays.coerceAtLeast(0)
    return when {
        days == 0 -> stringResource(R.string.archive_marked_today)
        days == 1 -> stringResource(R.string.archive_marked_yesterday)
        days < 60 -> pluralStringResource(R.plurals.archive_marked_days_ago, days, days)
        days < 730 -> {
            val months = (days / 30).coerceAtLeast(2)
            pluralStringResource(R.plurals.archive_marked_months_ago, months, months)
        }
        else -> {
            val years = (days / 365).coerceAtLeast(2)
            pluralStringResource(R.plurals.archive_marked_years_ago, years, years)
        }
    }
}

@Composable
internal fun highlightEchoLabel(memory: HighlightMemory): String? {
    if (memory.echoDepth == EchoDepth.FRESH || !memory.ageKnown) return null
    val days = memory.ageDays.coerceAtLeast(0)
    return when {
        days < 60 -> pluralStringResource(R.plurals.archive_echo_days_ago, days, days)
        days < 730 -> {
            val months = (days / 30).coerceAtLeast(2)
            pluralStringResource(R.plurals.archive_echo_months_ago, months, months)
        }
        else -> {
            val years = (days / 365).coerceAtLeast(2)
            pluralStringResource(R.plurals.archive_echo_years_ago, years, years)
        }
    }
}

@Composable
internal fun highlightLastViewedLabel(memory: HighlightMemory): String? {
    val days = memory.lastViewedDaysAgo ?: return null
    return when {
        days == 0 -> stringResource(R.string.archive_last_viewed_today)
        days == 1 -> stringResource(R.string.archive_last_viewed_yesterday)
        days < 60 -> pluralStringResource(R.plurals.archive_last_viewed_days_ago, days, days)
        days < 730 -> {
            val months = (days / 30).coerceAtLeast(2)
            pluralStringResource(R.plurals.archive_last_viewed_months_ago, months, months)
        }
        else -> {
            val years = (days / 365).coerceAtLeast(2)
            pluralStringResource(R.plurals.archive_last_viewed_years_ago, years, years)
        }
    }
}
