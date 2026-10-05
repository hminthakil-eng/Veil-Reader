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
    val CompactTouchTarget = TouchTarget
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
    val motion: Float,
    val richness: Float
)

fun arenaDensityFor(realm: VeilRealm): ArenaDensityBudget =
    when (realm) {
        VeilRealm.SANCTUARY -> ArenaDensityBudget(0.00f, 0.00f, 0.04f, 0.08f, 0.05f)
        VeilRealm.THRESHOLD -> ArenaDensityBudget(0.86f, 0.34f, 0.56f, 0.26f, 0.48f)
        VeilRealm.ARCHIVE -> ArenaDensityBudget(0.68f, 0.48f, 0.50f, 0.22f, 0.58f)
        VeilRealm.CASTLE,
        VeilRealm.WORLD -> ArenaDensityBudget(0.62f, 0.66f, 0.72f, 0.42f, 0.76f)
        VeilRealm.RITUAL -> ArenaDensityBudget(0.46f, 0.82f, 0.74f, 0.58f, 0.86f)
        VeilRealm.SANCTUM -> ArenaDensityBudget(0.54f, 0.74f, 0.78f, 0.28f, 0.82f)
    }

/** Semantic material aliases; no second palette or per-screen color system. */
object VeilMaterials {
    val RealmBackground = ArenaPalette.Void
    val Surface = ArenaPalette.Archive
    val ElevatedSurface = ArenaPalette.RaisedArchive
    val Parchment = ArenaPalette.Parchment
    val Ink = ArenaPalette.InkOnPaper
    val Brass = ArenaPalette.AntiqueGold
    val Moonlight = ArenaPalette.Moon
    val TextPrimary = ArenaPalette.Moon
    val TextSecondary = ArenaPalette.Mist
    // Readable archival metadata; ornament may use Ash, text must not.
    val TextMuted = ArenaPalette.Mist
    val Divider = ArenaPalette.Border
    val Frame = ArenaPalette.GoldHairline
    val Ornament = ArenaPalette.Brass
    val Depth = ArenaPalette.Cathedral
    // World fields remain continuous behind useful rooms, rather than boxed into a card.
    val WorldFieldWash = Depth.copy(alpha = 0.28f)
    val WorldFieldFoundation = RealmBackground.copy(alpha = 0.90f)
    val ChamberRecess = Surface.copy(alpha = 0.40f)
    val ChamberFoundation = RealmBackground.copy(alpha = 0.86f)
    val Error = Color(0xFFFFB4AB)
    val Warning = Color(0xFFE2C18B)
    val Success = Color(0xFFA9C6B2)
}

/** The general archive inset is deliberately quieter than an artifact or ceremonial chamber. */
object VeilFrame {
    val Inset = 6.dp
    val CornerLength = 16.dp
    const val StructuralAlpha = 0.28f
    const val OrnamentStrength = 0.24f
}
