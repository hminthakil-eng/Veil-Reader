package com.veilreader.app.domain

enum class ReaderHardwareKeyAction {
    SYSTEM,
    PREVIOUS_PAGE,
    NEXT_PAGE,
    TOGGLE_CONTROLS
}

data class ReaderHardwareKeyMap(
    val volumeUp: ReaderHardwareKeyAction = ReaderHardwareKeyAction.SYSTEM,
    val volumeDown: ReaderHardwareKeyAction = ReaderHardwareKeyAction.SYSTEM
)

fun decodeReaderHardwareKeyAction(
    raw: String?,
    fallback: ReaderHardwareKeyAction = ReaderHardwareKeyAction.SYSTEM
): ReaderHardwareKeyAction =
    runCatching {
        ReaderHardwareKeyAction.valueOf(raw.orEmpty())
    }.getOrDefault(fallback)
