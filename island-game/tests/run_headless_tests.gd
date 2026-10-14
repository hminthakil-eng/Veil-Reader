extends SceneTree
const Ledger = preload("res://core/shop_ledger.gd")
const Saver = preload("res://core/shop_save.gd")
var passed := 0
var failed := 0

func _initialize() -> void:
    call_deferred("_run")

func test(ok: bool, title: String) -> void:
    if ok:
        passed += 1
        print("PASS: ", title)
    else:
        failed += 1
        printerr("FAIL: ", title)

func _run() -> void:
    var l = Ledger.new()
    test(l.state["stock"].size() == 6, "six imaginary book definitions")
    test(l.customer()["id"] == "mara", "deterministic starting customer")
    test(l.inspect("sunken")["clue"].begins_with("Still hidden"), "mystery clue initially obscured")
    test(not l.transact("wrong","sell","tide")["ok"] and l.state["coins"] == 30, "wrong suggestion non-destructive")
    test(l.transact("sale1","sell","lantern")["ok"], "matching sale completes")
    test(l.state["coins"] == 41 and l.state["customer_index"] == 1, "coins and customer index updated")
    test(not l.transact("sale1","sell","lantern")["ok"], "duplicate transaction id rejected")
    test(not l.transact("sale2","sell","lantern")["ok"], "sold physical copy unavailable")
    test(l.transact("repair1","repair","sunken")["ok"], "damaged book repair")
    test(l.inspect("sunken")["clue"].find("ship") != -1, "physical clue revealed after restoration")
    test(not l.transact("repair1","repair","sunken")["ok"], "repair transaction cannot replay")
    test(l.transact("loan1","lend","tide")["ok"], "loan matches second customer")
    test(l.state["trust"] == 2 and l.state["coins"] == 38, "loan changes trust not sale money")
    test(not l.transact("sell_loan","sell","tide")["ok"], "loaned copy may not be sold")
    var again = Ledger.new()
    test(again.restore(l.snapshot()) and again.state["coins"] == 38, "in-memory snapshot restore")
    var corrupt := l.snapshot()
    corrupt["customer_index"] = 900
    test(not again.restore(corrupt) and again.state["coins"] == 38, "invalid state fails closed")
    var save = Saver.new()
    test(save.save_state(l.snapshot()), "save to versioned user JSON")
    var copy = Ledger.new()
    test(copy.restore(save.load_state()) and copy.state["coins"] == 38, "reload disk state")
    test(not copy.transact("sale1","sell","lantern")["ok"], "idempotence survives serialized reload")
    print("TESTS: %d passed, %d failed" % [passed,failed])
    quit(1 if failed > 0 else 0)
