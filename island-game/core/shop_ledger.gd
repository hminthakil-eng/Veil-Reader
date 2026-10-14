class_name IslandShopLedger
extends RefCounted
## Single-writer, pure in-memory greybox economy. Persistence adapter is a separate gate.

const SAVE_VERSION: int = 1
var coins: int = 30
var copies: Dictionary = {}
var applied_transactions: Dictionary = {}
var sales: Array[Dictionary] = []

func add_copy(copy_id: String, fictional_book_id: String, condition: int) -> bool:
	if copy_id.is_empty() or fictional_book_id.is_empty() or copies.has(copy_id) or condition < 0 or condition > 100:
		return false
	copies[copy_id] = {"book_id": fictional_book_id, "condition": condition, "location": "stock"}
	return true

func repair(tx_id: String, copy_id: String, cost: int) -> bool:
	if _reject(tx_id) or not copies.has(copy_id) or cost < 1 or cost > coins:
		return false
	var item: Dictionary = copies[copy_id]
	if item.get("location") != "stock" or int(item.get("condition", 0)) >= 100:
		return false
	coins -= cost
	item["condition"] = mini(100, int(item["condition"]) + 50)
	copies[copy_id] = item
	applied_transactions[tx_id] = true
	return true

func sell(tx_id: String, copy_id: String, customer_id: String, price: int) -> bool:
	if _reject(tx_id) or not copies.has(copy_id) or customer_id.is_empty() or price < 1 or price > 100000 or coins > 2000000000 - price:
		return false
	var item: Dictionary = copies[copy_id]
	if item.get("location") != "stock" or int(item.get("condition", 0)) < 50:
		return false
	item["location"] = "sold"
	copies[copy_id] = item
	coins += price
	sales.append({"tx_id": tx_id, "copy_id": copy_id, "customer_id": customer_id, "price": price})
	applied_transactions[tx_id] = true
	return true

func _reject(tx_id: String) -> bool:
	return tx_id.is_empty() or applied_transactions.has(tx_id)

func snapshot() -> Dictionary:
	return {"save_version": SAVE_VERSION, "coins": coins, "copies": copies.duplicate(true), "applied_transactions": applied_transactions.duplicate(true), "sales": sales.duplicate(true)}

static func from_snapshot(state: Dictionary) -> IslandShopLedger:
	if state.get("save_version") != SAVE_VERSION:
		return null
	var raw_coins: Variant = state.get("coins")
	# JSON.parse_string returns JSON numbers as floats; reject fractional/invalid values.
	if typeof(raw_coins) not in [TYPE_INT, TYPE_FLOAT]:
		return null
	if not is_finite(float(raw_coins)) or float(raw_coins) < 0.0 or float(raw_coins) > 2000000000.0 or float(raw_coins) != floorf(float(raw_coins)):
		return null
	if typeof(state.get("copies")) != TYPE_DICTIONARY or typeof(state.get("applied_transactions")) != TYPE_DICTIONARY or typeof(state.get("sales")) != TYPE_ARRAY:
		return null
	var fresh := IslandShopLedger.new()
	fresh.coins = int(state["coins"])
	fresh.copies = (state["copies"] as Dictionary).duplicate(true)
	fresh.applied_transactions = (state["applied_transactions"] as Dictionary).duplicate(true)
	for record in state["sales"]:
		if typeof(record) != TYPE_DICTIONARY:
			return null
		fresh.sales.append((record as Dictionary).duplicate(true))
	return fresh
