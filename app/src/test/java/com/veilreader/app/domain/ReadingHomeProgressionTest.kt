package com.veilreader.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingHomeProgressionTest {
    @Test fun emptyAndCorruptNegativeCountersStartAtCamp() {
        val empty = deriveReadingHomeProgression(0, 0)
        assertEquals(empty, deriveReadingHomeProgression(-100, -200))
        assertEquals(ReadingHomeStage.CAMP, empty.stage)
        assertEquals(15, empty.minutesToNextStage)
        assertEquals(0f, empty.stageProgress, 0f)
    }

    @Test fun unfinishedLongNovelCanEarnEveryHome() {
        listOf(0 to ReadingHomeStage.CAMP, 14 to ReadingHomeStage.CAMP,
            15 to ReadingHomeStage.HUT, 119 to ReadingHomeStage.HUT,
            120 to ReadingHomeStage.HOUSE, 599 to ReadingHomeStage.HOUSE,
            600 to ReadingHomeStage.CASTLE).forEach { (minutes, expected) ->
            assertEquals(expected, deriveReadingHomeProgression(minutes, 0).stage)
        }
    }

    @Test fun restoredFinishedShelfCannotReplaceFirstActiveReading() {
        for (minutes in 0..14) {
            val home = deriveReadingHomeProgression(minutes, Int.MAX_VALUE)
            assertEquals(ReadingHomeStage.CAMP, home.stage)
            assertEquals(0, home.completionBoost)
            assertEquals(15 - minutes, home.minutesToNextStage)
        }
        assertEquals(ReadingHomeStage.HUT, deriveReadingHomeProgression(15, 100).stage)
    }

    @Test fun completionBoostCannotDominateConstructionOrInflateReadingTime() {
        for (minutes in 0..650) {
            val home = deriveReadingHomeProgression(minutes, Int.MAX_VALUE)
            assertEquals(minutes, home.activeMinutes)
            assertTrue(home.completionBoost <= 120)
            assertTrue(home.completionBoost * 4 <= home.activeMinutes)
            assertEquals(minutes.toLong() + home.completionBoost, home.growthUnits)
        }
        assertEquals(deriveReadingHomeProgression(480, 4).completionBoost,
            deriveReadingHomeProgression(480, 1000).completionBoost)
        assertEquals(ReadingHomeStage.CASTLE, deriveReadingHomeProgression(480, 4).stage)
    }

    @Test fun nextStageEstimateIsExactIncludingBonusAccrual() {
        for (books in 0..6) {
            for (minutes in 0..600) {
                val home = deriveReadingHomeProgression(minutes, books)
                val next = home.nextStage ?: continue
                assertTrue(home.minutesToNextStage > 0)
                assertEquals(next, deriveReadingHomeProgression(
                    minutes + home.minutesToNextStage, books).stage)
                assertEquals(home.stage, deriveReadingHomeProgression(
                    minutes + home.minutesToNextStage - 1, books).stage)
            }
        }
    }

    @Test fun growthNeverRegressesAsLifetimeEvidenceIncreases() {
        for (books in 0..5) {
            var previous = deriveReadingHomeProgression(0, books)
            for (minutes in 1..700) {
                val home = deriveReadingHomeProgression(minutes, books)
                assertTrue(home.growthUnits >= previous.growthUnits)
                assertTrue(home.stage.ordinal >= previous.stage.ordinal)
                assertTrue(home.stageProgress in 0f..1f)
                previous = home
            }
        }
    }

    @Test fun maximumIntegerCountersCannotOverflowIntoAnEarlierHome() {
        val home = deriveReadingHomeProgression(Int.MAX_VALUE, Int.MAX_VALUE)
        assertEquals(ReadingHomeStage.CASTLE, home.stage)
        assertEquals(Int.MAX_VALUE.toLong() + 120L, home.growthUnits)
        assertEquals(null, home.nextStage)
        assertEquals(0, home.minutesToNextStage)
        assertEquals(1f, home.stageProgress, 0f)
    }

    @Test fun addingCompletedBooksNeverRevokesConstruction() {
        for (minutes in 0..600) {
            var previous = deriveReadingHomeProgression(minutes, 0)
            for (books in 1..6) {
                val home = deriveReadingHomeProgression(minutes, books)
                assertTrue(home.stage.ordinal >= previous.stage.ordinal)
                assertTrue(home.growthUnits >= previous.growthUnits)
                previous = home
            }
        }
    }
}
