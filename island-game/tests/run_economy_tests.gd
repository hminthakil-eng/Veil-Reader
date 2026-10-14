extends SceneTree
## Run as isolated financial proof: no ShopLedger, no real EPUB file reads.
var passed: int = 0
var failed: int = 0

func _initialize() -> void:
	_test_balanced_revival()
	_test_attack_after_restore()
	_test_account_validation()
	print("ISLAND_ECONOMY_TEST_RESULTS passes=%d failures=%d" % [passed, failed])
	quit(1 if failed > 0 else 0)

func _assert(condition: bool, label: String) -> void:
	if condition:
		passed += 1
		print("PASS ", label)
	else:
		failed += 1
		push_error("FAIL " + label)

func _test_balanced_revival() -> void:
	var ledger = load("res://core/economy_ledger.gd").new()
	_assert(ledger.total_coins() == 88, "four-wallet starting economy has 88 coins")
	_assert(not ledger.commission_street_lanterns("early"), "cannot build before financing")
	_assert(not ledger.receive_off_island_order("overspend_foreign", 51), "finite export buyer cannot overdraft")
	_assert(ledger.receive_off_island_order("export_sale_01", 12), "named external order pays local bookshop")
	_assert(ledger.wallets["bookshop"] == 42 and ledger.wallets["external_trade"] == 38, "foreign money actually debited")
	_assert(not ledger.receive_off_island_order("export_sale_01", 12), "replay of off-island order rejected")
	_assert(not ledger.purchase_local_weaving("negative_order", -2), "negative money movement rejected")
	_assert(ledger.purchase_local_weaving("weaving_order_01", 8), "bookshop orders from independent weaver")
	_assert(ledger.wallets["bookshop"] == 34 and ledger.wallets["weaver"] == 16, "weaver receives actual transferred money")
	_assert(ledger.pledge_street_lanterns("shop_pledge_01", "bookshop", 10), "player funds a civic project")
	_assert(ledger.wallets["bookshop"] == 24 and ledger.wallets["town_escrow"] == 10, "civic escrow holds real coins")
	_assert(not ledger.pledge_street_lanterns("overfund_project", "weaver", 5), "can't exceed a project's cap")
	_assert(ledger.pledge_street_lanterns("guild_pledge_01", "weaver", 4), "NPC-owned business cofunds civic project")
	_assert(ledger.street_lanterns_state == "fully_funded" and ledger.wallets["town_escrow"] == 14, "funded stage is not yet installed art")
	_assert(not ledger.pledge_street_lanterns("double_fund", "bookshop", 1), "cannot fund a full project again with new ID")
	_assert(ledger.commission_street_lanterns("install_lanterns_01"), "escrow pays contracted weaver and restores lanterns")
	_assert(ledger.street_is_lit() and ledger.wallets["town_escrow"] == 0 and ledger.wallets["weaver"] == 26, "completed project only after actual contractor payout")
	_assert(not ledger.commission_street_lanterns("install_twice"), "project cannot pay or complete twice")
	_assert(ledger.total_coins() == 88, "total coins conserved across full real transfers")
	var signs: Dictionary = ledger.read_only_town_signals()
	_assert(signs["market_turnover"] == 20 and signs["community_participants"] == 2 and signs["street_restored"], "town-facing signals derived from actual transactions")

func _test_attack_after_restore() -> void:
	var type = load("res://core/economy_ledger.gd")
	var ledger = type.new()
	ledger.receive_off_island_order("sale", 12)
	ledger.purchase_local_weaving("weaver_order", 8)
	ledger.pledge_street_lanterns("p_bookshop", "bookshop", 10)
	ledger.pledge_street_lanterns("p_weaver", "weaver", 4)
	ledger.commission_street_lanterns("built")
	var raw = JSON.parse_string(JSON.stringify(ledger.snapshot()))
	var restored = type.from_snapshot(raw)
	_assert(restored != null and restored.street_is_lit(), "restored project retains completed status")
	_assert(restored != null and restored.total_coins() == 88, "snapshot roundtrip preserves all economic wallets")
	if restored != null:
		var before: String = JSON.stringify(restored.snapshot())
		_assert(not restored.purchase_local_weaving("overspend", 25), "24-coin bookshop cannot spend 25")
		_assert(not restored.pledge_street_lanterns("more_after_build", "bookshop", 2), "cannot reopen civic project after load")
		_assert(not restored.pledge_street_lanterns("p_weaver", "weaver", 4), "replayed donation ID rejected after load")
		_assert(not restored.commission_street_lanterns("built"), "replayed commission rejected after load")
		_assert(JSON.stringify(restored.snapshot()) == before, "rejected operations never mutate coins or logs")

func _test_account_validation() -> void:
	var type = load("res://core/economy_ledger.gd")
	var ledger = type.new()
	var snapshot: Dictionary = ledger.snapshot()
	snapshot["schema_version"] = 999
	_assert(type.from_snapshot(snapshot) == null, "unknown save schema rejected")
	snapshot = ledger.snapshot()
	var changed_wallets: Dictionary = (snapshot["wallets"] as Dictionary).duplicate()
	changed_wallets["bookshop"] = 30.5
	snapshot["wallets"] = changed_wallets
	_assert(type.from_snapshot(snapshot) == null, "fractional coins rejected")
	snapshot = ledger.snapshot()
	changed_wallets = (snapshot["wallets"] as Dictionary).duplicate()
	changed_wallets["bookshop"] = 31
	snapshot["wallets"] = changed_wallets
	_assert(type.from_snapshot(snapshot) == null, "forged extra coin detected at import")
