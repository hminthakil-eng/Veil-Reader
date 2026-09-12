---
name: compose-kotlin-agent-skills
description: Production Android/Kotlin/Jetpack Compose development guidance for Veil Reader. Use for Android architecture, Compose UI, Room/DataStore, navigation, coroutines/Flow, accessibility, performance, testing, CI, release work, and code review.
license: MIT
metadata:
  source: https://github.com/haidrrrry/compose-kotlin-agent-skills
  adapted_for: Veil Reader
---

# Veil Reader Android/Kotlin Skill

Use this skill for all Kotlin/Android work in this repository. Preserve Veil Reader's existing architecture and release discipline; do not rewrite working subsystems without a concrete reason.

## Core defaults
- Kotlin + Jetpack Compose first.
- Keep state in ViewModels using StateFlow; expose immutable state.
- Prefer atomic `_state.update { ... }` changes over ad-hoc mutation.
- Use `collectAsStateWithLifecycle()` in Android UI.
- Keep composables stateless where practical; pass state and callbacks rather than ViewModels deep into the tree.
- Use stable keys in lazy lists.
- Keep UI strings in resources.
- Use structured coroutines; avoid GlobalScope and unmanaged jobs.
- Preserve offline-first behavior and data durability.
- Treat Room as structured source of truth and DataStore as preference storage unless the repository architecture says otherwise.
- Pass IDs through navigation and load the model at the destination instead of passing large mutable objects.

## Veil Reader-specific priorities
1. Reading must remain the primary, distraction-free experience.
2. Never regress EPUB/PDF import, reading position restoration, annotations, bookmarks, backup/restore, or migration safety.
3. Gamification must remain ethical and non-punitive.
4. Avoid cloud/account dependencies unless explicitly requested; the current product is offline-first.
5. Protect user-owned books, notes, highlights, sessions, and backup data from corruption.
6. Prefer incremental improvements over architectural churn.

## Architecture checks
Before editing a feature, identify:
- current ViewModel / state owner,
- repository or DAO boundary,
- persistence implications,
- navigation entry/exit,
- migration or backup impact,
- test coverage that should move with the change.

Do not introduce a new abstraction if an existing repository pattern already solves the problem cleanly.

## Compose review rules
When adding or reviewing Compose code:
- watch for unnecessary recomposition,
- keep expensive work out of composition,
- use `remember` / `derivedStateOf` only when justified,
- use stable item keys,
- respect edge-to-edge and window insets,
- preserve accessibility semantics and minimum touch targets,
- test dynamic font sizes and TalkBack-relevant labels,
- avoid hardcoded dimensions where they break responsiveness.

## Room / storage rules
For schema or storage work:
- never rely on destructive migration for user data,
- add/verify migration tests,
- preserve backup compatibility where required,
- keep restore transactional,
- validate imported/archive paths and sizes,
- avoid duplicating large book content unnecessarily.

## Testing expectations
For meaningful behavior changes, add or update the smallest useful test layer:
- ViewModel/unit tests for state transitions,
- repository/DAO tests for persistence,
- migration tests for schema changes,
- Compose UI tests for critical interaction behavior,
- manual-device QA only when automation cannot prove the behavior.

Before calling work complete, run the repository's existing policy/build/test commands relevant to the touched area.

## Performance
Prioritize:
- reader smoothness,
- startup/import latency,
- scrolling stability,
- memory usage with large EPUB/PDF libraries,
- avoiding unnecessary bitmap/object retention,
- background work that respects lifecycle.

Do not optimize speculatively; measure or tie the optimization to a concrete hot path.

## Release discipline
Respect the existing release gates and CI. Do not call a change production-ready if device QA, accessibility, migrations, signing, or other documented gates remain unverified.

## Source
Adapted from the MIT-licensed `compose-kotlin-agent-skills` project by haidrrrry. Keep the attribution/license file next to this skill.
