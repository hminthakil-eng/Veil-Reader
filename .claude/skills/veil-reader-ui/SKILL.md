---
name: veil-reader-ui
description: Project-specific UI/UX workflow for Veil Reader. Use for designing, reviewing, or implementing any screen, reader chrome, navigation, component, animation, accessibility behavior, RTL behavior, or adaptive layout.
---

# Veil Reader UI Director

## Required context
Read these before UI work:
1. `design-system/veil-reader/MASTER.md`
2. Matching file under `design-system/veil-reader/pages/` when present
3. Existing Compose theme at `app/src/main/java/com/veilreader/app/ui/theme/Theme.kt`

## Platform
Veil Reader is native Android: Kotlin + Jetpack Compose + Material 3. Never introduce a React/web runtime dependency for UI work.

## Design-intelligence workflow
When UI UX Pro Max is installed, use its `jetpack-compose` stack guidance for implementation-specific decisions. The approved upstream reference is pinned in `.eyad-studio/ui-stack.yml`.

21st.dev may be used to discover visual/interface patterns, but only as inspiration. Translate patterns into native Compose using Veil tokens, Material semantics, Android navigation/back behavior, accessibility, adaptive layout, and RTL rules. Never paste React/shadcn code into the app.

## Work sequence
1. State the user outcome and screen states.
2. Read MASTER + page override.
3. Audit the existing screen before replacing components.
4. Reuse existing components/tokens where practical.
5. If using external inspiration, extract the pattern rather than the implementation.
6. Implement natively in Compose.
7. Verify compact/expanded layouts, dark/light, large font, RTL, TalkBack semantics, touch targets, reduced motion, and system back behavior.
8. Do not call UI complete until relevant tests/QA pass.

## Design priorities
Reading clarity > navigation predictability > library discoverability > progress/motivation > atmosphere > decorative motion.

## Hard constraints
- Preserve the established Veil identity unless a deliberate redesign explicitly changes it.
- 48dp practical touch targets for primary controls.
- No left/right layout assumptions when start/end is correct.
- No color-only state communication.
- No fixed phone-only layouts.
- No gamification interruption while the user is actively reading.
- No generic AI-dashboard styling.

## Review checklist
- Does it look like Veil Reader rather than a template?
- Is the main action obvious without visual noise?
- Can the screen survive long text and 200% font scale?
- Is every icon-only control labeled semantically?
- Is it usable in RTL?
- Does it adapt to larger windows?
- Is motion meaningful and reducible?
- Does the reader remain the quietest, least decorative surface?
