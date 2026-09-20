# Veil Reader 0.10.0 — Release Candidate Notes

0.10.0 is the current pre-1.0 reader line. It keeps reading offline-first while hardening the Readium 3.4 boundary, persistent settings, adaptive layout behavior and release verification.

## Reader interaction candidate

- Readium Kotlin Toolkit 3.4.0 is the EPUB/PDF engine boundary.
- EPUB text size is passed to Readium as a ratio, where 1.0 means 100%, with Veil's supported range clamped before submission.
- Paper, sepia, dusk and OLED use deterministic page/text colors when publisher styling is disabled.
- Paginated EPUB reading supports a selectable Paper curl mode with Simple slide as the rollback-safe fallback.
- Continuous scroll remains independent of the page-turn style.
- PDF reader controls include explicit zoom in/out, slider, reset and fit-to-width actions.

## Settings and adaptive UI candidate

- Reader & app Settings is a secondary destination opened from Profile.
- App theme and persistent reader defaults share the existing settings store; no second preference database is introduced.
- Backup, restore and notebook export are consolidated under Settings → Data & backup.
- Privacy & about documents the local-first model, app version and reader engine.
- Adaptive navigation uses WindowSizeClass breakpoint checks rather than deprecated width/height size-class enums.

## Host-side verification

- Reading-policy regression suite: 39 checks.
- Performance parser suites: 4 / 5 / 6 checks.
- Unit tests, Android-test compilation, Android lint, debug APK assembly and benchmark APK assembly are part of the RC host gate.
## Remaining release gates

- Physical-device acceptance for page curl, PDF zoom, text/theme persistence and RTL/LTR navigation.
- Backup/restore durability on hardware plus large-book and large-PDF verification.
- TalkBack, large-text, short-landscape and expanded/tablet acceptance.
- Reproducible signed release artifact and Play signing/release review.
- Self-hosted CI registration or equivalent executed CI evidence where cloud jobs cannot allocate a runner.

Host-side success must not be reported as device acceptance. `MANUAL_QA.md` is the source of truth for runtime sign-off.