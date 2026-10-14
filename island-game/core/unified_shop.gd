class_name IslandUnifiedShop
extends RefCounted
## Single financial authority in this NEW integrated slice.
## The older IslandShopLedger remains in the legacy greybox only.
## In-memory copy-on-write: failed second stage cannot debit real coins.

const SAVE_VERSION: int = 1
var _economy: IslandEconomyLedger = IslandEconomyLedger.new()
var _copies: Dictionary = {}
var _story_events: Array[Dictionary] = []

## Deliberately test-only; rejects an operation after staged payment.
var test_fail_after_payment: bool = false

func wallet_balance(account: String) -> int:
	if not _economy.wallets.has(account):
		return -1
	return int(_economy.wallets[account])

func total_coins() -> int:
	return _economy.total_coins()

func get_copy(copy_id: String) -> Dictionary:
	if not _copies.has(copy_id):
		return {}
	return (_copies[copy_id] as Dictionary).duplicate(true)

func book_events() -> Array[Dictionary]:
	var events: Array[Dictionary] = []
	for ev in _story_events:
		events.append(ev.duplicate(true))
	return events

func register_copy(copy_id: String, book_id: String, condition: int) -> bool:
	if copy_id.strip_edges().is_empty() or book_id.strip_edges().is_empty():
		return false
	if _copies.has(copy_id) or condition < 0 or condition > 100:
		return false
	_copies[copy_id] = {
		"book_id": book_id,
		"condition": condition,
		"location": "stock",
		"repair_tx_ids": [],
	}
	return true

func repair_copy(tx_id: String, copy_id: String, price: int) -> bool:
	if not _valid_new_tx(tx_id) or price < 1:
		return false
	if not _copies.has(copy_id):
		return false
	var current: Dictionary = _copies[copy_id]
	if current["location"] != "stock" or int(current["condition"]) >= 100:
		return false
	# Money + inventory both operate on staged *clones*, not live instances.
	var staged: IslandEconomyLedger = IslandEconomyLedger.from_snapshot(_economy.snapshot())
	if staged == null or not staged.purchase_local_weaving(tx_id, price):
		return false
	var new_inventory: Dictionary = _copies.duplicate(true)
	var copy: Dictionary = new_inventory[copy_id]
	copy["condition"] = mini(100, int(copy["condition"]) + 50)
	var repairs: Array = copy["repair_tx_ids"]
	repairs.append(tx_id)
	copy["repair_tx_ids"] = repairs
	new_inventory[copy_id] = copy
	if test_fail_after_payment or staged.total_coins() != IslandEconomyLedger.INITIAL_SUPPLY:
		return false
	_economy = staged
	_copies = new_inventory
	_story_events.append({"tx_id": tx_id, "kind": "repair", "copy_id": copy_id, "cost": price})
	return true

func sell_copy_to_off_island_buyer(tx_id: String, copy_id: String, price: int) -> bool:
	# This is explicitly an OFF-ISLAND order. No local customer wallet yet.
	if not _valid_new_tx(tx_id) or price < 1 or price > 100000:
		return false
	if not _copies.has(copy_id):
		return false
	var current: Dictionary = _copies[copy_id]
	if current["location"] != "stock" or int(current["condition"]) < 50:
		return false
	var staged: IslandEconomyLedger = IslandEconomyLedger.from_snapshot(_economy.snapshot())
	if staged == null or not staged.receive_off_island_order(tx_id, price):
		return false
	var new_inventory: Dictionary = _copies.duplicate(true)
	var copy: Dictionary = new_inventory[copy_id]
	copy["location"] = "sold"
	copy["sale_tx_id"] = tx_id
	new_inventory[copy_id] = copy
	if test_fail_after_payment or staged.total_coins() != IslandEconomyLedger.INITIAL_SUPPLY:
		return false
	_economy = staged
	_copies = new_inventory
	_story_events.append({"tx_id": tx_id, "kind": "sale", "copy_id": copy_id, "price": price, "buyer_wallet": "external_trade"})
	return true

func pledge_lanterns(tx_id: String, contributor_id: String, amount: int) -> bool:
	if not _valid_new_tx(tx_id):
		return false
	# Stage even purely financial changes for consistent owned references.
	var staged: IslandEconomyLedger = IslandEconomyLedger.from_snapshot(_economy.snapshot())
	if staged == null or not staged.pledge_street_lanterns(tx_id, contributor_id, amount):
		return false
	_economy = staged
	return true

func commission_lanterns(tx_id: String) -> bool:
	if not _valid_new_tx(tx_id):
		return false
	var staged: IslandEconomyLedger = IslandEconomyLedger.from_snapshot(_economy.snapshot())
	if staged == null or not staged.commission_street_lanterns(tx_id):
		return false
	_economy = staged
	return true

func street_is_lit() -> bool:
	return _economy.street_is_lit()

func town_signals() -> Dictionary:
	return _economy.read_only_town_signals().duplicate(true)

func _valid_new_tx(tx_id: String) -> bool:
	return not tx_id.strip_edges().is_empty() and tx_id == tx_id.strip_edges() and not _economy.applied_transactions.has(tx_id)

func snapshot() -> Dictionary:
	return {
		"save_version": SAVE_VERSION,
		"economy": _economy.snapshot(),
		"copies": _copies.duplicate(true),
		"book_events": book_events(),
	}

static func from_snapshot(raw: Dictionary) -> IslandUnifiedShop:
	if raw.get("save_version") != SAVE_VERSION or typeof(raw.get("economy")) != TYPE_DICTIONARY:
		return null
	if typeof(raw.get("copies")) != TYPE_DICTIONARY or typeof(raw.get("book_events")) != TYPE_ARRAY:
		return null
	var economy: IslandEconomyLedger = IslandEconomyLedger.from_snapshot(raw["economy"])
	if economy == null:
		return null
	var copies: Dictionary = (raw["copies"] as Dictionary).duplicate(true)
	var events: Array = raw["book_events"]

	# Cross-domain audit: book transitions must correspond to real money events,
	# with consistent endpoints, amounts, IDs and final wallet balances.
	var validated: Dictionary = {}
	var current_cash: Dictionary = {
		"bookshop": 30, "weaver": 8, "town_escrow": 0, "external_trade": 50,
	}
	var finance_by_tx: Dictionary = {}
	var civic_pledged: int = 0
	var civic_commissions: int = 0
	for finance_event in economy.economic_events:
		if typeof(finance_event) != TYPE_DICTIONARY:
			return null
		var economic_id: Variant = finance_event.get("tx")
		var source: Variant = finance_event.get("from")
		var recipient: Variant = finance_event.get("to")
		var amount: Variant = finance_event.get("amount")
		if typeof(economic_id) != TYPE_STRING or String(economic_id).is_empty() or finance_by_tx.has(economic_id):
			return null
		if typeof(source) != TYPE_STRING or typeof(recipient) != TYPE_STRING:
			return null
		if not current_cash.has(source) or not current_cash.has(recipient) or source == recipient:
			return null
		if not _strict_positive_integer(amount):
			return null
		if int(amount) > int(current_cash[source]):
			return null
		var kind: Variant = finance_event.get("kind")
		match kind:
			"off_island_order":
				if source != "external_trade" or recipient != "bookshop":
					return null
			"local_weaving":
				if source != "bookshop" or recipient != "weaver":
					return null
			"community_pledge":
				if recipient != "town_escrow" or source not in ["bookshop", "weaver"]:
					return null
				civic_pledged += int(amount)
			"local_street_works":
				if source != "town_escrow" or recipient != "weaver" or int(amount) != IslandEconomyLedger.STREET_LANTERNS_COST:
					return null
				civic_commissions += 1
			_:
				return null
		current_cash[source] = int(current_cash[source]) - int(amount)
		current_cash[recipient] = int(current_cash[recipient]) + int(amount)
		finance_by_tx[economic_id] = finance_event
	for account in current_cash:
		if current_cash[account] != economy.wallets[account]:
			return null
	if civic_pledged > IslandEconomyLedger.STREET_LANTERNS_COST or civic_commissions > 1:
		return null
	var expected_project_state: String = "fundraising"
	if civic_pledged == IslandEconomyLedger.STREET_LANTERNS_COST:
		expected_project_state = "completed" if civic_commissions == 1 else "fully_funded"
	elif civic_commissions != 0:
		return null
	if economy.street_lanterns_state != expected_project_state:
		return null

	var typed_events: Array[Dictionary] = []
	var seen_book_txs: Dictionary = {}
	for ev in events:
		if typeof(ev) != TYPE_DICTIONARY:
			return null
		var tx_id: Variant = ev.get("tx_id")
		var copy_id: Variant = ev.get("copy_id")
		if typeof(tx_id) != TYPE_STRING or typeof(copy_id) != TYPE_STRING:
			return null
		if seen_book_txs.has(tx_id) or not copies.has(copy_id) or not finance_by_tx.has(tx_id):
			return null
		var money_event: Dictionary = finance_by_tx[tx_id]
		var book_event_kind: Variant = ev.get("kind")
		if book_event_kind == "repair":
			if money_event.get("kind") != "local_weaving" or money_event.get("from") != "bookshop" or money_event.get("to") != "weaver":
				return null
			if not _strict_positive_integer(ev.get("cost")) or int(money_event["amount"]) != int(ev["cost"]):
				return null
		elif book_event_kind == "sale":
			if money_event.get("kind") != "off_island_order" or money_event.get("from") != "external_trade" or money_event.get("to") != "bookshop":
				return null
			if not _strict_positive_integer(ev.get("price")) or int(money_event["amount"]) != int(ev["price"]):
				return null
			if ev.get("buyer_wallet") != "external_trade":
				return null
		else:
			return null
		seen_book_txs[tx_id] = ev
		typed_events.append((ev as Dictionary).duplicate(true))

	# These transfer categories have no other public routes in this coordinator.
	for money_event in economy.economic_events:
		if money_event.get("kind") in ["local_weaving", "off_island_order"]:
			if not seen_book_txs.has(money_event["tx"]):
				return null

	for copy_id in copies:
		var c: Dictionary = copies[copy_id]
		if c.has("coins") or not c.has("repair_tx_ids") or typeof(c["repair_tx_ids"]) != TYPE_ARRAY:
			return null
		var repair_ids: Array = c["repair_tx_ids"]
		var seen_repairs: Dictionary = {}
		for repair_id in repair_ids:
			if typeof(repair_id) != TYPE_STRING or seen_repairs.has(repair_id) or not seen_book_txs.has(repair_id):
				return null
			var saved_ev: Dictionary = seen_book_txs[repair_id]
			if saved_ev["kind"] != "repair" or saved_ev["copy_id"] != copy_id:
				return null
			seen_repairs[repair_id] = true
		if c["location"] == "sold":
			if typeof(c.get("sale_tx_id")) != TYPE_STRING or not seen_book_txs.has(c["sale_tx_id"]):
				return null
			var sale_ev: Dictionary = seen_book_txs[c["sale_tx_id"]]
			if sale_ev["kind"] != "sale" or sale_ev["copy_id"] != copy_id:
				return null
		elif c.has("sale_tx_id"):
			return null

	for book_tx_id in seen_book_txs:
		var b: Dictionary = seen_book_txs[book_tx_id]
		var matching_copy: Dictionary = copies[b["copy_id"]]
		if b["kind"] == "repair" and book_tx_id not in matching_copy["repair_tx_ids"]:
			return null
		if b["kind"] == "sale" and matching_copy.get("sale_tx_id") != book_tx_id:
			return null
	var restored := IslandUnifiedShop.new()
	restored._economy = economy
	restored._copies = copies
	restored._story_events = typed_events
	return restored

static func _strict_positive_integer(value: Variant) -> bool:
	if typeof(value) not in [TYPE_INT, TYPE_FLOAT]:
		return false
	var numeric: float = float(value)
	return is_finite(numeric) and numeric == floorf(numeric) and numeric >= 1.0 and numeric <= 2000000000.0
