class_name IslandVirtualClock
extends RefCounted
## No real-world countdown, elder illness penalty or wall-clock dependency.

enum Phase { MORNING, NOON, EVENING, NIGHT }
var day: int = 1
var phase: Phase = Phase.MORNING
var minutes_into_phase: int = 0
var paused: bool = false

func advance_action(minutes: int) -> bool:
	if paused or minutes <= 0 or minutes > 180:
		return false
	minutes_into_phase += minutes
	while minutes_into_phase >= 180:
		minutes_into_phase -= 180
		if phase == Phase.NIGHT:
			phase = Phase.MORNING
			day += 1
		else:
			phase = (int(phase) + 1) as Phase
	return true

func npc_location(npc_id: String, sample_day: int, sample_phase: int) -> String:
	# Deterministic even if the NPC scene is not loaded.
	if sample_day < 1 or sample_phase < 0 or sample_phase > 3:
		return "unknown"
	if npc_id == "elder":
		return "bookshop" if sample_phase in [Phase.MORNING, Phase.EVENING] else "loft"
	if npc_id == "lighthouse_keeper":
		return "pier" if sample_phase in [Phase.MORNING, Phase.NOON] else "tea_house"
	return "unknown"
