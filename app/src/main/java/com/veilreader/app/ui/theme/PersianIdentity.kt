package com.veilreader.app.ui.theme

/**
 * Script-sensitive display policy for app-owned UI strings.
 *
 * Never run these transforms over publication text, book titles, author names, notes, highlights,
 * file names, identifiers, seal codes, URLs, or other user/source-owned content.
 */
enum class VeilDigitSet {
    LATIN,
    ARABIC_INDIC,
    EASTERN_ARABIC
}

data class VeilScriptOrnamentPolicy(
    val headerRuleWidthDp: Float,
    val headerTerminalMarks: Int,
    val eyebrowTrackingSp: Float,
    val metadataLabelWidthDp: Float
)

fun digitSetForLanguage(language: String): VeilDigitSet =
    when (language.lowercase()) {
        "fa", "ur", "ps", "ckb" -> VeilDigitSet.EASTERN_ARABIC
        "ar" -> VeilDigitSet.ARABIC_INDIC
        else -> VeilDigitSet.LATIN
    }

fun scriptOrnamentPolicyFor(script: VeilScriptGroup): VeilScriptOrnamentPolicy =
    when (script) {
        VeilScriptGroup.LATIN -> VeilScriptOrnamentPolicy(
            headerRuleWidthDp = 72f,
            headerTerminalMarks = 0,
            eyebrowTrackingSp = 1.75f,
            metadataLabelWidthDp = 82f
        )
        VeilScriptGroup.PERSIAN_ARABIC -> VeilScriptOrnamentPolicy(
            headerRuleWidthDp = 84f,
            headerTerminalMarks = 2,
            eyebrowTrackingSp = 0f,
            metadataLabelWidthDp = 104f
        )
    }

/**
 * Localizes ASCII numerals only for app-generated metrics and counters.
 *
 * Decimal/group separators are localized only when they sit between ASCII digits. This avoids
 * corrupting punctuation in normal prose. Percent is localized for Arabic-script languages.
 */
fun localizeAppNumerals(
    text: String,
    language: String
): String {
    val digitSet = digitSetForLanguage(language)
    if (digitSet == VeilDigitSet.LATIN) return text

    val zero = when (digitSet) {
        VeilDigitSet.ARABIC_INDIC -> '٠'
        VeilDigitSet.EASTERN_ARABIC -> '۰'
        VeilDigitSet.LATIN -> '0'
    }

    fun isAsciiDigitAt(index: Int): Boolean =
        index in text.indices && text[index] in '0'..'9'

    return buildString(text.length) {
        text.forEachIndexed { index, char ->
            when {
                char in '0'..'9' ->
                    append((zero.code + (char.code - '0'.code)).toChar())
                char == '%' ->
                    append('٪')
                char == '.' &&
                    isAsciiDigitAt(index - 1) &&
                    isAsciiDigitAt(index + 1) ->
                    append('٫')
                char == ',' &&
                    isAsciiDigitAt(index - 1) &&
                    isAsciiDigitAt(index + 1) ->
                    append('٬')
                else -> append(char)
            }
        }
    }
}

/** Unicode FSI/PDI keeps mixed-script metadata values from leaking direction into neighbors. */
fun bidiIsolate(value: String): String =
    if (value.isEmpty()) value else "\u2068$value\u2069"

fun appMetadataDivider(script: VeilScriptGroup): String =
    when (script) {
        VeilScriptGroup.LATIN -> " · "
        VeilScriptGroup.PERSIAN_ARABIC -> " · "
    }

fun localizedMetadataValue(
    value: String,
    language: String,
    localizeNumerals: Boolean = false
): String {
    val display = if (localizeNumerals) {
        localizeAppNumerals(value, language)
    } else {
        value
    }
    return bidiIsolate(display)
}
