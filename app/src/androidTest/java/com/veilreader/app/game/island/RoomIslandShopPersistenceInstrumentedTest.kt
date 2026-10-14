package com.veilreader.app.game.island

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Requires real Android device/emulator execution before claiming a durability gate pass. */
@RunWith(AndroidJUnit4::class)
class RoomIslandShopPersistenceInstrumentedTest {
    private fun newDb() = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(), IslandShopDatabase::class.java
    ).build()

    @Test fun concurrentSalesOfTheSameCopyOnlyTransferOnce() = runBlocking {
        val db = newDb()
        try {
            val port = RoomIslandShopStatePort(db)
            val requests = listOf(
                ShopCommand.Sell("sale-race-a", "initial:fog_tales", "1-a", 10),
                ShopCommand.Sell("sale-race-b", "initial:fog_tales", "1-a", 10)
            )
            val results = requests.map { command ->
                async(Dispatchers.Default) { port.dispatch(command) }
            }.awaitAll()
            assertEquals(1, results.count { it.outcome == ShopOutcome.APPLIED })
            assertEquals(1, results.count { it.outcome == ShopOutcome.REJECTED })
            assertEquals(40, port.load().coins)
            assertEquals(1, port.load().sales)
            assertEquals(1, port.load().appliedIds.size)
            assertFalse(port.load().copies.any { it.id == "initial:fog_tales" })
        } finally { db.close() }
    }

    @Test fun exportRestoreAndDuplicateActionSurviveFreshDatabase() = runBlocking {
        val one = newDb()
        val two = newDb()
        try {
            val source = RoomIslandShopStatePort(one)
            val order = ShopCommand.Order("order-7", "garden")
            assertEquals(ShopOutcome.APPLIED, source.dispatch(order).outcome)
            val backup = source.exportSnapshot()
            val target = RoomIslandShopStatePort(two)
            assertTrue(target.restoreIfEmpty(backup))
            assertFalse(target.restoreIfEmpty(backup))
            assertEquals(source.load(), target.load())
            assertEquals(ShopOutcome.DUPLICATE, target.dispatch(order).outcome)
            assertEquals(18, target.load().coins)
        } finally { one.close(); two.close() }
    }

    @Test fun malformedBackupFailsClosedWithoutErasingProgress() = runBlocking {
        val db = newDb()
        try {
            val port = RoomIslandShopStatePort(db)
            port.dispatch(ShopCommand.Repair("repair-7", "initial:garden"))
            val old = port.load()
            try {
                port.restoreIfEmpty("{bad-json")
                fail("Corrupt restore accepted")
            } catch (_: IllegalStateException) { }
            assertEquals(old, port.load())
        } finally { db.close() }
    }

    @Test fun rejectedCommandDoesNotBurnItsActionId() = runBlocking {
        val db = newDb()
        try {
            val port = RoomIslandShopStatePort(db)
            assertEquals(ShopOutcome.REJECTED, port.dispatch(
                ShopCommand.Sell("sale-id", "initial:fog_tales", "1-a", 999)).outcome)
            assertTrue(port.load().appliedIds.isEmpty())
            assertEquals(ShopOutcome.APPLIED, port.dispatch(
                ShopCommand.Sell("sale-id", "initial:fog_tales", "1-a", 10)).outcome)
        } finally { db.close() }
    }
}
