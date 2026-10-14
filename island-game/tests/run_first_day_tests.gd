extends SceneTree
## End-to-end DAY1 controller tests. Supplement, never replace, older 74 asserts.
var passed: int = 0
var failed: int = 0

func _initialize() -> void:
	_test_initial_and_guards()
	_test_sealed_family_branches()
	_test_day_routes()
	_test_forged_saves()
	call_deferred("_run_scene_stage")

func check(ok: bool, message: String) -> void:
	if ok:
		passed += 1
		print("PASS ", message)
	else:
		failed += 1
		push_error("FAIL " + message)

func _test_initial_and_guards() -> void:
	var day := IslandFirstDayController.new()
	check(IslandFirstDayController.BOOK_IDS.size() == 6, "six distinct fictional book descriptions")
	check(day.commerce.economy.total_coins() == 88, "fresh day conserves four-wallet supply")
	check(day.inspect("names")["ok"] and day.inspect("names")["text"].find("امانت") >= 0, "family book inspection explicitly says private")
	check(not day.commerce.sell_shop_copy("illegal_private_sale", "names", "external_trade", 12), "family-owned chronicle cannot be sold")
	check(not day.choose_family("consent"), "can't resolve Darya before meeting")
	check(not day.meet_bahram(), "cannot meet buyer before family scene")
	check(not day.resolve_visitor(false), "cannot skip both customer conversations")
	check(not day.fund_lanterns("solo"), "project unavailable before shop day customers")

func _test_sealed_family_branches() -> void:
	for choice in IslandFirstDayController.FAMILY_CHOICES:
		var day := IslandFirstDayController.new()
		check(day.meet_darya(), "%s: meet Darya" % choice)
		check(day.choose_family(choice), "%s: authentic story route accepted" % choice)
		check(day.family_choice == choice and not day.choose_family("sealed"), "%s: cannot overwrite consent choice" % choice)
		check(day.commerce.get_copy("names")["owner"] == "nazhat_family" and day.commerce.get_copy("names")["location"] == "stock", "%s: seal and legal ownership preserved" % choice)
		check(day.commerce.economy.total_coins() == 88, "%s: relationship decision does not magically add money" % choice)

func _test_day_routes() -> void:
	for choice in IslandFirstDayController.FUNDING_CHOICES:
		for visitor_buys in [true, false]:
			var day := IslandFirstDayController.new()
			var name: String = "%s / buyer=%s" % [choice, visitor_buys]
			check(day.meet_darya() and day.choose_family("public_archive"), name + ": first customer with public-record route")
			check(day.meet_bahram(), name + ": second customer conversation actually occurs")
			if visitor_buys:
				check(not day.resolve_visitor(true), name + ": ruined lantern book cannot be sold")
				check(day.repair("lantern"), name + ": bookbinder pays real local provider 3")
				check(day.resolve_visitor(true) and day.buyer_choice == "sold", name + ": 12-coin sale passes real buyer wallet")
				check(not day.resolve_visitor(true), name + ": rapid tap won't create a second sale")
			else:
				check(day.resolve_visitor(false) and day.buyer_choice == "declined", name + ": refusal is playable")
				check(not day.resolve_visitor(false), name + ": rapid repeat of refusal rejected")
			check(day.fund_lanterns(choice), name + ": chosen funding route can finish actual civic work")
			check(day.commerce.economy.street_is_lit() and int(day.commerce.economy.wallets["town_escrow"]) == 0, name + ": workers paid; street now lit")
			check(day.commerce.economy.total_coins() == 88, name + ": book+city cash conserved without minting")
			var before: String = JSON.stringify(day.snapshot())
			check(not day.fund_lanterns(choice) and not day.fund_lanterns("residents"), name + ": cannot replay or switch funding")
			check(JSON.stringify(day.snapshot()) == before, name + ": rejected funding leaves finances/story unchanged")
			check(day.close_day() and day.day_closed and not day.close_day(), name + ": first day closes only once")
			var restored: IslandFirstDayController = IslandFirstDayController.from_snapshot(JSON.parse_string(JSON.stringify(day.snapshot())))
			check(restored != null and restored.family_choice == "public_archive" and restored.buyer_choice == day.buyer_choice, name + ": combined day JSON restores story choices")
			check(restored != null and restored.commerce.economy.total_coins() == 88 and restored.commerce.economy.street_is_lit(), name + ": combined day restore money, civic work and exact book inventory")
			check(restored != null and restored.commerce.get_copy("names")["owner"] == "nazhat_family", name + ": private family property retained after reload")

func _test_forged_saves() -> void:
	var d := IslandFirstDayController.new()
	var forged: Dictionary = d.snapshot()
	forged["version"] = 999
	check(IslandFirstDayController.from_snapshot(forged) == null, "reject incompatible version")
	forged = d.snapshot()
	forged["family_choice"] = "consent"
	check(IslandFirstDayController.from_snapshot(forged) == null, "reject family decision never preceded by conversation")
	forged = d.snapshot()
	forged["funding_choice"] = "solo"
	check(IslandFirstDayController.from_snapshot(forged) == null, "reject invented reconstruction without actual ledgers")
	forged = d.snapshot()
	(forged["commerce"]["copies"]["names"] as Dictionary)["owner"] = "bookshop"
	check(IslandFirstDayController.from_snapshot(forged) == null, "reject forged family-book ownership")
	forged = d.snapshot()
	(forged["commerce"]["economy"]["wallets"] as Dictionary)["bookshop"] = 31
	check(IslandFirstDayController.from_snapshot(forged) == null, "reject minted extra coin")

func _run_scene_stage() -> void:
	_test_scene_events()
	print("FIRST_DAY_TEST_RESULTS passes=%d failures=%d" % [passed, failed])
	quit(1 if failed > 0 else 0)

func _test_scene_events() -> void:
	var packed: PackedScene = load("res://scenes/first_day.tscn")
	check(packed != null, "first-day scene is a loadable PackedScene")
	if packed == null:
		return
	var scene: Control = packed.instantiate()
	root.add_child(scene)
	check(scene.get("day") is IslandFirstDayController, "Godot scene owns integrated day session")
	var art: Control = scene.get("pixel_stage")
	check(art != null and not art.get("street_lit"), "initial street preview is actually dark")
	var buttons: Dictionary = scene.get("click_buttons")
	(buttons["meet_darya"] as Button).emit_signal("pressed")
	(buttons["sealed"] as Button).emit_signal("pressed")
	(buttons["meet_bahram"] as Button).emit_signal("pressed")
	(scene.get("picked") as OptionButton).select(3)
	(buttons["repair"] as Button).emit_signal("pressed")
	(buttons["sell"] as Button).emit_signal("pressed")
	(buttons["residents"] as Button).emit_signal("pressed")
	check(scene.get("day").commerce.economy.street_is_lit() and art.get("street_lit"), "UI event makes a lit pixel street after actual NPC funding")
	check(scene.get("day").commerce.economy.total_coins() == 88, "UI did not mint money via street painting")
	(buttons["finish"] as Button).emit_signal("pressed")
	check(scene.get("day").day_closed, "scene day closes via real UI action methods")
	scene.queue_free()
