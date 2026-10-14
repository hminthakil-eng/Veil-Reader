package com.veilreader.app.game.island

/**
 * Pure offline game logic. Reader locators, real EPUB contents, Rank/Path and avatar appearance
 * belong to OTHER repositories and must never be copied into this state.
 * The future persistence adapter must save both game state and applied action IDs atomically.
 */
enum class BookGenre { STORY, NAVIGATION, BOTANY, HISTORY }
enum class StockCondition { DAMAGED, USED, RESTORED }
enum class BookPlace { STOCKROOM, SHELF, ON_LOAN }

data class GameBook(val id: String, val titleKey: String, val genre: BookGenre, val fairPrice: Int)

/** These are fictional in-game titles, not files from a user's library. */
object IslandBookCatalog {
    val titles: Map<String, GameBook> = listOf(
        GameBook("fog_tales", "island.book.fog_tales", BookGenre.STORY, 10),
        GameBook("lighthouse", "island.book.lighthouse", BookGenre.NAVIGATION, 14),
        GameBook("garden", "island.book.garden", BookGenre.BOTANY, 12),
        GameBook("sea_chart", "island.book.sea_chart", BookGenre.NAVIGATION, 11),
        GameBook("letters", "island.book.letters", BookGenre.HISTORY, 9),
        GameBook("warm_stories", "island.book.warm_stories", BookGenre.STORY, 8)
    ).associateBy { it.id }
}

data class StockCopy(
    val id: String,
    val bookId: String,
    val condition: StockCondition = StockCondition.USED,
    val place: BookPlace = BookPlace.STOCKROOM
)
data class ShopCustomer(val id: String, val interest: BookGenre, val budget: Int)
data class ShopLoan(val id: String, val copyId: String, val customerId: String, val returnDay: Int)
data class PortOrder(val id: String, val bookId: String, val arrivalDay: Int)

/** No player identity, real book, Rank/Path, Reader state or cosmetic inventory here. */
data class IslandShopState(
    val schemaVersion: Int = 1,
    val gameDay: Int = 1,
    val coins: Int = 30,
    val copies: List<StockCopy> = startingStock(),
    val served: Set<String> = emptySet(),
    val appliedIds: Set<String> = emptySet(),
    val loans: List<ShopLoan> = emptyList(),
    val orders: List<PortOrder> = emptyList(),
    val sales: Int = 0,
    val communityTrust: Int = 0
)

fun startingStock(): List<StockCopy> = IslandBookCatalog.titles.values.map { book ->
    StockCopy(
        id = "initial:" + book.id,
        bookId = book.id,
        condition = if (book.id == "garden") StockCondition.DAMAGED else StockCondition.USED,
        place = if (book.id in setOf("fog_tales", "lighthouse")) BookPlace.SHELF else BookPlace.STOCKROOM
    )
}

/** Fixed per virtual day; reloading must not reroll a better customer. */
fun customersForDay(day: Int): List<ShopCustomer> {
    require(day in 1..9999)
    val genres = BookGenre.entries
    return listOf(
        ShopCustomer("$day-a", genres[(day - 1) % genres.size], 12 + day % 5),
        ShopCustomer("$day-b", genres[day % genres.size], 10 + day % 7)
    )
}

sealed interface ShopCommand {
    val transactionId: String
    data class Shelve(override val transactionId: String, val copyId: String) : ShopCommand
    data class Sell(override val transactionId: String, val copyId: String, val customerId: String, val price: Int) : ShopCommand
    data class Lend(override val transactionId: String, val copyId: String, val customerId: String) : ShopCommand
    data class Return(override val transactionId: String, val loanId: String) : ShopCommand
    data class Repair(override val transactionId: String, val copyId: String) : ShopCommand
    data class Order(override val transactionId: String, val bookId: String) : ShopCommand
    data class NextDay(override val transactionId: String) : ShopCommand
}

enum class ShopOutcome { APPLIED, DUPLICATE, REJECTED }
data class ShopDecision(val state: IslandShopState, val outcome: ShopOutcome, val reason: String)

/** Reject tampered or partially restored snapshots before applying economic commands. */
private fun validShopSnapshot(state: IslandShopState): Boolean {
    if (state.schemaVersion != 1 || state.gameDay !in 1..9999 ||
        state.coins < 0 || state.sales < 0 || state.communityTrust < 0) return false
    val copyIds = state.copies.map { it.id }
    if (copyIds.size != copyIds.toSet().size) return false
    if (state.copies.any { it.id.isBlank() || it.bookId !in IslandBookCatalog.titles }) return false
    if (state.copies.count { it.place == BookPlace.SHELF } > 4) return false
    if (state.appliedIds.any { it.isBlank() || it.length > 128 }) return false
    val loanCopies = state.loans.map { it.copyId }
    if (loanCopies.size != loanCopies.toSet().size ||
        state.loans.map { it.id }.distinct().size != state.loans.size) return false
    val loanedCopies = state.copies.filter { it.place == BookPlace.ON_LOAN }.map { it.id }.toSet()
    if (loanCopies.toSet() != loanedCopies) return false
    if (state.loans.any { it.id.isBlank() || it.returnDay !in 1..9999 }) return false
    if (state.orders.size > 2 || state.orders.map { it.id }.distinct().size != state.orders.size) return false
    if (state.orders.any { it.id.isBlank() || it.bookId !in IslandBookCatalog.titles ||
            it.arrivalDay < state.gameDay || it.arrivalDay > 9999 }) return false
    return true
}

/** Deterministic; always persist APPLIED state + transaction ID in one atomic write. */
fun reduceShop(state: IslandShopState, command: ShopCommand): ShopDecision {
    fun reject(why: String) = ShopDecision(state, ShopOutcome.REJECTED, why)
    if (command.transactionId.isBlank() || command.transactionId.length > 128) return reject("bad-transaction-id")
    if (command.transactionId in state.appliedIds) return ShopDecision(state, ShopOutcome.DUPLICATE, "already-applied")
    if (!validShopSnapshot(state)) return reject("invalid-save")
    fun find(id: String) = state.copies.firstOrNull { it.id == id }
    fun replace(copy: StockCopy) = state.copies.map { if (it.id == copy.id) copy else it }
    fun buyer(customerId: String) = customersForDay(state.gameDay).firstOrNull { it.id == customerId }
    val next = when (command) {
        is ShopCommand.Shelve -> {
            val copy = find(command.copyId) ?: return reject("no-such-copy")
            if (copy.place != BookPlace.STOCKROOM || copy.condition == StockCondition.DAMAGED) return reject("not-shelf-ready")
            if (state.copies.count { it.place == BookPlace.SHELF } >= 4) return reject("full-shelf")
            state.copy(copies = replace(copy.copy(place = BookPlace.SHELF)))
        }
        is ShopCommand.Sell -> {
            val copy = find(command.copyId) ?: return reject("no-such-copy")
            val book = IslandBookCatalog.titles[copy.bookId] ?: return reject("unknown-fictional-book")
            val customer = buyer(command.customerId) ?: return reject("customer-not-present")
            if (customer.id in state.served || copy.place != BookPlace.SHELF) return reject("unavailable")
            if (customer.interest != book.genre || copy.condition == StockCondition.DAMAGED) return reject("not-wanted")
            if (command.price !in (book.fairPrice / 2)..minOf(book.fairPrice * 2, customer.budget)) return reject("unfair-price")
            if (state.coins > Int.MAX_VALUE - command.price ||
                state.sales == Int.MAX_VALUE || state.communityTrust == Int.MAX_VALUE) return reject("overflow")
            state.copy(copies = state.copies.filterNot { it.id == copy.id },
                coins = state.coins + command.price, served = state.served + customer.id,
                sales = state.sales + 1, communityTrust = state.communityTrust + 1)
        }
        is ShopCommand.Lend -> {
            val copy = find(command.copyId) ?: return reject("no-such-copy")
            val book = IslandBookCatalog.titles[copy.bookId] ?: return reject("unknown-fictional-book")
            val customer = buyer(command.customerId) ?: return reject("customer-not-present")
            if (customer.id in state.served || copy.place != BookPlace.SHELF ||
                copy.condition == StockCondition.DAMAGED || customer.interest != book.genre) return reject("cannot-lend")
            if (state.gameDay > 9997) return reject("calendar-limit")
            if (state.communityTrust > Int.MAX_VALUE - 2) return reject("overflow")
            state.copy(copies = replace(copy.copy(place = BookPlace.ON_LOAN)),
                loans = state.loans + ShopLoan("loan:" + command.transactionId, copy.id, customer.id, state.gameDay + 2),
                served = state.served + customer.id, communityTrust = state.communityTrust + 2)
        }
        is ShopCommand.Return -> {
            val loan = state.loans.firstOrNull { it.id == command.loanId } ?: return reject("no-such-loan")
            val copy = find(loan.copyId) ?: return reject("loan-copy-missing")
            if (copy.place != BookPlace.ON_LOAN || state.gameDay < loan.returnDay) return reject("not-due")
            state.copy(loans = state.loans.filterNot { it.id == loan.id },
                copies = replace(copy.copy(place = BookPlace.STOCKROOM)))
        }
        is ShopCommand.Repair -> {
            val copy = find(command.copyId) ?: return reject("no-such-copy")
            if (copy.place != BookPlace.STOCKROOM || copy.condition != StockCondition.DAMAGED) return reject("cannot-repair")
            if (state.coins < 3) return reject("insufficient-coins")
            state.copy(coins = state.coins - 3,
                copies = replace(copy.copy(condition = StockCondition.RESTORED)))
        }
        is ShopCommand.Order -> {
            if (command.bookId !in IslandBookCatalog.titles) return reject("unknown-fictional-book")
            if (state.coins < 12) return reject("insufficient-coins")
            if (state.gameDay >= 9999 || state.orders.size >= 2) return reject("port-capacity")
            state.copy(coins = state.coins - 12,
                orders = state.orders + PortOrder("order:" + command.transactionId, command.bookId, state.gameDay + 1))
        }
        is ShopCommand.NextDay -> {
            if (state.gameDay >= 9999) return reject("calendar-limit")
            val nextDay = state.gameDay + 1
            val arrivals = state.orders.filter { it.arrivalDay <= nextDay }
            state.copy(gameDay = nextDay, served = emptySet(),
                copies = state.copies + arrivals.map { StockCopy("crate:" + it.id, it.bookId) },
                orders = state.orders.filterNot { it.arrivalDay <= nextDay })
        }
    }
    return ShopDecision(next.copy(appliedIds = next.appliedIds + command.transactionId), ShopOutcome.APPLIED, "ok")
}
