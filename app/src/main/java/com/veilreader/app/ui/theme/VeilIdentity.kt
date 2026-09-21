package com.veilreader.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Veil identity foundations.
 *
 * Screens should consume semantic MaterialTheme roles whenever possible. These raw identity values
 * exist to keep the visual language coherent; they are not an invitation to hard-code brand colors
 * inside feature composables.
 */
object VeilIdentity {
    const val NORTH_STAR = "The Library Beyond the Veil"
    const val PRINCIPLE = "Outside the story, Veil may speak. Inside the story, Veil falls silent."
}

enum class VeilRealm {
    ARCHIVE,
    THRESHOLD,
    SANCTUARY
}

object VeilIdentityColor {
    // Dark / archival world
    val Obsidian = Color(0xFF0A0B0E)
    val Ink = Color(0xFF151820)
    val Slate = Color(0xFF20242D)
    val RaisedSlate = Color(0xFF2A2F39)

    // Light / editorial world
    val Ivory = Color(0xFFF4F0E6)
    val WarmPaper = Color(0xFFFAF8F2)
    val InkOnPaper = Color(0xFF25231F)

    // Identity accents
    val AgedBrass = Color(0xFFB49A63)
    val DeepBrass = Color(0xFF765E35)
    val Moonlight = Color(0xFFD5E5F4)

    // Supporting text / rules
    val MistOnDark = Color(0xFFB5B0A5)
    val MistOnLight = Color(0xFF6E675C)
    val RuleDark = Color(0xFF3A3F49)
    val RuleLight = Color(0xFFD7D0C4)
}

object VeilRadius {
    val Precision = 4.dp
    val Control = 8.dp
    val Panel = 12.dp
    val Feature = 16.dp
}

object VeilMotionGrammar {
    const val RESPONSE_FAST_MS = 100
    const val RESPONSE_MS = 140
    const val CONTINUITY_MS = 220
    const val THRESHOLD_MS = 320

    /** Ambient motion is opt-in and never used on the reading canvas. */
    const val AMBIENT_MIN_MS = 1200
}

object VeilReaderFoundation {
    /** Reader chrome should feel lighter than Archive surfaces. */
    const val CHROME_SURFACE_ALPHA = 0.94f

    /** Avoid fully opaque visual slabs floating over prose unless accessibility requires it. */
    const val CHROME_SCRIM_ALPHA = 0.10f

    /** Stronger decorative atmosphere belongs outside Sanctuary. */
    const val SANCTUARY_DECORATION_ALPHA = 0f
}
