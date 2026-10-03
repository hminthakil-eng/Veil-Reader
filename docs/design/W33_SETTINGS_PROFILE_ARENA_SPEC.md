# W33 — Settings + Profile Arena Spec

Date: 2026-09-30
Canonical branch: `alpha/w33-settings-profile-rebuild-v1`
Parent: `alpha/w32-collections-search-rebuild-v1`

## Settings role

Settings is calibration, not worldbuilding. It must remain the most literal, predictable and
reversible shell outside the Reader.

### Laws
- Group by consequence: App / Reading / Sensory / Backup & Data / About.
- One section hierarchy, no nested card wall.
- Choices are flat registers with clear selected state.
- Switches remain familiar platform controls.
- Reader-specific deep tuning belongs in Reader Appearance; global defaults may remain here.
- Destructive/restore actions require explicit confirmation.
- Backup/export/import remain plainly named.

## Profile role

Profile is the reader's private dossier and reading record, not a social profile.

### Laws
- Identity / path / level and durable reading history first.
- Mastery axes read as a ledger, not a dashboard card.
- Discoveries are archival records, not collectible-store tiles.
- Hidden Archive remains directly reachable.
- No followers, sharing metrics, public status or engagement mechanics.

## W33 implementation slice

1. Remove card shell from Settings sections.
2. Convert choice controls into flat calibration registers.
3. Preserve familiar switches/sliders and all persistence callbacks.
4. Flatten mastery panel into dossier ledger language.
5. Flatten discoveries into archival record rows.
6. Preserve all existing path/mastery/history derivation.

## Rejection conditions

Reject if:
- settings becomes more mysterious than readable;
- labels are replaced by lore terms;
- global settings duplicate Reader live controls unnecessarily;
- profile feels like a social/gacha dashboard;
- any backup/restore safety behavior changes.
