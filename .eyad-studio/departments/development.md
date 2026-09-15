# Veil Reader — Development Department

## Mission
Turn approved product decisions into reliable, testable Android software while preserving reading quality, data durability, accessibility, performance, and offline-first behavior.

## Ownership
Development owns architecture, implementation, integration, automated tests, migration safety, performance verification, build health, and release-candidate readiness.

Development should not silently invent product behavior when an important assumption is unresolved. Route material uncertainty back to Research.

## Core roles
- Development Director — scope, sequencing, technical execution and delivery quality.
- Software Architect — boundaries, data model, migrations, dependency discipline.
- Android Engineer — Kotlin/Compose/Readium implementation and device behavior.
- UX implementation partner — translates approved interaction/design evidence into reusable components.
- QA Engineer — automated/manual gates, regression coverage and release evidence.

## Working model
Build vertical slices. A slice should include the relevant data/logic/UI/tests/accessibility behavior rather than leaving quality work for a later cleanup phase.

For v0.9 → 1.0, preserve the existing architecture guardrails: Room as structured source of truth, DataStore for preferences, app-private imported publications, Readium behind its existing boundary, offline-first defaults, and no architecture rewrite without measured need.

## Entry criteria from Research
A research-led feature is ready for Development when it has:
- a clear problem statement;
- recommended decision;
- evidence summary;
- explicit acceptance criteria;
- known risks and constraints;
- a GO or GO WITH GUARDRAILS state.

## Definition of done
- User goal works end-to-end.
- Reading continuity and data durability are preserved.
- Empty/loading/error/restore states are covered where relevant.
- TalkBack, large text, touch targets, contrast and reduced motion are checked.
- Relevant automated tests pass.
- Performance claims have measurement evidence.
- No silent permission, privacy or dependency expansion occurred.
- CI is green and the change is reviewable.

## Escalation back to Research
Return the work when a critical product assumption is ambiguous, a technical approach needs comparative evidence, accessibility/usability risk is unresolved, or measured performance contradicts the expected design.
