---
name: veil-reader-ui
description: Project-specific UI/UX workflow for Veil Reader. Use for designing, reviewing, or implementing any screen, reader chrome, navigation, component, animation, accessibility behavior, RTL behavior, or adaptive layout.
---

# Veil Reader UI Director

Read `design-system/veil-reader/MASTER.md`, the relevant page override, and the existing Compose theme before changing UI.

Veil Reader is native Android: Kotlin + Jetpack Compose + Material 3. Do not add React/web runtime dependencies.

When UI UX Pro Max is available, use `jetpack-compose` stack guidance. The approved pinned upstream reference is recorded in `.eyad-studio/ui-stack.yml`.

21st.dev is visual/pattern inspiration only. Extract layout, hierarchy, component behavior, and interaction ideas; translate them into native Compose with Veil tokens, Android semantics, system back behavior, adaptive layouts, accessibility, and RTL support. Never paste React/shadcn code into the app.

Workflow:
1. Define the user outcome and states.
2. Read MASTER + page override.
3. Audit existing UI and reuse before rebuilding.
4. Implement native Compose.
5. Verify dark/light, compact/expanded, large font, RTL, TalkBack semantics, >=48dp practical touch targets, reduced motion, and back behavior.

Priorities: reading clarity > predictable navigation > library discoverability > progress/motivation > atmosphere > decorative motion.

Hard constraints: preserve Veil identity; no generic AI-dashboard styling; no color-only state; no fixed phone-only layouts; no left/right assumptions where start/end is correct; no gamification interruption during active reading.
