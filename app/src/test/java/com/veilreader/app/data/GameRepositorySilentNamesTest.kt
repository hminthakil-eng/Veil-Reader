package com.veilreader.app.data

import android.content.Context
import com.veilreader.app.domain.SilentNamesChoice
import com.veilreader.app.domain.SilentNamesEncounter
import com.veilreader.app.domain.SilentNamesMode
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class GameRepositorySilentNamesTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("veil_game_v1", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        context.getSharedPreferences("veil_game_v1", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `first seal persists one receipt and revisit never rerolls`() = runBlocking {
        val repository = GameRepository(context)
        assertTrue(repository.choosePath("oracle"))

        val rolls = AtomicInteger(0)
        val first = repository.sealSilentNamesEncounter(
            choice = SilentNamesChoice.EXAMINE_SEAL,
            mode = SilentNamesMode.DICE,
            nextD20 = {
                rolls.incrementAndGet()
                7
            },
            nowEpochMs = { 1_000L }
        )

        val recreatedRepository = GameRepository(context)
        val replay = recreatedRepository.sealSilentNamesEncounter(
            choice = SilentNamesChoice.SPEAK_TO_KEEPER,
            mode = SilentNamesMode.STORY,
            nextD20 = {
                rolls.incrementAndGet()
                20
            },
            nowEpochMs = { 9_999L }
        )

        assertTrue(first.newlyCommitted)
        assertFalse(replay.newlyCommitted)
        assertEquals(first.receipt, replay.receipt)
        assertEquals(1, rolls.get())
        assertTrue(recreatedRepository.ownsSilentNamesReward())
        assertEquals(SilentNamesEncounter.REWARD_ID, replay.receipt.rewardId)
    }

    @Test
    fun `two repository instances racing create only one durable result`() {
        val firstRepository = GameRepository(context)
        val secondRepository = GameRepository(context)
        val start = CountDownLatch(1)
        val rolls = AtomicInteger(0)
        val pool = Executors.newFixedThreadPool(2)

        try {
            val one = pool.submit<SilentNamesCommitResult> {
                start.await(5, TimeUnit.SECONDS)
                runBlocking { firstRepository.sealSilentNamesEncounter(
                    choice = SilentNamesChoice.EXAMINE_SEAL,
                    mode = SilentNamesMode.DICE,
                    nextD20 = {
                        rolls.incrementAndGet()
                        3
                    },
                    nowEpochMs = { 2_000L }
                ) }
            }
            val two = pool.submit<SilentNamesCommitResult> {
                start.await(5, TimeUnit.SECONDS)
                runBlocking { secondRepository.sealSilentNamesEncounter(
                    choice = SilentNamesChoice.FOLLOW_LIGHT,
                    mode = SilentNamesMode.DICE,
                    nextD20 = {
                        rolls.incrementAndGet()
                        19
                    },
                    nowEpochMs = { 3_000L }
                ) }
            }

            start.countDown()
            val results = listOf(one.get(5, TimeUnit.SECONDS), two.get(5, TimeUnit.SECONDS))

            assertEquals(1, results.count { it.newlyCommitted })
            assertEquals(1, rolls.get())
            assertEquals(1, results.map { it.receipt }.toSet().size)
            assertEquals(results.first().receipt, GameRepository(context).silentNamesReceipt())
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `unsupported receipt is retained visible and fails closed without read crash`() {
        val prefs = context.getSharedPreferences("veil_game_v1", Context.MODE_PRIVATE)
        val raw = """{"encounterId":"silent_names_window","contentVersion":99}"""
        assertTrue(prefs.edit().putString("encounter:silent_names_window:receipt", raw).commit())

        val repository = GameRepository(context)
        val state = repository.silentNamesStorageState()
        assertTrue(state is SilentNamesStorageState.Unsupported)
        assertEquals(99, (state as SilentNamesStorageState.Unsupported).contentVersion)
        assertEquals(raw, state.raw)
        assertNull(repository.silentNamesReceipt())
        assertFalse(repository.ownsSilentNamesReward())

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                repository.sealSilentNamesEncounter(
                    choice = SilentNamesChoice.EXAMINE_SEAL,
                    mode = SilentNamesMode.STORY,
                    nowEpochMs = { 4_000L }
                )
            }
        }

        assertEquals(raw, prefs.getString("encounter:silent_names_window:receipt", null))
    }

    @Test
    fun `corrupt receipt is retained visible and cannot be replayed into a reward`() {
        val prefs = context.getSharedPreferences("veil_game_v1", Context.MODE_PRIVATE)
        val raw = "{not-json"
        assertTrue(prefs.edit().putString("encounter:silent_names_window:receipt", raw).commit())

        val repository = GameRepository(context)
        val state = repository.silentNamesStorageState()
        assertTrue(state is SilentNamesStorageState.Corrupt)
        assertEquals(raw, (state as SilentNamesStorageState.Corrupt).raw)
        assertNull(repository.silentNamesReceipt())
        assertFalse(repository.ownsSilentNamesReward())

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                repository.sealSilentNamesEncounter(
                    choice = SilentNamesChoice.FOLLOW_LIGHT,
                    mode = SilentNamesMode.DICE,
                    nextD20 = { 20 },
                    nowEpochMs = { 4_100L }
                )
            }
        }
        assertEquals(raw, prefs.getString("encounter:silent_names_window:receipt", null))
    }

    @Test
    fun `story mode seals without consuming randomness`() = runBlocking {
        val repository = GameRepository(context)
        val rolls = AtomicInteger(0)

        val result = repository.sealSilentNamesEncounter(
            choice = SilentNamesChoice.FOLLOW_LIGHT,
            mode = SilentNamesMode.STORY,
            nextD20 = {
                rolls.incrementAndGet()
                20
            },
            nowEpochMs = { 5_000L }
        )

        assertTrue(result.newlyCommitted)
        assertEquals(0, rolls.get())
        assertEquals(SilentNamesMode.STORY, result.receipt.mode)
        assertEquals(null, result.receipt.dice)
    }
}
