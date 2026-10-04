package com.veilreader.app.ui.reader.material

import com.veilreader.app.domain.PageMaterial

/** Inputs for the production mesh/sensory/release models, not alternate demo UI trees.
 * A review host supplies a publication snapshot (or deterministic text fixture in cloud tests).
 */
internal data class MaterialReviewState(
    val name: String, val progress: Float = .40f, val originY: Float = .85f,
    val tilt: Float = .08f, val material: PageMaterial = PageMaterial.MATTE,
    val age: Float = .15f, val rtl: Boolean = false, val persian: Boolean = false,
    val dark: Boolean = false, val sepia: Boolean = false, val reducedMotion: Boolean = false,
    val releaseTarget: Float? = null, val velocityPagesPerSecond: Float = 0f
) {
    fun sampledProgress(seconds: Float = .10f): Float = releaseTarget?.let {
        MaterialRelease(progress, it, velocityPagesPerSecond, PageMaterials.forId(material)).position(seconds)
    } ?: progress
}

internal object MaterialReviewStates {
    val paper = listOf(
        MaterialReviewState("idle", 0f),
        MaterialReviewState("corner-lift", .045f, .97f, -.12f),
        MaterialReviewState("medium-drag", .35f),
        MaterialReviewState("deep-curl", .60f),
        MaterialReviewState("almost-complete", .91f),
        MaterialReviewState("complete-release", .40f, releaseTarget = 1f),
        MaterialReviewState("cancel-release", .22f, releaseTarget = 0f),
        MaterialReviewState("fast-flick", .16f, material = PageMaterial.GLOSSY,
            releaseTarget = 1f, velocityPagesPerSecond = 3.5f),
        MaterialReviewState("slow-heavy-drag", .38f, material = PageMaterial.PARCHMENT),
        MaterialReviewState("glossy", material = PageMaterial.GLOSSY),
        MaterialReviewState("matte"),
        MaterialReviewState("parchment", material = PageMaterial.PARCHMENT),
        MaterialReviewState("papyrus", material = PageMaterial.PAPYRUS),
        MaterialReviewState("aged", age = 1f),
        MaterialReviewState("new", age = 0f),
        MaterialReviewState("ltr"),
        MaterialReviewState("rtl", rtl = true),
        MaterialReviewState("persian", rtl = true, persian = true),
        MaterialReviewState("dark", dark = true),
        MaterialReviewState("sepia", sepia = true),
        MaterialReviewState("reduced-motion", 0f, reducedMotion = true)
    )
    val slide = listOf("idle", "active-transition", "completed-transition", "rtl", "large-text", "persian")
}
