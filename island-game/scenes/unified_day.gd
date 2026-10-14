extends Control
## Stage-one bookshop+city economic proof, no art or actual external book file.
var game: IslandUnifiedShop = IslandUnifiedShop.new()
var status: Label
var log_message: String = "The bookshop and weaver both need work."

func _ready() -> void:
	game.register_copy("copy_salt_001", "fiction_salt_ledger", 30)
	var content := VBoxContainer.new()
	content.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	content.add_theme_constant_override("separation", 10)
	add_child(content)
	var heading := Label.new()
	heading.text = "ISLAND BOOKSHOP / SINGLE MONEY AUTHORITY GREYBOX"
	content.add_child(heading)
	status = Label.new()
	status.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	content.add_child(status)
	_add_button(content, "Inspect damaged fiction book", _inspect)
	_add_button(content, "Restore binding / pay local weaver 3 coins", _repair)
	_add_button(content, "Sell repaired book to outside buyer / 12 coins", _sell)
	_add_button(content, "Bookshop pledges 10 to street lights", _pledge)
	_add_button(content, "Weaver guild pledges 4", _guild)
	_add_button(content, "City pays weaver to finish street lanterns / 14", _commission)
	_redraw()

func _add_button(parent: VBoxContainer, caption: String, callback: Callable) -> void:
	var button := Button.new()
	button.text = caption
	button.pressed.connect(callback)
	parent.add_child(button)

func _inspect() -> void:
	var item: Dictionary = game.get_copy("copy_salt_001")
	log_message = "Water-damaged fictional Salt Ledger. Condition %s, stock status %s. Clue becomes visible after careful repair." % [item["condition"], item["location"]]
	_redraw()

func _repair() -> void:
	_apply(game.repair_copy("repair_one", "copy_salt_001", 3), "A weaver received repair materials payment. The cover reveals a clue.")

func _sell() -> void:
	_apply(game.sell_copy_to_off_island_buyer("offshore_sale_one", "copy_salt_001", 12), "The overseas buyer paid from a limited wallet; this particular copy is now sold.")

func _pledge() -> void:
	_apply(game.pledge_lanterns("bookshop_pledge_one", "bookshop", 10), "The bookshop contributes to restoring the street.")

func _guild() -> void:
	_apply(game.pledge_lanterns("weaver_pledge_one", "weaver", 4), "The independently owned guild contributes without player ownership.")

func _commission() -> void:
	_apply(game.commission_lanterns("street_lamps_install_once"), "Town escrow paid the weaver. The street lanterns are now restored!")

func _apply(ok: bool, success_text: String) -> void:
	log_message = success_text if ok else "Unable to repeat or afford this action; both book and money remain unchanged."
	_redraw()

func _redraw() -> void:
	var copy: Dictionary = game.get_copy("copy_salt_001")
	var light: String = "RESTORED" if game.street_is_lit() else "BROKEN / DARK"
	status.text = "%s\n\nCopy: %s | Condition: %d\nBookshop: %d | Weaver: %d | Outside buyer: %d | Civic: %d\nTotal: %d conserved coins | Street lantern: %s\n\nNOTE: one ledger in this scene only. All book names & trade orders are FICTIONAL. Legacy shop scene kept isolated." % [
		log_message, copy["location"], int(copy["condition"]),
		game.wallet_balance("bookshop"), game.wallet_balance("weaver"),
		game.wallet_balance("external_trade"), game.wallet_balance("town_escrow"),
		game.total_coins(), light
	]
