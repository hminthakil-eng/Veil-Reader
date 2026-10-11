# Grand Forge — Mandatory Work Start and Stage Exit Protocol (GOV-08)

Effective 2026-10-11 UTC. This document is an operational contract, NOT a source/device/product acceptance certificate.

Authority: [Master Roadmap](GRAND_FORGE_MASTER_ROADMAP.md), [Live Status](LIVE_STATUS.md), [Branch Policy](BRANCH_AND_ARCHIVE_POLICY.md), [Command Center #495](https://github.com/hminthakil-eng/Veil-Reader/issues/495).

## 1. BEFORE EVERY material work stage

The following applies to every new/resumed engineering, research, QA, product, design, tool, security, and release stage, including work by AI agents:

1. Read GitHub main SHA and default branch live. New draft commits do not supersede integrated main.
2. Read latest Master Roadmap, Live Status, Branch Policy and command-center issue #495. Compare new issue/PR status and workflow runs to the last report.
3. Reuse-first: search open/closed issues, merged/unmerged PRs, documents, test results, existing architecture and Company OS duplicate/reuse findings. Record REUSE / EXTEND / INTEGRATE / BUILD_NEW / HOLD / REJECT with rationale.
4. Identify user problem, exact current source and branch, ownership boundary, affected files, prior accepted capabilities, competing work, dependencies, severity, unknowns and blockers.
5. Define a bounded stage: ID, objective, steps, required automated tests, exact physical/UX acceptance criteria, expected files and output, rollback and next action.
6. Before editing code, post a PRE-WORK receipt to the open issue. Use Actions → Grand Forge Stage Receipts → Run workflow on main, action preflight, status PLANNED, with all required fields. Receipt workflow re-checks current main and roadmap files. If the workflow is unavailable, a trusted owner must post a manual preflight with the same marker, fields and reason; log this exception before edits.
7. Only then create a reversible branch from main, implement the smallest independent stage, preserve existing functionality, and submit the source for review. Each PR must include Grand Forge work issue and Grand Forge stage lines matching the receipt.
8. If main changes, or new evidence contradicts the plan, recheck the affected plan and record the changed decision before proceeding. Do not continue against assumptions known to be stale.

**Emergency exception:** containment of an active user-data loss or security defect may use an abbreviated preflight, but must record exact SHA, user impact, owner, containment step and rollback before changing code. Expand the plan after containment. No emergency patch is automatically release-ready.

## 2. AFTER EVERY stage — even a failed or rejected one

1. Compare plan to actual: files changed, verified features, preserved features, regressions and unfinished work.
2. Collect exact commit/source SHA, PR, specific workflow runs and outcome, logs, benchmark evidence, fixture hash, physical device/Android version and actual screenshot/video/audio when relevant. Missing evidence is UNKNOWN, not PASS.
3. Choose a stage outcome. Permitted receipt statuses: DONE_SOURCE (source slice complete with evidence), TEST_FAILED, BLOCKED, NEEDS_REWORK, REJECTED, SUPERSEDED. Do not declare DEVICE_VERIFIED / PRODUCT_ACCEPTED / RELEASED through this source receipt workflow.
4. Use the SAME stage ID, action stage_end, and publish an evidence-linked POST-STAGE receipt on the work issue. Tool refuses no-preflight, duplicate endings and evidence-free DONE_SOURCE.
5. Stage completion automatically adds a short link to #495. Living status document is then reconciled on verified changes (immediate when meaningful, otherwise in daily monitoring). Do not quietly overwrite history.
6. A new attempt requires a NEW stage identifier and new current-main preflight. The prior failed/rejected decision must remain accessible.
7. PR review checks real evidence before promoting to main. Mark source INTEGRATED only when reachable from main. Mark DEVICE_VERIFIED only after exact-build physical test; PRODUCT_ACCEPTED only after independent product/design/QA decision; RELEASED only after signed distribution evidence.
8. Do not erase open failed tests, merge a verification-only PR, archive an active dependent branch, or skip owner review because three CI jobs are green.

## 3. GitHub PR enforcement (new PRs)

Exact PR body lines:

    Grand Forge work issue: #408
    Grand Forge stage: RDR-01-W2

- The read-only Grand Forge Roadmap Preflight workflow checks new PRs targeting main.
- New PR preflight must have been posted by a trusted issue collaborator or GitHub Actions bot NO LATER than PR opening.
- Ready-for-review PR additionally requires matching post-stage DONE_SOURCE receipt with exact source evidence. A Draft only requires preflight.
- New PRs opened after 2026-10-11 09:32:42 UTC must use this protocol.
- Older PRs are labeled in check output LEGACY — MANUAL REVIEW REQUIRED; they are NOT deemed ready automatically.
- Validation-only/shadow PRs against a stacked branch require documented explicit reviewer exception and must not merge by accident.
- IMPORTANT: GitHub main branch protection is not configured yet. New checks can run, but **are not mandatory** until an authorized GitHub administrator sets branch protection / required status checks and blocks direct pushes. Our workflow cannot enforce steps done outside GitHub or independently certify truth.

## 4. Concrete acceptance matrix

| Case | Gate outcome |
|---|---|
| Valid PRE-WORK on main, open work issue, substantive plan | Receipt posted |
| Invalid issue or stale main SHA | Refuse |
| Repeated preflight for still-open stage | Refuse |
| New PR without linked work issue + stage | Fail |
| New PR with receipt created after PR opening | Fail |
| Ready PR without matching post-stage DONE_SOURCE | Fail |
| Stage marked DONE_SOURCE with no exact SHA/run link | Refuse |
| Stage ends without preflight or twice | Refuse |
| PRODUCT_ACCEPTED claimed via receipt workflow | Refuse |
| Physical Paper still slides despite green CI | BLOCKED / NEEDS_REWORK |
| Neural TTS Pause fails on phone despite unit pass | BLOCKED / NEEDS_REWORK |
| Legacy PR predates rollout | Warn, demand manual review |
| Direct push to unprotected main | KNOWN REMAINING RISK — require admin enforcement |

## 5. Minimum handoff and work ledger

Each stage has one pre-work receipt and one post-stage receipt, with:
- Work ID, stage ID, exact main SHA, author/owner discipline, date, PR/issue.
- User-visible problem, before capability, after capability and rejected alternatives.
- Reuse search, canonical owners, dependencies and planned scope.
- Exact tests, real-device/visual/audio evidence when claimed, results or missing evidence.
- Risk, rollback, continuation plan and explicit next owner/step.

The issue ledger is immediate append-only stage history. LIVE_STATUS.md is the concise reconciled summary; Grand Forge Master Roadmap is the current implementation priority and design/quality contract.

## 6. First execution waves

- GOV-08: add this protocol, root AGENTS instructions, additive PR template fields, stage receipts workflow, PR preflight workflow and regression tests. Initial pre-work recorded on issue #495. Complete by source-tested PR, not a product release.
- RDR-01 (#408): true optical physical Paper and independent open-source experiment #468/#479; user still rejects slide-looking Curl.
- TTS-01 (#480): real audible pause, voice stability, lockscreen and headset.
- DATA-01 (#409/#417): crash-safe semantic progress and true durability.
- UX-01 (#488): reader settings bottom sheet, actual text, 200% font, Persian, modal ownership.
- QA-01 (#464/#466): visual corpus, locale parity, signed build/device evidence.
- P1/P2: annotations, library scale, PDF, local neural speech, world systems and fonts in detailed Master Roadmap.

Do not treat source CI, GPU epochs, an APK compile, or a beautiful static mock as proof of a complete world-class reading app. No automatic release or branch deletion occurs through the governance workflows.
