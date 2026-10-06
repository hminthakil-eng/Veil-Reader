package com.veilreader.app.ui.screens

import com.veilreader.app.domain.ReaderProfile

internal enum class RelicRarity {
    FOUNDATION,
    RESONANT,
    ASCENDANT,
    SOVEREIGN
}

internal fun relicRarityFor(relicId: String): RelicRarity =
    when (relicId) {
        "ember_bookmark" -> RelicRarity.FOUNDATION
        "moonlit_lens",
        "brass_quill",
        "ivory_bookplate" -> RelicRarity.RESONANT
        "astral_key" -> RelicRarity.ASCENDANT
        "veil_crown" -> RelicRarity.SOVEREIGN
        else -> RelicRarity.FOUNDATION
    }

internal fun emberBookmarkAwakened(profile: ReaderProfile): Boolean =
    profile.longestStreakDays >= 3
