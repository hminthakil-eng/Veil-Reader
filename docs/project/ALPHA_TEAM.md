# Veil Reader Project Leadership

## Alpha

Alpha is the single project lead for product vision, architecture, integration, quality, sequencing, and release readiness.

## Night Wolves Team

- Reader Core: EPUB, PDF, Readium, page modes, gestures, restore, Reader chrome.
- World Architecture: Threshold, Great Hall, Observatory, Treasury, Sanctum, realm topology.
- Memory and Archive: Notes, Living Mirror, Highlights, Echoes, Time Capsules, Reading Cycles, Memory Atlas.
- Art Direction: Grayfog, materials, lighting, authored assets, procedural art, shaders.
- Motion and Sensory: transitions, springs, sound, haptics, temporal atmosphere, reduced motion.
- Persian and Accessibility: localization, RTL, numerals, bidi, TalkBack, high contrast, focus order.
- Performance: quality tiers, thermal policy, macrobenchmarks, baseline profiles, asset budgets.
- QA: regression testing, lifecycle, process death, large libraries, accessibility and performance checks.
- Integration and Release: branch convergence, pull request sequencing, package provenance and release promotion.

## Project Rules

GitHub is canonical. Reuse existing systems before creating new ones. Keep one Reader architecture, one persistence owner per data type, one design system, and one active integration path. Every major visual feature needs a lower-cost fallback. Persistent features must define backup and migration behavior. Motion must have a reduced-motion equivalent. Spatial UI must have accessible semantics.

A feature is complete only when its behavior, ownership, persistence, tests, accessibility, performance cost and release verification are understood.
