# Official Android Skills and specialist agent libraries — verified intake

Date: 2026-10-10. This is an intake and compatibility record, not a claim of installation.

Google's Android team publishes official Apache-2.0 agent skills: https://github.com/android/skills and https://developer.android.com/tools/agents/android-skills . It supports selective installation via Android CLI, e.g. `android skills add r8-analyzer --project=.`; the repository also documents `android skills add --all`, which is **not** the default for Veil Reader because many skills are irrelevant or could conflict with established project rules.

| Upstream candidate | Veil decision | Why |
|---|---|---|
| performance/r8-analyzer | HIGH PRIORITY pilot | Release optimized build and R8 examination; existing Baseline Profile gate remains critical |
| profilers/android-profiler | HIGH PRIORITY pilot | Exact-head frame, memory and native/graphics capture for Paper and reading |
| testing/testing-setup | TARGETED pilot | Improve evidence gaps without replacing existing Robolectric/Android instrumented test coverage |
| security/android-intent-security | HIGH PRIORITY | Selection Translate and provider handoff correctness, intent validation |
| security/android-permissions-security | HIGH PRIORITY | No accidental permissions/over-collection |
| jetpack-compose/adaptive | SELECTIVE | Existing adaptive dependency is present; compare real phone, tablet and foldable behavior |
| jetpack-compose/theming | SELECTIVE | Preserve proprietary Grayfog visual identity and Android accessibility |
| system/edge-to-edge | WHEN NEEDED | Prevent insets clipping during full-screen reading |
| build-system/agp/agp-9-upgrade | SKIP on observed baseline | The canonical branch already uses AGP 9.4.0; no value in redundant migration |
| navigation/navigation-3 | DEFER | Avoid framework churn without product blocker |
| media/media3-cast-integration | DEFER | Not a current Reader/TTS release requirement |
| identity, billing, TV, Wear, XR, camera | DEFER | Not relevant to offline-first Reader release blockers |

Specialist Compose performance skills: https://github.com/skydoves/compose-performance-skills — review focused measurement and baseline-profile subskills only; prevent overlap with existing Veil Android Engineer guidance.

External visual tools: Roborazzi https://github.com/takahirom/roborazzi and Paparazzi https://github.com/cashapp/paparazzi are alternative screenshot stacks. Prioritize an isolated Roborazzi compatibility spike because Robolectric is in current app tests. Demand stable golden captures on SDK 37, Compose 1.12.1, AGP 9.4.0 and Android Studio/JDK 21; back out if not robust.

Sources of truth: `app/build.gradle.kts`, `build.gradle.kts`, `WORLD_CLASS_RELEASE_GATES.md`, and the exact branch head. Use the project-specific `.agents/skills/` gates to avoid accidental architecture churn.
