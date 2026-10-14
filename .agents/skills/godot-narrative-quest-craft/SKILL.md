---
name: godot-narrative-quest-craft
description: Design and review layered anime visual novel dialogue over a Godot pixel RPG, meaningful branching microquests, NPC personalities, schedules, story flags, bilingual localization, accessibility and quest/save tests.
---
# Narrative Quest and Portrait Craft

## World promise
Rich walkable pixel scenes in a seafront town; high-resolution anime dialogue portrait overlay with subtle blink/breath/expression/pose swap, not full animation. Each NPC must have wants, daily schedules, relationship changes, contradictory secrets and agency.

## Designing an NPC
Define immutable ID, role, public goal, private dilemma, physical location/schedule, 3 relationship states, 2 expressive portrait poses, 4 facial moods, localized voice, one ordinary life interaction, two quest hooks, consequence of player neglect, and a scene-trigger condition.

## Designing a quest
Before writing dialogue define: player fantasy, offer conditions, meaningful choice(s), item/economy/relationship costs, success/failure and graceful expiration, saved branch flag, NPC reaction and ripple effect. Return 3 variants: character-driven, place-driven and merchant-driven. No mandatory combat/reading; alternate resolution where meaningful.

## State machine and acceptance
Use typed states `UNDISCOVERED, OFFERED, ACTIVE, RESOLVED, EXPIRED`; guard legal transitions. Unique quest IDs; idempotent reward; no references to missing characters, locations or items; optional quests can't softlock progression; no spoilers from real books. Per quest: automated path tests for 2 alternatives, reload after each choice, reentry on next day, translation parity.

## Writing/animation dialogue
A 2D anime portrait is composited at higher resolution than the pixel world (not nearest-neighbor scaled). Expression changes with dialogue beat; blinking state is random but bounded and never occurs during rapid emotional pose transitions. Offer tap-to-complete, skip, history/backlog, optional auto text, per-language typography, TalkBack/readable contrast and 200% font settings. Support Persian RTL without mirroring artwork or camera direction.

## Scene owner rules
Quest and dialogue reference game state via declared ports, never directly rewrite economic or save internals from arbitrary dialogue scripts. Side effects logged as typed events, validated at a transaction boundary.

## Review criteria
For each 5-minute scene check: do NPCs have distinct dialogue voices? Does at least one player action change the world? Is there a non-obvious but foreshadowed clue? Is the scene replayable? Is the art original with license provenance? Can the user quit/reload mid-conversation safely?

## Research
Study Fields of Mistria, Eastward, Coffee Talk, VA-11 Hall-A and To the Moon through permitted sources. Implement original mechanics, characters, text, art and stories.
