extends SceneTree
## Real disk recovery tests; UI click smoke; leaves no QA test files.
var pass_count: int = 0
var fail_count: int = 0
var store: IslandSecondDayCheckpointStore
var folder: String

func check(ok: bool, note: String) -> void:
	if ok:
		pass_count += 1
		print("PASS ", note)
	else:
		fail_count += 1
		push_error("FAIL " + note)

func _make_day() -> IslandSecondDayController:
	var d := IslandFirstDayController.new()
	d.meet_darya()
	d.choose_family("sealed")
	d.meet_bahram()
	d.resolve_visitor(false)
	d.fund_lanterns("residents")
	d.close_day()
	return IslandSecondDayController.new(d)

func _initialize() -> void:
	store = IslandSecondDayCheckpointStore.new("qa_day2_%d" % Time.get_ticks_usec())
	folder = ProjectSettings.globalize_path("user://" + store.folder_name)
	_test_disk()
	call_deferred("_test_ui_then_quit")

func _file(n: int) -> String:
	return folder.path_join("checkpoint_%08d.json" % n)

func _write(path: String, text: String) -> bool:
	var f: FileAccess = FileAccess.open(path, FileAccess.WRITE)
	if f == null:
		return false
	f.store_string(text)
	f.flush()
	f.close()
	return true

func _test_disk() -> void:
	check(IslandSecondDayCheckpointStore.new("../bad").folder_name == "", "day2 checkpoint blocks parent traversal")
	check(IslandSecondDayCheckpointStore.new("bad/sub").folder_name == "", "day2 checkpoint blocks nested folders")
	check(store.load_latest() == null and store.last_status == "no_valid_checkpoint", "empty day2 save is an honest empty result")
	var day := _make_day()
	check(day.ready, "day two ready from completed first-day family seal route")
	check(store.save_day(day), "version two writes checked append-only checkpoint")
	check(store.latest_valid_number() == 1 and FileAccess.file_exists(_file(1)), "first completed checkpoint stored to disk")
	var original: String = FileAccess.get_file_as_string(_file(1))
	check(original.length() > 100 and original.find("user://books") == -1, "checkpoint has serialized data, no private reader URI")
	var restored: IslandSecondDayController = store.load_latest()
	check(restored != null and restored.grain == 6 and restored.day_one.day_closed, "first reload restores old narrative and new goods")
	check(day.meet_baker() and day.meet_archivist() and day.choose_archive("seek_permission"), "day two story choices entered")
	check(day.fund_bakery("external_trade") and day.bake_bread() and day.deliver("mixed"), "production and real export order completed")
	check(store.save_day(day), "second checkpoint captures physical bread and money")
	check(store.latest_valid_number() == 2, "newer save advances sequence")
	check(FileAccess.get_file_as_string(_file(1)) == original, "older save remains byte-for-byte untouched")
	restored = store.load_latest()
	check(restored != null and restored.pantry_delivered == 2 and restored.stalls_delivered == 2, "reload preserves distinct distribution outcomes")
	check(restored != null and restored.day_one.commerce.economy.total_coins() == 88, "saved bakery trade conserves exactly 88 coins")
	check(day.close_day(), "chapter closes only after delivery")
	check(not store.save_day(day,true) and store.last_status == "abort_injected", "injected write interruption intentionally aborts")
	check(FileAccess.file_exists(folder.path_join("staging_00000003.part")), "partial file is retained only as crash evidence")
	check(store.latest_valid_number() == 2, "uncommitted file is ignored by load")
	check(store.load_latest().day_closed == false, "load after failed write never invents ending")
	check(store.save_day(day), "retry resumes to new committed version")
	check(store.latest_valid_number() == 3 and store.load_latest().day_closed, "successful third checkpoint retains finished status")
	check(_write(_file(3), "{CORRUPTED"), "corrupt latest committed file")
	check(store.latest_valid_number() == 2, "corrupt latest falls back to previous valid version")
	var forged: Dictionary = JSON.parse_string(FileAccess.get_file_as_string(_file(2)))
	forged["snapshot"]["grain"] = 99
	forged["digest"] = JSON.stringify(forged["snapshot"]).sha256_text()
	check(_write(_file(2), JSON.stringify(forged)), "recompute checksum for semantically forged grain")
	check(store.latest_valid_number() == 1, "semantically forged checksum does not bypass inventory validation")
	check(store.load_latest().grain == 6, "healthy day-two predecessor remains available")
	check(not store.save_day(null), "null day two is rejected")
	check(store.latest_valid_number() == 1, "failed attempt leaves clean backup untouched")

func _test_ui_then_quit() -> void:
	var scene: Control = load("res://scenes/second_day.tscn").instantiate()
	root.add_child(scene)
	check(scene.get("buttons") is Dictionary, "day two actual Godot UI scene instantiates")
	var buttons: Dictionary = scene.get("buttons")
	check(buttons.has("meet_baker") and buttons.has("save") and buttons.has("load"), "dialogue and both save controls exist")
	check((buttons["meet_baker"] as Button).custom_minimum_size.y >= 48.0, "main action meets 48px touch size minimum")
	check((buttons["meet_archivist"] as Button).text_direction == Control.TEXT_DIRECTION_RTL, "scene explicitly requests RTL layout")
	var live: IslandSecondDayController = _make_day()
	scene.day = live
	scene._refresh()
	(buttons["meet_baker"] as Button).emit_signal("pressed")
	(buttons["meet_archivist"] as Button).emit_signal("pressed")
	(buttons["public_registry"] as Button).emit_signal("pressed")
	(buttons["weaver"] as Button).emit_signal("pressed")
	(buttons["bake"] as Button).emit_signal("pressed")
	(buttons["mixed"] as Button).emit_signal("pressed")
	check(live.pantry_delivered == 2 and live.stalls_delivered == 2, "real pressed signals complete market + pantry split")
	check(live.day_one.commerce.economy.total_coins() == 88, "UI actions preserve economy supply")
	check((buttons["bake"] as Button).disabled and (buttons["mixed"] as Button).disabled, "UI prevents second cook or re-delivery")
	(buttons["finish"] as Button).emit_signal("pressed")
	check(live.day_closed, "actual finish button ends second day")
	check(not buttons["save"].disabled and not buttons["load"].disabled, "save and recovery remain available after ending")
	scene.queue_free()
	var legacy_scene: Control = load("res://scenes/first_day.tscn").instantiate()
	root.add_child(legacy_scene)
	check(legacy_scene.click_buttons.has("enter_day2"), "first-day scene includes actual transition button")
	check((legacy_scene.click_buttons["enter_day2"] as Button).disabled, "cannot enter next chapter before finished first day")
	legacy_scene.queue_free()
	_cleanup()
	print("SECOND_DAY_CHECKPOINT_TEST_RESULTS passes=%d failures=%d" % [pass_count, fail_count])
	quit(1 if fail_count > 0 else 0)

func _cleanup() -> void:
	if not DirAccess.dir_exists_absolute(folder):
		return
	for name in DirAccess.get_files_at(folder):
		DirAccess.remove_absolute(folder.path_join(name))
	DirAccess.remove_absolute(folder)
