# W33 — Settings + Profile Source QA

Date: 2026-09-30
Branch: `alpha/w33-settings-profile-rebuild-v1`
Parent: `alpha/w32-collections-search-rebuild-v1`

## Canonical drift

- W33 is ahead of W32 and zero commits behind.
- No settings persistence, backup, restore, profile mastery or history derivation fork introduced.

## Settings

- Sections are flat calibration ledgers rather than nested cards.
- Choice controls are flat selected registers with radio semantics and >=48dp targets.
- Existing sliders, switches and callbacks are preserved.
- Backup/restore confirmation behavior remains intact.
- Global Reader defaults still use the same canonical ReaderAppearance model.

## Profile

- Existing dossier identity remains.
- Mastery panel is a ledger record rather than a dashboard card.
- Discovery records are archival rows rather than collectible-store tiles.
- Hidden Archive entry remains available.
- Reading history/path/mastery derivation remains unchanged.

## Exit status

Settings hierarchy gate: PASS.
Profile dossier gate: PASS.
Canonical-line gate: PASS.
Runtime/build gate: pending an executing CI runner.
