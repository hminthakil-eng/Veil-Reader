package com.veilreader.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Grayfog shell geometry for Home and Archive.
 *
 * Reader Sanctuary owns its own visual contract; these tokens only shape the atmospheric shell.
 */
object ShellVisualGeometry {
    val HomeHeroRadius = 16.dp
    val RecentBookRadius = 12.dp
    val ArchiveHeaderRadius = 14.dp

    val ArchiveReturningCompactMinHeight = 72.dp
    val ArchiveReturningWideMinHeight = 88.dp
    val ArchiveEmptyCompactMinHeight = 112.dp
    val ArchiveEmptyWideMinHeight = 144.dp
}

object ShellVisualOpacity {
    const val RecentAccent = 0.44f
    const val RecentRule = 0.22f
    const val ReturningArchiveImage = 0.72f
}
