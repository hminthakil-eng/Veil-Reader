# Agent Infrastructure Controlled Pilot Matrix v1

Date: 2026-09-18  
Related: #103 #104 #105 #106 #107 #109 #110 #111 #73 #84

## Utopia

Board state: **INTEGRATE FOR CONTROLLED PILOT**

Role:
candidate CompanyKnowledgeProvider.

Pilot data:
synthetic/public company-like data only.

Required tests:

- bitemporal fact update/history
- decision-ledger fidelity
- provenance retention
- permission/workspace boundaries
- MCP read boundary
- export/escape path
- database + data-directory backup
- restore from complete loss
- version upgrade on a cloned dataset
- failed-upgrade recovery
- ontology change and conflict handling

No production adoption while:
- migration is forward-only without proven recovery,
- schema churn remains unbounded,
- permission/MCP boundaries are unverified.

Decision:
INTEGRATE / WATCH / REJECT.

## ArcBox

Board state: **PILOT + WATCH PORTABILITY**

Role:
candidate ExecutionSandboxProvider implementation.

Current constraint:
agent sandboxes require supported Apple Silicon/macOS nested-virtualization environment.

Required tests under #107:

- no host mount by default
- network deny/allow behavior
- filesystem isolation
- process isolation
- resource limits
- TTL/destruction
- file ingress/egress
- snapshot/restore
- daemon/API authorization boundary
- crash cleanup
- integration with Harness and browser/MCP tasks

Company OS must retain at least one non-ArcBox portable path.

Decision:
INTEGRATE / WATCH / REJECT.

## Orca

Board state: **REUSE-PARTS / BENCHMARK**

Role:
candidate FleetWorkspaceProvider / AgentControlSurface.

Verified useful primitives:

- parallel agent terminals/worktrees
- multi-agent CLI compatibility
- remote/headless operation
- mobile companion
- diff review
- automation CLI
- persistent workspace/session surfaces

Do not assume:
- full autonomous coordinator
- portfolio scheduler
- automatic production merge authority

Required benchmark:

- worktree isolation
- concurrent-agent identity
- crash/reconnect recovery
- remote-host identity
- mobile device revocation
- relay/auth availability
- self-hosted/local-only operation
- version skew
- diff/result comparison
- safe handoff to Company OS approval and merge gates

Decision:
REUSE-PARTS / INTEGRATE / WATCH / REJECT.

## Project NOMAD

Board state: **RESEARCH + CONTROLLED PILOT**

Role:
candidate OfflineContinuityNodeProvider / Business Continuity Appliance.

Pilot pin:
- stable release target: v1.34.1
- release commit: f8e40fbd01f4ce0e6bc4cc3e37167b4bf4065028
- Apache-2.0

Primary value under test:
- offline service bundle
- Kiwix / static reference access
- local LLM access
- local RAG
- LAN/browser access
- continuity during internet loss

Do not assume:
- enterprise authentication
- secure Docker-host isolation
- authoritative Company Knowledge ownership
- safe bidirectional resync after reconnect
- production-ready backup/restore semantics

Required tests:
- online → offline → online scenario
- local AI and RAG quality
- Kiwix/content availability
- phone/LAN usability
- hardware/RAM/storage/power
- backup and clean-machine restore
- stale-data/conflict behavior
- controlled resync with Utopia exports
- Docker socket/host-FS trust review
- update/supply-chain behavior
- comparison against Open WebUI + Ollama, AnythingLLM and standalone Kiwix/RAG components

Decision:
ADOPT / INTEGRATE / REUSE-PARTS / WATCH / REJECT.

Canonical work:
#109 #110 #111.

## HumanLayer Skills

Board state: **SELECTIVE REUSE**

Route:
Public Skill → Registry intake → static scan → license/provenance → dedup → quality/security tests → approved collection.

Current verified candidate names:

- build-iterated-agentic-loop
- design-control-loop
- improve-claude-md
- show-me
- narrow-react-prop-types

Priority:
audit the first three for overlap with existing Company OS / coding-agent workflows.

Mass installation is prohibited.

## Anthropic Commerce Agents

Board state: **ARCHITECTURE PATTERN REUSE**

Role:
reference implementation only; not a Company OS runtime dependency by default.

Patterns to extract:

- untrusted third-party content fencing
- provenance-aware write gates
- staged changes
- host approval
- server-side authorization/business rules
- capped/budgeted delegation
- separation of model text from enforceable tool policy

Deployment/auth/business rules remain Eyad-owned.

## Cross-cutting exit gate

No provider reaches production until:

- owner-layer boundary passes,
- Work Reuse Gate is satisfied,
- security review passes,
- data/secret scope is approved,
- rollback/kill switch works,
- verification is independent,
- current provider/version is pinned,
- operational dependency risk is documented.
