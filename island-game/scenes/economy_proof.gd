extends Control
## Standalone financial UI proof. Deliberately separate from IslandShopLedger.
var economy: IslandEconomyLedger = IslandEconomyLedger.new()
var town_report: Label
var last_event: String = "A boarded street awaits work. Its bookseller and weaver still have choices."

func _ready() -> void:
	var content := VBoxContainer.new()
	content.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	content.add_theme_constant_override("separation", 8)
	add_child(content)
	var heading := Label.new()
	heading.text = "ISLAND BOOKSHOP / Neighborhood recovery financial proof"
	content.add_child(heading)
	town_report = Label.new()
	town_report.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	content.add_child(town_report)
	_add_action(content, "1 / Offshore buyer pays 12 for a book order", _receive)
	_add_action(content, "2 / Commission the local weaver (8 coins)", _contract)
	_add_action(content, "3 / Contribute 10 coins toward street lanterns", _pledge)
	_add_action(content, "4 / Weaver voluntarily contributes 4 coins", _guild)
	_add_action(content, "5 / City pays weaver to finish lantern shades", _build)
	_show()

func _add_action(parent: VBoxContainer, label: String, action: Callable) -> void:
	var control := Button.new()
	control.text = label
	control.pressed.connect(action)
	parent.add_child(control)

func _receive() -> void:
	_attempt(economy.receive_off_island_order("offshore_01", 12), "First overseas order reaches the shop. The outside buyer pays for it.")

func _contract() -> void:
	_attempt(economy.purchase_local_weaving("cloth_01", 8), "Your order gives an independent craftsperson work and income.")

func _pledge() -> void:
	_attempt(economy.pledge_street_lanterns("bookshop_gift_01", "bookshop", 10), "The bookshop contributes to neighborhood recovery.")

func _guild() -> void:
	_attempt(economy.pledge_street_lanterns("weaver_gift_01", "weaver", 4), "The weaver decides to co-fund the neighborhood.")

func _build() -> void:
	_attempt(economy.commission_street_lanterns("street_works_01"), "The city pays for local work. The first street lamps are restored.")

func _attempt(ok: bool, description: String) -> void:
	last_event = description if ok else "Action unavailable or already completed. Coins and town state stay unchanged."
	_show()

func _show() -> void:
	var signals: Dictionary = economy.read_only_town_signals()
	var scene_text := "ILLUMINATED STREET / neighbors are at work" if economy.street_is_lit() else "BOARDED STREET / old lamps are dark"
	town_report.text = "%s\n\n%s\n\nBookshop coins: %d | Weaver coins: %d\nCivic escrow: %d | Outside trade reserve: %d\n\nMarket turnover: %d | Participating guilds: %d\nCivic lights: %s | Conserved total: %d coins\n\nIndependent ledger proof: NOT the real ShopLedger. No paper/cloth simulation or Reader access." % [
		scene_text, last_event,
		int(economy.wallets["bookshop"]), int(economy.wallets["weaver"]),
		int(economy.wallets["town_escrow"]), int(economy.wallets["external_trade"]),
		int(signals["market_turnover"]), int(signals["community_participants"]),
		economy.street_lanterns_state, economy.total_coins()
	]
