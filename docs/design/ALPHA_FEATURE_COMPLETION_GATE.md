# Alpha Feature Completion Gate — Build Deferred

Product directive: finish the currently committed Alpha feature program before final build verification. Do not treat CI/build activity as the next milestone while material product work below remains open.

## Gate definition

Feature-complete means the current recovery program in #312–#316 plus the explicitly planned G1/G2 layers are implemented coherently on one canonical integration path. Build/test/device gauntlet happens after this gate.

### R0/R1 Reader Capability Recovery
- [x] One-tap Reading Console entry
- [x] TYPE / PAGE / LIGHT / MORE console
- [x] Persisted font family, alignment, columns, hyphenation, ligatures, normalization, paragraph/letter/word spacing, type scale
- [x] Publisher-style effectiveness communication
- [x] Adjustable Paper Patina
- [x] Font-weight control for reflowable EPUB mapped to Readium 3.4 `0.0..2.5`
- [x] Paragraph-indent control for supported reflowable EPUB layouts
- [x] Dark-theme EPUB image treatment (`Original / Darken / Invert`) with Readium effectiveness guard
- [ ] Final capability matrix proving no previous reader feature became hidden

### R2 Page Engine
- [x] Distinct static PAGED behavior
- [x] Distinct weighted SLIDE engine with preview/cancel/commit
- [x] Reduced Motion + RTL ownership for Slide
- [ ] Final Curl / Slide / Paged / Scroll capability matrix and arbitration audit
- [ ] Final gesture conflict audit against selection, chrome, PDF zoom, and Reading Console

### R3 Sanctuary Material Engine
- [x] Paper/Sepia material profiles
- [x] User-adjustable patina
- [ ] Controlled fidelity/quality policy for expensive material effects
- [ ] Material fallback contract for low-power / reduced-motion contexts

### G1/G2 Gamification + World
- [x] WorldProgressionProjection
- [x] Path-aware directives
- [x] Path mastery: Embodiment / Insight / Stability
- [x] Dissonance diagnostic
- [x] Rank-scoped mastery evidence
- [x] Mystery Chains
- [x] WorldMutationLedger
- [x] Ritual Aftermath
- [x] Evidence-backed relics
- [x] Silent Names playable vertical slice foundation
- [x] Dual-book provenance contract for Silent Names
- [x] Optional Covenants
- [x] Relic temperaments
- [x] Veil-original Orders / factions
- [x] Deeper rank-specific world mutations
- [x] Dual-book provenance registry extended across Paths, directives, mutations, relics and discoveries

### World projection coverage
- [x] Great Hall
- [x] Observatory lens
- [x] Archive mutation echo
- [x] Mirror mutation echo
- [x] Treasury mutation echo
- [x] Sanctum mutation echo
- [x] Rank-specific mutations project distinct copy into all six realms
- [ ] Silent Names consequence is visible beyond encounter page (Hall + Treasury completed; remaining realm reactions as designed)

### W1 Living Mirror
- [x] Physical frame/glass/fog/silvering
- [x] Reflection marks instead of generic cards
- [x] Return to Passage
- [x] Accessible Index renderer
- [ ] Final factual-memory boundary audit
- [ ] Reduced Motion / TalkBack equivalence audit

### W2 / Q1 World Art + Sensory Quality
- [x] Grayfog material direction in core world surfaces
- [ ] Quality governor contract for expensive world effects
- [ ] Haptic/audio event map audited against user settings
- [ ] Low-power / thermal / reduced-motion fallback behavior defined consistently

### QA Capability + Visual Gate
- [ ] Before/after capability matrix is complete for every recovery workstream
- [ ] No hidden or effectively undiscoverable capability remains
- [ ] Static integration audit clean across canonical branch
- [ ] Only after every open feature box above is resolved: run unit + lint + assemble + storage + performance + device visual/runtime gauntlet

## Non-negotiable narrative rule

Every authored gamification element must identify:
1. its Lord of the Mysteries concept/theme basis,
2. its Circle of Inevitability concept/theme basis,
3. the original Veil transformation.

This is a design-provenance requirement, not permission to copy source characters, dialogue, plot, lore text, named factions, named rank ladders, or prose.

## Build policy

Until this document reaches feature-complete:
- do not spend product time chasing hosted CI pre-execution failures;
- do not call the branch GREEN;
- do not merge merely because static checks look clean;
- keep changes reversible and converge duplicate implementations before adding new architecture.
