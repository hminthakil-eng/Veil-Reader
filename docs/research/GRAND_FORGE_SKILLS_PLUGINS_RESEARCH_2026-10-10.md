# Grand Forge — Evidence-led skills, plugins and tooling intake (2026-10-10)

## Scope and baseline
Canonical integration target observed: `grand-forge/p0-kindle-reader-quality-20261008`; this is a research/intake branch only. Do **not** merge verification-only PRs or assume `main` contains current Reader development.
Current draft blockers include Paper input/GPU #442; durable-position retry #451; search foundation #452; Translate #449; visual foundation #436. Existing competitor research #441 and APK metadata tool #448 should be reused.

## Inventory — avoid duplication
The repository already carries .agents/skills for Android engineering, world-class-app development, product design, release guardianship, Superpowers, Impeccable and related skills; see `skills-lock.json` and `.github/workflows/install-agent-skills.yml`.
This PR adds original targeted orchestration and seven deeper project-specific skills, not another copy of third-party skill code.
Skill markdown adds operator instructions only: it does **not** install Android plugins, run tests, connect ChatGPT apps, or automatically dispatch agents.

## Candidate decisions
| Candidate | Current decision | Reason / gate |
| --- | --- | --- |
| Readium Kotlin Toolkit 3.4.0 | REUSE existing; evaluate exact current pin | Official BSD-3; EPUB/PDF/TTS/search; 3.4 changes PDF layout defaults and locator migration |
| Roborazzi | PILOT one isolated visual component | Robolectric integration, Compose screenshots; verify current AGP/Kotlin/Compose and golden stability |
| Paparazzi | ALTERNATIVE, not alongside Roborazzi by default | Screenshot tests without device; avoid overlapping golden stacks |
| Android Macrobenchmark + Baseline Profiles | EXTEND existing benchmark + CI | Release-like physical-device trace and profile-artifact proof, not emulator-only pass |
| Detekt | OPTIONAL PILOT | Kotlin code smell/static reports; configure noise threshold and baseline before gating CI |
| Gradle dependency verification/locking | PILOT controlled migration | Hash/key trust review, pinned graph and reproducibility; no blind trust bootstrap |
| GitHub Dependency Review + Dependabot | REUSE existing | Already present; audit current configuration and verify PR checks |
| OWASP MASVS | ADOPT as review checklist | Storage/crypto/network/platform/code privacy coverage |
| Firebase/Crashlytics | CONNECT ONLY IF APPROVED | Optional external diagnostics; user consent, privacy policy, data retention and project setup required |
| Sentry SDK | ALTERNATIVE to Crashlytics | Avoid installing two crash pipelines; privacy/performance evaluation required |
| Figma, Product Design, Mobbin | USE when design task warrants | Reference/design tools; not an Android runtime dependency |
| GitHub and existing Codemagic runner | KEEP canonical execution | Preserve exact-head CI, review isolation, artifacts and release gates |
| Linear | DEFER | GitHub Issues/PRs already canonical; duplicative work tracking adds operational drift |
| Vercel/Render/backend agents | DEFER | Local/offline-first Android app has no demonstrated deployment need |
| AI agent marketplace/large skills bundles | REVIEW ONLY | Pre-execution supply-chain/permission, prompt injection and overlap audit |

## Ordered implementation and verification
P0: resolve Paper (#442) and reader locator durability (#451) with device repro and exact-head CI; preserve four modes (PAGED, SLIDE, PAPER CURL, SCROLL).
P1: converge draft visual/selection/search work without guessing branch precedence. Add screenshot + RTL/large-font/TalkBack corpus through one isolated Roborazzi/Paparazzi compatibility pilot.
P1: cover Room migration, process-death, backup/restore and source ZIP resilience. Expand Macrobenchmark profile proof and release-artifact checks.
P2: add controlled Detekt and Gradle verification/locking pilots; compare failures and build-time changes before release gating.
P2: consider opt-in crash diagnostics only with written privacy and consent decision; compare Crashlytics vs Sentry, select at most one.
P3: benchmark format-specific search/TTS/PDF quality and evaluate original gamification visuals independently after core Reader stability.

## Non-negotiable acceptance
No fake green. No magic plugin that fixes runtime code by itself. Every adopted tool must state license, exact version/hash, active maintainer, compatibility, data exposure, setup cost, rollback, before/after measurements and named owner.
All changes are separate, reversible PRs. No proprietary Kindle/Moon+ implementation/code/assets, DRM bypass, user-book uploads or unsafe APK execution. A physical-device demonstration is required to declare Paper Curl functional.

## Research and reference URLs
- Readium Kotlin Toolkit 3.4.0: https://github.com/readium/kotlin-toolkit ; https://blog.readium.org/release-note-kotlin-toolkit-version-3-4-0/
- Screenshot frameworks: https://github.com/takahirom/roborazzi ; https://github.com/cashapp/paparazzi
- Android accessibility: https://developer.android.com/guide/topics/ui/accessibility/testing
- Android Baseline Profiles: https://developer.android.com/topic/performance/baselineprofiles/overview
- Android vitals: https://developer.android.com/games/optimize/vitals
- Gradle dependency verification: https://developer.android.com/build/dependency-verification ; https://docs.gradle.org/current/userguide/dependency_locking.html
- GitHub dependency review: https://github.com/actions/dependency-review-action
- Kotlin static analysis: https://github.com/detekt/detekt
- OWASP MASVS: https://mas.owasp.org/MASVS/
