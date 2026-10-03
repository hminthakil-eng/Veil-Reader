package com.veilreader.app.ui.reader.material

/**
 * Canonical source-level definition of the physical reading surfaces supported by
 * Material Page Engine v1. Values are deliberately normalized and renderer-agnostic
 * so physics, geometry, acoustics and haptics can share one material identity.
 */
internal enum class MaterialPagePreset {
    GLOSSY,
    MATTE_BOOK,
    PARCHMENT,
    PAPYRUS,
    MANUSCRIPT
}

internal data class MaterialPagePhysicsProfile(
    val apparentMass: Float,
    val bendStiffness: Float,
    val dragResistance: Float,
    val settleDamping: Float,
    val bindingConstraint: Float,
    val completionThreshold: Float,
    val cancelThreshold: Float,
    val flickVelocityDpPerSec: Float
)

internal data class MaterialPageOpticalProfile(
    val frontArgb: Long,
    val backArgb: Long,
    val edgeArgb: Long,
    val specularResponse: Float,
    val roughness: Float,
    val translucency: Float,
    val inkGhosting: Float,
    val grain: Float,
    val directionalFiber: Float,
    val edgeBody: Float,
    val patinaResponse: Float
)

internal data class MaterialPageSensoryProfile(
    val acousticBrightness: Float,
    val acousticDryness: Float,
    val acousticBody: Float,
    val acousticFiber: Float,
    val hapticSharpness: Float,
    val hapticWeight: Float,
    val liftThreshold: Float
)

internal data class MaterialPageProfile(
    val preset: MaterialPagePreset,
    val physics: MaterialPagePhysicsProfile,
    val optics: MaterialPageOpticalProfile,
    val sensory: MaterialPageSensoryProfile
)

internal object MaterialPageProfiles {
    val Glossy = MaterialPageProfile(
        preset = MaterialPagePreset.GLOSSY,
        physics = MaterialPagePhysicsProfile(
            apparentMass = 0.72f,
            bendStiffness = 0.88f,
            dragResistance = 0.66f,
            settleDamping = 0.94f,
            bindingConstraint = 0.84f,
            completionThreshold = 0.31f,
            cancelThreshold = 0.11f,
            flickVelocityDpPerSec = 820f
        ),
        optics = MaterialPageOpticalProfile(
            frontArgb = 0xFFF7F5F0L,
            backArgb = 0xFFF0EEE9L,
            edgeArgb = 0xFFFFFFFFL,
            specularResponse = 0.78f,
            roughness = 0.22f,
            translucency = 0.16f,
            inkGhosting = 0.06f,
            grain = 0.05f,
            directionalFiber = 0.02f,
            edgeBody = 0.34f,
            patinaResponse = 0.08f
        ),
        sensory = MaterialPageSensoryProfile(
            acousticBrightness = 0.88f,
            acousticDryness = 0.20f,
            acousticBody = 0.30f,
            acousticFiber = 0.04f,
            hapticSharpness = 0.82f,
            hapticWeight = 0.34f,
            liftThreshold = 0.10f
        )
    )

    val MatteBook = MaterialPageProfile(
        preset = MaterialPagePreset.MATTE_BOOK,
        physics = MaterialPagePhysicsProfile(
            apparentMass = 0.92f,
            bendStiffness = 0.74f,
            dragResistance = 0.80f,
            settleDamping = 0.93f,
            bindingConstraint = 0.88f,
            completionThreshold = 0.34f,
            cancelThreshold = 0.13f,
            flickVelocityDpPerSec = 900f
        ),
        optics = MaterialPageOpticalProfile(
            frontArgb = 0xFFF2E9D8L,
            backArgb = 0xFFE7DCC8L,
            edgeArgb = 0xFFF9F1E4L,
            specularResponse = 0.24f,
            roughness = 0.76f,
            translucency = 0.20f,
            inkGhosting = 0.10f,
            grain = 0.22f,
            directionalFiber = 0.10f,
            edgeBody = 0.48f,
            patinaResponse = 0.38f
        ),
        sensory = MaterialPageSensoryProfile(
            acousticBrightness = 0.52f,
            acousticDryness = 0.52f,
            acousticBody = 0.48f,
            acousticFiber = 0.22f,
            hapticSharpness = 0.48f,
            hapticWeight = 0.50f,
            liftThreshold = 0.12f
        )
    )

    val Parchment = MaterialPageProfile(
        preset = MaterialPagePreset.PARCHMENT,
        physics = MaterialPagePhysicsProfile(
            apparentMass = 1.18f,
            bendStiffness = 0.58f,
            dragResistance = 1.02f,
            settleDamping = 0.91f,
            bindingConstraint = 0.93f,
            completionThreshold = 0.38f,
            cancelThreshold = 0.16f,
            flickVelocityDpPerSec = 1_040f
        ),
        optics = MaterialPageOpticalProfile(
            frontArgb = 0xFFE7D0A3L,
            backArgb = 0xFFD8BC88L,
            edgeArgb = 0xFFF0DDB8L,
            specularResponse = 0.16f,
            roughness = 0.88f,
            translucency = 0.13f,
            inkGhosting = 0.08f,
            grain = 0.38f,
            directionalFiber = 0.24f,
            edgeBody = 0.72f,
            patinaResponse = 0.76f
        ),
        sensory = MaterialPageSensoryProfile(
            acousticBrightness = 0.31f,
            acousticDryness = 0.69f,
            acousticBody = 0.72f,
            acousticFiber = 0.46f,
            hapticSharpness = 0.34f,
            hapticWeight = 0.72f,
            liftThreshold = 0.15f
        )
    )

    val Papyrus = MaterialPageProfile(
        preset = MaterialPagePreset.PAPYRUS,
        physics = MaterialPagePhysicsProfile(
            apparentMass = 1.08f,
            bendStiffness = 0.64f,
            dragResistance = 0.96f,
            settleDamping = 0.90f,
            bindingConstraint = 0.90f,
            completionThreshold = 0.37f,
            cancelThreshold = 0.15f,
            flickVelocityDpPerSec = 980f
        ),
        optics = MaterialPageOpticalProfile(
            frontArgb = 0xFFE1C892L,
            backArgb = 0xFFD4B77DL,
            edgeArgb = 0xFFEBD7A9L,
            specularResponse = 0.10f,
            roughness = 0.94f,
            translucency = 0.10f,
            inkGhosting = 0.06f,
            grain = 0.58f,
            directionalFiber = 0.88f,
            edgeBody = 0.62f,
            patinaResponse = 0.68f
        ),
        sensory = MaterialPageSensoryProfile(
            acousticBrightness = 0.38f,
            acousticDryness = 0.88f,
            acousticBody = 0.58f,
            acousticFiber = 0.86f,
            hapticSharpness = 0.48f,
            hapticWeight = 0.64f,
            liftThreshold = 0.14f
        )
    )

    val Manuscript = MaterialPageProfile(
        preset = MaterialPagePreset.MANUSCRIPT,
        physics = MaterialPagePhysicsProfile(
            apparentMass = 1.38f,
            bendStiffness = 0.52f,
            dragResistance = 1.14f,
            settleDamping = 0.90f,
            bindingConstraint = 0.96f,
            completionThreshold = 0.41f,
            cancelThreshold = 0.18f,
            flickVelocityDpPerSec = 1_160f
        ),
        optics = MaterialPageOpticalProfile(
            frontArgb = 0xFFB99A69L,
            backArgb = 0xFFA98758L,
            edgeArgb = 0xFFC8AA79L,
            specularResponse = 0.20f,
            roughness = 0.82f,
            translucency = 0.04f,
            inkGhosting = 0.02f,
            grain = 0.44f,
            directionalFiber = 0.32f,
            edgeBody = 0.92f,
            patinaResponse = 0.86f
        ),
        sensory = MaterialPageSensoryProfile(
            acousticBrightness = 0.22f,
            acousticDryness = 0.64f,
            acousticBody = 0.92f,
            acousticFiber = 0.52f,
            hapticSharpness = 0.28f,
            hapticWeight = 0.92f,
            liftThreshold = 0.17f
        )
    )

    val all: List<MaterialPageProfile> = listOf(
        Glossy,
        MatteBook,
        Parchment,
        Papyrus,
        Manuscript
    )

    fun canonical(preset: MaterialPagePreset): MaterialPageProfile =
        when (preset) {
            MaterialPagePreset.GLOSSY -> Glossy
            MaterialPagePreset.MATTE_BOOK -> MatteBook
            MaterialPagePreset.PARCHMENT -> Parchment
            MaterialPagePreset.PAPYRUS -> Papyrus
            MaterialPagePreset.MANUSCRIPT -> Manuscript
        }
}
