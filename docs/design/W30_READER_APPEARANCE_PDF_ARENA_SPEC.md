# W30 — Reader Appearance + PDF Workspace Arena Spec

Date: 2026-09-30
Canonical branch: `alpha/w30-reader-appearance-pdf-workspace-v1`
Parent: `alpha/w29-sanctuary-reader-rebuild-v1`

## Purpose

Appearance is an instrument panel for the Sanctuary, not a second settings app. PDF is a separate
document workspace, not EPUB with different content.

## EPUB Appearance — hierarchy

### Quick
The default surface exposes only:
1. live page preview;
2. publication material/theme;
3. text size;
4. exactly one navigation mode: Paper / Slide / Paged / Scroll;
5. return to reading.

No separate Scroll toggle may coexist with a page-turn selector.

### Advanced
Advanced contains typography and layout controls that are genuinely renderer-supported:
- font family / weight;
- line height / margins;
- alignment / columns;
- script-safe spacing and shaping;
- publisher style override;
- image treatment / patina where supported.

Unsupported controls must disable or disappear based on publication capabilities rather than fake a
working state.

## Live preview law

- Preview reflects the draft immediately.
- Slider motion can remain draft-only until release to protect renderer churn.
- Preview is a material sample, not a decorative card.
- Navigation mode label in preview must match the single canonical `navigationMode`.
- RTL preview follows current layout direction and shaping-safe typography rules.

## PDF Workspace — ownership

PDF owns:
- Paginated vs Continuous layout;
- zoom;
- zoom reset;
- fit width;
- brightness;
- PDF renderer behavior.

PDF does **not** own or inherit:
- Paper Curl;
- Slide;
- EPUB font family;
- EPUB line height / text spacing;
- EPUB columns.

`ReaderAppearance.toPdfiumPreferences()` must remain independent of `pageTurnStyle`.

## Visual language

- Sanctuary density remains low.
- Flat instrument rails and hairlines over card walls.
- Parchment belongs to reading surfaces, not every control.
- Brass indicates selection/structure.
- Oxblood is not needed for ordinary settings.
- 48dp minimum targets remain mandatory.

## W30 implementation slice

1. Flatten Quick / Advanced selector.
2. Flatten reusable Appearance choices while preserving semantics.
3. Recompose live preview as a material specimen.
4. Recompose PDF layout selector as a dedicated document-mode rail.
5. Demote zoom +/-/reset into compact instrument controls.
6. Add tests proving PDF preferences are invariant to EPUB `pageTurnStyle`.

## Rejection conditions

Reject if:
- Appearance becomes visually denser than the Reader itself;
- Scroll and page-turn controls can contradict each other;
- PDF displays Curl/Slide options;
- pageTurnStyle changes PDF preferences;
- live preview lies about the selected navigation mode;
- disabled renderer capabilities appear functional;
- control styling becomes a wall of rounded cards.
