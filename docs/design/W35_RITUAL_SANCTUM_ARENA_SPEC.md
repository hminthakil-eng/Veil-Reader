# W35 — Ritual + Sanctum Arena Spec

Date: 2026-09-30
Canonical branch: `alpha/w35-ritual-sanctum-rebuild-v1`
Parent: `alpha/w34-castle-path-rebuild-v1`

## Principle

Ritual is a rare transition. Sanctum is durable memory of long-term reading. Neither is a store,
inventory economy or reward vending machine.

## Ritual

- Advancement ceremony may be theatrical because it is intentionally rare.
- Confirmation remains explicit and reversible until the final action.
- Reduced motion must collapse ceremony motion without hiding information.
- The ritual visual does not leak into ordinary reading/navigation controls.

## Treasury / Sanctum

- Sigils are durable earned records that may be displayed/equipped.
- Relics are museum records tied to reading milestones, not loot drops.
- Bookplates are archival inscriptions.
- Locked objects show meaningful clues without countdowns or artificial scarcity.
- Rarity is descriptive hierarchy only; it has no price, currency or randomized acquisition.

## Hierarchy

1. Current displayed identity / seal.
2. Earned sigils.
3. Durable relic record.
4. Bookplates.
5. Sovereign/Sanctum gate.
6. Permanent title identity.

## W35 implementation slice

1. Demote sigil equip action from primary gold button.
2. Replace two-column relic inventory grid with readable museum records.
3. Preserve rarity, clue and awakening rules.
4. Keep bookplates as physical archival material.
5. Flatten permanent-title selection in Sanctum.
6. Preserve ceremony, equip, title and unlock callbacks.

## Rejection conditions

Reject if:
- relics resemble loot boxes, store inventory or gacha rarity;
- locked records create urgency or countdown pressure;
- equip becomes more prominent than the relic meaning;
- ceremony occurs during routine navigation;
- long-term achievements can be bought or bypassed.
