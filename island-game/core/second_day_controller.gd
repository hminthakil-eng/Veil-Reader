class_name IslandSecondDayController
extends RefCounted
## Day two vertical slice: verified cash provenance and finite physical goods.
## This class does not read actual Reader books or private family archives.
const VERSION: int = 1
const FUNDERS := ["bookshop", "weaver", "external_trade"]
const ARCHIVE_ROUTES := ["public_registry", "seek_permission", "preserve_seal"]
const DELIVERY_ROUTES := ["pantry", "stalls", "mixed"]

var day_one: IslandFirstDayController
var ready: bool = false
var baker_met: bool = false
var archivist_met: bool = false
var archive_route: String = ""
var funder: String = ""
var baked: bool = false
var delivery_route: String = ""
var day_closed: bool = false

# Physical stock is distinct from coins and never replenishes on repeated taps.
var grain: int = 6
var weaver_cloth: int = 2
var loaves: int = 0
var pantry_delivered: int = 0
var stalls_delivered: int = 0
var story_log: Array[String] = []

func _init(completed_day: IslandFirstDayController = null, create_account: bool = true) -> void:
	if completed_day == null or not completed_day.day_closed or not completed_day.commerce.economy.street_is_lit():
		return
	var ledger: IslandEconomyLedger = completed_day.commerce.economy
	if create_account:
		if not ledger.open_local_bakery():
			return
	elif not ledger.wallets.has("bakery"):
		return
	day_one = completed_day
	ready = true
	story_log.append("صبح دوم، زیر فانوس روشن کوچه، بوی خمیر ترش می‌پیچد. اما تنور قدیمی هنوز خاموش است.")

func meet_baker() -> bool:
	if not ready or day_closed or baker_met:
		return false
	baker_met = true
	story_log.append("مهتاب، نانوا، کف دست آردی‌اش را نشان می‌دهد: «شش پیمانه گندم مانده. نان می‌پزم، اگر کارگر و پیش‌بند داشته باشم؛ وعدهٔ پول خیالی نده.»")
	return true

func meet_archivist() -> bool:
	if not ready or day_closed or archivist_met:
		return false
	archivist_met = true
	story_log.append("بایگان می‌گوید: «در دفتر عمومی، از نانواییِ پیشین چیزی نوشته‌اند. اسناد خصوصی خانواده‌ها همچنان امانت است.»")
	return true

func choose_archive(route: String) -> bool:
	if not ready or day_closed or not archivist_met or archive_route != "" or route not in ARCHIVE_ROUTES:
		return false
	archive_route = route
	match route:
		"public_registry":
			story_log.append("دفتر عمومی بندر نشان می‌دهد تنور تعطیل‌شده به کوچهٔ فانوس سوم آرد می‌رسانده؛ نام خانواده‌ها پوشیده می‌ماند.")
		"seek_permission":
			story_log.append("دریا تنها درخواست دیدار با مالک سند را می‌برد. هنوز هیچ اجازه‌ای صادر نشده و مهر سند باز نمی‌شود.")
		"preserve_seal":
			story_log.append("سند خانوادگی در صندوق می‌ماند. سرنخ دیگری در مُهرهای عمومیِ جواز کسب جست‌وجو می‌کنید.")
	return true

func fund_bakery(payer: String) -> bool:
	if not ready or day_closed or not baker_met or archive_route == "" or funder != "" or payer not in FUNDERS:
		return false
	# Prepaid delivery order: a fixed FOUR coins from a real payer, no minted cash.
	if not day_one.commerce.economy.prepay_bakery_order("day2_bakery_order", payer, 4):
		return false
	funder = payer
	match payer:
		"bookshop":
			story_log.append("چهار سکه از دخل کتاب‌فروشی به حساب مستقل نانوایی رفت؛ سرمایه‌گذاری شما هزینه دارد.")
		"weaver":
			story_log.append("ارغوانِ نساج چهار سکه از درآمد خودش پیش‌پرداخت نان کارکنان می‌کند؛ شما مالک پول او نیستید.")
		"external_trade":
			story_log.append("بهرام از بودجهٔ محدود خریدار خارجی چهار سکه پیش‌خرید نان ثبت می‌کند؛ پول واقعاً از حساب او کم شد.")
	return true

func bake_bread() -> bool:
	if not ready or day_closed or funder == "" or baked or grain < 4 or weaver_cloth < 1:
		return false
	# The baker purchases real sewing service from the independent weaver.
	if not day_one.commerce.economy.bakery_buy_apron("day2_apron_order", 2):
		return false
	weaver_cloth -= 1
	grain -= 4
	loaves += 4
	baked = true
	story_log.append("چهار پیمانه گندم و یک طاقه پارچه مصرف شد؛ چهار قرص نان از تنور درآمد. دو سکه دستمزد به نساج پرداخت شد.")
	return true

func deliver(route: String) -> bool:
	if not ready or day_closed or not baked or delivery_route != "" or loaves != 4 or route not in DELIVERY_ROUTES:
		return false
	match route:
		"pantry":
			pantry_delivered = 4
			stalls_delivered = 0
			story_log.append("چهار نان برای سفرهٔ عمومی فرستاده شد. حجره‌داران امروز از این محموله سهمی نگرفتند.")
		"stalls":
			pantry_delivered = 0
			stalls_delivered = 4
			story_log.append("چهار نان طبق سفارش پیش‌پرداخت‌شده به دکه‌های محله رسید. انبار عمومی امروز خالی ماند.")
		"mixed":
			pantry_delivered = 2
			stalls_delivered = 2
			story_log.append("دو نان سهم سفرهٔ عمومی و دو نان سهم دکه‌های محله شد؛ هیچ قرصی دوبار ثبت نشد.")
	loaves = 0
	delivery_route = route
	return true

func close_day() -> bool:
	if not ready or day_closed or archive_route == "" or delivery_route == "":
		return false
	day_closed = true
	story_log.append("بایگان پس از تحویل نان به دفتر بندر اشاره می‌کند: «شهر دارد برمی‌گردد، اما کسی هنوز نمی‌داند چرا نانوایی اول بسته شد.»")
	return true

func town_signals() -> Dictionary:
	return {
		"lanterns_lit": ready and day_one.commerce.economy.street_is_lit(),
		"bakery_produced": 4 if baked else 0,
		"pantry_received": pantry_delivered,
		"stalls_received": stalls_delivered,
		"grain_remaining": grain,
		"cloth_remaining": weaver_cloth,
		"cash_total": day_one.commerce.economy.total_coins() if ready else 0
	}

func snapshot() -> Dictionary:
	if not ready:
		return {}
	return {
		"version": VERSION,
		"day_one": day_one.snapshot(),
		"baker_met": baker_met,
		"archivist_met": archivist_met,
		"archive_route": archive_route,
		"funder": funder,
		"baked": baked,
		"delivery_route": delivery_route,
		"day_closed": day_closed,
		"grain": grain,
		"weaver_cloth": weaver_cloth,
		"loaves": loaves,
		"pantry_delivered": pantry_delivered,
		"stalls_delivered": stalls_delivered,
		"story_log": story_log.duplicate()
	}

static func from_snapshot(data: Dictionary) -> IslandSecondDayController:
	if data.get("version") != VERSION or typeof(data.get("day_one")) != TYPE_DICTIONARY:
		return null
	var d: IslandFirstDayController = IslandFirstDayController.from_snapshot(data["day_one"])
	if d == null or not d.day_closed or not d.commerce.economy.wallets.has("bakery"):
		return null
	for key in ["baker_met", "archivist_met", "baked", "day_closed"]:
		if typeof(data.get(key)) != TYPE_BOOL:
			return null
	for key in ["archive_route", "funder", "delivery_route"]:
		if typeof(data.get(key)) != TYPE_STRING:
			return null
	for key in ["grain", "weaver_cloth", "loaves", "pantry_delivered", "stalls_delivered"]:
		if typeof(data.get(key)) not in [TYPE_INT, TYPE_FLOAT]:
			return null
		if float(data[key]) != floorf(float(data[key])):
			return null
	if data["archive_route"] not in [""] + ARCHIVE_ROUTES or data["funder"] not in [""] + FUNDERS or data["delivery_route"] not in [""] + DELIVERY_ROUTES:
		return null
	if (data["archive_route"] != "" and not data["archivist_met"]) or (data["funder"] != "" and (not data["baker_met"] or data["archive_route"] == "")):
		return null
	if (data["baked"] and data["funder"] == "") or (data["delivery_route"] != "" and not data["baked"]) or (data["day_closed"] and data["delivery_route"] == ""):
		return null
	var has_baked: bool = data["baked"]
	var delivered: bool = data["delivery_route"] != ""
	if int(data["grain"]) != (2 if has_baked else 6) or int(data["weaver_cloth"]) != (1 if has_baked else 2):
		return null
	if int(data["loaves"]) != (0 if delivered or not has_baked else 4):
		return null
	var expected_pantry: int = 4 if data["delivery_route"] == "pantry" else (2 if data["delivery_route"] == "mixed" else 0)
	var expected_stalls: int = 4 if data["delivery_route"] == "stalls" else (2 if data["delivery_route"] == "mixed" else 0)
	if int(data["pantry_delivered"]) != expected_pantry or int(data["stalls_delivered"]) != expected_stalls:
		return null
	var econ: IslandEconomyLedger = d.commerce.economy
	var found_order: bool = false
	var found_apron: bool = false
	for event in econ.economic_events:
		if event.get("tx") == "day2_bakery_order":
			if found_order or event.get("kind") != "prepaid_bread_order" or event.get("amount") != 4 or event.get("from") != data["funder"] or event.get("to") != "bakery":
				return null
			found_order = true
		if event.get("tx") == "day2_apron_order":
			if found_apron or event.get("kind") != "bakery_textile_purchase" or event.get("amount") != 2 or event.get("from") != "bakery" or event.get("to") != "weaver":
				return null
			found_apron = true
	if found_order != (data["funder"] != "") or found_apron != has_baked:
		return null
	if typeof(data.get("story_log")) != TYPE_ARRAY:
		return null
	var restored := IslandSecondDayController.new(d, false)
	if not restored.ready:
		return null
	restored.baker_met = data["baker_met"]
	restored.archivist_met = data["archivist_met"]
	restored.archive_route = data["archive_route"]
	restored.funder = data["funder"]
	restored.baked = has_baked
	restored.delivery_route = data["delivery_route"]
	restored.day_closed = data["day_closed"]
	restored.grain = int(data["grain"])
	restored.weaver_cloth = int(data["weaver_cloth"])
	restored.loaves = int(data["loaves"])
	restored.pantry_delivered = int(data["pantry_delivered"])
	restored.stalls_delivered = int(data["stalls_delivered"])
	restored.story_log.clear()
	for line in data["story_log"]:
		if typeof(line) != TYPE_STRING:
			return null
		restored.story_log.append(line)
	return restored
