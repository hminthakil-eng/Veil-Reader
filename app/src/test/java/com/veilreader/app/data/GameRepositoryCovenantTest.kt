package com.veilreader.app.data

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class GameRepositoryCovenantTest {
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
    fun `covenant persists across repository recreation and can be released without penalty`() = runBlocking {
        val first = GameRepository(context)
        assertNull(first.covenantId.first())

        assertTrue(first.setCovenant("unwritten_margin"))
        assertEquals("unwritten_margin", first.covenantId.first())

        val recreated = GameRepository(context)
        assertEquals("unwritten_margin", recreated.covenantId.first())

        assertTrue(recreated.setCovenant(null))
        assertNull(GameRepository(context).covenantId.first())
    }

    @Test
    fun `unknown covenant never overwrites durable selection`() = runBlocking {
        val repository = GameRepository(context)
        assertTrue(repository.setCovenant("returning_lamp"))

        assertFalse(repository.setCovenant("not_a_real_covenant"))
        assertEquals("returning_lamp", repository.covenantId.first())
        assertEquals(
            "returning_lamp",
            context.getSharedPreferences("veil_game_v1", Context.MODE_PRIVATE)
                .getString("covenantId", null)
        )
    }
}
