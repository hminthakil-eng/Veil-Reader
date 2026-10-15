extends SceneTree
## Deterministic economic and narrative day-two gate, additive to all 237 old tests.
var passed: int = 0
var failed: int = 0

func verify(value: bool, message: String) -> void:
	if value:
		passed += 1
		print("PASS ", message)
	else:
		failed += 1
		push_error("FAIL " + message)

func _complete_first_day() -> IslandFirstDayController:
	var d := IslandFirstDayController.new()
	if not d.meet_darya() or not d.choose_family("public_archive"):
		return null
	if not d.meet_bahram() or not d.resolve_visitor(false):
		return null
	if not d.fund_lanterns("residents") or not d.close_day():
		return null
	return d

func _initialize() -> void:
	_test_guardrails()
	_test_all_routes()
	_test_corrupt_state()
	print("SECOND_DAY_TEST_RESULTS passes=%d failures=%d" % [passed, failed])
	quit(1 if failed > 0 else 0)

func _test_guardrails() -> void:
	var unfinished := IslandFirstDayController.new()
	var bad := IslandSecondDayController.new(unfinished)
	verify(not bad.ready, "cannot unlock day two without closing actual day one")
	var first := _complete_first_day()
	verify(first != null and first.day_closed, "day one can be completed with independent-resident funding")
	if first == null:
		return
	var first_checkpoint: Dictionary = first.snapshot()
	verify(first.commerce.economy.wallets.size() == 4, "legacy day one retains original four-wallet schema")
	var newday := IslandSecondDayController.new(first)
	verify(newday.ready and newday.day_one == first, "day two starts from real completed day one")
	verify(first.commerce.economy.wallets.size() == 5 and int(first.commerce.economy.wallets["bakery"]) == 0, "opening bakery adds ZERO coins, preserving cash conservation")
	verify(not IslandSecondDayController.new(first).ready, "reopening bakery on same live session forbidden")
	verify(not newday.fund_bakery("bookshop") and not newday.bake_bread() and not newday.deliver("pantry"), "can't skip narrative and prepaid order gates")
	verify(IslandFirstDayController.from_snapshot(first_checkpoint) != null, "old four-wallet checkpoint remains loadable after upgrade")
	verify(newday.town_signals()["cash_total"] == 88, "cash stays at 88 after account creation")

func _test_all_routes() -> void:
	for payer in IslandSecondDayController.FUNDERS:
		for delivery in IslandSecondDayController.DELIVERY_ROUTES:
			var first := _complete_first_day()
			var d := IslandSecondDayController.new(first)
			var test_name: String = "%s / %s" % [payer, delivery]
			verify(d.ready and not d.fund_bakery(payer), test_name + ": no prepay before talks")
			verify(d.meet_baker() and d.meet_archivist(), test_name + ": baker and archivist are individually met")
			verify(not d.choose_archive("read_private_family_book"), test_name + ": protected private papers cannot be read")
			verify(d.choose_archive("public_registry") and not d.choose_archive("seek_permission"), test_name + ": public evidence decision cannot be silently changed")
			var coins_before: int = int(first.commerce.economy.wallets[payer])
			verify(d.fund_bakery(payer), test_name + ": four real coins pay independent bakery")
			verify(int(first.commerce.economy.wallets[payer]) == coins_before - 4 and int(first.commerce.economy.wallets["bakery"]) == 4, test_name + ": funding debits correct real payer")
			verify(not d.fund_bakery(payer), test_name + ": rapid repeat prepayment rejected")
			verify(d.bake_bread(), test_name + ": grain and textile consumed to bake four loaves")
			verify(d.grain == 2 and d.weaver_cloth == 1 and d.loaves == 4 and int(first.commerce.economy.wallets["bakery"]) == 2, test_name + ": finite stock and apprentice wages accounted")
			verify(not d.bake_bread() and not d.deliver("forged"), test_name + ": no phantom second production or delivery route")
			verify(d.deliver(delivery), test_name + ": supply delivery route accepted")
			verify(d.pantry_delivered + d.stalls_delivered == 4 and d.loaves == 0, test_name + ": exactly four distinct loaves distributed")
			verify(not d.deliver("mixed") and d.close_day() and not d.close_day(), test_name + ": final choice and end-of-day exactly once")
			verify(first.commerce.economy.total_coins() == 88, test_name + ": money conserved across all named accounts")
			var restored := IslandSecondDayController.from_snapshot(JSON.parse_string(JSON.stringify(d.snapshot())))
			verify(restored != null and restored.day_closed and restored.delivery_route == delivery, test_name + ": complete day two round trips as JSON")
			verify(restored != null and restored.day_one.commerce.economy.total_coins() == 88 and restored.pantry_delivered == d.pantry_delivered, test_name + ": reload preserves full economy and bread outcomes")
			verify(restored != null and not restored.fund_bakery(payer) and not restored.bake_bread(), test_name + ": replay after reload rejected")

func _test_corrupt_state() -> void:
	var first := _complete_first_day()
	var d := IslandSecondDayController.new(first)
	d.meet_baker()
	d.meet_archivist()
	d.choose_archive("preserve_seal")
	d.fund_bakery("weaver")
	d.bake_bread()
	d.deliver("mixed")
	d.close_day()
	var snap: Dictionary = d.snapshot()
	verify(IslandSecondDayController.from_snapshot(snap) != null, "a fully legitimate completed day validates")
	var forged: Dictionary = d.snapshot()
	forged["grain"] = 88
	verify(IslandSecondDayController.from_snapshot(forged) == null, "reject invented grain")
	forged = d.snapshot()
	forged["loaves"] = 4
	verify(IslandSecondDayController.from_snapshot(forged) == null, "reject duplicate loaves after delivery")
	forged = d.snapshot()
	forged["pantry_delivered"] = 4
	verify(IslandSecondDayController.from_snapshot(forged) == null, "reject duplicated bakery delivery")
	forged = d.snapshot()
	forged["funder"] = "bookshop"
	verify(IslandSecondDayController.from_snapshot(forged) == null, "reject different payer than booked economic event")
	forged = d.snapshot()
	(forged["day_one"]["commerce"]["economy"]["wallets"] as Dictionary)["bakery"] = 99
	verify(IslandSecondDayController.from_snapshot(forged) == null, "reject minted bakery money")
	forged = d.snapshot()
	forged["day_closed"] = false
	forged["delivery_route"] = "unrecognized"
	verify(IslandSecondDayController.from_snapshot(forged) == null, "reject unknown delivery route")
