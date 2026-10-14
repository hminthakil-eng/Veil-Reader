extends SceneTree
## Run: Godot --headless --path island-game --script res://tests/run_headless_tests.gd
var failures: int = 0
var passes: int = 0

func _initialize() -> void:
	_test_three_layer_book()
	_test_repair_sale_and_idempotency()
	_test_paused_clock_and_offscreen_schedule()
	_test_versioned_snapshot_roundtrip()
	print("ISLAND_TEST_RESULTS passes=%d failures=%d" % [passes, failures])
	quit(1 if failures > 0 else 0)

func _check(condition: bool, label: String) -> void:
	if condition:
		passes += 1
		print("PASS ", label)
	else:
		failures += 1
		push_error("FAIL " + label)

func _test_three_layer_book() -> void:
	var book = load("res://core/book_definition.gd").new(&"book_salt_ledger", "blue salt stain", "lighthouse notes", "secret seal", 12)
	_check(book.visible_layers(false)["marginalia"] != "secret seal", "hidden clue until repair")
	_check(book.visible_layers(true)["marginalia"] == "secret seal", "three-layer lore unlocked by repair")

func _test_repair_sale_and_idempotency() -> void:
	var ledger = load("res://core/shop_ledger.gd").new()
	_check(ledger.add_copy("copy_a", "book_salt_ledger", 30), "stock fictional copy")
	_check(not ledger.sell("tx_sell", "copy_a", "keeper", 12), "damaged book cannot sell")
	_check(ledger.repair("tx_repair", "copy_a", 3), "repair succeeds")
	_check(ledger.coins == 27, "repair deducts once")
	_check(not ledger.repair("tx_repair", "copy_a", 3), "duplicate repair rejected")
	_check(ledger.sell("tx_sell", "copy_a", "keeper", 12), "valid sale succeeds")
	_check(ledger.coins == 39 and ledger.sales.size() == 1, "atomic model of money and sale record")
	_check(not ledger.sell("tx_sell", "copy_a", "keeper", 12), "duplicate transaction rejected")
	_check(not ledger.sell("tx_sell_other", "copy_a", "keeper", 12), "already-sold inventory rejected")

func _test_paused_clock_and_offscreen_schedule() -> void:
	var clock = load("res://core/virtual_clock.gd").new()
	clock.paused = true
	_check(not clock.advance_action(30), "clock pauses during dialogue")
	_check(clock.phase == 0 and clock.minutes_into_phase == 0, "paused time remains unchanged")
	clock.paused = false
	_check(clock.advance_action(90) and clock.advance_action(90), "virtual clock advances by player actions")
	_check(clock.phase == 1, "time enters noon after completed actions")
	_check(clock.npc_location("lighthouse_keeper", 1, 0) == "pier" and clock.npc_location("lighthouse_keeper", 1, 3) == "tea_house", "NPC projection works without scene instances")

func _test_versioned_snapshot_roundtrip() -> void:
	var ledger = load("res://core/shop_ledger.gd").new()
	ledger.add_copy("copy_b", "book_salt_ledger", 30)
	ledger.repair("fix", "copy_b", 3)
	var json_text: String = JSON.stringify(ledger.snapshot())
	var parsed = JSON.parse_string(json_text)
	var restored = load("res://core/shop_ledger.gd").from_snapshot(parsed)
	_check(restored != null and restored.coins == 27, "versioned JSON snapshot roundtrip")
	_check(restored != null and not restored.repair("fix", "copy_b", 3), "replayed repair rejected after restore")
	var corrupt = ledger.snapshot()
	corrupt["save_version"] = 999
	_check(load("res://core/shop_ledger.gd").from_snapshot(corrupt) == null, "unsupported save version rejected")
