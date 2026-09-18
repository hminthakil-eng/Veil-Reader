# DeepSeek Harness Pilot — Runtime & Session Reliability Plan v0.1

Parent: #84  
Workstream: #89  
Prerequisite: #88 security baseline must be verified in a real disposable environment.

## Objective

Determine whether DeepSeek Harness can operate as a reliable execution substrate under Company OS without owning governance or browser policy.

## Required fixtures

### R1 — Session lifecycle
1. create session
2. execute deterministic task
3. checkpoint
4. stop runtime
5. restart
6. resume
7. independently verify state

PASS: no lost committed event and no duplicated side effect.

### R2 — Replay
Replay an immutable session history into a clean runtime.

PASS: trace lineage and final derived state match expected fixture.

### R3 — Fork
Fork from a defined committed point and continue two branches.

PASS: child branches do not contaminate each other or parent history.

### R4 — Crash recovery
Terminate runtime during:
- model wait
- tool call
- checkpoint boundary

PASS: recovery behavior is explicit, auditable, and does not silently duplicate side effects.

### R5 — Stale state
Inject stale cached/projection state.

PASS: committed event log remains authoritative or conflict is surfaced; stale state does not overwrite newer committed state.

### R6 — Subagent lineage
Parent spawns child, child returns result, parent continues.

Record:
- parent session id
- child id
- provider
- task
- terminal state
- retry count
- trace linkage

### R7 — Child failure
Child fails or times out.

PASS:
- parent receives explicit failure
- bounded retry policy applies
- no false success
- trace remains complete

### R8 — Plugin failure isolation
Crash/unload a test-scoped plugin.

PASS:
- unrelated session state remains valid
- failure is visible
- runtime can continue or fail closed predictably

### R9 — Tool timeout
Approved tool exceeds deadline.

PASS:
- cancellation/timeout propagates
- session records the event
- no hung hidden process remains

## Metrics

- resume success rate
- replay success rate
- fork isolation success
- crash recovery success
- duplicate-side-effect count
- stale-state defects
- subagent lineage completeness
- operator intervention
- mean recovery time

## Stop conditions

Immediately stop testing on:
- sandbox escape
- unexpected host access
- secret access
- uncontrolled network access
- cross-session data leakage
- silent privilege escalation

## Exit decision

PASS / CONDITIONAL / HOLD / REJECT
