# W35 — Ritual + Sanctum Source QA

Date: 2026-09-30
Branch: `alpha/w35-ritual-sanctum-rebuild-v1`
Parent: `alpha/w34-castle-path-rebuild-v1`

## Canonical drift

- W35 is ahead of W34 and zero commits behind.
- Relic, sigil, bookplate, title and advancement rules remain unchanged.

## Treasury

- Earned sigils remain durable records.
- Equip/display action is secondary text action rather than a dominant gold button.
- Reading relics are vertical museum records rather than inventory tiles.
- Rarity remains descriptive only.
- Locked relics show clues without timers, currency or scarcity pressure.
- Bookplates remain archival material objects.

## Sanctum

- Sovereign gate calculations remain unchanged.
- Permanent-title selection is presented as identity records rather than shopping options.
- Active identity is clearly registered.
- Hidden sanctum record remains intact.

## Ritual

- Advancement ceremony remains rare and explicit.
- Existing confirmation callback and reduced-motion behavior remain intact.
- No ritual styling leaked into ordinary navigation.

## Exit status

Durable-achievement hierarchy: PASS.
Non-commercial/non-coercive progression gate: PASS.
Canonical-line gate: PASS.
Runtime/build gate: pending an executing CI runner.
