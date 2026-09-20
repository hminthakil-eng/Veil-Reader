# Veil Reader — Dual-Team Operating Model

Status: active
Date: 2026-09-20

Veil Reader is developed by two tightly coupled teams with separate expertise and shared release accountability.

## Team A — Product Experience & Frontend Engineering (PXFE)

Mission:
Create a world-class, beautiful, calm, accessible and distinctive reading experience while staying native to Android interaction patterns.

Responsibilities:
- continuous competitor research: Apple Books, Kindle, Kobo, Google Play Books, ReadEra, Moon+ Reader, high-quality Android/iOS apps, Fluent and current Material guidance.
- product UX, information architecture and user flows.
- Veil Design System.
- Compose implementation of user-facing surfaces.
- typography, spacing, iconography, layout, motion, adaptive UI.
- Persian/RTL and localization design.
- accessibility semantics.
- screenshot/golden regression.
- visual QA on physical devices.
- identifying "cheap", prototype-like, inconsistent or overly decorative UI before release.

Research rule:
Before a major user-facing interaction is designed:
1. inspect current Apple/Google/Microsoft platform guidance.
2. inspect at least two mature reading-product implementations.
3. document the pattern being reused.
4. identify the Veil-specific improvement.
5. do not copy proprietary brand assets or exact visual identity.

Design principles:
- content before branding.
- familiar behavior, distinctive details.
- accent color used sparingly.
- spacing and typography create hierarchy before borders/elevation.
- dark mode is art-directed, not inverted.
- sentence case by default; avoid all-caps except deliberate compact metadata.
- motion is functional, natural, interruptible and reduced-motion aware.
- no AI-dashboard visual language.
- no nested-card soup.
- no decorative glass/gradient/shadow without a hierarchy reason.

## Team B — Reader Core & Reliability Strike Team

Mission:
Make reading boringly reliable under real-world input, lifecycle, file, device and persistence failure.

Responsibilities:
- Readium integration.
- ReaderSession / engine boundaries.
- EPUB/PDF/Image/TTS/Audio adapters.
- page-turn/input correctness.
- state/persistence/data durability.
- Room/DataStore/backup.
- performance and memory.
- lifecycle/process-death.
- large/corrupt publication handling.
- sync architecture.
- security.
- aggressive bug hunts and regression tests.

Reader invariants:
- visual effects never own the Android touch path.
- page-turn effects never become the authoritative reading state.
- transient previews do not persist progress.
- cancelled gestures restore exact reading state.
- no reader feature depends on network, game, AI or telemetry.
- input listeners have explicit lifecycle ownership/removal.
- format-specific implementation details stay behind adapters.

## Shared contract

Neither team can mark a feature GREEN alone.

Every user-facing feature has two parallel tracks:

```
Experience track:
Research -> UX spec -> Design System -> Compose -> Visual QA

Engineering track:
Architecture -> State/Data -> Reliability -> Tests -> Performance
```

They meet at Integration Gate.

### Required artifacts per feature
PXFE provides:
- benchmark references.
- flow/state map.
- responsive/RTL/accessibility rules.
- light/dark treatment.
- visual acceptance screenshots.
- motion spec when applicable.

Core provides:
- capability contract.
- failure/degraded states.
- lifecycle/state semantics.
- persistence rules.
- unit/integration/performance evidence.

### Cross-review
- PXFE reviews Core work for user-visible failure states and visual side effects.
- Core reviews PXFE work for state ownership, performance, lifecycle and engine leakage.
- neither team may bypass the other's RED issue.

## Joint Definition of Done

A feature is GREEN only when:
- unit/integration gates pass.
- lint/build pass.
- physical-device behavior passes.
- visual QA passes.
- dark/light pass.
- RTL/LTR pass when applicable.
- large text/accessibility sanity passes.
- no P0 architecture boundary is violated.
- performance does not regress beyond established budget.
- failure/offline/degraded behavior is intentional.
- no unresolved user-reported regression remains.

## Bug-hunt protocol

Any user screenshot/video/report can immediately open a Strike incident.

Severity:
- S0: data loss/security.
- S1: reading blocked, navigation broken, crash, unusable contrast/accessibility.
- S2: wrong behavior, persistent state issue, major visual defect.
- S3: polish/consistency issue.

For S0/S1:
1. stop promotion/merge.
2. reproduce or statically isolate.
3. identify root cause, not cosmetic patch.
4. add regression evidence/test when possible.
5. verify adjacent surfaces for same class of bug.
6. run affected full gates.
7. physical-device recheck before GREEN.

## Shared backlog lanes

- P0 Reliability
- P0 Visual quality
- P0 Accessibility/RTL
- P0 Reader parity
- P1 Adaptive/large screen
- P1 Manga/Webtoon
- P1 Performance
- P2 Differentiation/AI/Fun

A visually broken P0 and a functionally broken P0 have equal authority to block release.

## Cadence

Daily:
- 10-minute shared defect/acceptance review.
- PXFE reports visual/user-flow blockers.
- Core reports reliability/performance/data blockers.
- one shared top-risk list.

Per feature:
- benchmark before build.
- contract before integration.
- visual and technical QA before GREEN.

Quarterly:
- refresh Apple/Android/Microsoft/reading-product standards under Eyad Studio Technology & Standards Refresh policy.
