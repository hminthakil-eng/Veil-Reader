package com.veilreader.app.domain

enum class PathArchitecturalMotif {
    CIPHER, DREAM, ARCHIVE, VANGUARD, NOCTURNE, ARTIFICE
}

data class PathWorldSignature(
    val motif: PathArchitecturalMotif,
    val strength: Float,
    val ornamentCount: Int,
    val inscription: String
)

fun derivePathWorldSignature(
    pathId: String,
    rankIndex: Int,
    rankCount: Int,
    ritualCharge: Float
): PathWorldSignature {
    val count = rankCount.coerceAtLeast(1)
    val rank = rankIndex.coerceIn(0, count - 1)
    val growth = if (count <= 1) 0f else rank.toFloat() / (count - 1).toFloat()
    val ritual = ritualCharge.coerceIn(0f, 1f)
    val strength = (0.28f + growth * 0.48f + ritual * 0.24f).coerceIn(0f, 1f)
    val ornaments = (2 + rank + (ritual * 2f).toInt()).coerceIn(2, 9)

    val motif = when (pathId) {
        "dreamwalker" -> PathArchitecturalMotif.DREAM
        "archivist" -> PathArchitecturalMotif.ARCHIVE
        "vanguard" -> PathArchitecturalMotif.VANGUARD
        "nocturne" -> PathArchitecturalMotif.NOCTURNE
        "artificer" -> PathArchitecturalMotif.ARTIFICE
        else -> PathArchitecturalMotif.CIPHER
    }
    val inscription = when (motif) {
        PathArchitecturalMotif.CIPHER -> "Cipher rings are aligning above the central archive."
        PathArchitecturalMotif.DREAM -> "Impossible arches are beginning to overlap the known geometry."
        PathArchitecturalMotif.ARCHIVE -> "New ribs of shelving are entering the load-bearing stone."
        PathArchitecturalMotif.VANGUARD -> "The Hall is taking on sharper lines and a forward-bearing stance."
        PathArchitecturalMotif.NOCTURNE -> "Light is withdrawing from the upper vault around a darkened orbit."
        PathArchitecturalMotif.ARTIFICE -> "Measured teeth and radial marks are appearing beneath the brasswork."
    }
    return PathWorldSignature(motif, strength, ornaments, inscription)
}
