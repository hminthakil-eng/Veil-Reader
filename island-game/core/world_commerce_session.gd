class_name IslandWorldCommerceSession
extends RefCounted
## P0: Additive new shop+town transaction authority. The legacy ShopLedger is not used.
## All *money* lives in one injected IslandEconomyLedger; copy stock has no coin field.
## This is an in-memory proof, NOT durable disk-atomic save or a production economy.

const SAVE_VERSION: int = 1
const MAX_PRICE: int = 100000

var economy: IslandEconomyLedger
var _copies: Dictionary = {}
var _copy_events: Array[Dictionary] = []

func _init(shared_economy: IslandEconomyLedger = null) -> void:
	economy = shared_economy if shared_economy != null else IslandEconomyLedger.new()

func register_copy(copy_id: String, fictional_book_id: String, condition: int, legal_owner: String = "bookshop") -> bool:
	if copy_id.is_empty() or fictional_book_id.is_empty() or legal_owner.is_empty():
		return false
	if _copies.has(copy_id) or condition < 0 or condition > 100:
		return false
	_copies[copy_id] = {
		"book_id": fictional_book_id,
		"owner": legal_owner,
		"condition": condition,
		"location": "stock",
		"sold_to": ""
	}
	return true

func get_copy(copy_id: String) -> Dictionary:
	if not _copies.has(copy_id):
		return {}
	return (_copies[copy_id] as Dictionary).duplicate(true)

func available_coins() -> int:
	return int(economy.wallets.get("bookshop", 0))

func repair_shop_copy(tx_id: String, copy_id: String, cost: int, provider_wallet: String = "weaver") -> bool:
	# No wallet or book side effects on any preflight error.
	if tx_id.is_empty() or economy.applied_transactions.has(tx_id):
		return false
	if cost < 1 or cost > MAX_PRICE or not _copies.has(copy_id):
		return false
	if not economy.wallets.has(provider_wallet) or provider_wallet == "bookshop":
		return false
	var item: Dictionary = _copies[copy_id]
	if item.get("owner") != "bookshop" or item.get("location") != "stock":
		return false
	if int(item.get("condition", 0)) >= 100:
		return false
	# _transfer is synchronous. No callback, signal, await or fallible external
	# operation between the single money mutation and guaranteed local item write.
	if not economy._transfer(tx_id, "bookshop", provider_wallet, cost, "repair_book"):
		return false
	item["condition"] = mini(100, int(item["condition"]) + 50)
	_copies[copy_id] = item
	_copy_events.append({"tx": tx_id, "kind": "repair_book", "copy_id": copy_id})
	return true

func sell_shop_copy(tx_id: String, copy_id: String, buyer_wallet: String, price: int) -> bool:
	if tx_id.is_empty() or economy.applied_transactions.has(tx_id):
		return false
	if price < 1 or price > MAX_PRICE or not _copies.has(copy_id):
		return false
	if buyer_wallet == "bookshop" or not economy.wallets.has(buyer_wallet):
		return false
	var item: Dictionary = _copies[copy_id]
	# A pledge, loan or private family document is NEVER sellable as shop stock.
	if item.get("owner") != "bookshop" or item.get("location") != "stock":
		return false
	if int(item.get("condition", 0)) < 50:
		return false
	if not economy._transfer(tx_id, buyer_wallet, "bookshop", price, "sell_book"):
		return false
	item["location"] = "sold"
	item["sold_to"] = buyer_wallet
	_copies[copy_id] = item
	_copy_events.append({"tx": tx_id, "kind": "sell_book", "copy_id": copy_id})
	return true

func snapshot() -> Dictionary:
	return {
		"save_version": SAVE_VERSION,
		"economy": economy.snapshot(),
		"copies": _copies.duplicate(true),
		"copy_events": _copy_events.duplicate(true)
	}

static func from_snapshot(data: Dictionary) -> IslandWorldCommerceSession:
	if data.get("save_version") != SAVE_VERSION:
		return null
	if typeof(data.get("economy")) != TYPE_DICTIONARY:
		return null
	if typeof(data.get("copies")) != TYPE_DICTIONARY or typeof(data.get("copy_events")) != TYPE_ARRAY:
		return null
	var restored_economy: IslandEconomyLedger = IslandEconomyLedger.from_snapshot(data["economy"])
	if restored_economy == null:
		return null
	var restored_copies: Dictionary = {}
	var sold_ids: Dictionary = {}
	for raw_id in data["copies"]:
		if typeof(raw_id) != TYPE_STRING or (raw_id as String).is_empty():
			return null
		var raw_item: Variant = data["copies"][raw_id]
		if typeof(raw_item) != TYPE_DICTIONARY:
			return null
		var item: Dictionary = raw_item
		if item.size() != 5 or not item.has_all(["book_id", "owner", "condition", "location", "sold_to"]):
			return null
		if typeof(item["book_id"]) != TYPE_STRING or (item["book_id"] as String).is_empty():
			return null
		if typeof(item["owner"]) != TYPE_STRING or (item["owner"] as String).is_empty():
			return null
		if typeof(item["sold_to"]) != TYPE_STRING or typeof(item["location"]) != TYPE_STRING:
			return null
		var cond: Variant = item["condition"]
		if typeof(cond) not in [TYPE_INT, TYPE_FLOAT]:
			return null
		if not is_finite(float(cond)) or float(cond) != floorf(float(cond)) or float(cond) < 0.0 or float(cond) > 100.0:
			return null
		if item["location"] not in ["stock", "sold"]:
			return null
		if item["location"] == "sold":
			if item["owner"] != "bookshop" or (item["sold_to"] as String).is_empty():
				return null
			sold_ids[raw_id] = true
		elif item["sold_to"] != "":
			return null
		restored_copies[raw_id] = item.duplicate(true)
	var typed_events: Array[Dictionary] = []
	var seen_txs: Dictionary = {}
	var sold_event_counts: Dictionary = {}
	for raw_event in data["copy_events"]:
		if typeof(raw_event) != TYPE_DICTIONARY:
			return null
		var event: Dictionary = raw_event
		if event.size() != 3 or not event.has_all(["tx", "kind", "copy_id"]):
			return null
		if typeof(event["tx"]) != TYPE_STRING or typeof(event["copy_id"]) != TYPE_STRING:
			return null
		if typeof(event["kind"]) != TYPE_STRING or event["kind"] not in ["repair_book", "sell_book"]:
			return null
		var tx: String = event["tx"]
		var copy_id: String = event["copy_id"]
		if tx.is_empty() or not restored_economy.applied_transactions.has(tx) or seen_txs.has(tx):
			return null
		if not restored_copies.has(copy_id):
			return null
		var matching: bool = false
		for financial_event in restored_economy.economic_events:
			if financial_event.get("tx") == tx and financial_event.get("kind") == event["kind"]:
				matching = true
				break
		if not matching:
			return null
		if event["kind"] == "sell_book":
			sold_event_counts[copy_id] = int(sold_event_counts.get(copy_id, 0)) + 1
		seen_txs[tx] = true
		typed_events.append(event.duplicate(true))
	for sold_id in sold_ids:
		if int(sold_event_counts.get(sold_id, 0)) != 1:
			return null
	for sale_id in sold_event_counts:
		if not sold_ids.has(sale_id):
			return null
	var session := IslandWorldCommerceSession.new(restored_economy)
	session._copies = restored_copies
	session._copy_events = typed_events
	return session
