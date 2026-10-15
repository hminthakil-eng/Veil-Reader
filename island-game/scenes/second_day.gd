extends Control
## Touch-oriented, single-column DAY TWO greybox, not finished illustration.
var day: IslandSecondDayController
var checkpoints: IslandSecondDayCheckpointStore = IslandSecondDayCheckpointStore.new()
var first_store: IslandDayCheckpointStore = IslandDayCheckpointStore.new()
var buttons: Dictionary = {}
var message: Label
var counters: Label
var progress: Label
var pixel_stage: Control

func _ready() -> void:
	day = checkpoints.load_latest()
	if day == null:
		var first: IslandFirstDayController = first_store.load_latest()
		if first != null and first.day_closed:
			day = IslandSecondDayController.new(first)
	var scroll := ScrollContainer.new()
	scroll.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	add_child(scroll)
	var column := VBoxContainer.new()
	column.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	column.add_theme_constant_override("separation", 8)
	scroll.add_child(column)
	_section(column, "روز دوم — تنور روشن / پروندهٔ بندر")
	pixel_stage = load("res://scenes/pixel_district.gd").new()
	pixel_stage.custom_minimum_size = Vector2(0, 195)
	pixel_stage.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	column.add_child(pixel_stage)
	counters = _section(column, "")
	message = _section(column, "")
	progress = _section(column, "")
	_section(column, "گفت‌وگو / آشنایی")
	_button(column, "meet_baker", "گفت‌وگو با مهتاب نانوا", _meet_baker)
	_button(column, "meet_archivist", "گفت‌وگو با بایگان بندر", _meet_archivist)
	_section(column, "سه راه اخلاقی بایگانی")
	_button(column, "public_registry", "بررسی سند عمومی", func() -> void: _archive("public_registry"))
	_button(column, "seek_permission", "درخواست اجازه از صاحب سند", func() -> void: _archive("seek_permission"))
	_button(column, "preserve_seal", "حفظ مهر و پیگیری سرنخ دیگر", func() -> void: _archive("preserve_seal"))
	_section(column, "پیش‌خرید نان؛ چهار سکه واقعی")
	_button(column, "bookshop", "سرمایه‌گذاری کتاب‌فروشی", func() -> void: _fund("bookshop"))
	_button(column, "weaver", "سفارش از بودجهٔ مستقل نساج", func() -> void: _fund("weaver"))
	_button(column, "external_trade", "پیش‌خرید بهرام با پول خارجی", func() -> void: _fund("external_trade"))
	_button(column, "bake", "تولید نان و خرید پیش‌بند", _bake)
	_section(column, "تحویل چهار نانِ تولیدشده")
	_button(column, "pantry", "همه به سفرهٔ عمومی", func() -> void: _deliver("pantry"))
	_button(column, "stalls", "همه به دکه‌های محله", func() -> void: _deliver("stalls"))
	_button(column, "mixed", "دو نان عمومی، دو نان بازار", func() -> void: _deliver("mixed"))
	_button(column, "finish", "پایان روز دوم", _finish)
	_section(column, "ذخیرهٔ مستقل روز دوم")
	_button(column, "save", "ذخیرهٔ پیشرفت", _save)
	_button(column, "load", "بازیابی آخرین ذخیرهٔ سالم", _load)
	_button(column, "back", "بازگشت به روز اول", _back)
	_refresh()

func _section(parent: VBoxContainer, value: String) -> Label:
	var item := Label.new()
	item.text = value
	item.text_direction = Control.TEXT_DIRECTION_RTL
	item.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	item.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	parent.add_child(item)
	return item

func _button(parent: VBoxContainer, key: String, label: String, callback: Callable) -> void:
	var btn := Button.new()
	btn.custom_minimum_size = Vector2(0, 52)
	btn.text_direction = Control.TEXT_DIRECTION_RTL
	btn.text = label
	btn.pressed.connect(callback)
	parent.add_child(btn)
	buttons[key] = btn

func _meet_baker() -> void:
	var ok: bool = day != null and day.meet_baker()
	message.text = day.story_log[-1] if ok else "این گفت‌وگو پیش‌تر انجام شده یا روز دوم در دسترس نیست."
	_refresh()

func _meet_archivist() -> void:
	var ok: bool = day != null and day.meet_archivist()
	message.text = day.story_log[-1] if ok else "بایگانی هنوز در دسترس نیست یا پیش‌تر صحبت کرده‌اید."
	_refresh()

func _archive(route: String) -> void:
	var ok: bool = day != null and day.choose_archive(route)
	message.text = day.story_log[-1] if ok else "این مسیر قبلاً انتخاب شده یا هنوز با بایگان صحبت نکرده‌اید."
	_refresh()

func _fund(payer: String) -> void:
	var ok: bool = day != null and day.fund_bakery(payer)
	message.text = day.story_log[-1] if ok else "برای این قرارداد پول یا پیش‌نیاز کافی ندارید؛ پرداخت تکراری ممنوع است."
	_refresh()

func _bake() -> void:
	var ok: bool = day != null and day.bake_bread()
	message.text = day.story_log[-1] if ok else "قرارداد یا مواد کافی نیست؛ تنور دوباره بی‌هزینه نان تولید نمی‌کند."
	_refresh()

func _deliver(route: String) -> void:
	var ok: bool = day != null and day.deliver(route)
	message.text = day.story_log[-1] if ok else "اول باید چهار نان واقعی تولید کنید؛ تحویل دوباره ممنوع است."
	_refresh()

func _finish() -> void:
	var ok: bool = day != null and day.close_day()
	message.text = day.story_log[-1] if ok else "بایگانی، نانوایی و تحویل نان هنوز تکمیل نشده‌اند."
	_refresh()

func _save() -> void:
	var ok: bool = checkpoints.save_day(day)
	message.text = "پیشرفت روز دوم به‌صورت نسخه‌دار ذخیره شد." if ok else "ذخیرهٔ روز دوم ناموفق بود: " + checkpoints.last_status
	_refresh()

func _load() -> void:
	var loaded: IslandSecondDayController = checkpoints.load_latest()
	if loaded != null:
		day = loaded
		message.text = "وضعیت روز دوم، کالاها و حساب‌ها از نسخهٔ سالم بازیابی شدند."
	else:
		message.text = "هیچ ذخیرهٔ سالمی برای روز دوم موجود نیست؛ پیشرفت فعلی تغییر نکرد."
	_refresh()

func _back() -> void:
	get_tree().change_scene_to_file("res://scenes/first_day.tscn")

func _refresh() -> void:
	var ready: bool = day != null and day.ready
	pixel_stage.call("show_view", "street", ready)
	if not ready:
		counters.text = "روز دوم قفل است؛ روز اول را کامل و ذخیره کنید."
		progress.text = "بازگشت به روز اول و تکمیل چراغ‌ها ضروری است."
		for key in buttons:
			buttons[key].disabled = key not in ["back"]
		return
	buttons["save"].disabled = false
	buttons["load"].disabled = false
	buttons["back"].disabled = false
	var eco: IslandEconomyLedger = day.day_one.commerce.economy
	counters.text = "دخل کتاب‌فروشی: %d | نساجی: %d | نانوایی: %d | بازرگان: %d | جمع: %d" % [
		int(eco.wallets["bookshop"]), int(eco.wallets["weaver"]),
		int(eco.wallets["bakery"]), int(eco.wallets["external_trade"]), eco.total_coins()]
	progress.text = "گندم: %d | پارچه: %d | نان: %d | سفرهٔ عمومی: %d | دکه‌ها: %d" % [
		day.grain, day.weaver_cloth, day.loaves, day.pantry_delivered, day.stalls_delivered]
	if message.text.is_empty():
		message.text = day.story_log[-1]
	buttons["meet_baker"].disabled = day.baker_met or day.day_closed
	buttons["meet_archivist"].disabled = day.archivist_met or day.day_closed
	for key in IslandSecondDayController.ARCHIVE_ROUTES:
		buttons[key].disabled = not day.archivist_met or day.archive_route != "" or day.day_closed
	for key in IslandSecondDayController.FUNDERS:
		buttons[key].disabled = not day.baker_met or day.archive_route == "" or day.funder != "" or day.day_closed
	buttons["bake"].disabled = day.funder == "" or day.baked or day.day_closed
	for key in IslandSecondDayController.DELIVERY_ROUTES:
		buttons[key].disabled = not day.baked or day.delivery_route != "" or day.day_closed
	buttons["finish"].disabled = day.delivery_route == "" or day.day_closed
