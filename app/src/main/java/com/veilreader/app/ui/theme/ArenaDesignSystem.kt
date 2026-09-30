package com.veilreader.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Arena visual language — W26 canonical source of truth.
 *
 * This is not a second theme. Existing Veil* tokens delegate to these values while screens are
 * rebuilt in-place. The target is the approved reference set: blue-black cathedral/archive shell,
 * antique brass linework, parchment reading material, sparse oxblood accents, and almost no
 * generic rounded-card language.
 */
object ArenaPalette {
    val Void = Color(0xFF05070A)
    val Cathedral = Color(0xFF09111A)
    val Archive = Color(0xFF0D151F)
    val RaisedArchive = Color(0xFF141F2B)
    val Iron = Color(0xFF1A2734)

    val Moon = Color(0xFFF3E8D4)
    val Mist = Color(0xFFA8A39A)
    val Ash = Color(0xFF6E737A)

    val AntiqueGold = Color(0xFFD7B77A)
    val Brass = Color(0xFFC99A52)
    val DeepBrass = Color(0xFF4A321A)
    val Candle = Color(0xFFF2C47B)

    val Oxblood = Color(0xFF6D2028)
    val OxbloodDeep = Color(0xFF321116)

    val Parchment = Color(0xFFE9D8B7)
    val ParchmentLight = Color(0xFFF4E7CC)
    val ParchmentShadow = Color(0xFFC7AC7D)
    val InkOnPaper = Color(0xFF2A2118)

    val Border = Color(0xFF34414E)
    val StrongBorder = Color(0xFF6B7781)
    val GoldHairline = Color(0xFF7D6033)
}

object ArenaGeometry {
    val Hairline = 1.dp
    val Emphasis = 1.5.dp

    val PlateRadius = 2.dp
    val FolioRadius = 3.dp
    val ArchitectureRadius = 4.dp
    val ChamberRadius = 8.dp
    val HeroRadius = 10.dp
    val SealRadius = 999.dp

    val TouchTarget = 48.dp
    val CompactTouchTarget = 44.dp
}

object ArenaOpacity {
    const val Primary = 1f
    const val Secondary = 0.82f
    const val Muted = 0.64f
    const val Hairline = 0.36f
    const val Ghost = 0.14f
    const val CandleGlow = 0.12f
}

/**
 * Reference density contract.
 *
 * Reader/Sanctuary intentionally has the lowest decoration. Threshold and Archive may carry
 * authored imagery and architectural linework. Castle/Ritual/Sanctum can become richer, but text
 * and tap targets always win over ornament.
 */
data class ArenaDensityBudget(
    val authoredImage: Float,
    val ornament: Float,
    val atmosphere: Float,
    val motion: Float
)

fun arenaDensityFor(realm: VeilRealm): ArenaDensityBudget =
    when (realm) {
        VeilRealm.SANCTUARY -> ArenaDensityBudget(0.00f, 0.00f, 0.04f, 0.08f)
        VeilRealm.THRESHOLD -> ArenaDensityBudget(0.86f, 0.34f, 0.56f, 0.26f)
        VeilRealm.ARCHIVE -> ArenaDensityBudget(0.68f, 0.48f, 0.50f, 0.22f)
        VeilRealm.CASTLE,
        VeilRealm.WORLD -> ArenaDensityBudget(0.62f, 0.66f, 0.72f, 0.42f)
        VeilRealm.RITUAL -> ArenaDensityBudget(0.46f, 0.82f, 0.74f, 0.58f)
        VeilRealm.SANCTUM -> ArenaDensityBudget(0.54f, 0.74f, 0.78f, 0.28f)
    }
