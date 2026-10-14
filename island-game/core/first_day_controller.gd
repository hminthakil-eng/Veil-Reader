class_name IslandFirstDayController
extends RefCounted
## First playable shop-day: invented books only; no real EPUB, reader account or data.
## All currency transfers go through ONE IslandWorldCommerceSession.economy.
const VERSION: int = 1
const BOOK_IDS := ["tides", "bread", "names", "lantern", "herbarium", "letters"]
const TITLES := {
	"tides": "دفتر جزر خاموش",
	"bread": "نانِ فردا",
	"names": "دو نام بر یک شیرازه",
	"lantern": "فانوس سوم",
	"herbarium": "گیاهان لبهٔ نمک",
	"letters": "نامه‌هایی که پست نشدند"
}
const DESCRIPTIONS := {
	"tides": "دفتر کاپیتانی که زنگ فانوس را یک ساعت دیرتر شنیده.",
	"bread": "دستور نان‌هایی که قصهٔ خانواده‌های گرسنه را پنهان کرده.",
	"names": "تاریخ خانوادگیِ امانتی؛ شیرازه‌اش دو بار از نو دوخته شده.",
	"lantern": "کتابی دربارهٔ چراغ‌های بندر و مسیرهایی که عمداً خط خورده‌اند.",
	"herbarium": "تصویرهای گیاه‌شناسی با نمونه‌های نم‌کشیدهٔ جزیره.",
	"letters": "نامه‌های مهروموم‌شده‌ای که به صاحبانشان نرسیده‌اند."
}
const CLUES := {
	"tides": "پشت نقشه، تاریخ ورود یک کشتی پاک شده است.",
	"bread": "در حاشیه، نشانی یک نانوایی تعطیل دیده می‌شود.",
	"names": "فقط علامت بیرونیِ دو بار دوختن قابل مشاهده است؛ متن خصوصی محفوظ است.",
	"lantern": "جای یک مُهر بندری زیر جلد آشکار می‌شود.",
	"herbarium": "چند گل از بندری دیگر به کتاب چسبانده شده است.",
	"letters": "نامهٔ شخصی باز نمی‌شود؛ نام گیرنده روی پاکت باقی مانده."
}
const OWNERS := {"names": "nazhat_family", "letters": "private_sender"}
const CONDITIONS := {"tides": 75, "bread": 85, "names": 35, "lantern": 30, "herbarium": 70, "letters": 65}
const FAMILY_CHOICES := ["consent", "sealed", "public_archive"]
const FUNDING_CHOICES := ["solo", "shared", "residents"]

var commerce: IslandWorldCommerceSession
var darya_met: bool = false
var bahram_met: bool = false
var family_choice: String = ""
var buyer_choice: String = ""
var funding_choice: String = ""
var day_closed: bool = false
var story_log: Array[String] = []

func _init(existing_commerce: IslandWorldCommerceSession = null, seed_books: bool = true) -> void:
	commerce = existing_commerce if existing_commerce != null else IslandWorldCommerceSession.new()
	if seed_books:
		for book_id in BOOK_IDS:
			commerce.register_copy(book_id, book_id, CONDITIONS[book_id], OWNERS.get(book_id, "bookshop"))
		story_log.append("پیرمرد صندوق کتاب‌های نم‌زده را روی پیشخوان می‌گذارد. بیرون شهر خاموش است.")

func inspect(book_id: String) -> Dictionary:
	if not TITLES.has(book_id):
		return {"ok": false, "text": "این کتاب در صندوق نیست."}
	var copy: Dictionary = commerce.get_copy(book_id)
	if copy.is_empty():
		return {"ok": false, "text": "نسخه پیدا نشد."}
	var narrative: String = "%s — %s" % [TITLES[book_id], DESCRIPTIONS[book_id]]
	if OWNERS.has(book_id):
		narrative += "\nاین کتاب امانت مردم است؛ بدون رضایت مالک، مرمت یا فروش نمی‌شود. %s" % CLUES[book_id]
	elif int(copy["condition"]) >= 50:
		narrative += "\nحاشیهٔ قابل خواندن: %s" % CLUES[book_id]
	else:
		narrative += "\nکاغذ هنوز برای دیدن حاشیه سالم نیست؛ می‌توانید مرمتش کنید."
	return {"ok": true, "text": narrative, "condition": int(copy["condition"])}

func repair(book_id: String) -> bool:
	if day_closed or commerce.get_copy(book_id).is_empty():
		return false
	var done: bool = commerce.repair_shop_copy("firstday_repair_" + book_id, book_id, 3)
	if done:
		story_log.append("صحاف به کارگاه همسایه سه سکه می‌پردازد؛ جلد %s دوباره زنده می‌شود." % TITLES[book_id])
	return done

func meet_darya() -> bool:
	if day_closed or darya_met:
		return false
	darya_met = true
	story_log.append("دریا به دفتر اشاره می‌کند: «این دفتر امانت مادرم است. اجازهٔ گشودنش باید از خود خانواده بیاید.»")
	return true

func meet_bahram() -> bool:
	if day_closed or bahram_met or family_choice.is_empty():
		return false
	bahram_met = true
	story_log.append("بهرام، تاجر گذری: «فانوس سوم را برای چه کسی نگه داشته‌اید؟ برای من فقط جلدش مهم نیست.»")
	return true
func choose_family(route: String) -> bool:
	if day_closed or not darya_met or not family_choice.is_empty() or route not in FAMILY_CHOICES:
		return false
	family_choice = route
	match route:
		"consent":
			story_log.append("دریا می‌گوید: «اسم روی جلد مال مادرم است. اجازه‌اش را می‌گیرم.» قرار ملاقات بدون دست‌زدن به سند ثبت شد.")
		"sealed":
			story_log.append("نامه و دفتر مهروموم می‌مانند. دریا قول می‌دهد نخست از وارث واقعی بپرسد.")
		"public_archive":
			story_log.append("به‌جای شکستن شیرازه، از رونوشت عمومیِ دفتر بندر سرنخ می‌گیرید. راز خصوصی دست‌نخورده می‌ماند.")
	return true

func resolve_visitor(sell: bool) -> bool:
	if day_closed or not bahram_met or family_choice.is_empty() or not buyer_choice.is_empty():
		return false
	if sell:
		if not commerce.sell_shop_copy("firstday_lantern_sale", "lantern", "external_trade", 12):
			return false
		buyer_choice = "sold"
		story_log.append("بهرام دفتر فانوس را می‌خرد؛ ۱۲ سکه از تاجر خارجی وارد کتاب‌فروشی می‌شود. چشمش روی مُهر مکث می‌کند.")
	else:
		buyer_choice = "declined"
		story_log.append("بهرام با لبخند می‌گوید: «قیمت هر کتابی را می‌دانم، جز آن‌هایی که هنوز نفروخته‌ای.» معامله به فردا می‌ماند.")
	return true

func fund_lanterns(route: String) -> bool:
	if day_closed or buyer_choice.is_empty() or not funding_choice.is_empty():
		return false
	if route not in FUNDING_CHOICES:
		return false
	if commerce.economy.street_lanterns_state != "fundraising" or int(commerce.economy.wallets["town_escrow"]) != 0:
		return false
	var eco: IslandEconomyLedger = commerce.economy
	# All three paths are priced from explicit wallets; no timed cash regeneration.
	match route:
		"solo":
			if commerce.available_coins() < 14:
				return false
			if not eco.pledge_street_lanterns("firstday_solo_fund", "bookshop", 14):
				return false
			story_log.append("کتاب‌فروشی هزینهٔ تمام چراغ‌ها را به صندوق محله می‌سپارد.")
		"shared":
			if commerce.available_coins() < 10 or int(eco.wallets["weaver"]) < 4:
				return false
			if not eco.pledge_street_lanterns("firstday_shop_fund", "bookshop", 10):
				return false
			if not eco.pledge_street_lanterns("firstday_weaver_fund", "weaver", 4):
				return false
			story_log.append("ده سکه از کتاب‌فروشی و چهار سکه از نساجی ارغوان جمع می‌شود. کوچه، کار مشترک است.")
		"residents":
			if int(eco.wallets["external_trade"]) < 8 or int(eco.wallets["weaver"]) + 8 < 14:
				return false
			if not eco._transfer("firstday_weaver_export", "external_trade", "weaver", 8, "abstract_weaving_export"):
				return false
			if not eco.pledge_street_lanterns("firstday_resident_fund", "weaver", 14):
				return false
			story_log.append("شما پولی کمک نکردید. ارغوان از سفارشِ خارجیِ پارچه درآمد گرفت و خودش ساخت فانوس‌ها را تأمین کرد.")
	if not eco.commission_street_lanterns("firstday_lantern_commission"):
		return false
	funding_choice = route
	story_log.append("صندوق شهر، چهارده سکه به کارگاه محلی دستمزد می‌دهد. فانوس کوچه روشن شد؛ بازسازی واقعاً انجام گرفت.")
	return true

func close_day() -> bool:
	if day_closed or family_choice.is_empty() or buyer_choice.is_empty() or funding_choice.is_empty():
		return false
	if not commerce.economy.street_is_lit():
		return false
	day_closed = true
	story_log.append("صبح فردا، یک مشتری تازه زیر فانوس روشن می‌ایستد. پیرمرد می‌پرسد: «بخش دومِ این قصه را می‌خواهی؟»")
	return true

func snapshot() -> Dictionary:
	return {
		"version": VERSION,
		"commerce": commerce.snapshot(),
		"family_choice": family_choice,
		"darya_met": darya_met,
		"bahram_met": bahram_met,
		"buyer_choice": buyer_choice,
		"funding_choice": funding_choice,
		"day_closed": day_closed,
		"story_log": story_log.duplicate()
	}

static func from_snapshot(data: Dictionary) -> IslandFirstDayController:
	if data.get("version") != VERSION:
		return null
	if typeof(data.get("commerce")) != TYPE_DICTIONARY or typeof(data.get("story_log")) != TYPE_ARRAY:
		return null
	var met_darya: Variant = data.get("darya_met")
	var met_bahram: Variant = data.get("bahram_met")
	var family: Variant = data.get("family_choice")
	var buyer: Variant = data.get("buyer_choice")
	var funding: Variant = data.get("funding_choice")
	var closed: Variant = data.get("day_closed")
	if typeof(family) != TYPE_STRING or (family != "" and family not in FAMILY_CHOICES):
		return null
	if typeof(buyer) != TYPE_STRING or buyer not in ["", "sold", "declined"]:
		return null
	if typeof(funding) != TYPE_STRING or (funding != "" and funding not in FUNDING_CHOICES):
		return null
	if typeof(met_darya) != TYPE_BOOL or typeof(met_bahram) != TYPE_BOOL:
		return null
	if (family != "" and not met_darya) or (buyer != "" and not met_bahram) or (met_bahram and family == ""):
		return null
	if typeof(closed) != TYPE_BOOL:
		return null
	if (buyer != "" and family == "") or (funding != "" and buyer == "") or (closed and funding == ""):
		return null
	var c: IslandWorldCommerceSession = IslandWorldCommerceSession.from_snapshot(data["commerce"])
	if c == null:
		return null
	for book_id in BOOK_IDS:
		var item: Dictionary = c.get_copy(book_id)
		if item.is_empty() or item.get("book_id") != book_id or item.get("owner") != OWNERS.get(book_id, "bookshop"):
			return null
	if (buyer == "sold") != (c.get_copy("lantern").get("location") == "sold"):
		return null
	if (funding != "") != c.economy.street_is_lit():
		return null
	if closed and not c.economy.street_is_lit():
		return null
	var clean_log: Array[String] = []
	for line in data["story_log"]:
		if typeof(line) != TYPE_STRING:
			return null
		clean_log.append(line)
	var result := IslandFirstDayController.new(c, false)
	result.family_choice = family
	result.darya_met = met_darya
	result.bahram_met = met_bahram
	result.buyer_choice = buyer
	result.funding_choice = funding
	result.day_closed = closed
	result.story_log = clean_log
	return result
