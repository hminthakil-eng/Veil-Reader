extends Control
## Player-operated first-day vertical-slice greybox, not final pixel art or audiobook app.
## Persian dialogue is rendered in the high-resolution Control layer above the world.
var day: IslandFirstDayController = IslandFirstDayController.new()
var checkpoints: IslandDayCheckpointStore = IslandDayCheckpointStore.new()
var pixel_stage: Control
var status: Label
var narrative: Label
var briefing: Label
var picked: OptionButton
var click_buttons: Dictionary = {}
var scene_zone: String = "shop"
var latest: String = "استاد صحاف در را باز می‌کند: «کار کتاب‌فروشی با یک مشتری شروع می‌شود، نه با یک امپراتوری.»"

func _ready() -> void:
	var shell := HBoxContainer.new()
	shell.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	shell.add_theme_constant_override("separation", 8)
	add_child(shell)

	var story_area := VBoxContainer.new()
	story_area.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	story_area.custom_minimum_size = Vector2(600,0)
	shell.add_child(story_area)

	var title := Label.new()
	title.text = "ISLAND BOOKSHOP  /  THE FIRST LANTERN"
	title.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	story_area.add_child(title)

	pixel_stage = load("res://scenes/pixel_district.gd").new()
	pixel_stage.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	pixel_stage.custom_minimum_size = Vector2(610,220)
	story_area.add_child(pixel_stage)

	var zone_bar := HBoxContainer.new()
	story_area.add_child(zone_bar)
	_make_button(zone_bar, "view_shop", "داخل کتاب‌فروشی", func() -> void: _change_zone("shop"))
	_make_button(zone_bar, "view_street", "کوچهٔ بندر", func() -> void: _change_zone("street"))

	status = Label.new()
	status.text_direction = Control.TEXT_DIRECTION_RTL
	status.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	status.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	story_area.add_child(status)

	narrative = Label.new()
	narrative.text_direction = Control.TEXT_DIRECTION_RTL
	narrative.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	narrative.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	narrative.custom_minimum_size = Vector2(0,96)
	story_area.add_child(narrative)

	briefing = Label.new()
	briefing.text_direction = Control.TEXT_DIRECTION_RTL
	briefing.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	briefing.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	story_area.add_child(briefing)

	var controls := ScrollContainer.new()
	controls.custom_minimum_size = Vector2(315,0)
	controls.size_flags_horizontal = Control.SIZE_SHRINK_END
	controls.size_flags_vertical = Control.SIZE_EXPAND_FILL
	shell.add_child(controls)
	var action_stack := VBoxContainer.new()
	action_stack.custom_minimum_size = Vector2(287,0)
	action_stack.add_theme_constant_override("separation",5)
	controls.add_child(action_stack)
	_section(action_stack, "صندوق شش کتاب خیالی")
	picked = OptionButton.new()
	picked.text_direction = Control.TEXT_DIRECTION_RTL
	for book_id in IslandFirstDayController.BOOK_IDS:
		picked.add_item(IslandFirstDayController.TITLES[book_id])
	action_stack.add_child(picked)
	_make_button(action_stack,"inspect","بررسی جلد و حاشیه", _inspect)
	_make_button(action_stack,"repair","مرمت کتاب (۳ سکه)", _repair)
	_section(action_stack,"مشتری اول / دریا، شاگرد نساج")
	_make_button(action_stack,"meet_darya","شنیدن درخواست دریا",_meet_darya)
	_make_button(action_stack,"consent","درخواست رضایت خانواده",func() -> void: _family("consent"))
	_make_button(action_stack,"sealed","حفظ سند مهروموم‌شده",func() -> void: _family("sealed"))
	_make_button(action_stack,"public_archive","بررسی نسخهٔ عمومی بندر",func() -> void: _family("public_archive"))
	_section(action_stack,"مشتری دوم / بهرام، تاجر گذری")
	_make_button(action_stack,"meet_bahram","گفت‌وگو با بهرام",_meet_bahram)
	_make_button(action_stack,"sell","فروش فانوس سوم (۱۲ سکه)",func() -> void: _visitor(true))
	_make_button(action_stack,"decline","فروش را فعلاً رد کن",func() -> void: _visitor(false))
	_section(action_stack,"بازسازی محله / هزینهٔ ساخت ۱۴ سکه")
	_make_button(action_stack,"solo","کتاب‌فروشی همهٔ هزینه را بدهد",func() -> void: _fund("solo"))
	_make_button(action_stack,"shared","مشارکت: ما ۱۰، نساجی ۴",func() -> void: _fund("shared"))
	_make_button(action_stack,"residents","مردم مستقل از کمک مالی ما اقدام کنند",func() -> void: _fund("residents"))
	_make_button(action_stack,"finish","پایان روز اول",_finish)
	_section(action_stack,"\u0630\u062e\u06cc\u0631\u0647\u0020\u0648\u0020\u0628\u0627\u0632\u06cc\u0627\u0628\u06cc\u0020\u067e\u06cc\u0634\u0631\u0641\u062a")
	_make_button(action_stack,"save","\u0630\u062e\u06cc\u0631\u0647\u0654\u0020\u0627\u0645\u0646\u0020\u067e\u06cc\u0634\u0631\u0641\u062a", _save_checkpoint)
	_make_button(action_stack,"load","\u0628\u0627\u0632\u06cc\u0627\u0628\u06cc\u0020\u0622\u062e\u0631\u06cc\u0646\u0020\u0630\u062e\u06cc\u0631\u0647\u0654\u0020\u0633\u0627\u0644\u0645", _load_checkpoint)
	_refresh()

func _section(parent: VBoxContainer, value: String) -> void:
	var header := Label.new()
	header.text = value
	header.text_direction = Control.TEXT_DIRECTION_RTL
	header.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	parent.add_child(header)

func _make_button(parent: BoxContainer, key: String, value: String, handler: Callable) -> void:
	var button := Button.new()
	button.text = value
	button.text_direction = Control.TEXT_DIRECTION_RTL
	button.pressed.connect(handler)
	parent.add_child(button)
	click_buttons[key] = button

func _selected_book() -> String:
	return String(IslandFirstDayController.BOOK_IDS[picked.get_selected_id()])

func _change_zone(value: String) -> void:
	scene_zone = value
	_refresh()

func _inspect() -> void:
	var read: Dictionary = day.inspect(_selected_book())
	latest = read.get("text","کتاب پیدا نشد.")
	_refresh()

func _repair() -> void:
	latest = "مرمت انجام شد. کاغذ جان تازه گرفت." if day.repair(_selected_book()) else "این مرمت مجاز نیست یا پیش از این انجام شده است."
	_refresh()

func _meet_darya() -> void:
	var ok: bool = day.meet_darya()
	latest = "دریا با آستین‌های نیلی: «مادرم می‌گوید اسم خانواده‌مان از این دفتر پاک شده. می‌شود بدون باز کردنش کمکمان کنید؟»" if ok else "درخواست دریا پیش‌تر شنیده شده است."
	_refresh()

func _family(route: String) -> void:
	var success: bool = day.choose_family(route)
	latest = day.story_log[-1] if success else "این تصمیم قبلاً گرفته شده؛ اجازه ندارید بی‌سر و صدا عوضش کنید."
	_refresh()

func _meet_bahram() -> void:
	var ok: bool = day.meet_bahram()
	latest = "بهرام دستکش‌هایش را صاف می‌کند: «فانوس سوم را می‌خواهم. شنیده‌ام بعضی نقشه‌ها ارزشِ خاموش‌ماندن دارند.»" if ok else "بهرام پیش‌تر وارد مغازه شده است."
	_refresh()

func _visitor(sell: bool) -> void:
	var success: bool = day.resolve_visitor(sell)
	latest = day.story_log[-1] if success else "باید اول دربارهٔ دفتر خانوادگی تصمیم بگیری و در صورت فروش، کتابِ فانوس را مرمت کنی."
	_refresh()

func _fund(route: String) -> void:
	var success: bool = day.fund_lanterns(route)
	latest = day.story_log[-1] if success else "بودجهٔ واقعی کافی نیست یا پروژه پیش‌تر تکمیل شده است."
	if success:
		scene_zone = "street"
	_refresh()

func _finish() -> void:
	var success: bool = day.close_day()
	latest = day.story_log[-1] if success else "هنوز باید دو درخواست مشتری‌ها و تصمیم روشنایی محله را به پایان برسانی."
	if success:
		scene_zone = "street"
	_refresh()

func _save_checkpoint() -> void:
	var ok: bool = checkpoints.save_day(day)
	latest = "\u067e\u06cc\u0634\u0631\u0641\u062a\u0020\u062b\u0628\u062a\u0020\u0634\u062f\u061b\u0020\u0646\u0633\u062e\u0647\u200c\u0647\u0627\u06cc\u0020\u0633\u0627\u0644\u0645\u0020\u0642\u0628\u0644\u06cc\u0020\u0645\u062d\u0641\u0648\u0638\u0020\u0647\u0633\u062a\u0646\u062f\u002e" if ok else "\u0630\u062e\u06cc\u0631\u0647\u0020\u0646\u0627\u0645\u0648\u0641\u0642\u0020\u0628\u0648\u062f\u003a\u0020" + checkpoints.last_status
	_refresh()

func _load_checkpoint() -> void:
	var loaded: IslandFirstDayController = checkpoints.load_latest()
	if loaded == null:
		latest = "\u0630\u062e\u06cc\u0631\u0647\u0654\u0020\u0633\u0627\u0644\u0645\u06cc\u0020\u067e\u06cc\u062f\u0627\u0020\u0646\u0634\u062f\u061b\u0020\u067e\u06cc\u0634\u0631\u0641\u062a\u0020\u0641\u0639\u0644\u06cc\u0020\u062a\u063a\u06cc\u06cc\u0631\u0020\u0646\u06a9\u0631\u062f\u002e"
	else:
		day = loaded
		latest = "\u0622\u062e\u0631\u06cc\u0646\u0020\u0646\u0633\u062e\u0647\u0654\u0020\u0633\u0627\u0644\u0645\u0020\u0647\u0645\u0631\u0627\u0647\u0020\u067e\u0648\u0644\u0020\u0648\u0020\u062a\u0635\u0645\u06cc\u0645\u200c\u0647\u0627\u06cc\u0020\u062f\u0627\u0633\u062a\u0627\u0646\u06cc\u0020\u0628\u0627\u0632\u06cc\u0627\u0628\u06cc\u0020\u0634\u062f\u002e"
		scene_zone = "street" if day.commerce.economy.street_is_lit() else "shop"
	_refresh()
func _refresh() -> void:
	var economy: IslandEconomyLedger = day.commerce.economy
	pixel_stage.call("show_view", scene_zone, economy.street_is_lit())
	status.text = "کتاب‌فروشی: %d سکه  |  نساجی: %d  |  تاجر: %d  |  صندوق شهر: %d  |  جمع: %d" % [
		day.commerce.available_coins(), int(economy.wallets["weaver"]),
		int(economy.wallets["external_trade"]),int(economy.wallets["town_escrow"]),economy.total_coins()
	]
	narrative.text = latest
	var tasks := "روز نخست  •  داستان خانواده: %s  •  تاجر: %s  •  تامین چراغ: %s" % [
		"انجام شد" if not day.family_choice.is_empty() else "منتظر تصمیم",
		"انجام شد" if not day.buyer_choice.is_empty() else "منتظر تصمیم",
		"پایان‌یافته" if economy.street_is_lit() else "خاموش"
	]
	briefing.text = tasks + ("\nروز اول به پایان رسید. کوچه اکنون روشن است." if day.day_closed else "")
	var family_pending: bool = day.family_choice.is_empty() and not day.day_closed
	click_buttons["meet_darya"].disabled = day.darya_met or day.day_closed
	for key in ["consent","sealed","public_archive"]:
		click_buttons[key].disabled = not family_pending or not day.darya_met
	click_buttons["meet_bahram"].disabled = family_pending or day.day_closed or day.bahram_met
	var buyer_pending: bool = not family_pending and day.bahram_met and day.buyer_choice.is_empty() and not day.day_closed
	for key in ["sell","decline"]:
		click_buttons[key].disabled = not buyer_pending
	var funding_pending: bool = not day.buyer_choice.is_empty() and day.funding_choice.is_empty() and not day.day_closed
	for key in ["solo","shared","residents"]:
		click_buttons[key].disabled = not funding_pending
	click_buttons["finish"].disabled = day.funding_choice.is_empty() or day.day_closed
	click_buttons["repair"].disabled = day.day_closed
