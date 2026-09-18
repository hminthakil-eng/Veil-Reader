# Eyad Studio Company OS — Mobile Command Layer Pilot

Status: APPROVED / PILOT
Pilot project: Veil Reader
Operating model: Phone-first, local-first, cloud-fallback

## Objective

Make the phone the primary control surface for Eyad Studio while keeping heavy development, testing, AI inference, automation, and rendering on trusted computers or self-hosted runners.

## Target architecture

```text
Phone / PWA
  |
  | HTTPS / authenticated command API
  v
Mobile Command Gateway
  |
  +--> Project Registry
  +--> Job Queue
  +--> Approval Gates
  +--> Audit Log
  |
  v
Execution Router
  |
  +--> GitHub Actions self-hosted runner
  +--> Local coding agent (OpenCode/Cline class)
  +--> Local LLM runtime (Ollama/llama.cpp class)
  +--> Cloud fallback only when policy allows
  |
  v
Project workers
  +--> Veil Reader
  +--> Manga Production
  +--> Video Connector
```

## Design rules

1. Phone is a control plane, not the compute plane.
2. Default execution is local/self-hosted where practical.
3. Cloud providers are fallback paths, not the primary dependency.
4. Every consequential action has an explicit approval gate.
5. No secrets are stored in the mobile UI or committed to Git.
6. Workers run with least privilege and project-scoped credentials.
7. Public inbound access to local machines is forbidden by default; use authenticated private networking or a hardened gateway.
8. Every command produces an audit record with actor, project, action, status, timestamps, and result links.
9. Project jobs must be idempotent where practical and safe to retry.
10. Veil Reader is the first vertical slice; other projects connect through the same interfaces later.

## Phase 0 — Foundation

Deliver:
- Project registry
- Command schema
- Job states
- Approval model
- Audit event schema
- Mobile-first dashboard wire contract

Canonical job states:
`queued -> awaiting_approval -> running -> succeeded | failed | cancelled`

Initial commands:
- project.status
- test.run
- build.run
- qa.run
- issue.create
- pr.prepare
- agent.task

## Phase 1 — Veil Reader vertical slice

A phone user must be able to:
1. See current Veil Reader health.
2. Start a test job.
3. See live/refreshable status.
4. Read the final result.
5. Open the related GitHub run/issue/PR.
6. Approve a gated agent task.
7. Cancel a queued/running job when supported.

## Phase 2 — Local AI execution

Add an execution adapter for a local model runtime.

Policy:
- Local model first for routine coding/review tasks.
- Escalate only when task complexity or quality gates require it.
- Provider quotas must never be treated as a single point of failure.
- API keys remain server-side.

## Phase 3 — Multi-project Company OS

Connect:
- Manga Production Director
- Google Video Connector
- Research/SID jobs
- Future Eyad Studio projects

All projects use the same command/event interfaces.

## Security baseline

- No direct unauthenticated port exposure from a home PC.
- Separate service account/token per integration where possible.
- Read-only by default; elevate only for specific operations.
- Destructive actions require explicit confirmation.
- Runner workspace isolation between projects.
- Secrets injected at runtime.
- Audit logs must not contain raw secrets.
- Remote code execution agents run inside an isolated environment when possible.

## Mobile UX contract

Primary screen:

```text
EYAD STUDIO
------------
Veil Reader        GREEN
Manga Production   GREEN
Video Connector    YELLOW
Local AI           GREEN
CI / Runner        GREEN

[Continue Development]
[Run Tests]
[Run QA]
[Research]
[Prepare PR]
[Company Report]
```

Each action must show:
- what will run
- where it will run
- whether approval is required
- current state
- result or failure reason

## Definition of Done for Pilot

The pilot is complete when, from a phone, the owner can trigger a Veil Reader test workflow on a trusted runner, observe status, receive the result, and open the resulting GitHub artifact without using the desktop UI.

## Non-goals for Pilot

- Building a full custom IDE on mobile
- Replacing GitHub
- Replacing all cloud models
- Exposing a local machine directly to the public internet
- Auto-merging production changes without approval

## Migration

This pilot lives under Veil Reader only because no dedicated Company OS repository is currently connected. The content is intentionally project-agnostic and should be moved into a dedicated Eyad Studio Company OS repository once available.
