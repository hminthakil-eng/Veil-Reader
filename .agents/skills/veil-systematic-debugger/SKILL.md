---
name: veil-systematic-debugger
description: Root-cause debugging workflow for Veil Reader. Use for crashes, regressions, flaky behavior, state/lifecycle bugs, reader issues, persistence failures, migration problems, CI failures, or any unexpected behavior. Reproduce first, form ranked hypotheses, test one variable at a time, make the smallest causal fix, and add regression coverage.
license: MIT-compatible adaptation
metadata:
  inspiration: Desquared/agents-rules-skills shared-bug-investigation
---

# Veil Systematic Debugger

Debug by evidence, not by patch accumulation.

## Workflow

### 1. Define the failure
Capture:
- expected behavior;
- actual behavior;
- exact error/stack trace/log signal;
- reproducibility;
- environment or device conditions;
- most recent relevant change when known.

For reader problems, also identify EPUB vs PDF, current reading position/restoration state, process/configuration changes, and whether the issue happens after backgrounding.

### 2. Reproduce before fixing
Prefer the smallest reliable reproduction. If reproduction is not possible, gather enough evidence to narrow the state space before editing code.

### 3. Form ranked hypotheses
Write 2–4 falsifiable causes, highest probability first. For each hypothesis define the observation that would confirm or reject it.

Common Veil Reader categories:
- lifecycle/process restoration;
- stale Flow/UI state;
- coroutine cancellation or race;
- Room transaction/migration/schema mismatch;
- backup/archive validation;
- Readium lifecycle or publication ownership;
- EPUB/PDF capability mismatch;
- duplicate import/fingerprint logic;
- recomposition/state identity;
- filesystem/URI permission handling.

### 4. Isolate one variable
Use logs, tests, git diff/bisect, focused instrumentation, minimal reproduction, or a working-vs-broken comparison. Do not change several plausible causes at once.

### 5. State the root cause
Before the final fix, be able to express the causal chain in one or two sentences: **what state/condition was wrong, why it became wrong, and why that produced the observed failure**.

### 6. Make the narrowest causal fix
Avoid shotgun null checks, retries, delays, broad exception swallowing, state resets, or destructive migration unless they address the proven cause.

### 7. Prove the fix
- reproduce the failure on the old behavior when practical;
- verify the targeted fix;
- test the nearest edge cases;
- add a regression test at the lowest useful layer;
- run the relevant repository verification.

## Stop conditions

Do not call the bug fixed if:
- the failure merely stopped appearing once;
- an exception is swallowed but data/state remains wrong;
- tests pass only because coverage bypasses the real path;
- a delay/retry hides a race without establishing ordering;
- user data can still be lost or corrupted.

## Output discipline

Report only:
1. root cause;
2. fix;
3. regression test/evidence;
4. remaining uncertainty, if any.

This workflow adapts the scientific-method debugging approach from the MIT-licensed Desquared `shared-bug-investigation` skill to Veil Reader's Android/offline-reader risks.
