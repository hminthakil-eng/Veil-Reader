package com.veilreader.rd

enum class LegacyFormat {
    MOBI, AZW3, FB2, CHM, DJVU, DOCX, ODT, RTF, MHTML, UMD, UNKNOWN
}

data class FormatAdapterCapability(
    val format: LegacyFormat,
    val canOpen: Boolean,
    val canSearch: Boolean = false,
    val canSelectText: Boolean = false,
    val canAnnotate: Boolean = false
)

object LegacyFormatDetector {
    fun fromFileName(name: String): LegacyFormat {
        val lower = name.substringBefore('?').substringBefore('#').lowercase()
        return when {
            lower.endsWith(".mobi") || lower.endsWith(".prc") -> LegacyFormat.MOBI
            lower.endsWith(".azw3") -> LegacyFormat.AZW3
            lower.endsWith(".fb2") || lower.endsWith(".fb2.zip") -> LegacyFormat.FB2
            lower.endsWith(".chm") -> LegacyFormat.CHM
            lower.endsWith(".djvu") || lower.endsWith(".djv") -> LegacyFormat.DJVU
            lower.endsWith(".docx") -> LegacyFormat.DOCX
            lower.endsWith(".odt") -> LegacyFormat.ODT
            lower.endsWith(".rtf") -> LegacyFormat.RTF
            lower.endsWith(".mht") || lower.endsWith(".mhtml") -> LegacyFormat.MHTML
            lower.endsWith(".umd") -> LegacyFormat.UMD
            else -> LegacyFormat.UNKNOWN
        }
    }
}
