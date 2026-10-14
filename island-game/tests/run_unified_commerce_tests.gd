extends SceneTree
## New additive integration tests; old 19+30 test suites MUST remain unchanged.
const EXPECTED_ASSERTIONS: int = 25
var passed: int = 0
var failed: int = 0

func _initialize() -> void:
	_test_owned_stock_and_sale()
	_test_guards_and_repair()
	_test_single_state_snapshot_and_invalid_restore()
	_test_100_balanced_transfers()
	if passed + failed != EXPECTED_ASSERTIONS:
		_fail("missing assertions: expected %d, executed %d" % [EXPECTED_ASSERTIONS, passed + failed])
	print("WORLD_COMMERCE_TEST_RESULTS passes=%d failures=%d" % [passed, failed])
	quit(1 if failed > 0 else 0)

func _check(value: bool, label: String) -> void:
	if value:
		passed += 1
		print("PASS ", label)
	else:
		_fail(label)

func _fail(label: String) -> void:
	failed += 1
	push_error("FAIL " + label)

func _test_owned_stock_and_sale() -> void:
	var world := IslandWorldCommerceSession.new()
	_check(world.available_coins() == 30 and world.economy.total_coins() == 88, "new session uses the shared 30/8/0/50 world wallet")
	_check(world.register_copy("original_1", "book_tides", 80), "register original shop-owned book")
	_check(not world.register_copy("original_1", "book_tides", 80), "reject duplicate book copy ID")
	_check(world.sell_shop_copy("sale_1", "original_1", "external_trade", 12), "sell owned undamaged book from external buyer cash")
	_check(world.available_coins() == 42 and world.economy.wallets["external_trade"] == 38, "debit finite buyer; credit bookshop exactly once")
	_check(not world.sell_shop_copy("sale_2", "original_1", "external_trade", 12), "already sold book cannot be sold again")
	_check(not world.sell_shop_copy("sale_1", "original_1", "external_trade", 12), "same transaction ID cannot replay")
	_check(world.get_copy("original_1")["location"] == "sold", "sold copy state updates once")
	_check(world.economy.total_coins() == 88, "money conserved after world-book sale")

func _test_guards_and_repair() -> void:
	var world := IslandWorldCommerceSession.new()
	_check(world.register_copy("private_1", "family_chronicle", 90, "family_nazhat") and not world.sell_shop_copy("private_sale", "private_1", "external_trade", 10), "private pledged family book cannot be sold")
	_check(world.register_copy("damaged_1", "book_map", 25) and not world.sell_shop_copy("damaged_sale", "damaged_1", "external_trade", 12), "unrestored damaged book cannot sell")
	_check(world.repair_shop_copy("repair_1", "damaged_1", 3, "weaver"), "repair transfers real money to independent weaver")
	_check(world.get_copy("damaged_1")["condition"] == 75 and world.available_coins() == 27 and world.economy.wallets["weaver"] == 11, "after repair, condition and both wallet balances agree")
	_check(not world.repair_shop_copy("repair_1", "damaged_1", 3, "weaver"), "repair ID replay rejected")
	_check(not world.repair_shop_copy("repair_private", "private_1", 3, "weaver"), "no unauthorized repair of privately owned book")
	_check(world.register_copy("owned2", "book_roads", 95) and not world.sell_shop_copy("overdraft_sale", "owned2", "weaver", 25), "buyer with only 11 coins cannot overpay")
	_check(world.get_copy("owned2")["location"] == "stock" and not world.economy.applied_transactions.has("overdraft_sale"), "rejected sale leaves book and ID untouched")
	_check(not world.sell_shop_copy("broken_source", "unknown", "external_trade", 12) and not world.sell_shop_copy("bad_wallet", "owned2", "nobody", 12), "invalid copy and invalid wallet rejected")
	_check(world.economy.total_coins() == 88, "money preserved across rejected and successful repair decisions")

func _test_single_state_snapshot_and_invalid_restore() -> void:
	var world := IslandWorldCommerceSession.new()
	world.register_copy("save_book", "book_ledger", 90)
	world.sell_shop_copy("tx_saved_sale", "save_book", "external_trade", 9)
	var raw: Variant = JSON.parse_string(JSON.stringify(world.snapshot()))
	var restored: IslandWorldCommerceSession = IslandWorldCommerceSession.from_snapshot(raw)
	_check(restored != null and restored.get_copy("save_book").get("location") == "sold", "single combined JSON snapshot loads sold inventory")
	_check(restored != null and restored.available_coins() == 39 and restored.economy.total_coins() == 88, "single snapshot includes all financial wallets")
	_check(restored != null and not restored.sell_shop_copy("tx_saved_sale", "save_book", "external_trade", 9), "replayed sale blocked after JSON restore")
	var forged: Dictionary = world.snapshot()
	(forged["economy"]["wallets"] as Dictionary)["bookshop"] = 999
	_check(IslandWorldCommerceSession.from_snapshot(forged) == null, "tampered coin supply rejected on load")
	var missing_sale: Dictionary = world.snapshot()
	missing_sale["copy_events"] = []
	_check(IslandWorldCommerceSession.from_snapshot(missing_sale) == null, "sold item without matching journal entry rejected")

func _test_100_balanced_transfers() -> void:
	var world := IslandWorldCommerceSession.new()
	var succeeded: bool = true
	for i in range(100):
		var from_id: String = "bookshop" if i % 2 == 0 else "weaver"
		var to_id: String = "weaver" if i % 2 == 0 else "bookshop"
		if not world.economy._transfer("stress_%d" % i, from_id, to_id, 1, "stress_test"):
			succeeded = false
	_check(succeeded and world.economy.total_coins() == 88 and world.available_coins() == 30, "100 alternating transfers preserve exact cash balance")
