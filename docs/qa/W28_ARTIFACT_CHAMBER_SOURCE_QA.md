# W28 — Artifact Chamber Source QA

Date: 2026-09-30
Branch: `alpha/w28-artifact-chamber-rebuild-v1`
Parent: `alpha/w27-grayfog-archive-rebuild-v1`

## Canonical drift

- W28 is ahead of W27 and zero commits behind.
- No divergent historical Arena branch is merged.
- Changes are restricted to Artifact Chamber presentation plus its design contract.

## Hierarchy

- Cover remains the dominant repeated object.
- Title/author/series remain adjacent to the artifact.
- Reading state is visible before secondary metadata.
- One primary reading CTA dominates.
- Favorite/edit are secondary.
- Archive history, preserved fragments, collections and delete remain available without extra navigation.

## Material language

- Dark archive shell.
- Parchment reserved for primary reading action.
- Brass used as structure/progress.
- Oxblood limited to favorite/seal emphasis.
- No generic bright primary button.
- Fragment cards replaced by archival traces.
- Metadata converted to ledger rows.

## Responsive / RTL

- Compact mode responds to width and font scale.
- Long identity fields remain bounded.
- Existing shaping-safe typography remains untouched.
- Primary action remains full-width and >=48dp.

## Functional invariants

- Open book availability gate preserved.
- Favorite action preserved.
- Metadata edit flow preserved.
- Delete flow preserved.
- Highlights preserved.
- Reading milestones/cycles preserved.
- Archive memory and artifact memory preserved.

## CI evidence

Latest Android CI again created a failed job with an empty step list. This is runner/allocation
infrastructure evidence, not a code-step failure. W28 remains draft until a runner executes actual
build/test steps.

## Exit status

Source-level hierarchy gate: PASS.
Canonical-line gate: PASS.
Runtime/build gate: BLOCKED ON EXECUTING RUNNER.
