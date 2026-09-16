# Eyad Studio — Headroom Pilot for Veil Reader

Status: approved pilot only. Do not merge into `main` until acceptance gates pass.

## Goal

Evaluate Headroom as a local context-compression layer for Codex while developing Veil Reader. The pilot must reduce context/token use without lowering code quality, test reliability, or agent task completion.

## Safety rules

- Local/private-first. Do not add cloud memory or third-party telemetry for this pilot.
- Do not add API keys, OAuth tokens, secrets, or credentials to this repository.
- Do not change Veil Reader application code merely to enable Headroom.
- Use Headroom only as a developer-side tool around Codex.
- Skip optional Serena installation during the pilot (`--code-memory none`) to minimize persistent machine changes.
- Keep rollback available at all times with `headroom unwrap codex`.
- No production/release workflow changes until the pilot is accepted.

## Pilot procedure

Run from the Veil Reader repository root on the authorized Windows development PC.

### 1. Preflight

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\headroom-pilot.ps1 -Mode Check
```

### 2. Install Headroom

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\headroom-pilot.ps1 -Mode Install
```

### 3. Baseline task without Headroom

Run Codex normally and use this fixed, read-only evaluation task:

> Inspect the Veil Reader codebase and produce a concise architecture map covering modules, data flow, persistence, reading UI, tests, and the three highest-risk technical areas. Do not edit files.

Record approximate context/token consumption and whether the answer is correct and complete.

### 4. Headroom task

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\headroom-pilot.ps1 -Mode Run
```

In the wrapped Codex session, run the same fixed evaluation task.

### 5. Measure

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\headroom-pilot.ps1 -Mode Stats
```

Compare baseline vs. Headroom on:

- total context/token reduction;
- correctness and completeness;
- latency;
- tool-call failures;
- missed files or architectural details;
- any unexpected persistent configuration changes.

## Acceptance gates

Approve Headroom for normal Veil Reader development only when all are true:

1. No secrets leave the expected model/provider path because of Headroom.
2. The fixed architecture task is materially equivalent in correctness and coverage.
3. No test/build regression is introduced.
4. No unexpected user-level tooling remains installed or configured.
5. Context reduction is useful enough to justify the extra layer. A practical target is at least 15% on codebase exploration; lower savings are acceptable only if long-session stability improves measurably.
6. `headroom unwrap codex` restores normal Codex behavior.

## Rollback

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\headroom-pilot.ps1 -Mode Rollback
```

After rollback, rerun Codex normally and confirm Veil Reader is unaffected.

## Next stage

Only after this pilot passes: evaluate `claude-mem` in local/private-first mode. Task Observer remains review-only, and OmniRoute remains isolated from primary credentials until separately approved.
