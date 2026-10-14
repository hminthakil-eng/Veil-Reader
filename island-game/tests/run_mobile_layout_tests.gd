extends SceneTree
## Structural mobile viewport and control-input QA. NOT visual font/touch hardware QA.
const EXPECTED_CHECKS: int = 25
var passed: int = 0
var failed: int = 0

func _initialize() -> void:
	call_deferred("_run")

func _verify(ok: bool, label: String) -> void:
	if ok:
		passed += 1
		print("PASS ",label)
	else:
		failed += 1
		push_error("FAIL "+label)

func _run() -> void:
	var scene: Control = load("res://scenes/first_day.tscn").instantiate()
	root.add_child(scene)
	await process_frame
	_verify(scene.shell is HBoxContainer, "960x540 scene initially uses landscape columns")
	_verify(scene.category_panels.size() == 5, "actions divided into five smaller categories")
	_verify(scene.category_picker.item_count == 5 and scene.category_picker.get_item_text(0).unicode_at(0) >= 0x0600, "five localized Persian task categories are selectable")
	_verify(scene.category_panels[0].visible, "books category visible by default")
	_verify(not scene.category_panels[2].visible, "customer actions hidden until category selected")
	var all_touch_adequate: bool = true
	for key in scene.click_buttons:
		var btn: Button = scene.click_buttons[key]
		if btn.custom_minimum_size.y < 48:
			all_touch_adequate = false
	_verify(all_touch_adequate, "all actions specify 48 logical units minimum height")
	_verify(scene.picked.custom_minimum_size.y >= 48, "book selector is larger than 48 logical units")
	_verify(scene.pixel_stage.custom_minimum_size.x == 0, "pixel stage no longer demands 610 desktop-width units")

	scene._apply_layout_for_viewport(Vector2(720, 1280))
	_verify(scene.shell is VBoxContainer and scene.portrait_layout, "720x1280 orientation reflows vertically")
	_verify(scene.controls.get_parent() == scene.shell and scene.story_area.get_parent() == scene.shell, "real same controls reparent instead of cloning state")
	_verify(scene.controls.custom_minimum_size.x == 0, "portrait menu panel not fixed to desktop 315 minimum")
	_verify(scene.pixel_stage.custom_minimum_size.y <= 124, "portrait pixel preview shrinks for real choices")
	scene.category_picker.select(1)
	scene.category_picker.item_selected.emit(1)
	_verify(scene.category_panels[1].visible and not scene.category_panels[0].visible, "family actions become active and books panel hides")
	(scene.click_buttons["meet_darya"] as Button).emit_signal("pressed")
	(scene.click_buttons["sealed"] as Button).emit_signal("pressed")
	_verify(scene.day.family_choice == "sealed", "moving/hidden category retains narrative button callback")

	scene._apply_layout_for_viewport(Vector2(540, 960))
	_verify(scene.portrait_layout and scene.shell is VBoxContainer, "540x960 smaller portrait remains one-column")
	_verify(scene.day.commerce.economy.total_coins() == 88, "reflow doesn't duplicate wallets or mint money")
	scene._apply_layout_for_viewport(Vector2(1280, 720))
	_verify(scene.shell is HBoxContainer and not scene.portrait_layout, "1280x720 transitions back to two-column")
	_verify(scene.controls.custom_minimum_size.x >= 315, "landscape controls get a usable minimum width")
	_verify(scene.category_panels[1].visible, "reflow preserves the player's selected category")
	scene.category_picker.select(2)
	scene.category_picker.item_selected.emit(2)
	(scene.click_buttons["meet_bahram"] as Button).emit_signal("pressed")
	(scene.click_buttons["decline"] as Button).emit_signal("pressed")
	_verify(scene.day.buyer_choice == "declined", "landscape dialogue works after two layout reparentings")
	scene.category_picker.select(3)
	scene.category_picker.item_selected.emit(3)
	(scene.click_buttons["residents"] as Button).emit_signal("pressed")
	_verify(scene.day.commerce.economy.street_is_lit(), "funding through reflowed menu actually restores city street")
	_verify(scene.day.commerce.economy.total_coins() == 88, "real UI events conserve money across rotation")
	scene._apply_layout_for_viewport(Vector2(800, 480))
	_verify(scene.portrait_layout, "narrow landscape avoids squeezing shop and menu into unusable tiny columns")
	_verify(scene.category_panels[3].visible, "funding category remains selected at narrow resolution")
	await process_frame
	_verify(scene.shell.get_child_count() == 2, "after deferred reparent only the two live UI panes remain")
	scene.queue_free()
	await process_frame
	if passed + failed != EXPECTED_CHECKS:
		failed += 1
		push_error("ASSERTION COUNT MISMATCH expected=%d actual=%d" % [EXPECTED_CHECKS,passed+failed-1])
	print("MOBILE_LAYOUT_TEST_RESULTS passes=%d failures=%d" % [passed,failed])
	quit(1 if failed > 0 else 0)
