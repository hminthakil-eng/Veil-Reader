extends Control
## Hand-drawn low-resolution procedural stage placeholder, NOT final game pixel art.
## The illuminated project is read from the completed ledger state, not escrow cash.
var zone: String = "shop"
var street_lit: bool = false

func show_view(next_zone: String, completed: bool) -> void:
	zone = next_zone
	street_lit = completed
	queue_redraw()

func _ready() -> void:
	custom_minimum_size = Vector2(0, 180)
	mouse_filter = Control.MOUSE_FILTER_IGNORE

func _rect(x: float, y: float, w: float, h: float, c: Color) -> void:
	draw_rect(Rect2(x,y,w,h),c)

func _draw() -> void:
	var unit: float = minf(size.x / 640.0, size.y / 216.0)
	draw_set_transform(Vector2((size.x-640.0*unit)/2.0,(size.y-216.0*unit)/2.0),0.0,Vector2(unit,unit))
	_rect(0,0,640,216,Color("#121b28"))
	if zone == "street":
		_street()
	else:
		_shop()
	draw_set_transform(Vector2.ZERO,0.0,Vector2.ONE)

func _shop() -> void:
	_rect(0,0,640,175,Color("#252737"))
	for x in range(0,640,64):
		_rect(float(x),8,2,164,Color("#303246"))
	_rect(0,175,640,41,Color("#59473b"))
	for i in range(20):
		_rect(float(i*34),191,28,2,Color("#49382e"))
	# tall shelf, books and period lamps
	for shelf_x in [36,400]:
		_rect(shelf_x,25,184,145,Color("#604330"))
		_rect(shelf_x+8,31,168,131,Color("#1f2230"))
		for row in range(3):
			var y: int = 38+row*39
			for col in range(13):
				var x: int = shelf_x+13+col*12
				var hue: Color = [Color("#a88457"),Color("#526d72"),Color("#754d57"),Color("#927351"),Color("#737a68")][(col+row)%5]
				_rect(x,y,8,26-(col%3)*4,hue)
			_rect(shelf_x+5,y+30,174,4,Color("#986f48"))
	_rect(272,110,112,60,Color("#6b4b36"))
	_rect(283,104,90,8,Color("#b08a5f"))
	_rect(290,90,39,12,Color("#b3a181"))
	_rect(337,91,30,13,Color("#e2d1a5"))
	_rect(314,63,8,26,Color("#998668"))
	draw_circle(Vector2(318,58),13,Color("#c1a36e"))
	draw_circle(Vector2(318,58),8,Color("#efd4a0"))
	_rect(229,22,147,59,Color("#413a3c"))
	_rect(236,29,133,45,Color("#6d8592"))
	for k in range(14):
		_rect(238.0+k*10,37,3,24,Color("#8da9ba"))
	_rect(238,57,128,3,Color("#2d424d"))
	_rect(304,28,3,46,Color("#293942"))
	# old bookbinder: tiny sprite-like silhouette
	_rect(351,134,17,29,Color("#34435b"))
	_rect(351,125,16,13,Color("#b9a58d"))
	_rect(354,121,12,7,Color("#a6a5a2"))

func _street() -> void:
	_rect(0,0,640,128,Color("#182839") if not street_lit else Color("#384153"))
	_rect(0,128,640,88,Color("#45464a") if not street_lit else Color("#5c5554"))
	for i in range(16):
		_rect(float(i*41),138+(i%2)*19,31,2,Color("#343d47") if not street_lit else Color("#7d6960"))
	# seaside in the background
	_rect(0,102,640,18,Color("#315064") if not street_lit else Color("#426c7b"))
	for wave in range(20):
		_rect(float(wave*34),111+(wave%2)*4,18,1,Color("#71919b"))
	# bookstore: left
	_rect(27,62,260,80,Color("#413640") if not street_lit else Color("#77554a"))
	_rect(18,48,276,20,Color("#292738"))
	_rect(37,74,75,58,Color("#1f2634"))
	_rect(51,83,52,44,Color("#76858b") if not street_lit else Color("#d2a977"))
	_rect(124,76,70,66,Color("#534235"))
	_rect(202,74,72,58,Color("#253a40") if not street_lit else Color("#ad8869"))
	_rect(33,38,230,8,Color("#ba936b"))
	# weaver independent workshop: right
	_rect(373,70,235,72,Color("#4f3a3f") if not street_lit else Color("#777054"))
	_rect(366,57,250,19,Color("#323441"))
	_rect(388,82,65,48,Color("#2c343b") if not street_lit else Color("#9f9a72"))
	_rect(472,82,116,50,Color("#202f36"))
	for i in range(6):
		_rect(480+i*16,89,6,32,Color("#40515d") if not street_lit else Color("#b29b75"))
	# worn cobblestones and public street lantern
	_rect(323,60,5,112,Color("#62656d"))
	_rect(311,55,28,8,Color("#7c6970"))
	if street_lit:
		draw_circle(Vector2(325,66),51,Color(1.0,0.69,0.31,0.19))
		draw_circle(Vector2(325,66),22,Color(1.0,0.77,0.42,0.43))
		_rect(316,56,18,14,Color("#fff0b2"))
		_rect(385,134,13,28,Color("#8c7b63"))
		_rect(388,128,12,7,Color("#c2a98a"))
		_rect(413,137,14,25,Color("#4d6871"))
		_rect(413,129,14,10,Color("#bf9c82"))
		for i in range(8):
			_rect(float(72+i*53),177+(i%3)*9,18,2,Color("#9d8770"))
	else:
		_rect(316,56,18,14,Color("#3c3638"))
		_rect(49,84,56,4,Color("#413c3e"))
		_rect(202,78,71,5,Color("#413c3e"))
