package com.veilreader.app.ui.screens

import com.veilreader.app.domain.PathArchitecturalMotif
import com.veilreader.app.domain.ReaderProfile
import com.veilreader.app.domain.ReadingPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObservatoryPathLensPolicyTest {
    private val path = ReadingPath(
        id = "nocturne",
        name = "Nocturne",
        epithet = "Night",
        description = "",
        ranks = listOf("Listener", "Watcher", "Shade", "Nocturne", "Nightwarden", "Eclipse")
    )

    @Test
    fun `missing profile leaves the factual atlas without a gamification lens`() {
        assertNull(observatoryPathSignature(null))
    }

    @Test
    fun `path lens reflects path and ritual while atlas data stays externally owned`() {
        val profile = ReaderProfile(
            level = 8,
            xp = 20,
            xpForNextLevel = 400,
            streakDays = 4,
            pagesRead = 500,
            minutesRead = 900,
            booksFinished = 5,
            path = path,
            rankIndex = 3,
            ritualProgress = 8,
            ritualTarget = 10
        )

        val signature = requireNotNull(observatoryPathSignature(profile))

        assertEquals(PathArchitecturalMotif.NOCTURNE, signature.motif)
        assertTrue(signature.strength in 0f..1f)
        assertTrue(signature.ornamentCount in 2..9)
    }
}
