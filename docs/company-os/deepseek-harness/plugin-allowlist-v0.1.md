# DeepSeek Harness Pilot — Plugin Allowlist Policy v0.1

Status: Initial policy  
Parent: #84  
Related: #87, #88, #89

## Default

**DENY ALL → ALLOWLIST EXPLICITLY**

No package is allowed merely because it ships in upstream.

## Allowlist record

Every enabled plugin/package requires:

- canonical package name
- exact version/commit
- role
- dependency set
- filesystem capability
- network capability
- subprocess/shell capability
- credential access
- session/storage effects
- telemetry behavior
- rollback/unload procedure
- reviewer

## Initial categories

### Core runtime — CONDITIONAL ALLOW

Only the minimum runtime composition needed to boot the pilot.

### Session persistence/query — CONDITIONAL ALLOW

Purpose:

- resume
- replay
- fork
- durability tests

Must remain inside pilot storage paths.

### Subagent providers — TEST-SCOPED

Enable one at a time for a named fixture.

No blanket multi-provider activation.

### MCP — TEST-SCOPED

Each server requires separate approval.

### Browser/computer-use plugins — NOT CORE-ALLOWED

Browser policy belongs to Company OS Browser Execution Router.

Harness may use an adapter only.

### Dynamic Cordis packages — DENY

No model-authored package generation/execution during Pilot v0.1.

### Shell/subprocess — RESTRICTED

Only benchmark-defined commands inside isolated workspace.

No arbitrary package install or host access.

## Change control

A new plugin cannot be added during a benchmark run.

Any change to the allowlist:

1. stops the run,
2. records the change,
3. triggers review,
4. starts a new benchmark cohort/version.

## Rollback

Plugin unload/disable must be tested before it is considered pilot-approved.

Failure to unload cleanly is recorded as a reliability defect.
