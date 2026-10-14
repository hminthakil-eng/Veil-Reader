extends SceneTree
## Real on-disk checkpoint tests in a unique user:// sandbox, no private Reader data.
var passed: int = 0
var failed: int = 0
var path: String
var store: IslandDayCheckpointStore

func _initialize() -> void:
	store = IslandDayCheckpointStore.new("qa_island_checkpoint_%d" % Time.get_ticks_usec())
	path = ProjectSettings.globalize_path("user://" + store.folder_name)
	_run()
	call_deferred("_run_ui_and_finish")

func verify(ok: bool, label: String) -> void:
	if ok:
		passed += 1
		print("PASS ",label)
	else:
		failed += 1
		push_error("FAIL "+label)

func _path_of(n: int) -> String:
	return path.path_join("checkpoint_%08d.json" % n)

func _write_text(filename: String, value: String) -> bool:
	var f: FileAccess = FileAccess.open(filename,FileAccess.WRITE)
	if f == null:
		return false
	f.store_string(value)
	f.flush()
	f.close()
	return true

func _run() -> void:
	verify(IslandDayCheckpointStore.new("..\\outside").folder_name.is_empty(), "reject parent directory traversal")
	verify(IslandDayCheckpointStore.new("invalid/subdirectory").folder_name.is_empty(), "reject nested user input")
	verify(store.load_latest() == null and store.last_status == "no_valid_checkpoint", "no checkpoint starts as a truthful empty state")
	var day := IslandFirstDayController.new()
	verify(store.save_day(day), "persist first checkpoint through verified temp rename")
	verify(store.latest_valid_number() == 1, "first checkpoint sequence numbered one")
	verify(FileAccess.file_exists(_path_of(1)), "final checkpoint file exists on disk")
	var original_bytes: String = FileAccess.get_file_as_string(_path_of(1))
	verify(original_bytes.length() > 150 and not original_bytes.contains("user://books"), "serialized save contains data and no personal Reader path")
	var first: IslandFirstDayController = store.load_latest()
	verify(first != null and first.commerce.economy.total_coins() == 88, "load from disk validates money supply")
	verify(first != null and first.family_choice == "" and not first.day_closed, "load preserves initial narrative phase")

	verify(day.meet_darya() and day.choose_family("sealed"), "advance in-memory player choice")
	verify(store.save_day(day), "write second append-only checkpoint")
	verify(store.latest_valid_number() == 2, "second checkpoint supersedes but does not overwrite first")
	verify(FileAccess.get_file_as_string(_path_of(1)) == original_bytes, "older checkpoint bytes preserved")
	var second: IslandFirstDayController = store.load_latest()
	verify(second != null and second.family_choice == "sealed", "latest saved narrative decision restored")
	verify(second != null and second.commerce.get_copy("names")["owner"] == "nazhat_family", "family-owned book remains private on disk")

	verify(day.meet_bahram() and day.resolve_visitor(false), "advance unsaved visitor choice")
	verify(not store.save_day(day,true), "simulated crash before commit reports incomplete save")
	verify(store.last_status == "injected_abort_before_commit", "fault injection has explicit status")
	verify(FileAccess.file_exists(path.path_join("staging_00000003.part")), "crash simulation leaves incomplete transaction artifact")
	verify(store.latest_valid_number() == 2, "uncommitted .part is never a checkpoint")
	var after_abort: IslandFirstDayController = store.load_latest()
	verify(after_abort != null and after_abort.buyer_choice == "" and after_abort.family_choice == "sealed", "after abort, load last good snapshot with no fake sale or choices")

	verify(store.save_day(day), "retry after crash uses verified and committed checkpoint")
	verify(store.latest_valid_number() == 3, "retry finalizes next checkpoint without duplicating sequence")
	verify(store.load_latest().buyer_choice == "declined", "successful retry restores later visitor choice")
	verify(_write_text(_path_of(3),"{CRASHED_JSON"), "inject corrupt latest checkpoint bytes")
	verify(store.latest_valid_number() == 2, "corrupt newest save automatically falls back to earlier healthy one")
	var recovered: IslandFirstDayController = store.load_latest()
	verify(recovered != null and recovered.family_choice == "sealed" and recovered.buyer_choice == "", "automatic recovery maintains coherent earlier state")
	verify(recovered != null and recovered.commerce.economy.total_coins() == 88, "fallback preserves all 88 coins")
	verify(store.save_day(day), "can save after corrupt newest file by advancing sequence")
	verify(store.latest_valid_number() == 4, "appends version 4 without overwriting tampered version 3")
	var illegal_envelope: Dictionary = JSON.parse_string(FileAccess.get_file_as_string(_path_of(4)))
	illegal_envelope["snapshot"]["commerce"]["economy"]["wallets"]["bookshop"] = 31
	illegal_envelope["digest"] = JSON.stringify(illegal_envelope["snapshot"]).sha256_text()
	verify(_write_text(_path_of(4),JSON.stringify(illegal_envelope)), "simulate valid checksum but forged game economy")
	verify(store.latest_valid_number() == 2, "semantic validation rejects forged coin even with recomputed checksum")
	verify(store.load_latest().commerce.economy.total_coins() == 88, "fallback does not mint currency")
	verify(not store.save_day(null), "null game state cannot be persisted")
	verify(store.latest_valid_number() == 2, "invalid attempted save leaves prior checkpoint intact")
	verify(day.fund_lanterns("residents") and day.close_day(), "complete independent guild funding and close actual day")
	verify(store.save_day(day), "save a fully finished story and finance checkpoint")
	var completed: IslandFirstDayController = store.load_latest()
	verify(completed != null and completed.day_closed and completed.commerce.economy.street_is_lit(), "completed street lamps and day close survive process-style reload")
	verify(completed != null and completed.commerce.economy.total_coins() == 88 and int(completed.commerce.economy.wallets["town_escrow"]) == 0, "complete checkpoint preserves funded construction and payment")
	verify(completed != null and not completed.fund_lanterns("residents"), "loaded completed project cannot be paid again")

func _cleanup() -> void:
	if not DirAccess.dir_exists_absolute(path):
		return
	for item in DirAccess.get_files_at(path):
		DirAccess.remove_absolute(path.path_join(item))
	DirAccess.remove_absolute(path)

func _run_ui_and_finish() -> void:
	_test_user_buttons()
	print("DAY_CHECKPOINT_TEST_RESULTS passes=%d failures=%d" % [passed,failed])
	_cleanup()
	quit(1 if failed > 0 else 0)

func _test_user_buttons() -> void:
	var scene: Control = load("res://scenes/first_day.tscn").instantiate()
	root.add_child(scene)
	var buttons: Dictionary = scene.get("click_buttons")
	verify(buttons.has("save") and buttons.has("load"), "player UI actually exposes save and load buttons")
	var ui_store := IslandDayCheckpointStore.new("qa_island_ui_%d" % Time.get_ticks_usec())
	scene.checkpoints = ui_store
	(buttons["save"] as Button).emit_signal("pressed")
	verify(ui_store.latest_valid_number() == 1, "pressing actual save button commits a file")
	(buttons["meet_darya"] as Button).emit_signal("pressed")
	(buttons["sealed"] as Button).emit_signal("pressed")
	verify(scene.day.family_choice == "sealed", "UI changes actual family choice before restore")
	(buttons["load"] as Button).emit_signal("pressed")
	verify(scene.day.family_choice == "" and not scene.day.darya_met, "load button reverts only to valid saved narrative state")
	verify(scene.day.commerce.economy.total_coins() == 88, "load button restores exactly one cash authority")
	var ui_path: String = ProjectSettings.globalize_path("user://" + ui_store.folder_name)
	for file in DirAccess.get_files_at(ui_path):
		DirAccess.remove_absolute(ui_path.path_join(file))
	DirAccess.remove_absolute(ui_path)
	scene.queue_free()