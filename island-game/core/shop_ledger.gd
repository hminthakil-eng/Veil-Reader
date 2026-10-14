extends RefCounted
## Fictional shop books only. No private reader filenames, titles, URIs or locators.
const SCHEMA_VERSION := 1
const CUSTOMERS := [
    {"id":"mara", "request":"I want a hopeful journey", "genre":"journey", "tone":"hopeful", "budget":15},
    {"id":"keeper", "request":"A scholarly book about the sea", "genre":"maritime", "tone":"scholarly", "budget":22}
]
const CATALOG := [
    {"uid":"lantern", "title":"Paper Lantern", "genre":"journey", "tone":"hopeful", "condition":90, "price":11, "blurb":"A traveler follows a light home.", "clue":"A ticket stamped Pier Three."},
    {"uid":"tide", "title":"Tide and Ink", "genre":"maritime", "tone":"scholarly", "condition":100, "price":18, "blurb":"An atlas of ancient currents.", "clue":"A handwritten lighthouse route."},
    {"uid":"herbs", "title":"Moth Gardens", "genre":"botany", "tone":"hopeful", "condition":95, "price":9, "blurb":"Drawings of unfamiliar herbs.", "clue":"A preserved silver leaf."},
    {"uid":"salt", "title":"Salt Ledger", "genre":"maritime", "tone":"melancholy", "condition":65, "price":12, "blurb":"Accounts kept by harbor workers.", "clue":"One ship has no captain's signature."},
    {"uid":"embers", "title":"House of Embers", "genre":"journey", "tone":"melancholy", "condition":100, "price":13, "blurb":"A tale of returning home.", "clue":"A folded, never-sent letter."},
    {"uid":"sunken", "title":"The Sunken Almanac", "genre":"mystery", "tone":"ominous", "condition":35, "price":20, "blurb":"The water erased most pages.", "clue":"The drawing matches your wrecked ship."}
]
var state: Dictionary = {}

func _init() -> void:
    reset()

func reset() -> void:
    var stock: Array[Dictionary] = []
    for item in CATALOG:
        var entry: Dictionary = item.duplicate(true)
        entry["sold"] = false
        entry["loaned"] = false
        entry["clue_revealed"] = false
        stock.append(entry)
    state = {"schema_version":SCHEMA_VERSION,"coins":30,"day":1,"customer_index":0,"stock":stock,"event_ids":[],"trust":0}

func customer() -> Dictionary:
    var index: int = int(state["customer_index"])
    return CUSTOMERS[index].duplicate(true) if index < CUSTOMERS.size() else {}

func book(uid: String) -> Dictionary:
    for item in state["stock"]:
        if item["uid"] == uid:
            return item
    return {}

func inspect(uid: String) -> Dictionary:
    var item: Dictionary = book(uid)
    if item.is_empty():
        return {}
    return {"cover":str(item["title"])+" ("+str(item["genre"])+")",
        "blurb":item["blurb"],
        "clue":item["clue"] if item["clue_revealed"] else "Still hidden inside its damaged binding."}

func transact(event_id: String, action: String, uid: String) -> Dictionary:
    if event_id.is_empty() or state["event_ids"].has(event_id):
        return {"ok":false,"reason":"Duplicate or missing transaction"}
    var item: Dictionary = book(uid)
    if item.is_empty() or item["sold"] or item["loaned"]:
        return {"ok":false,"reason":"Copy unavailable"}
    if action == "repair":
        if item["condition"] >= 70 or state["coins"] < 3:
            return {"ok":false,"reason":"No repair needed, or not enough coins"}
        item["condition"] = mini(100, int(item["condition"])+50)
        item["clue_revealed"] = true
        state["coins"] -= 3
        state["event_ids"].append(event_id)
        return {"ok":true,"reason":"Book repaired; a hidden clue is now visible"}
    var buyer: Dictionary = customer()
    if buyer.is_empty():
        return {"ok":false,"reason":"No customers left this shift"}
    if item["condition"] < 50:
        return {"ok":false,"reason":"Too damaged: repair the copy first"}
    if item["genre"] != buyer["genre"] or item["tone"] != buyer["tone"]:
        return {"ok":false,"reason":"The book does not meet this customer's needs"}
    if action == "sell":
        if item["price"] > buyer["budget"]:
            return {"ok":false,"reason":"Price exceeds the customer's budget"}
        item["sold"] = true
        state["coins"] += item["price"]
    elif action == "lend":
        item["loaned"] = true
        state["trust"] += 2
    else:
        return {"ok":false,"reason":"Unknown action"}
    state["customer_index"] += 1
    state["event_ids"].append(event_id)
    return {"ok":true,"reason":"Customer served; your choice matters"}

func snapshot() -> Dictionary:
    return state.duplicate(true)

func restore(value: Dictionary) -> bool:
    if int(value.get("schema_version",-1)) != SCHEMA_VERSION or not value.get("stock") is Array:
        return false
    if not value.get("event_ids") is Array:
        return false
    var coins: int = int(value.get("coins",-1))
    var index: int = int(value.get("customer_index",-1))
    if coins < 0 or index < 0 or index > CUSTOMERS.size():
        return false
    if value["stock"].size() != CATALOG.size():
        return false
    var seen: Dictionary = {}
    for item in value["stock"]:
        if not item is Dictionary or not item.has("uid") or not item.has("condition"):
            return false
        if seen.has(item["uid"]) or int(item["condition"]) < 0 or int(item["condition"]) > 100:
            return false
        if not item.has("sold") or not item.has("loaned") or (item["sold"] and item["loaned"]):
            return false
        seen[item["uid"]] = true
    for template in CATALOG:
        if not seen.has(template["uid"]):
            return false
    state = value.duplicate(true)
    state["coins"] = coins
    state["customer_index"] = index
    return true
