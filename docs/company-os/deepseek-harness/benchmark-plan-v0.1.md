# DeepSeek Harness Pilot — Benchmark Plan v0.1

Parent: #84  
Workstream: #90  
Baseline: current Eyad Studio agent stack  
Candidate: pinned DeepSeek Harness pilot runtime

## Fair-comparison rules

Use the same:
- task definition
- input fixture
- model where possible
- tool permissions
- timeout
- verifier
- retry budget

Do not count model/agent self-reported DONE as success.

## Task cohorts

### B1 — Long-running software task

Stages:
plan → implement → test → inject failure → recover → resume → verify.

### B2 — Multi-agent task

Parent delegates two bounded subtasks, one child fails once, parent recovers.

### B3 — MCP/tool task

Approved MCP server:
- normal success
- timeout
- denied capability
- malformed response

### B4 — Session durability

Pause/restart/resume/replay/fork.

### B5 — Plugin failure

Test plugin crash/unload/conflict without unrelated session corruption.

### B6 — Browser interop

Harness requests browser work through external Browser Execution Router only.

## Primary metrics

- Verified Task Success
- median latency
- P95 latency
- token usage
- API cost
- tool calls
- recovery rate
- operator intervention

## Reliability/security metrics

- resume reliability
- replay reliability
- trace completeness
- policy violations
- plugin failure isolation
- crash recovery
- unauthorized capability attempts

## Decision rule

Primary score:

Verified Success / Cost / Latency

INTEGRATE requires:
- verified success within 2 percentage points of strongest baseline
- no critical security/policy failure
- demonstrated resume/replay reliability
- demonstrated plugin failure isolation
- external browser routing without core modification
- at least one material improvement in latency, cost, recovery, traceability, or operational simplicity

WATCH:
architecture works but churn/reliability/maintenance is not production-ready.

REJECT:
material reliability regression, isolation failure, policy bypass, or deep fork requirement.

## Minimum repetitions

For deterministic fixtures:
- target >= 10 runs per stack/fixture where cost permits.

For expensive model-driven fixtures:
- target >= 5 runs and report confidence limitations.

Never publish a percentage without raw denominator.
