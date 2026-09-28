package com.veilreader.app.ui.screens

import com.veilreader.app.data.SampleData
import org.junit.Assert.assertEquals
import org.junit.Test

class PathLocalizationContractTest {
    @Test
    fun `every canonical Path has identity and doctrine localization`() {
        assertEquals(
            SampleData.paths.map { it.id }.toSet(),
            knownLocalizedPathIds()
        )
    }

    @Test
    fun `every canonical Path localizes every rank exactly once`() {
        SampleData.paths.forEach { path ->
            assertEquals(
                "rank resource count for ${path.id}",
                path.ranks.size,
                localizedPathRankResourceCount(path.id)
            )
        }
    }
}
