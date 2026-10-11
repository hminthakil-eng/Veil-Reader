## Mandatory Grand Forge stage evidence (new work after 2026-10-11)

Grand Forge work issue: #
Grand Forge stage: 

- [ ] Current main SHA, roadmap, Live Status, archive policy and #495 checked BEFORE work
- [ ] Company OS reuse/duplicate check and canonical owner recorded on work issue
- [ ] PRE-WORK receipt posted on the linked issue before this PR was opened
- [ ] Stage tests/acceptance/rollback defined before code changes
- [ ] POST-STAGE receipt posted with exact source evidence before marking Ready for Review
- [ ] Physical / product / release gates kept separate from source tests

**Receipt links (preflight / completion):**

**Plan deviation, rejected approaches and remaining blockers:**

See [stage execution protocol](../docs/roadmap/GRAND_FORGE_EXECUTION_PROTOCOL.md). A receipt is a provenance gate, not proof that the app is ready.

---

## User problem

<!-- What user-visible problem does this solve? Do not describe only the code change. -->

## Before status

- [ ] RED
- [ ] YELLOW
- [ ] GREEN-SOURCE
- [ ] GREEN-DEVICE
- [ ] GREEN-PRODUCT

## Scope

<!-- What is intentionally included and excluded? -->

## Ownership / architecture impact

- [ ] Reader navigation/input ownership changed
- [ ] Persistence/schema changed
- [ ] Import/export/backup changed
- [ ] Network/sync behavior changed
- [ ] Security/privacy behavior changed
- [ ] Release/package behavior changed
- [ ] None of the above

If checked, explain:

## Reforge GREEN gates

### Functional
- [ ] Primary path works
- [ ] Cancel/back/retry is defined
- [ ] No dead/fake/placeholder control is presented as finished

### State / resilience
- [ ] Rotation/window change considered
- [ ] Background/process recreation considered
- [ ] Async ownership/cancellation considered

### Failure states
- [ ] Loading
- [ ] Empty
- [ ] Error
- [ ] Retry/recovery where applicable

### UI/UX
- [ ] Primary action hierarchy is correct
- [ ] Spacing/composition reviewed
- [ ] No unnecessary decoration blocks the task
- [ ] Pressed/selected/focused/disabled states reviewed

### Accessibility / localization
- [ ] >=48dp essential targets
- [ ] TalkBack semantics/focus
- [ ] Persian/RTL
- [ ] Mixed script
- [ ] 200% text
- [ ] High Contrast
- [ ] Reduced Motion

### Adaptive
- [ ] Compact portrait
- [ ] Short landscape
- [ ] Expanded/tablet/foldable where applicable
- [ ] IME/keyboard where applicable

### Performance / memory
- [ ] No main-thread heavy work introduced
- [ ] Memory impact reviewed
- [ ] Benchmark/profiling evidence added where applicable

## Exact evidence

Executable SHA:

Automated runs:

Rendered evidence:

Physical device / OS:

Performance evidence:

## Remaining YELLOW items

<!-- Be explicit. Empty means you are claiming the applicable gates are complete. -->

## Final status after this PR

- [ ] RED
- [ ] YELLOW
- [ ] GREEN-SOURCE
- [ ] GREEN-DEVICE
- [ ] GREEN-PRODUCT

## Arena rejection check

- [ ] No duplicate architecture
- [ ] No test/coverage removal to force green
- [ ] No unsupported feature claim
- [ ] No emulator/source evidence mislabeled as physical-device proof
- [ ] No local-first regression
