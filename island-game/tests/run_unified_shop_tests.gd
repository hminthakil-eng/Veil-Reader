extends SceneTree
var passed: int = 0
var failed: int = 0

func _initialize() -> void:
	_test_real_inventory_money_one_authority()
	_test_dry_run_injected_failure()
	_test_save_replay_and_tampering()
	_test_cross_domain_tamper_cases()
	print("ISLAND_UNIFIED_TEST_RESULTS passes=%d failures=%d" % [passed, failed])
	quit(1 if failed > 0 else 0)

func check(ok: bool, label: String) -> void:
	if ok:
		passed += 1
		print("PASS ", label)
	else:
		failed += 1
		push_error("FAIL " + label)

func _new_game() -> IslandUnifiedShop:
	return load("res://core/unified_shop.gd").new()

func _test_real_inventory_money_one_authority() -> void:
	var game: IslandUnifiedShop = _new_game()
	check(game.total_coins() == 88 and game.wallet_balance("bookshop") == 30, "single wallet source starts with bookshop30")
	check(game.register_copy("book_1", "fiction_tide_ledger", 30), "fiction book copy registered once")
	check(not game.register_copy("book_1", "fiction_duplicate", 10), "copy duplicate rejected")
	check(not game.sell_copy_to_off_island_buyer("sell_early", "book_1", 12), "damaged book cannot be sold")
	check(not game.repair_copy("repair_invalid", "missing_book", 3), "missing item does not debit repair")
	check(game.repair_copy("repair_1", "book_1", 3), "repair moves real money and condition")
	check(game.wallet_balance("bookshop") == 27 and game.wallet_balance("weaver") == 11, "repair costs credited to actual local weaver")
	check(int(game.get_copy("book_1")["condition"]) == 80, "condition improved by 50")
	check(not game.repair_copy("repair_1", "book_1", 3), "repair tx cannot be replayed")
	check(not game.sell_copy_to_off_island_buyer("too_much", "book_1", 51), "bounded external buyer refuses overdraft")
	check(game.wallet_balance("bookshop") == 27, "failed external order does not mint cash")
	check(game.sell_copy_to_off_island_buyer("sale_1", "book_1", 12), "off-island book sale debits named buyer")
	check(game.wallet_balance("bookshop") == 39 and game.wallet_balance("external_trade") == 38, "sale credits shop and debits outside wallet")
	check(game.get_copy("book_1")["location"] == "sold", "book has moved into sold state")
	check(not game.sell_copy_to_off_island_buyer("sale_repeat", "book_1", 12), "sold book can never be sold twice")
	check(game.pledge_lanterns("pledge_shop", "bookshop", 10), "bookshop gives from actual post-sale balance")
	check(game.pledge_lanterns("pledge_weaver", "weaver", 4), "local workshop joins civic reconstruction")
	check(not game.pledge_lanterns("overfund", "bookshop", 1), "can't overfund a lantern contract")
	check(game.commission_lanterns("lamp_commission"), "escrow pays physical project contractor")
	check(game.street_is_lit(), "district project state changed after actual payment")
	check(game.wallet_balance("bookshop") == 29 and game.wallet_balance("weaver") == 21 and game.wallet_balance("external_trade") == 38 and game.wallet_balance("town_escrow") == 0, "end-to-end exact balances after city project")
	check(game.total_coins() == 88, "entire bookshop+economy lifecycle conserved 88 coins")

func _test_dry_run_injected_failure() -> void:
	var game: IslandUnifiedShop = _new_game()
	game.register_copy("book_2", "fiction_binder", 25)
	game.test_fail_after_payment = true
	check(not game.repair_copy("fault_repair", "book_2", 3), "injected failure after staged payment refuses commit")
	check(game.wallet_balance("bookshop") == 30 and int(game.get_copy("book_2")["condition"]) == 25, "failed staged repair leaves BOTH inventory and real wallet unchanged")
	game.test_fail_after_payment = false
	check(game.repair_copy("fault_repair", "book_2", 3), "same transaction ID reusable after rejected stage")
	game.test_fail_after_payment = true
	check(not game.sell_copy_to_off_island_buyer("fault_sale", "book_2", 12), "injected sale failure rejects staged cash")
	check(game.wallet_balance("bookshop") == 27 and game.get_copy("book_2")["location"] == "stock", "no ghost cash or sold inventory after failure")
	game.test_fail_after_payment = false
	check(game.sell_copy_to_off_island_buyer("fault_sale", "book_2", 12), "failed sale may be retried once")
	check(game.wallet_balance("bookshop") == 39 and game.total_coins() == 88, "retried sale has correct money")

func _test_save_replay_and_tampering() -> void:
	var game: IslandUnifiedShop = _new_game()
	game.register_copy("copy_3", "fiction_story", 40)
	game.repair_copy("repair_3", "copy_3", 3)
	game.sell_copy_to_off_island_buyer("sale_3", "copy_3", 12)
	game.pledge_lanterns("p1", "bookshop", 10)
	game.pledge_lanterns("p2", "weaver", 4)
	game.commission_lanterns("c1")
	var raw = JSON.parse_string(JSON.stringify(game.snapshot()))
	var restored: IslandUnifiedShop = load("res://core/unified_shop.gd").from_snapshot(raw)
	check(restored != null and restored.street_is_lit(), "complete book and city state survived JSON roundtrip")
	if restored == null:
		return
	check(restored.wallet_balance("bookshop") == 29 and restored.total_coins() == 88, "wallet and invariant preserved across load")
	check(restored.get_copy("copy_3")["location"] == "sold", "physical sold book persisted")
	check(not restored.sell_copy_to_off_island_buyer("sale_3", "copy_3", 12), "repeated book sale forbidden after restore")
	check(not restored.commission_lanterns("c2"), "community project cannot pay twice after reload")
	var before: String = JSON.stringify(restored.snapshot())
	check(not restored.repair_copy("repair_after_sold", "copy_3", 3), "already sold book cannot be repaired")
	check(JSON.stringify(restored.snapshot()) == before, "invalid transactions leave both worlds unchanged after load")
	var corrupt: Dictionary = game.snapshot()
	corrupt["save_version"] = 999
	check(load("res://core/unified_shop.gd").from_snapshot(corrupt) == null, "unknown combined schema fails gracefully")
	corrupt = game.snapshot()
	var econ_data: Dictionary = (corrupt["economy"] as Dictionary).duplicate(true)
	var forged: Dictionary = (econ_data["wallets"] as Dictionary).duplicate()
	forged["bookshop"] = 999
	econ_data["wallets"] = forged
	corrupt["economy"] = econ_data
	check(load("res://core/unified_shop.gd").from_snapshot(corrupt) == null, "forged wallet rejected by conservation invariant")

func _test_cross_domain_tamper_cases() -> void:
	var original: IslandUnifiedShop = _new_game()
	original.register_copy("case_4", "fictional_bound_stories", 30)
	original.repair_copy("repair_case_4", "case_4", 3)
	original.sell_copy_to_off_island_buyer("sale_case_4", "case_4", 12)
	var class_script = load("res://core/unified_shop.gd")

	var fake_sold: Dictionary = original.snapshot()
	var fake_copies: Dictionary = (fake_sold["copies"] as Dictionary).duplicate(true)
	var fake_item: Dictionary = fake_copies["case_4"]
	fake_item["sale_tx_id"] = "fake_sale"
	fake_copies["case_4"] = fake_item
	fake_sold["copies"] = fake_copies
	check(class_script.from_snapshot(fake_sold) == null, "rejects book claimed sold by unrecorded transaction")

	var mismatch: Dictionary = original.snapshot()
	var fake_story_events: Array = (mismatch["book_events"] as Array).duplicate(true)
	var fake_repair: Dictionary = fake_story_events[0]
	fake_repair["cost"] = 4
	fake_story_events[0] = fake_repair
	mismatch["book_events"] = fake_story_events
	check(class_script.from_snapshot(mismatch) == null, "rejects repair story event whose price differs from money transfer")

	var redistributed: Dictionary = original.snapshot()
	var e: Dictionary = (redistributed["economy"] as Dictionary).duplicate(true)
	var w: Dictionary = (e["wallets"] as Dictionary).duplicate()
	w["bookshop"] = int(w["bookshop"]) - 1
	w["weaver"] = int(w["weaver"]) + 1
	e["wallets"] = w
	redistributed["economy"] = e
	check(class_script.from_snapshot(redistributed) == null, "rejects balanced but unexplained cash reassignment")

	var missing_link: Dictionary = original.snapshot()
	fake_copies = (missing_link["copies"] as Dictionary).duplicate(true)
	fake_item = fake_copies["case_4"]
	fake_item.erase("sale_tx_id")
	fake_copies["case_4"] = fake_item
	missing_link["copies"] = fake_copies
	check(class_script.from_snapshot(missing_link) == null, "rejects sold copy without an authentic sale link")

	var fractional: Dictionary = original.snapshot()
	e = (fractional["economy"] as Dictionary).duplicate(true)
	w = (e["wallets"] as Dictionary).duplicate(true)
	w["bookshop"] = float(w["bookshop"]) - 0.5
	w["weaver"] = float(w["weaver"]) + 0.5
	e["wallets"] = w
	fractional["economy"] = e
	check(class_script.from_snapshot(fractional) == null, "rejects fractional cash tamper even with unchanged total supply")

	var free_city: IslandUnifiedShop = _new_game()
	var civic_forger: Dictionary = free_city.snapshot()
	var forged_economy: Dictionary = (civic_forger["economy"] as Dictionary).duplicate(true)
	forged_economy["street_lanterns_state"] = "completed"
	civic_forger["economy"] = forged_economy
	check(class_script.from_snapshot(civic_forger) == null, "cannot unlock restored street without recorded funding and contract")

	var wrong_civic_tx: Dictionary = original.snapshot()
	var dirty_economy: Dictionary = (wrong_civic_tx["economy"] as Dictionary).duplicate(true)
	var dirty_events: Array = (dirty_economy["economic_events"] as Array).duplicate(true)
	var first_payment: Dictionary = dirty_events[0]
	first_payment["kind"] = "community_pledge"
	dirty_events[0] = first_payment
	dirty_economy["economic_events"] = dirty_events
	wrong_civic_tx["economy"] = dirty_economy
	check(class_script.from_snapshot(wrong_civic_tx) == null, "money event cannot be relabeled as an unrelated civic contract")

	var valid: Dictionary = JSON.parse_string(JSON.stringify(original.snapshot()))
	check(class_script.from_snapshot(valid) != null, "untampered paired book-and-money snapshot remains readable")
