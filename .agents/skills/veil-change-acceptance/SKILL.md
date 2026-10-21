---
name: veil-change-acceptance
description: Use when proposing, implementing, reviewing, or approving a Veil Reader feature, bugfix, refactor, or pull request, especially when scope creep, regressions, speculative architecture, or unverified completion are risks.
---

# Veil Change Acceptance

Apply four disciplined checks alongside existing engineering and Reforge rules. This is a lightweight decision gate, not a new build system or replacement for specialist skills.

## 1. Think before changing

- Identify the observable user problem, current behavior, desired outcome, and evidence/source SHA.
- Locate the existing owner and reuse paths before proposing code, dependencies, or agents.
- State assumptions; clarify only consequential unknowns. For routine low-risk details choose a reversible default and document it.
- Record invariants: reading position, notes, bookmarks, imported files, backups, privacy/offline behavior, accessibility, and reader-mode separation.

## 2. Simplicity first

- Choose the smallest coherent design that solves the actual problem.
- Reject speculative layers, duplicate state ownership, needless dependencies, and features outside the requested scope.
- Reuse `veil-world-class-app-development`, `systematic-debugging`, `test-driven-development`, and subsystem skills as relevant.

## 3. Surgical execution

- Write included/excluded paths and a narrow diff plan before edits; preserve unrelated work.
- Keep PAGED, PAPER/CURL, SLIDE, and SCROLL behavior independent when touching navigation.
- Add or update regression coverage for the failure or user-visible behavior under change. Do not delete tests or loosen gates to achieve a pass.
- Keep changes reversible; use feature switches for high-risk experiments and protect durable data.

## 4. Goal-based verification

- Define pass/fail evidence **before** implementation. Verify the user journey, not only compilation.
- Run `sh tools/test-policy.sh` and relevant tests/lint/build per `README.md`; add storage instrumentation or device/visual checks when required.
- Compare actual outcome against original acceptance criteria; repeat diagnose → minimal fix → re-test until proven or explicitly blocked.
- Record the exact tested commit SHA, commands/results, screenshots/videos, hardware when relevant, and remaining limitations. Never invent measurements.
- Respect `docs/governance/REFORGE_GREEN_CONTRACT.md`: CI is not device proof; unfinished user-visible work remains YELLOW/RED, not GREEN-PRODUCT.

## Failure and proportionality rules

- **Do not fake progress:** if original behavior remains broken, evidence is missing, or an applicable check fails, record `HOLD` or `FAIL` and name the blocker instead of silently weakening acceptance.
- **High-risk changes:** never trade away reader data, migration safety, privacy, accessibility, or independent page-turn modes for speed or aesthetics. Stop unsafe proposals and offer a reversible alternative.
- **Concurrent work:** resolve current GitHub SHA before publishing; preserve other agents' changes and do not overwrite drift or silently force-push.
- **Proportionate review:** for trivial documentation edits, use document-level validation; do not require device testing unrelated to the change. For user-visible UI/reader behavior, require applicable real-device proof.
- **Human-controlled release:** an agent's `PASS` is a review input, not automatic permission to merge, deploy, sign, or call work `GREEN-PRODUCT`.
- **Evaluate instructions:** consult `docs/quality/VEIL_CHANGE_ACCEPTANCE_PRESSURE_MATRIX_2026-10-21.md`. Pressure-case status is `NOT RUN` until actual agent tool traces have been evaluated.

## PR acceptance record

A proposed change is **reviewable** when its description records:

1. Problem and observable acceptance criterion.
2. Current ownership, assumptions, and intentional non-goals.
3. Narrow change summary, rollback/feature-switch strategy if risk warrants.
4. Fresh verification evidence (exact SHA) and unresolved gaps.

**Review result:** `PASS` only when all four discipline checks have evidence. Otherwise `HOLD` with the missing evidence. This gate never overrides release, security, accessibility, or physical-device requirements.
