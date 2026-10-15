class_name IslandSecondDayCheckpointStore
extends RefCounted
## Separate append-only chapter-two checkpoints. Never edits day-one save files.
const SCHEMA: int = 2
const MAX_NUMBER: int = 99999999
var folder_name: String
var last_status: String = "not_started"

func _init(name: String = "island_bookshop_day2") -> void:
	if name.is_empty() or ".." in name or "/" in name or "\\" in name or ":" in name:
		folder_name = ""
		last_status = "invalid_folder"
	else:
		folder_name = name

func _folder() -> String:
	return ProjectSettings.globalize_path("user://" + folder_name)

func _name(n: int) -> String:
	return "checkpoint_%08d.json" % n

func _numbers() -> Array[int]:
	var seqs: Array[int] = []
	if folder_name == "" or not DirAccess.dir_exists_absolute(_folder()):
		return seqs
	for name in DirAccess.get_files_at(_folder()):
		if name.length() != 24 or not name.begins_with("checkpoint_") or not name.ends_with(".json"):
			continue
		var digits: String = name.substr(11, 8)
		if not digits.is_valid_int():
			continue
		var n: int = digits.to_int()
		if n >= 1 and n <= MAX_NUMBER:
			seqs.append(n)
	seqs.sort()
	seqs.reverse()
	return seqs

func _read(filename: String, expected: int) -> IslandSecondDayController:
	if not FileAccess.file_exists(filename):
		return null
	var f: FileAccess = FileAccess.open(filename, FileAccess.READ)
	if f == null:
		return null
	var size: int = f.get_length()
	if size < 20 or size > 2000000:
		f.close()
		return null
	var raw: String = f.get_as_text()
	f.close()
	var parsed: Variant = JSON.parse_string(raw)
	if typeof(parsed) != TYPE_DICTIONARY:
		return null
	var env: Dictionary = parsed
	if env.size() != 4 or not env.has_all(["schema","sequence","digest","snapshot"]):
		return null
	if typeof(env["schema"]) not in [TYPE_INT,TYPE_FLOAT] or float(env["schema"]) != float(SCHEMA):
		return null
	if typeof(env["sequence"]) not in [TYPE_INT,TYPE_FLOAT] or float(env["sequence"]) != float(expected):
		return null
	if typeof(env["digest"]) != TYPE_STRING or typeof(env["snapshot"]) != TYPE_DICTIONARY:
		return null
	if JSON.stringify(env["snapshot"]).sha256_text() != env["digest"]:
		return null
	return IslandSecondDayController.from_snapshot(env["snapshot"])

func save_day(day: IslandSecondDayController, simulate_abort: bool = false) -> bool:
	if folder_name == "" or day == null or not day.ready:
		last_status = "invalid_day"
		return false
	var value: Dictionary = day.snapshot()
	if IslandSecondDayController.from_snapshot(value) == null:
		last_status = "invalid_state"
		return false
	var path: String = _folder()
	if DirAccess.make_dir_recursive_absolute(path) != OK:
		last_status = "mkdir_failed"
		return false
	var known: Array[int] = _numbers()
	var next_n: int = 1 if known.is_empty() else known[0] + 1
	if next_n > MAX_NUMBER:
		last_status = "sequence_exhausted"
		return false
	var canonical: Dictionary = JSON.parse_string(JSON.stringify(value))
	var env: Dictionary = {
		"schema": SCHEMA, "sequence": next_n,
		"snapshot": canonical, "digest": JSON.stringify(canonical).sha256_text()
	}
	var partial: String = path.path_join("staging_%08d.part" % next_n)
	var committed: String = path.path_join(_name(next_n))
	if FileAccess.file_exists(committed):
		last_status = "exists"
		return false
	var f: FileAccess = FileAccess.open(partial, FileAccess.WRITE)
	if f == null:
		last_status = "write_failed"
		return false
	f.store_string(JSON.stringify(env))
	f.flush()
	f.close()
	if _read(partial, next_n) == null:
		last_status = "staging_failed"
		return false
	if simulate_abort:
		last_status = "abort_injected"
		return false
	if DirAccess.rename_absolute(partial, committed) != OK:
		last_status = "commit_failed"
		return false
	if _read(committed, next_n) == null:
		last_status = "verification_failed"
		return false
	last_status = "saved_%08d" % next_n
	return true

func load_latest() -> IslandSecondDayController:
	if folder_name == "":
		last_status = "invalid_folder"
		return null
	for n in _numbers():
		var day: IslandSecondDayController = _read(_folder().path_join(_name(n)), n)
		if day != null:
			last_status = "loaded_%08d" % n
			return day
	last_status = "no_valid_checkpoint"
	return null

func latest_valid_number() -> int:
	if folder_name == "":
		return 0
	for n in _numbers():
		if _read(_folder().path_join(_name(n)), n) != null:
			return n
	return 0
