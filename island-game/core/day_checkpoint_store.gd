class_name IslandDayCheckpointStore
extends RefCounted
## Append-only versioned checkpoints under user:// with verified on-disk temp,
## rename-commit and fallback to earlier valid checkpoints.
## No claim of OS-level power-loss atomicity or adversarial anti-cheat security.

const SAVE_SCHEMA: int = 1
const PREFIX: String = "checkpoint_"
const SUFFIX: String = ".json"
const CHECKPOINT_DIGITS: int = 8
const MAX_CHECKPOINT_NUMBER: int = 99999999

var folder_name: String
var last_status: String = "not_started"

func _init(safe_folder_name: String = "island_bookshop_day1") -> void:
	if safe_folder_name.is_empty() or ".." in safe_folder_name or "/" in safe_folder_name or "\\" in safe_folder_name or ":" in safe_folder_name:
		folder_name = ""
		last_status = "invalid_folder"
	else:
		folder_name = safe_folder_name

func _absolute_folder() -> String:
	if folder_name.is_empty():
		return ""
	return ProjectSettings.globalize_path("user://" + folder_name)

func _checkpoint_name(sequence: int) -> String:
	return "%s%08d%s" % [PREFIX, sequence, SUFFIX]

func _parse_sequence(file_name: String) -> int:
	if not file_name.begins_with(PREFIX) or not file_name.ends_with(SUFFIX):
		return -1
	if file_name.length() != PREFIX.length() + CHECKPOINT_DIGITS + SUFFIX.length():
		return -1
	var number: String = file_name.substr(PREFIX.length(), CHECKPOINT_DIGITS)
	if not number.is_valid_int():
		return -1
	var seq: int = number.to_int()
	if seq < 1 or seq > MAX_CHECKPOINT_NUMBER:
		return -1
	return seq

func _all_sequences() -> Array[int]:
	var numbers: Array[int] = []
	if folder_name.is_empty():
		return numbers
	var absolute: String = _absolute_folder()
	if not DirAccess.dir_exists_absolute(absolute):
		return numbers
	for file_name in DirAccess.get_files_at(absolute):
		var n: int = _parse_sequence(file_name)
		if n > 0:
			numbers.append(n)
	numbers.sort()
	numbers.reverse()
	return numbers

func _decode_file(filename: String, expected_sequence: int) -> IslandFirstDayController:
	if not FileAccess.file_exists(filename):
		return null
	var file: FileAccess = FileAccess.open(filename, FileAccess.READ)
	if file == null:
		return null
	if file.get_length() < 20 or file.get_length() > 2000000:
		file.close()
		return null
	var raw: String = file.get_as_text()
	file.close()
	var parsed: Variant = JSON.parse_string(raw)
	if typeof(parsed) != TYPE_DICTIONARY:
		return null
	var envelope: Dictionary = parsed
	if envelope.size() != 4 or not envelope.has_all(["schema", "sequence", "digest", "snapshot"]):
		return null
	if typeof(envelope["schema"]) != TYPE_FLOAT and typeof(envelope["schema"]) != TYPE_INT:
		return null
	if int(envelope["schema"]) != SAVE_SCHEMA:
		return null
	if typeof(envelope["sequence"]) not in [TYPE_FLOAT, TYPE_INT] or float(envelope["sequence"]) != float(expected_sequence):
		return null
	if typeof(envelope["digest"]) != TYPE_STRING or typeof(envelope["snapshot"]) != TYPE_DICTIONARY:
		return null
	var checksum: String = JSON.stringify(envelope["snapshot"]).sha256_text()
	if checksum != envelope["digest"]:
		return null
	return IslandFirstDayController.from_snapshot(envelope["snapshot"])

func save_day(day: IslandFirstDayController, simulate_interrupted_write: bool = false) -> bool:
	if folder_name.is_empty() or day == null:
		last_status = "invalid_save"
		return false
	var snapshot: Dictionary = day.snapshot()
	if IslandFirstDayController.from_snapshot(snapshot) == null:
		last_status = "invalid_day_state"
		return false
	var path: String = _absolute_folder()
	if DirAccess.make_dir_recursive_absolute(path) != OK:
		last_status = "could_not_create_folder"
		return false
	var existing: Array[int] = _all_sequences()
	var new_number: int = 1 if existing.is_empty() else existing[0] + 1
	if new_number > MAX_CHECKPOINT_NUMBER:
		last_status = "sequence_exhausted"
		return false
	var canonical_snapshot: Dictionary = JSON.parse_string(JSON.stringify(snapshot))
	var payload: Dictionary = {
		"schema": SAVE_SCHEMA,
		"sequence": new_number,
		"digest": JSON.stringify(canonical_snapshot).sha256_text(),
		"snapshot": canonical_snapshot
	}
	var serialized: String = JSON.stringify(payload)
	var staging_path: String = path.path_join("staging_%08d.part" % new_number)
	var final_path: String = path.path_join(_checkpoint_name(new_number))
	if FileAccess.file_exists(final_path):
		last_status = "target_exists"
		return false
	var staging: FileAccess = FileAccess.open(staging_path, FileAccess.WRITE)
	if staging == null:
		last_status = "staging_open_failed"
		return false
	staging.store_string(serialized)
	staging.flush()
	staging.close()
	if _decode_file(staging_path, new_number) == null:
		last_status = "staging_validation_failed"
		return false
	if simulate_interrupted_write:
		# Fault-injection: an apparently complete .part file must NOT be selected as saved.
		last_status = "injected_abort_before_commit"
		return false
	if DirAccess.rename_absolute(staging_path, final_path) != OK:
		last_status = "rename_failed"
		return false
	if _decode_file(final_path, new_number) == null:
		last_status = "post_commit_validation_failed"
		return false
	last_status = "saved_%08d" % new_number
	return true

func load_latest() -> IslandFirstDayController:
	if folder_name.is_empty():
		last_status = "invalid_folder"
		return null
	var path: String = _absolute_folder()
	for n in _all_sequences():
		var candidate: IslandFirstDayController = _decode_file(path.path_join(_checkpoint_name(n)), n)
		if candidate != null:
			last_status = "loaded_%08d" % n
			return candidate
	last_status = "no_valid_checkpoint"
	return null

func latest_valid_number() -> int:
	var path: String = _absolute_folder()
	for n in _all_sequences():
		if _decode_file(path.path_join(_checkpoint_name(n)), n) != null:
			return n
	return 0
