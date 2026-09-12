---
name: veil-release-guardian
description: Audit Veil Reader changes and release candidates against CI, migrations, backup/restore safety, accessibility, physical-device QA, privacy, and the repository's world-class release gates.
---

# Veil Release Guardian

Use this skill for release readiness, PR review, regression analysis, CI failures, QA planning, or deciding whether Veil Reader is safe to ship.

## Review order

1. Read `WORLD_CLASS_RELEASE_GATES.md`, `MANUAL_QA.md`, the latest release notes, and any touched engineering docs.
2. Inspect changed files before forming conclusions. Rank findings by user impact and reversibility.
3. Check these risk areas explicitly:
   - data loss or corrupt restore paths;
   - Room schema/migration mismatches;
   - reading-position regressions;
   - EPUB/PDF feature asymmetry introduced unintentionally;
   - lifecycle/session accounting errors;
   - accessibility regressions;
   - backup privacy or archive traversal/size-validation weaknesses;
   - dependency or permission expansion;
   - CI coverage gaps.
4. For persistence changes, require migration and backup/restore coverage. Never accept destructive migration as a shortcut unless the task explicitly authorizes data loss.
5. For UI or interaction changes, require physical-device verification where emulator/unit coverage is insufficient.
6. Distinguish blockers from follow-ups. Do not call something production-ready while a documented release gate remains open.

## Required checks

Baseline:

```sh
sh tools/test-policy.sh
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Storage-sensitive:

```sh
gradle :app:connectedDebugAndroidTest
```

Also inspect GitHub Actions results when available.

## Output format

Report:

- **Verdict:** ready / ready with follow-ups / not ready.
- **Blockers:** only issues that must be fixed before release.
- **Important findings:** correctness, UX, accessibility, privacy, or maintainability risks.
- **Verification evidence:** tests, CI, manual checks, or missing evidence.
- **Next action:** the smallest concrete step that moves the release forward.

Do not inflate minor style issues into blockers.
