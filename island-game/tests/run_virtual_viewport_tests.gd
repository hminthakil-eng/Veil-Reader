extends SceneTree
## Godot virtual-canvas size smoke. These are simulated viewport sizes, NOT Android screenshots.
var failures: int = 0
var total: int = 0

func _initialize() -> void:
	call_deferred("_run")

func _check(ok: bool, label: String) -> void:
	total += 1
	if ok:
		print("PASS ",label)
	else:
		failures += 1
		push_error("FAIL ",label)

func _run() -> void:
	var original_size: Vector2i = root.content_scale_size
	var day_scene: Control = load("res://scenes/first_day.tscn").instantiate()
	root.add_child(day_scene)
	await process_frame
	for spec in [
		{"size":Vector2i(960,540), "vertical":false},
		{"size":Vector2i(1280,720), "vertical":false},
		{"size":Vector2i(720,1280), "vertical":true},
		{"size":Vector2i(540,960), "vertical":true}
	]:
		var current: Vector2i = spec["size"]
		root.content_scale_size = current
		await process_frame
		await process_frame
		day_scene._apply_layout_for_viewport(Vector2(current))
		await process_frame
		var vertical: bool = spec["vertical"]
		_check(day_scene.portrait_layout == vertical, "%s: correct reflow" % current)
		_check(day_scene.shell is VBoxContainer if vertical else day_scene.shell is HBoxContainer, "%s: layout class" % current)
		_check(day_scene.controls.get_parent() == day_scene.shell, "%s: action panel remains attached" % current)
		_check(day_scene.controls.size.x >= 280.0, "%s: menu has usable logical width" % current)
		_check(day_scene.controls.size.y >= 150.0, "%s: scrollable menu has minimum height" % current)
		_check(day_scene.category_panels.size() == 5 and day_scene.category_picker.custom_minimum_size.y >= 48, "%s: grouped touch navigation intact" % current)
		_check(day_scene.day.commerce.economy.total_coins() == 88, "%s: view change has no money side effect" % current)
		print("VIEWPORT_GEOMETRY requested=",current," actual=",root.get_visible_rect().size," shell=",day_scene.shell.get_global_rect()," panel=",day_scene.controls.get_global_rect())
	day_scene.queue_free()
	await process_frame
	root.content_scale_size = original_size
	print("VIRTUAL_VIEWPORT_TEST_RESULTS passes=%d failures=%d" % [total-failures,failures])
	quit(1 if failures else 0)
