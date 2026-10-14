class_name IslandBookDefinition
extends RefCounted
## Authored fictional book only. NEVER reference personal EPUB or PDF paths.

var id: StringName
var cover_observation: String
var lore_blurb: String
var hidden_marginalia: String
var base_price: int

func _init(book_id: StringName, cover: String, blurb: String, clue: String, price: int) -> void:
	id = book_id
	cover_observation = cover
	lore_blurb = blurb
	hidden_marginalia = clue
	base_price = maxi(1, price)

func visible_layers(repaired: bool) -> Dictionary:
	return {
		"cover": cover_observation,
		"blurb": lore_blurb,
		"marginalia": hidden_marginalia if repaired else "[Unrestored pages conceal a clue]"
	}
