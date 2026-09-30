# W36 — Full Harmony + Motion Contract

Date: 2026-09-30
Canonical branch: `alpha/w36-full-harmony-motion-v1`
Parent: `alpha/w35-ritual-sanctum-rebuild-v1`

## Purpose

W36 introduces no new realm. It removes cross-screen drift so Veil Reader behaves like one authored
product rather than a collection of individually polished screens.

## Harmony gates

### Typography
- Persian/Arabic connected scripts always use zero letter tracking, including local style overrides.
- Latin editorial tracking may remain where it improves hierarchy.
- Literal utility labels remain readable before atmospheric naming.
- Numbers, progress and metadata use stable utility hierarchy.

### Geometry
- Plate/Folio/Architecture/Chamber/Hero/Seal remain the only semantic shape families.
- Large rounded corners are reserved for rare architectural/hero surfaces.
- Repeated records prefer lines and registration marks over cards.

### Materials
- Dark shell: Void / Archive / Iron.
- Structure: Antique Brass.
- Exceptional mark: Oxblood.
- Reading/document action: Parchment.
- Parchment never becomes the generic world-shell surface.

### Motion
- Sanctuary is quietest.
- Threshold/Archive motion is functional and brief.
- Castle/World may use spatial reveal.
- Ritual may use the richest transition, only for rare advancement.
- Reduced-motion remains authoritative.
- No perpetual decorative animation.

### Accessibility
- Critical targets >=48dp.
- Selected state is not color-only where semantics are available.
- RTL and large-text hierarchy cannot be an afterthought.
- High-contrast mode must preserve information hierarchy.

## W36 implementation slice

1. Add script-safe tracking helper to the design system.
2. Apply it to high-visibility localized eyebrow/utility labels across rebuilt realms.
3. Add tests proving Arabic-script tracking cannot be reintroduced by local style overrides.
4. Audit shape/material drift in the rebuilt surface families.
5. Preserve all behavioral ownership from W23-W35.

## Rejection conditions

Reject if:
- Persian/Arabic glyphs receive Latin display tracking;
- a harmony refactor changes data or navigation behavior;
- global motion becomes more decorative;
- Reader inherits Castle-level ornament;
- a new one-off shape/color is introduced without a semantic role.
