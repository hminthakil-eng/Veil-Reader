class_name IslandEconomyLedger
extends RefCounted
## Isolated financial proof for fictional island recovery, NOT connected to ShopLedger.
## Four named wallets, every movement records an explicit debit and credit.
## Stage 2 will add inventory, prices and authoritative ShopLedger integration.

const SCHEMA_VERSION: int = 1
const INITIAL_SUPPLY: int = 88
const STREET_LANTERNS_COST: int = 14

var wallets: Dictionary = {
	"bookshop": 30,
	"weaver": 8,
	"town_escrow": 0,
	"external_trade": 50,
}
var applied_transactions: Dictionary = {}
var economic_events: Array[Dictionary] = []
var street_lanterns_state: String = "fundraising"

func _transfer(tx_id: String, from_id: String, to_id: String, amount: int, kind: String) -> bool:
	if tx_id.is_empty() or applied_transactions.has(tx_id):
		return false
	if from_id == to_id or not wallets.has(from_id) or not wallets.has(to_id):
		return false
	if amount < 1 or amount > 1000000:
		return false
	var available: int = int(wallets[from_id])
	if amount > available or int(wallets[to_id]) > 2000000000 - amount:
		return false
	# No await or signals inside the financial mutation boundary.
	wallets[from_id] = available - amount
	wallets[to_id] = int(wallets[to_id]) + amount
	applied_transactions[tx_id] = true
	economic_events.append({
		"tx": tx_id, "kind": kind, "from": from_id, "to": to_id, "amount": amount
	})
	return true

func open_local_bakery() -> bool:
	# A new real account starts empty; no new coins enter circulation.
	if wallets.has("bakery"):
		return false
	wallets["bakery"] = 0
	return true

func prepay_bakery_order(tx_id: String, payer: String, amount: int) -> bool:
	if not wallets.has("bakery") or payer not in ["bookshop", "weaver", "external_trade"]:
		return false
	return _transfer(tx_id, payer, "bakery", amount, "prepaid_bread_order")

func bakery_buy_apron(tx_id: String, amount: int) -> bool:
	if not wallets.has("bakery"):
		return false
	return _transfer(tx_id, "bakery", "weaver", amount, "bakery_textile_purchase")

func receive_off_island_order(tx_id: String, amount: int) -> bool:
	# A finite named buyer account; never spawn money per sale.
	return _transfer(tx_id, "external_trade", "bookshop", amount, "off_island_order")

func purchase_local_weaving(tx_id: String, amount: int) -> bool:
	# A first abstract textile *service*, NOT simulated cloth production.
	return _transfer(tx_id, "bookshop", "weaver", amount, "local_weaving")

func pledge_street_lanterns(tx_id: String, contributor_id: String, amount: int) -> bool:
	if contributor_id not in ["bookshop", "weaver"]:
		return false
	if street_lanterns_state != "fundraising":
		return false
	if amount < 1 or amount > STREET_LANTERNS_COST - int(wallets["town_escrow"]):
		return false
	if not _transfer(tx_id, contributor_id, "town_escrow", amount, "community_pledge"):
		return false
	if int(wallets["town_escrow"]) == STREET_LANTERNS_COST:
		street_lanterns_state = "fully_funded"
	return true

func commission_street_lanterns(tx_id: String) -> bool:
	# Prototype contract: local weaver supplies canvas lantern shades.
	# Scenery is considered restored only after escrow pays the contractor.
	if street_lanterns_state != "fully_funded":
		return false
	if not _transfer(tx_id, "town_escrow", "weaver", STREET_LANTERNS_COST, "local_street_works"):
		return false
	street_lanterns_state = "completed"
	return true

func street_is_lit() -> bool:
	return street_lanterns_state == "completed"

func total_coins() -> int:
	var amount: int = 0
	for name in wallets:
		amount += int(wallets[name])
	return amount

func read_only_town_signals() -> Dictionary:
	# Raw observed event-derived values, not a fictitious +10 prosperity meter.
	var market_turnover: int = 0
	var participating_donors: Dictionary = {}
	for event in economic_events:
		if event["kind"] in ["off_island_order", "local_weaving"]:
			market_turnover += int(event["amount"])
		elif event["kind"] == "community_pledge":
			participating_donors[event["from"]] = true
	return {
		"market_turnover": market_turnover,
		"community_participants": participating_donors.size(),
		"street_restored": street_is_lit(),
	}

func snapshot() -> Dictionary:
	return {
		"schema_version": SCHEMA_VERSION,
		"wallets": wallets.duplicate(true),
		"applied_transactions": applied_transactions.duplicate(true),
		"economic_events": economic_events.duplicate(true),
		"street_lanterns_state": street_lanterns_state,
	}

static func from_snapshot(state: Dictionary) -> IslandEconomyLedger:
	if state.get("schema_version") != SCHEMA_VERSION:
		return null
	if typeof(state.get("wallets")) != TYPE_DICTIONARY:
		return null
	if typeof(state.get("applied_transactions")) != TYPE_DICTIONARY:
		return null
	if typeof(state.get("economic_events")) != TYPE_ARRAY:
		return null
	# Legacy first-day checkpoints have four wallets; day two adds one EMPTY
	# bakery account and never alters the original 88-coin monetary supply.
	var valid_accounts := ["bookshop", "weaver", "town_escrow", "external_trade"]
	var raw_wallets: Dictionary = state["wallets"]
	if raw_wallets.has("bakery"):
		valid_accounts.append("bakery")
	if raw_wallets.size() != valid_accounts.size():
		return null
	var validated: Dictionary = {}
	var total: int = 0
	for account in valid_accounts:
		if not raw_wallets.has(account):
			return null
		var value: Variant = raw_wallets[account]
		if typeof(value) not in [TYPE_INT, TYPE_FLOAT]:
			return null
		var numeric: float = float(value)
		if not is_finite(numeric) or numeric != floorf(numeric) or numeric < 0.0 or numeric > 2000000000.0:
			return null
		validated[account] = int(numeric)
		total += int(numeric)
	if total != INITIAL_SUPPLY:
		return null
	var state_name: Variant = state.get("street_lanterns_state")
	if state_name not in ["fundraising", "fully_funded", "completed"]:
		return null
	if state_name == "fundraising" and int(validated["town_escrow"]) >= STREET_LANTERNS_COST:
		return null
	if state_name == "fully_funded" and int(validated["town_escrow"]) != STREET_LANTERNS_COST:
		return null
	if state_name == "completed" and int(validated["town_escrow"]) != 0:
		return null
	var seen: Dictionary = state["applied_transactions"]
	var raw_events: Array = state["economic_events"]
	if seen.size() != raw_events.size():
		return null
	var typed_events: Array[Dictionary] = []
	for event in raw_events:
		if typeof(event) != TYPE_DICTIONARY or not event.has("tx") or not event.has("kind") or not seen.has(event["tx"]):
			return null
		typed_events.append((event as Dictionary).duplicate(true))
	var restored := IslandEconomyLedger.new()
	restored.wallets = validated
	restored.applied_transactions = seen.duplicate(true)
	restored.economic_events = typed_events
	restored.street_lanterns_state = state_name
	return restored
