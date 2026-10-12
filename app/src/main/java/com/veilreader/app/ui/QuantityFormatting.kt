package com.veilreader.app.ui

/** Android selects quantities with Int; the original count stays in the format arguments. */
internal fun resourceQuantity(value: Int): Int = value

internal fun resourceQuantity(value: Long): Int =
    value.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
