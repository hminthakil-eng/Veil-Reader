# W30 — Reader Appearance + PDF Workspace Source QA

Date: 2026-09-30
Branch: `alpha/w30-reader-appearance-pdf-workspace-v1`
Parent: `alpha/w29-sanctuary-reader-rebuild-v1`

## Canonical drift

- W30 is ahead of W29 and zero commits behind.
- No Reader engine or persistence fork introduced.

## EPUB Appearance

- Quick / Advanced selector is a flat instrument rail.
- Live preview remains draft-driven.
- Preview reads from canonical `ReaderAppearance.navigationMode`.
- Reusable appearance choices preserve radio-selection semantics and >=48dp targets.
- Paper / Slide / Paged / Scroll remain a single mutually exclusive selector.
- Capability gating for fixed layout, RTL, CJK, columns and typography remains intact.

## PDF Workspace

- PDF exposes only Page / Continuous layout, zoom, fit width and brightness.
- PDF layout writes canonical PAGED / SCROLL navigation state.
- No Paper Curl or Slide control appears in PDF workspace.
- Zoom +/-/reset and fit-width are secondary instrument actions.
- Return to reading is the only parchment action.

## Ownership regression tests

Added tests proving both paginated and continuous PDF preferences are invariant across every
`PageTurnStyle` value. `toPdfiumPreferences()` remains defined only by PDF scroll/layout state.

## Reliability boundary

Preserved:
- Readium EPUB/PDF navigator ownership;
- Paper/Slide/Paged input policies;
- mode handoff;
- locator durability;
- appearance renderer-settle flow;
- reduced motion;
- PDF zoom renderer ownership.

## Exit status

Source-level hierarchy gate: PASS.
EPUB/PDF ownership gate: PASS.
Canonical-line gate: PASS.
Runtime/build gate: pending an executing CI runner.
