package com.veilreader.app.game.island

import org.json.JSONArray
import org.json.JSONObject

/**
 * Versioned, explicit encoding of fictional game data only.
 * The Reader's user-owned books, progress, Rank/Path and avatar never belong here.
 * A corrupt or newer save MUST fail closed rather than silently reset game currency.
 */
internal object IslandShopSnapshotCodec {
    fun encode(state: IslandShopState): String {
        require(validShopSnapshot(state)) { "invalid-island-save" }
        fun items(values: Iterable<JSONObject>) = JSONArray().also { array ->
            values.forEach { array.put(it) }
        }
        return JSONObject().apply {
            put("schemaVersion", state.schemaVersion)
            put("gameDay", state.gameDay)
            put("coins", state.coins)
            put("sales", state.sales)
            put("communityTrust", state.communityTrust)
            put("copies", items(state.copies.map {
                JSONObject().put("id", it.id).put("bookId", it.bookId)
                    .put("condition", it.condition.name).put("place", it.place.name)
            }))
            put("loans", items(state.loans.map {
                JSONObject().put("id", it.id).put("copyId", it.copyId)
                    .put("customerId", it.customerId).put("returnDay", it.returnDay)
            }))
            put("orders", items(state.orders.map {
                JSONObject().put("id", it.id).put("bookId", it.bookId)
                    .put("arrivalDay", it.arrivalDay)
            }))
            put("served", JSONArray(state.served.sorted()))
            put("appliedIds", JSONArray(state.appliedIds.sorted()))
        }.toString()
    }

    fun decode(raw: String): IslandShopState {
        val json = try {
            JSONObject(raw)
        } catch (error: Exception) {
            throw IllegalStateException("corrupt-island-save", error)
        }
        try {
            fun JSONObject.requiredArray(name: String): JSONArray = getJSONArray(name)
            fun JSONArray.strings(): Set<String> = buildSet {
                for (i in 0 until length()) add(getString(i))
            }
            fun JSONArray.copies(): List<StockCopy> = List(length()) { index ->
                val value = getJSONObject(index)
                StockCopy(
                    id = value.getString("id"),
                    bookId = value.getString("bookId"),
                    condition = StockCondition.valueOf(value.getString("condition")),
                    place = BookPlace.valueOf(value.getString("place"))
                )
            }
            fun JSONArray.loans(): List<ShopLoan> = List(length()) { index ->
                val value = getJSONObject(index)
                ShopLoan(value.getString("id"), value.getString("copyId"),
                    value.getString("customerId"), value.getInt("returnDay"))
            }
            fun JSONArray.orders(): List<PortOrder> = List(length()) { index ->
                val value = getJSONObject(index)
                PortOrder(value.getString("id"), value.getString("bookId"), value.getInt("arrivalDay"))
            }
            val state = IslandShopState(
                schemaVersion = json.getInt("schemaVersion"),
                gameDay = json.getInt("gameDay"),
                coins = json.getInt("coins"),
                copies = json.requiredArray("copies").copies(),
                served = json.requiredArray("served").strings(),
                appliedIds = json.requiredArray("appliedIds").strings(),
                loans = json.requiredArray("loans").loans(),
                orders = json.requiredArray("orders").orders(),
                sales = json.getInt("sales"),
                communityTrust = json.getInt("communityTrust")
            )
            check(validShopSnapshot(state)) { "invalid-island-save" }
            return state
        } catch (error: Exception) {
            throw IllegalStateException("invalid-or-unsupported-island-save", error)
        }
    }
}
