---
name: veil-compose-quality-reviewer
description: Focused Jetpack Compose quality review for Veil Reader. Use when creating or changing screens, lists, animations, themes, reader chrome, navigation UI, or reusable composables. Reviews architecture, state ownership, recomposition risk, accessibility, large text, touch targets, semantics, and interaction quality.
license: MIT-compatible adaptation
metadata:
  inspiration: Desquared/agents-rules-skills Android Compose, performance, accessibility, and Kotlin API skills
---

# Veil Compose Quality Reviewer

Review only the changed UI surface and its direct state path. Keep findings practical and tied to user impact.

## Architecture

- Composables render state and emit events; persistence/network/business logic stays outside composition.
- Prefer unidirectional data flow: state down, events up.
- Keep reusable composables stateless when practical.
- Collect long-lived `Flow`/`StateFlow` with lifecycle awareness.
- Use the existing project dependency pattern instead of introducing DI or architecture frameworks solely for stylistic purity.
- Keep side effects explicit and keyed correctly (`LaunchedEffect`, `DisposableEffect`, etc.).
- Do not pass heavyweight reader/domain objects through navigation when an ID/state key is sufficient.

## Recomposition and rendering

Check for:
- expensive parsing, sorting, allocation, I/O, or object creation in composable bodies;
- broad state reads that invalidate large trees;
- unstable identity in lazy lists;
- missing stable keys/content types where item identity matters;
- backwards state writes or state changes during composition;
- animation work that continues when not visible or overwhelms reading interaction;
- unnecessary image decoding or uncached cover work.

Do not recommend `@Stable`/`@Immutable`, `remember`, or `derivedStateOf` mechanically. Use them only when the type/derived value is semantically safe and the recomposition behavior justifies it.

## Accessibility

For changed interactive UI:
- targets should generally meet the Android 48dp minimum;
- controls need meaningful TalkBack semantics and localized labels;
- decorative content should not create noisy announcements;
- grouped content should have intentional merged semantics when useful;
- toggles/custom controls need role and state information;
- headings/focus order/traversal should make sense;
- text must scale without clipping or hiding required actions;
- color cannot be the only signal;
- important text/icon contrast should remain accessible;
- reduced-motion users should not depend on animation to understand state.

Test critical flows with TalkBack/manual large text where feasible, and add Compose accessibility checks for stable automated coverage when appropriate.

## Reading-first constraints

Reader chrome, overlays, progression UI, and animations must not:
- cover essential text/actions unexpectedly;
- steal focus during active reading;
- trigger celebrations/prompts over the page;
- cause visible scroll/page-navigation jank;
- make EPUB/PDF controls inconsistent without a capability reason.

## API and naming quality

- Composable `Unit` functions use clear PascalCase nouns.
- Booleans communicate state (`is`, `has`, `can`, `should`).
- Avoid redundant type words (`nameString`, `userList`).
- Public/reusable APIs should be obvious at the call site and avoid needless parameters.
- Prefer immutable UI models and clear event contracts.

## Severity

- **Blocker** — unusable interaction, inaccessible essential action, serious state/lifecycle bug, or reader regression.
- **Should fix** — likely jank/recomposition problem, poor semantics, large-text failure, architecture leak, or confusing API.
- **Polish** — optional quality improvement with clear value.

This skill adapts ideas from Desquared's MIT-licensed Android architecture, performance, accessibility, and Kotlin API review skills for Veil Reader's reading-first product.
