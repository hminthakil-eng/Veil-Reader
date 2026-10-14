extends RefCounted
## Isolated prototype save store. No Veil Reader database or private book references.
const DEST := "user://island_greybox_save.json"
const TMP := "user://island_greybox_save.tmp"
const BAK := "user://island_greybox_save.bak"

func save_state(data: Dictionary) -> bool:
    var f := FileAccess.open(TMP, FileAccess.WRITE)
    if f == null:
        return false
    f.store_string(JSON.stringify(data))
    f.flush()
    f.close()
    var target := ProjectSettings.globalize_path(DEST)
    var staged := ProjectSettings.globalize_path(TMP)
    var backup := ProjectSettings.globalize_path(BAK)
    if FileAccess.file_exists(DEST):
        if DirAccess.copy_absolute(target, backup) != OK:
            return false
        if DirAccess.remove_absolute(target) != OK:
            return false
    if DirAccess.rename_absolute(staged, target) != OK:
        if FileAccess.file_exists(BAK):
            DirAccess.copy_absolute(backup, target)
        return false
    return true

func load_state() -> Dictionary:
    for candidate in [DEST, BAK]:
        if FileAccess.file_exists(candidate):
            var content: String = FileAccess.get_file_as_string(candidate)
            var obj: Variant = JSON.parse_string(content)
            if obj is Dictionary:
                return obj
    return {}
