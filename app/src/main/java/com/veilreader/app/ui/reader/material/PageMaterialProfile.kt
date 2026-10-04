package com.veilreader.app.ui.reader.material

import com.veilreader.app.domain.PageMaterial

/** All coefficients are dimensionless except thicknessDp and acoustic duration. */
internal data class PageMaterialProfile(
    val id: PageMaterial,
    val mass: Float,
    val stiffness: Float,
    val resistance: Float,
    val radiusFraction: Float,
    val thicknessDp: Float,
    val roughness: Float,
    val specular: Float,
    val translucency: Float,
    val grain: Float,
    val directionalFibre: Float,
    val warmth: Float,
    val edgeIrregularity: Float,
    val soundSeconds: Float,
    val soundFilter: Float,
    val soundGain: Float,
    val hapticWeight: Float
)

internal object PageMaterials {
    val glossy = PageMaterialProfile(PageMaterial.GLOSSY, .72f, 1.18f, .10f, .070f,
        .55f, .20f, .25f, .035f, .008f, 0f, .015f, .01f, .070f, .38f, .18f, .35f)
    val matte = PageMaterialProfile(PageMaterial.MATTE, 1f, 1f, .17f, .085f,
        .80f, .78f, .075f, .065f, .018f, .08f, .035f, .03f, .115f, .77f, .22f, .50f)
    val parchment = PageMaterialProfile(PageMaterial.PARCHMENT, 1.52f, 1.32f, .25f, .112f,
        1.35f, .87f, .055f, .027f, .023f, .12f, .10f, .08f, .170f, .86f, .24f, .78f)
    val papyrus = PageMaterialProfile(PageMaterial.PAPYRUS, 1.20f, 1.60f, .29f, .095f,
        1.10f, .94f, .025f, .018f, .026f, .90f, .08f, .12f, .145f, .58f, .20f, .68f)
    fun forId(id: PageMaterial): PageMaterialProfile = when (id) {
        PageMaterial.GLOSSY -> glossy
        PageMaterial.MATTE -> matte
        PageMaterial.PARCHMENT -> parchment
        PageMaterial.PAPYRUS -> papyrus
    }
}

internal fun finiteUnit(value: Float): Float = if (value.isFinite()) value.coerceIn(0f, 1f) else 0f
