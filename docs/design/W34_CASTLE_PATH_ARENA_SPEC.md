# W34 — Castle + Path Arena Spec

Date: 2026-09-30
Canonical branch: `alpha/w34-castle-path-rebuild-v1`
Parent: `alpha/w33-settings-profile-rebuild-v1`

## Principle

Reading changes the world. The world never pressures the user to read for currency.

Castle and Path may be the richest realms, but progression must describe durable reading activity
rather than become a generic XP dashboard.

## Castle hierarchy

1. Castle identity.
2. Keep / current state.
3. Living world map and chambers.
4. World progression.
5. Memory / mutation / ritual aftermath records.
6. Growth note.

The map must appear before diagnostic ledgers.

## Path hierarchy

1. Current path identity.
2. Rank constellation / ascent.
3. Mastery and ritual readiness.
4. Advancement ceremony.
5. Alternate paths (only changeable where existing rules allow).

The user should see the journey before seeing the gate.

## Progression laws

- Existing mastery axes remain more meaningful than raw XP.
- XP may remain as a supporting record, never the dominant call to action.
- Advancement continues to use `GamificationEngine.canAdvanceRank`.
- No paywall, energy timer, streak punishment, loot box, currency shop or coercive daily loop.
- Reading, annotations, preserved memory and durable milestones remain the inputs.
- Locked chambers communicate future structure without fake urgency.

## Visual laws

- Castle can carry architectural density.
- Path can carry ritual geometry and constellation structure.
- Diagnostic inscriptions are flat records, not repeated cards.
- Advancement is exceptional; ordinary navigation is not styled as ritual.
- Reduced motion remains authoritative.

## W34 implementation slice

1. Move Castle world map directly after Keep.
2. Move diagnostic/mutation records below the map.
3. Put Path rank constellation before ritual readiness.
4. Flatten Castle mutation/aftermath record cards.
5. Preserve every progression calculation and route callback.

## Rejection conditions

Reject if:
- XP becomes the primary meaning of reading;
- a map is visually secondary to dashboard cards;
- progression introduces pressure mechanics;
- advancement rules change accidentally;
- decorative motion ignores reduced-motion preference.
