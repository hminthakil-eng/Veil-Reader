extends Control
## Interactive business-loop proof, intentionally no finished art or Reader bridge.
var ledger: IslandShopLedger = IslandShopLedger.new()
var book: IslandBookDefinition = IslandBookDefinition.new(&"book_salt_ledger", "Salt-stained blue cover", "A quiet account of lost harbor lights.", "The margin carries tomorrow's tide mark.", 12)
var narrative: Label
var action_count: int = 0

func _ready() -> void:
	ledger.add_copy("copy_salt_1", "book_salt_ledger", 30)
	var column := VBoxContainer.new()
	column.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	column.add_theme_constant_override("separation", 12)
	add_child(column)
	var heading := Label.new()
	heading.text = "ISLAND BOOKSHOP / Shop-day greybox"
	column.add_child(heading)
	narrative = Label.new()
	narrative.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	column.add_child(narrative)
	_add_action(column, "Inspect fictional book", _inspect)
	_add_action(column, "Restore water-damaged book (3 coins)", _repair)
	_add_action(column, "Recommend and sell to harbor keeper (12 coins)", _sell)
	_refresh("The elder offers you shelter above his old bookshop. First customer asks about the lost harbor lights.")

func _add_action(parent: VBoxContainer, title: String, callback: Callable) -> void:
	var button := Button.new()
	button.text = title
	button.pressed.connect(callback)
	parent.add_child(button)

func _inspect() -> void:
	var state: Dictionary = ledger.copies["copy_salt_1"]
	var visible: Dictionary = book.visible_layers(int(state["condition"]) >= 50)
	_refresh("Cover: %s | Summary: %s | Hidden clue: %s" % [visible["cover"], visible["blurb"], visible["marginalia"]])

func _repair() -> void:
	var success := ledger.repair("repair_salt_once", "copy_salt_1", 3)
	_refresh("Restoration completed; a clue emerges." if success else "Cannot repeat restoration.")

func _sell() -> void:
	var success := ledger.sell("sell_salt_once", "copy_salt_1", "lighthouse_keeper", 12)
	_refresh("Customer purchased the book; first story flag would unlock." if success else "The book must first be repaired, or is already sold.")

func _refresh(message: String) -> void:
	narrative.text = "%s\nCoins: %s | Book state: %s | Condition: %s" % [message, ledger.coins, ledger.copies["copy_salt_1"]["location"], ledger.copies["copy_salt_1"]["condition"]]
