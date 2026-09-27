# Veil Reader — Literary Inspiration Grammar

## Purpose

Veil may learn from the project's supplied fiction references at the level of atmosphere, material, spatial rhythm, reveal, and world-building. It must not reproduce protected prose, book-specific symbols, maps, character art, iconography, scene compositions, or named lore.

The goal is not to make a fan skin. The goal is to understand why a mysterious world feels inhabited, layered, historical, and discoverable, then express those principles through Veil's own reading product.

## Translation rules

### Passage, not card

Important transitions should feel spatial. A book can read as a threshold, an archive section as a catalog chamber, and Castle navigation as vertical movement. Generic rounded cards are the fallback, not the visual idea.

### Local light, not global glow

Use small pools of warm light to imply inhabited space. Do not wash the entire interface in gold. Brightness must never reduce text contrast.

### Vertical architecture, not wallpaper

Use a few large piers, arches, rails, stairs, and distant structural lines. Avoid repeated gothic patterns. Architecture should explain hierarchy and depth.

### Industrial precision, not steampunk decoration

Measured ticks, narrow brass rules, restrained iron geometry, and mechanical spacing can suggest precision. Literal gears, pipes, gauges, and decorative machinery should be rare and functional.

### Documents are physical memory

User-created or user-preserved memory should look materially different from system suggestions: folios, slips, dossiers, seals, index rails, and paper strata are valid. The original book content must remain readable and unmodified.

### Fracture means unstable memory or world boundary

Mirror/fracture geometry is reserved for Echo, Observatory, deep Archive, Ritual, or Sanctum contexts. It should be sparse. Never turn the app into broken-glass decoration.

### Fog creates distance

Fog belongs behind content. It should create depth and partial revelation, never obscure interaction targets or reading text.

### Bells and clocks are pacing language

Distant ring geometry and measured ticks can mark transition, chronology, or ceremony. They are not badges and they do not imply urgency.

## Realm allocation

- Threshold: passage, two lamp anchors, restrained clock ticks.
- Sanctuary: none. The page is the world.
- Archive: catalog rails, paper artifacts, old structure, sparse fracture.
- Castle: vertical architecture, inhabited lamps, central route, distant rings.
- World: architectural depth with stronger environmental variation.
- Ritual: precise geometry and ceremony, never visual noise.
- Sanctum: sparse, rare, materially dense, older than the rest of the shell.

## UX constraints

1. Resume reading remains the dominant action on Threshold.
2. Reader chrome never inherits world-shell ornament.
3. Decorative motifs cannot carry information by themselves.
4. All important states remain accessible through text/semantics.
5. Reduced motion changes timing/movement, not information.
6. User/source text is never rewritten to fit the art direction.
7. Visual mystery must not create navigational ambiguity.
8. Every ornamental layer needs a realm budget and an explicit reason to exist.

## Current implementation

NarrativeArtDirection.kt encodes a deterministic realm grammar for passage depth, lamplight, vertical piers, archive rails, mechanical ticks, fracture marks, and distant ring marks.

Threshold additionally gives the active book a dedicated return-passage frame. Preserved passages use a paper artifact surface so remembered user material is visually distinct from optional system prompts. Archive section navigation uses catalog-slip treatment. Library, Archive, Observatory and Castle receive different strengths of the same shared grammar.

This layer is static, offline, deterministic, and presentation-only.