extends Control
## Playable no-art shop-management proof: two customers, six fictional books.
const Ledger = preload("res://core/shop_ledger.gd")
const SaveStore = preload("res://core/shop_save.gd")
var ledger = Ledger.new()
var save_store = SaveStore.new()
var selection: String = "lantern"
var status_label: Label
var customer_label: Label
var detail_label: Label
var stock_list: VBoxContainer
var nonce: int = 0

func _ready() -> void:
    var restored: Dictionary = save_store.load_state()
    if not restored.is_empty():
        ledger.restore(restored)
    _layout()
    _refresh()

func _layout() -> void:
    var backdrop := ColorRect.new()
    backdrop.color = Color("#17212b")
    backdrop.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    add_child(backdrop)
    var shell := MarginContainer.new()
    shell.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    shell.add_theme_constant_override("margin_left", 24)
    shell.add_theme_constant_override("margin_top", 20)
    shell.add_theme_constant_override("margin_right", 24)
    shell.add_theme_constant_override("margin_bottom", 20)
    add_child(shell)
    var base := VBoxContainer.new()
    base.add_theme_constant_override("separation", 10)
    shell.add_child(base)
    var header := Label.new()
    header.text = "ISLAND BOOKSHOP  /  GODOT GREYBOX  /  ORIGINAL FICTION ONLY"
    header.add_theme_font_size_override("font_size", 23)
    base.add_child(header)
    var subhead := Label.new()
    subhead.text = "Test the loop before commissioning art. Choose a book, inspect 3 layers, recommend, sell or lend."
    base.add_child(subhead)
    customer_label = Label.new()
    customer_label.add_theme_font_size_override("font_size", 19)
    base.add_child(customer_label)
    var contents := HBoxContainer.new()
    contents.size_flags_vertical = Control.SIZE_EXPAND_FILL
    contents.add_theme_constant_override("separation", 24)
    base.add_child(contents)
    var shelf := VBoxContainer.new()
    shelf.custom_minimum_size.x = 290
    contents.add_child(shelf)
    var shelf_title := Label.new()
    shelf_title.text = "BOOKS IN STOCK (click one)"
    shelf.add_child(shelf_title)
    stock_list = VBoxContainer.new()
    shelf.add_child(stock_list)
    var right := VBoxContainer.new()
    right.size_flags_horizontal = Control.SIZE_EXPAND_FILL
    contents.add_child(right)
    detail_label = Label.new()
    detail_label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    detail_label.custom_minimum_size.y = 125
    right.add_child(detail_label)
    var actions := HBoxContainer.new()
    right.add_child(actions)
    for action in ["sell", "lend", "repair"]:
        var button := Button.new()
        button.text = action.capitalize()
        button.pressed.connect(_perform.bind(action))
        actions.add_child(button)
    var spacer := Control.new()
    spacer.custom_minimum_size.y = 14
    right.add_child(spacer)
    status_label = Label.new()
    status_label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    right.add_child(status_label)
    var footer := HBoxContainer.new()
    base.add_child(footer)
    for action in ["Save", "Reload", "Reset"]:
        var button := Button.new()
        button.text = action
        button.pressed.connect(_utility.bind(action))
        footer.add_child(button)

func _refresh() -> void:
    var c: Dictionary = ledger.customer()
    customer_label.text = "Coins %d | Trust %d | %s" % [ledger.state["coins"], ledger.state["trust"],
        "Shift complete" if c.is_empty() else (str(c["id"]) + ": " + str(c["request"]) + " (budget " + str(c["budget"]) + ")")]
    for child in stock_list.get_children():
        stock_list.remove_child(child)
        child.queue_free()
    for book in ledger.state["stock"]:
        var button := Button.new()
        button.text = ("%s %s | %d coins" % [
            str(book["title"]), "(unavailable)" if book["sold"] or book["loaned"] else "",
            int(book["price"])])
        button.disabled = bool(book["sold"]) or bool(book["loaned"])
        button.pressed.connect(_choose.bind(str(book["uid"])))
        stock_list.add_child(button)
    var selected: Dictionary = ledger.inspect(selection)
    detail_label.text = "INSPECT: %s\n%s\nCLUE: %s" % [
        str(selected.get("cover", "")), str(selected.get("blurb","")), str(selected.get("clue",""))]

func _choose(uid: String) -> void:
    selection = uid
    _refresh()

func _perform(action: String) -> void:
    nonce += 1
    var tx_id := "%d:%d:%s" % [Time.get_unix_time_from_system(), Time.get_ticks_usec() + nonce, action]
    var outcome: Dictionary = ledger.transact(tx_id, action, selection)
    status_label.text = str(outcome["reason"])
    if outcome["ok"]:
        if not save_store.save_state(ledger.snapshot()):
            status_label.text += "\nWARNING: Save failed; do not consider the transaction durable."
    _refresh()

func _utility(action: String) -> void:
    if action == "Save":
        status_label.text = "Saved successfully." if save_store.save_state(ledger.snapshot()) else "Save FAILED."
    elif action == "Reload":
        var result: Dictionary = save_store.load_state()
        status_label.text = "Reloaded." if ledger.restore(result) else "No valid save."
    elif action == "Reset":
        ledger.reset()
        selection = "lantern"
        status_label.text = "New sandbox game; click Save to persist."
    _refresh()
