extends SceneTree
## Device-layout logic proof; NOT a physical touch or Persian font rendering test.
var passed: int = 0
var failed: int = 0
var target: int = 20

func check(value: bool, label: String) -> void:
	if value:
		passed += 1
		print("PASS ", label)
	else:
		failed += 1
		push_error("FAIL " + label)

func _initialize() -> void:
	call_deferred("_run")

func _run() -> void:
	await _test_orientation(Vector2i(432, 960), true)
	await _test_orientation(Vector2i(960, 540), false)
	print("MOBILE_LAYOUT_TEST_RESULTS passes=%d failures=%d" % [passed, failed])
	if passed + failed != target:
		failed += 1
		push_error("FAIL expected exactly %d assertions" % target)
	quit(1 if failed > 0 else 0)

func _test_orientation(dimensions: Vector2i, portrait_expected: bool) -> void:
	root.size = dimensions
	await process_frame
	var packed: PackedScene = load("res://scenes/first_day.tscn")
	var scene: Control = packed.instantiate()
	root.add_child(scene)
	await process_frame
	await process_frame
	var name: String = "portrait" if portrait_expected else "landscape"
	check(scene.portrait_mode == portrait_expected, name + ": engine chooses correct adaptive layout")
	var shell_container: Control = scene.get_child(0)
	check((shell_container is VBoxContainer) == portrait_expected, name + ": portrait has fixed story and separate scrolling action deck")
	var buttons: Dictionary = scene.click_buttons
	check(buttons.size() >= 17, name + ": all story and commercial actions preserved")
	check(buttons.has("save") and buttons.has("load"), name + ": manual recovery remains reachable")
	check(scene.status.text_direction == Control.TEXT_DIRECTION_RTL and scene.narrative.text_direction == Control.TEXT_DIRECTION_RTL, name + ": ledger and novel text explicitly RTL")
	check(buttons["consent"].text_direction == Control.TEXT_DIRECTION_RTL, name + ": personal narrative choice explicitly RTL")
	check(scene.pixel_stage != null and scene.pixel_stage.size.x > 0, name + ": responsive stage instantiates and has positive width")
	check(scene.day.commerce.economy.total_coins() == 88, name + ": a layout change cannot duplicate any money")
	check(buttons["repair"].custom_minimum_size.y >= (56 if portrait_expected else 38), name + ": defined accessible minimum action height")
	if portrait_expected:
		check(scene.pixel_stage.size.x <= float(dimensions.x) + 5.0, name + ": no horizontal clipping from old 610px world art")
	else:
		check(scene.pixel_stage.size.x >= 400, name + ": wider layout keeps pixel stage usable")
	scene.queue_free()
	await process_frame
