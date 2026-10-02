package com.veilreader.rd

enum class LabStage { DISCOVERY, PROTOTYPE, VALIDATED, ARENA_CANDIDATE, INTEGRATION_CANDIDATE, HOLD, REJECTED }

data class ReaderLab(
    val id: String,
    val name: String,
    val stage: LabStage,
    val productionDependencyAllowed: Boolean = false
)

object ReaderLabRegistry {
    val all: List<ReaderLab> = listOf(
        ReaderLab("RDL-01", "Appearance Profiles", LabStage.PROTOTYPE),
        ReaderLab("RDL-02", "Input Profiles", LabStage.PROTOTYPE),
        ReaderLab("RDL-03", "Reading Ruler / Focus", LabStage.PROTOTYPE),
        ReaderLab("RDL-04", "PDF Annotations", LabStage.PROTOTYPE),
        ReaderLab("RDL-05", "Context Tools", LabStage.PROTOTYPE),
        ReaderLab("RDL-06", "TTS", LabStage.PROTOTYPE),
        ReaderLab("RDL-07", "Auto-scroll", LabStage.PROTOTYPE),
        ReaderLab("RDL-08", "Catalogs", LabStage.PROTOTYPE),
        ReaderLab("RDL-09", "Sync", LabStage.PROTOTYPE),
        ReaderLab("RDL-10", "Legacy Formats", LabStage.PROTOTYPE),
        ReaderLab("RDL-11", "Library Metadata", LabStage.PROTOTYPE),
        ReaderLab("RDL-12", "Name Replacement", LabStage.PROTOTYPE),
        ReaderLab("RDL-13", "E-Ink", LabStage.PROTOTYPE),
        ReaderLab("RDL-14", "App Lock", LabStage.PROTOTYPE),
        ReaderLab("RDL-15", "Widget / Shortcut", LabStage.PROTOTYPE),
        ReaderLab("RDL-16", "Reading Calendar", LabStage.PROTOTYPE),
        ReaderLab("RDL-17", "Page-turn Renderer", LabStage.PROTOTYPE)
    )
}
