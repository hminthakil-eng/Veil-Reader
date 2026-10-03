package com.veilreader.app.data

import com.veilreader.app.domain.ReadingPaceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
class ReadingPaceStoreCodecTest {
    @Test
    fun `empty and malformed payloads fail calm`() {
        assertEquals(emptyMap<String, ReadingPaceProfile>(), decodeReadingPaceProfiles(null))
        assertEquals(emptyMap<String, ReadingPaceProfile>(), decodeReadingPaceProfiles(""))
        assertEquals(emptyMap<String, ReadingPaceProfile>(), decodeReadingPaceProfiles("{bad"))
    }

    @Test
    fun `codec round trips valid per-book aggregates`() {
        val expected = mapOf(
            "book-a" to ReadingPaceProfile(
                sampleCount = 12,
                meanMillisPerPage = 42_000.0,
                m2MillisSquared = 8_000_000.0,
                totalObservedMillis = 504_000L
            ),
            "book-b" to ReadingPaceProfile(
                sampleCount = 20,
                meanMillisPerPage = 31_000.0,
                m2MillisSquared = 4_000_000.0,
                totalObservedMillis = 620_000L
            )
        )

        val decoded = decodeReadingPaceProfiles(
            encodeReadingPaceProfiles(expected)
        )

        assertEquals(expected, decoded)
    }

    @Test
    fun `invalid and empty records are dropped rather than resurrected`() {
        val decoded = decodeReadingPaceProfiles(
            """
            {
              "valid":{"n":8,"mean":30000.0,"m2":4000.0,"total":240000},
              "empty":{"n":0,"mean":0.0,"m2":0.0,"total":0},
              "negative":{"n":4,"mean":-20.0,"m2":-1.0,"total":-3}
            }
            """.trimIndent()
        )

        assertEquals(setOf("valid"), decoded.keys)
        assertTrue(decoded.getValue("valid").meanMillisPerPage > 0.0)
    }

    @Test
    fun `encoder ignores blank ids and zero sample profiles`() {
        val encoded = encodeReadingPaceProfiles(
            mapOf(
                " " to ReadingPaceProfile(
                    sampleCount = 8,
                    meanMillisPerPage = 30_000.0
                ),
                "empty" to ReadingPaceProfile()
            )
        )

        assertEquals(emptyMap<String, ReadingPaceProfile>(), decodeReadingPaceProfiles(encoded))
    }
}
