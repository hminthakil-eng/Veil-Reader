# Veil Reader Project Leadership

## Alpha

Alpha is the single project lead for product vision, architecture, integration, quality, sequencing, and release readiness.

## Night Wolves Team

- Reader Core: EPUB, PDF, Readium, page modes, gestures, restore, Reader chrome.
- World Architecture: Threshold, Great Hall, Observatory, Treasury, Sanctum, realm topology.
- Memory and Archive: Notes, Living Mirror, Highlights, Echoes, Time Capsules, Reading Cycles, Memory Atlas.
- **Art Design Team (dedicated, Alpha-owned):** world art direction, authored master art, realm silhouettes, material language, lighting, parchment/brass/stone/glass/ink/fog studies, relics, seals, iconography, visual continuity, art-asset production and visual QA. This team is not a final-polish service; it co-designs features from the first implementation.
- **Animation Design Team (dedicated, Alpha-owned):** motion constitution, interaction choreography, realm transitions, shared bounds/elements, spring tuning, ceremonial sequences, reduced-motion equivalents, visual continuity through navigation, shader/Rive motion studies, sound/haptic synchronization and motion QA.
- Motion and Sensory Engineering: runtime implementation of approved motion, sound, haptic and temporal-atmosphere systems.
- Art Systems Engineering: Canvas/procedural renderers, shaders, asset loading, Rive/Lottie integration where approved, quality-tier fallbacks and rendering performance.
- Persian and Accessibility: localization, RTL, numerals, bidi, TalkBack, high contrast, focus order.
- Performance: quality tiers, thermal policy, macrobenchmarks, baseline profiles, asset budgets.
- QA: regression testing, lifecycle, process death, large libraries, accessibility and performance checks.
- Integration and Release: branch convergence, pull request sequencing, package provenance and release promotion.

- **Innovation & Creativity Team (dedicated, Alpha-owned):** hunts for original product ideas, interaction inventions, new reader utilities, unconventional information architecture, playful/meaningful world behaviors, visual metaphors, novel accessibility patterns and technical-art opportunities. It is allowed to propose high-risk prototypes, but must attach a user value hypothesis, fallback, complexity cost and kill criteria before anything graduates.
- **Critical Team (dedicated, adversarial, Alpha-owned):** acts as Veil's internal opposition. It challenges assumptions, finds contradictions, hunts UX debt, architectural coupling, false-premium styling, accessibility regressions, performance traps, fake differentiation, overengineering, weak evidence and features that are beautiful but not useful. It does not own implementation; it owns pressure-testing and escalation.
- **Reviewing Team (independent acceptance board under Alpha):** reviews completed slices against source, tests, runtime evidence, accessibility, performance, product intent and visual truth. It does not accept "implemented" based on code presence alone. It can mark work PASS, PASS WITH DEBT, REWORK, BLOCKED or REJECTED and can withhold milestone closure until evidence is reproducible.

- **Development Team (dedicated implementation engine, Alpha-owned):** converts accepted product/design intent into production code. It owns implementation slices, architecture fit, tests, integration discipline, migration-safe changes, build health and exact branch/SHA traceability. It must prefer reuse over parallel systems and must not silently redefine product behavior while coding.
- **Ramifications Team (dedicated consequence-analysis team, independent from implementation):** examines second- and third-order effects before and after a change. It maps lifecycle, persistence, navigation, Reader, accessibility, RTL, localization, performance, thermal, memory, backup/migration, low-end-device and cross-realm consequences. Every substantial change receives a ramifications note identifying affected systems, regression vectors, reversible fallback and what evidence would invalidate the change.
- **Enhancement Team (dedicated depth/quality team):** takes a working feature and raises its experiential ceiling without changing ownership or core semantics. It improves visual authorship, interaction depth, motion, sensory feedback, adaptive behavior, discoverability and premium feel. Enhancements must preserve the fallback path, accessibility semantics and measurable performance budget.
- **Improvements Team (dedicated measurable-refinement team):** continuously improves existing behavior through smaller, lower-risk changes. It targets latency, clarity, touch ergonomics, empty/loading/error states, code simplicity, state hygiene, test coverage, copy quality, layout resilience and maintainability. Every improvement should identify a before/after metric or explicit qualitative defect it resolves.
- **Bug Hunters Team (dedicated adversarial defect-hunting unit):** actively searches for crashes, silent failures, stale state, races, lifecycle leaks, process-death faults, orientation issues, corrupted/future timestamps, large-library failures, gesture conflicts, RTL/bidi faults, TalkBack traps, low-memory problems, rendering defects and regression chains. It does not wait for user reports; it creates reproducible cases, smallest failing examples and verification tests before handing fixes to Development.

## Project Rules

GitHub is canonical. Reuse existing systems before creating new ones. Keep one Reader architecture, one persistence owner per data type, one design system, and one active integration path. Every major visual feature needs a lower-cost fallback. Persistent features must define backup and migration behavior. Motion must have a reduced-motion equivalent. Spatial UI must have accessible semantics.

A feature is complete only when its behavior, ownership, persistence, tests, accessibility, performance cost and release verification are understood.


## Alpha Creative Completion Order

Alpha enforces this sequence unless a safety/reliability blocker requires interruption:

1. **Unbuilt signature systems first** — Living Mirror, artifact-centric Great Hall, Shared Realm Motion, morphing seals, authored master art, shader/Rive pilots, Ink Marginalia, missing Reader utility.
2. **Incomplete systems second** — World Kernel persistence, Hall mutation, Archive 4.0, Observatory 3.0, Ritual aftermath, Sanctum object presentation, Reader 5.0 material/physics, Persian World, Quality Governor, accessibility renderer.
3. **Global polish third** — materials, light, depth, motion timing, sound, haptics, typography, touch behavior, empty/loading/error states and performance.
4. **Homogenization last** — every realm, dialog, sheet, transition and failure state must read as one Veil product while preserving the Sanctuary contrast.

## Asset Policy

There is no arbitrary APK-size ceiling for Cathedral work. Size is justified by visible or functional value, not by a fixed megabyte target.

Allowed when quality gain is real:
- authored master art,
- high-quality realm layers,
- stateful vectors,
- carefully selected sound assets,
- shader resources,
- fonts required by supported scripts,
- benchmark/profile assets.

Still rejected:
- duplicate assets,
- invisible fidelity,
- oversized decorative packs with negligible visual gain,
- unbounded decoding/memory cost,
- assets without fallback or provenance/licensing review.

Every substantial asset must earn its bytes through visible quality, utility, accessibility, or sensory value.


## Multi-Stage Cathedral Governance

Every substantial Cathedral feature now passes through distinct specialist ownership:

1. **Invent** — Innovation & Creativity Team proposes or improves the concept.
2. **Map Ramifications** — Ramifications Team traces cross-system consequences, regression vectors, fallbacks and evidence requirements.
3. **Develop** — Development Team implements the smallest coherent production slice on the canonical architecture.
4. **Hunt Bugs** — Bug Hunters Team actively attacks runtime/state/lifecycle/input/accessibility/performance edge cases and creates reproducible failures.
5. **Enhance** — Enhancement Team raises experiential depth, authored quality and sensory coherence without changing core ownership.
6. **Improve** — Improvements Team performs lower-risk measurable refinement and debt reduction.
7. **Attack** — Critical Team independently challenges usefulness, complexity, architecture, evidence and hidden failure.
8. **Prove** — Reviewing Team accepts only what is supported by code, tests, accessibility, performance and runtime evidence.

The stages can iterate, but ownership must remain distinct. The team that implements a feature cannot be the sole authority declaring it complete.

These roles must not collapse into one another. The team that invents a feature cannot be the sole authority declaring it complete.

### Innovation Graduation Gate

A proposed innovation must define:
- user value,
- why existing Veil systems are insufficient,
- minimum viable experiment,
- fallback,
- accessibility model,
- performance risk,
- persistence/ownership implications if any,
- kill criteria.

### Ramifications Gate

The Ramifications Team documents:
- directly affected modules and owners,
- indirect state/lifecycle/persistence effects,
- Reader and navigation consequences,
- backup/migration implications,
- RTL/localization/accessibility effects,
- low-end/performance/thermal/memory risks,
- failure propagation and recovery behavior,
- rollback/fallback path,
- evidence needed to prove no unacceptable regression.

### Development Gate

The Development Team must provide:
- exact branch/SHA,
- smallest coherent implementation slice,
- ownership/persistence compatibility,
- relevant unit/instrumentation coverage,
- no duplicate architecture,
- reversible changes where feasible,
- build/lint/test status when infrastructure permits.

### Bug Hunters Gate

The Bug Hunters Team actively probes:
- process death and restoration,
- background/foreground races,
- rotation/window resizing,
- rapid repeated gestures and navigation,
- malformed/corrupt/future data,
- large libraries and long sessions,
- low-memory/recreation behavior,
- TalkBack/keyboard/large text,
- RTL/bidi and mixed-script content,
- rendering/performance regressions,
- silent no-op routes and stale state.

Each confirmed defect must have a reproduction path and, where practical, a regression test.

### Enhancement Gate

The Enhancement Team may deepen a feature only when:
- the baseline behavior is functionally coherent,
- the enhancement does not create a second state owner,
- reduced-motion/accessibility fallbacks remain intact,
- cost is justified by visible or functional gain,
- the enhancement can be disabled or degraded safely if needed.

### Improvements Gate

The Improvements Team records a clear before/after target such as:
- fewer steps,
- lower latency/jank,
- stronger layout resilience,
- clearer copy/state communication,
- smaller code/state surface,
- better test coverage,
- better recovery behavior,
- lower memory/thermal cost.

### Critical Challenge Gate

The Critical Team asks:
- Is this genuinely useful or merely impressive?
- Does it create duplicated ownership?
- Can it fail silently?
- What happens on compact devices, RTL, large text, reduced motion and low-end hardware?
- Does it compromise Sanctuary?
- Is there a simpler mechanism with the same user value?
- Is the evidence real or inferred?
- What would make us delete this feature?

### Review Acceptance Gate

The Reviewing Team verifies:
- exact branch/SHA,
- build/test/lint state,
- runtime behavior,
- visual comparison against product intent,
- accessibility semantics and touch targets,
- performance/memory impact,
- persistence/restore/process-recreation behavior when relevant,
- fallback behavior,
- absence of hidden functionality loss across fidelity tiers.

A green CI badge alone is never sufficient for product acceptance.
