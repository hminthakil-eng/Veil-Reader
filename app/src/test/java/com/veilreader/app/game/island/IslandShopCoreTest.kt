package com.veilreader.app.game.island

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandShopCoreTest {
    private val initial = IslandShopState()

    @Test fun sixFictionalBooksWithoutRealReaderDependencies() {
        assertEquals(6, initial.copies.size)
        assertEquals(6, IslandBookCatalog.titles.size)
        assertEquals(2, initial.copies.count { it.place == BookPlace.SHELF })
    }

    @Test fun twoCustomersAreStableAcrossReload() {
        assertEquals(customersForDay(1), customersForDay(1))
        assertEquals(2, customersForDay(1).map { it.id }.distinct().size)
    }

    @Test fun saleIsIdempotentAndTransfersStockAndCoinsExactlyOnce() {
        val before = initial
        val command = ShopCommand.Sell("sale-1", "initial:fog_tales", "1-a", 10)
        val applied = reduceShop(before, command)
        assertEquals(ShopOutcome.APPLIED, applied.outcome)
        assertEquals(before.coins + 10, applied.state.coins)
        assertFalse(applied.state.copies.any { it.id == "initial:fog_tales" })
        assertEquals(1, applied.state.sales)
        assertEquals(ShopOutcome.DUPLICATE, reduceShop(applied.state, command).outcome)
        assertEquals(applied.state, reduceShop(applied.state, command).state)
    }

    @Test fun secondSaleOfSameCopyAndCustomerIsRejected() {
        val sold = reduceShop(initial, ShopCommand.Sell("sale-1", "initial:fog_tales", "1-a", 10)).state
        assertEquals(ShopOutcome.REJECTED, reduceShop(sold, ShopCommand.Sell("sale-2", "initial:fog_tales", "1-a", 10)).outcome)
        val restocked = reduceShop(sold, ShopCommand.Order("order-1", "fog_tales")).state
        assertEquals(ShopOutcome.REJECTED, reduceShop(restocked, ShopCommand.Sell("sale-3", "initial:lighthouse", "1-a", 11)).outcome)
    }

    @Test fun saleRejectsWrongGenreAndExcessivePriceWithoutMutation() {
        val bad = reduceShop(initial, ShopCommand.Sell("bad-1", "initial:lighthouse", "1-a", 10))
        assertEquals(ShopOutcome.REJECTED, bad.outcome)
        assertEquals(initial, bad.state)
        val expensive = reduceShop(initial, ShopCommand.Sell("bad-2", "initial:fog_tales", "1-a", 99))
        assertEquals(ShopOutcome.REJECTED, expensive.outcome)
        assertEquals(initial, expensive.state)
    }

    @Test fun loanRemovesSaleAvailabilityAndReturnsOnlyAfterDueDay() {
        val loaned = reduceShop(initial, ShopCommand.Lend("lend-1", "initial:fog_tales", "1-a"))
        assertEquals(ShopOutcome.APPLIED, loaned.outcome)
        assertEquals(BookPlace.ON_LOAN, loaned.state.copies.first { it.id == "initial:fog_tales" }.place)
        assertEquals(ShopOutcome.REJECTED,
            reduceShop(loaned.state, ShopCommand.Sell("sale-on-loan", "initial:fog_tales", "1-b", 10)).outcome)
        val loanId = loaned.state.loans.single().id
        assertEquals(ShopOutcome.REJECTED, reduceShop(loaned.state, ShopCommand.Return("return-too-soon", loanId)).outcome)
        val day2 = reduceShop(loaned.state, ShopCommand.NextDay("next-1")).state
        val day3 = reduceShop(day2, ShopCommand.NextDay("next-2")).state
        val returned = reduceShop(day3, ShopCommand.Return("return-1", loanId))
        assertEquals(ShopOutcome.APPLIED, returned.outcome)
        assertEquals(BookPlace.STOCKROOM, returned.state.copies.first { it.id == "initial:fog_tales" }.place)
        assertTrue(returned.state.loans.isEmpty())
    }

    @Test fun damagedBookCannotBeShelvedUntilRepairedWithMoney() {
        assertEquals(ShopOutcome.REJECTED, reduceShop(initial, ShopCommand.Shelve("shelve-bad", "initial:garden")).outcome)
        val repaired = reduceShop(initial, ShopCommand.Repair("repair-1", "initial:garden"))
        assertEquals(initial.coins - 3, repaired.state.coins)
        assertEquals(StockCondition.RESTORED, repaired.state.copies.first { it.id == "initial:garden" }.condition)
        val shelved = reduceShop(repaired.state, ShopCommand.Shelve("shelve-1", "initial:garden"))
        assertEquals(ShopOutcome.APPLIED, shelved.outcome)
    }

    @Test fun portOrderHasCapacityDeliveryAndNoDuplicateOnSameDay() {
        val order = ShopCommand.Order("port-1", "warm_stories")
        val first = reduceShop(initial, order)
        assertEquals(18, first.state.coins)
        assertEquals(ShopOutcome.DUPLICATE, reduceShop(first.state, order).outcome)
        assertEquals(1, first.state.orders.size)
        val delivered = reduceShop(first.state, ShopCommand.NextDay("next-1"))
        assertTrue(delivered.state.orders.isEmpty())
        assertEquals(7, delivered.state.copies.size)
        assertEquals(ShopOutcome.DUPLICATE, reduceShop(delivered.state, ShopCommand.NextDay("next-1")).outcome)
    }

    @Test fun insufficientFundsCannotRepairOrOrderAndNeverGoNegative() {
        val broke = initial.copy(coins = 2)
        assertEquals(broke, reduceShop(broke, ShopCommand.Repair("repair-1", "initial:garden")).state)
        assertEquals(broke, reduceShop(broke, ShopCommand.Order("port-1", "fog_tales")).state)
    }

    @Test fun unsupportedSchemaOrCorruptSaveWillNotBeOverwritten() {
        val future = initial.copy(schemaVersion = 999)
        assertEquals(future, reduceShop(future, ShopCommand.NextDay("go")).state)
        val invalid = initial.copy(coins = -10)
        assertEquals(ShopOutcome.REJECTED, reduceShop(invalid, ShopCommand.NextDay("go")).outcome)
    }

    @Test fun gameProgressNeverRequiresActualReadingMinutesOrAPageTurn() {
        val next = reduceShop(initial, ShopCommand.NextDay("sleep")).state
        assertEquals(2, next.gameDay)
        assertEquals(30, next.coins)
    }

    @Test fun firstBadEventDoesNotConsumeItsTransactionId() {
        val failed = reduceShop(initial, ShopCommand.Sell("same-id", "initial:fog_tales", "1-a", 500))
        assertEquals(ShopOutcome.REJECTED, failed.outcome)
        val valid = reduceShop(failed.state, ShopCommand.Sell("same-id", "initial:fog_tales", "1-a", 10))
        assertEquals(ShopOutcome.APPLIED, valid.outcome)
    }

    @Test fun restoredSnapshotWithDuplicateCopyIdsIsRejectedBeforeChanges() {
        val tampered = initial.copy(copies = initial.copies + initial.copies.first())
        val result = reduceShop(tampered, ShopCommand.NextDay("day-2"))
        assertEquals(ShopOutcome.REJECTED, result.outcome)
        assertEquals(tampered, result.state)
    }

    @Test fun damagedCopyCannotBeLoanedEvenIfItWasCorruptedOntoTheShelf() {
        val damaged = initial.copy(copies = initial.copies.map {
            if (it.id == "initial:fog_tales") it.copy(condition = StockCondition.DAMAGED) else it
        })
        val result = reduceShop(damaged, ShopCommand.Lend("lend-damaged", "initial:fog_tales", "1-a"))
        assertEquals(ShopOutcome.REJECTED, result.outcome)
        assertEquals(damaged, result.state)
    }

    @Test fun counterOverflowIsRejectedInsteadOfWrappingNegative() {
        val salesLimit = initial.copy(sales = Int.MAX_VALUE)
        val sale = reduceShop(salesLimit, ShopCommand.Sell("sale-overflow", "initial:fog_tales", "1-a", 10))
        assertEquals(ShopOutcome.REJECTED, sale.outcome)
        assertEquals(salesLimit, sale.state)
        val trustLimit = initial.copy(communityTrust = Int.MAX_VALUE)
        val loan = reduceShop(trustLimit, ShopCommand.Lend("loan-overflow", "initial:fog_tales", "1-a"))
        assertEquals(ShopOutcome.REJECTED, loan.outcome)
        assertEquals(trustLimit, loan.state)
    }

    @Test fun initialBookIdsRemainStableIndependentOfCatalogListPositions() {
        assertTrue(initial.copies.any { it.id == "initial:fog_tales" })
        assertTrue(initial.copies.any { it.id == "initial:garden" })
        assertTrue(initial.copies.any { it.id == "initial:warm_stories" })
        assertEquals(initial.copies.size, initial.copies.map { it.id }.toSet().size)
    }

    @Test fun overdueBookCanStillBeReturnedWithoutShamingThePlayer() {
        val loaned = reduceShop(initial, ShopCommand.Lend("loan-1", "initial:fog_tales", "1-a")).state
        val day2 = reduceShop(loaned, ShopCommand.NextDay("day-2")).state
        val day3 = reduceShop(day2, ShopCommand.NextDay("day-3")).state
        val day4 = reduceShop(day3, ShopCommand.NextDay("day-4")).state
        val returned = reduceShop(day4, ShopCommand.Return("return-overdue", "loan:loan-1"))
        assertEquals(ShopOutcome.APPLIED, returned.outcome)
    }
}
