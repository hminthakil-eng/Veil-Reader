# Eyad Studio Company OS — Agent Infrastructure Owner Layers v1

Status: Board-approved architecture baseline  
Parent: #106  
Date: 2026-09-18

## Principle

**One capability = one primary Owner Layer.**

External projects are implementations, adapters, references, or providers beneath the owner contract. They do not become independent control planes unless the Board explicitly changes ownership.

## Owner matrix

| Capability | Primary Owner | Candidate implementation/provider | Hard boundary |
| --- | --- | --- | --- |
| Governance, policy, approval | Eyad Company OS | internal policy layer; commerce-agent patterns as reference | provider cannot approve itself |
| Human approval for consequential actions | Eyad Company OS | approval gateway inspired by Anthropic Commerce Agents | chat text alone is not approval |
| Portfolio/project orchestration | Eyad Company OS | internal orchestration contract | external runtime cannot reprioritize portfolio |
| Agent dispatch authority | Eyad Company OS | provider adapters | dispatch policy remains above provider |
| Fleet workspace / agent control surface | FleetWorkspaceProvider contract | Orca candidate | Orca is not Company OS control plane |
| Agent runtime / sessions / subagents | AgentRuntimeProvider contract | DeepSeek Harness candidate | runtime cannot own company governance |
| Prompt / Skill governance | Eyad Prompt & Skill Registry | approved HumanLayer/public skills | no direct public skill execution |
| Company knowledge / temporal memory | CompanyKnowledgeProvider contract | Utopia candidate | provider does not own truth/policy semantics |
| Offline business-continuity appliance / local fallback | OfflineContinuityNodeProvider contract | Project NOMAD candidate | node is not authoritative company memory and must remain replaceable |
| Execution isolation | ExecutionSandboxProvider contract | ArcBox + portable baseline providers | no single-provider lock-in |
| Browser backend selection | Browser Execution Router | Eyad router | browser backend does not choose policy |
| DOM fast path | browser backend | Jev candidate | fast path must fall back safely |
| Structured browser automation | browser backend | Playwright | verification stays external |
| Browser agent / computer use | browser backend | Browser Use / agent-browser / Vision | not authoritative completion signal |
| Verification / trace / QA | Eyad Verification Layer | internal verification contracts | agent DONE is not success |
| Challenge / risk / research governance | Company OS + SID + RIU | Challenge Registry / Risk Radar | project cannot hide material challenge |
| Decision history | Company OS Decision Log | may be persisted in Utopia provider | storage provider cannot rewrite decision authority |

## Consequential-action pattern

For deployments, publishing, spending, credential changes, destructive operations, and production mutations:

Agent proposes
→ Policy validates
→ Human / authorized approval surface approves
→ System executes
→ Independent verification checks outcome
→ Outcome and provenance are recorded

A natural-language statement from the model is never sufficient proof of approval or successful execution.

## Provider replacement rule

Every provider contract must define:

- version pinning
- capability declaration
- health check
- failure classification
- data export/escape path
- kill switch
- rollback/disable path
- trace/audit handoff
- least-privilege scope
- migration ownership

If a provider cannot be replaced without changing Company OS policy semantics, the boundary is wrong.

## Prohibited ownership leaks

### Utopia must not own

- project priority
- approval decisions
- policy authority
- irreversible exclusive data formats without export
- agent execution routing

### Project N.O.M.A.D. / ContinuityNodeProvider must not own

- Company Knowledge historical truth
- project priority or governance
- canonical decision history
- unrestricted LAN/public access policy
- production secret authority
- bidirectional synchronization semantics unless Company OS explicitly defines them

### ArcBox must not own

- agent governance
- sandbox policy semantics for the whole company
- provider selection
- production secret management

### Orca must not own

- portfolio scheduling
- final merge approval
- production deployment authority
- Company OS policy
- global agent identity or secret authority

### Project NOMAD / Offline Continuity Node must not own

- authoritative Company Knowledge history
- ontology/provenance semantics
- Company OS governance or approval
- production secret authority
- secure execution isolation policy
- bidirectional resync policy without explicit reconciliation rules

### DeepSeek Harness must not own

- Company OS governance
- browser backend policy
- challenge/risk policy
- production approval

### Public skills must not own

- project architecture
- shell/network/secret privileges by default
- immutable Company OS instructions

## Build-new gate

No equivalent internal subsystem may be built from scratch while an approved reusable provider can satisfy the owner contract.

A new internal build requires:

1. valid Work Reuse Gate receipt,
2. documented provider gap,
3. quantified impact,
4. SID recommendation,
5. rollback plan,
6. Board/project approval when architecture ownership changes.

## Linked canonical work

- #72 Company AI Gateway evaluation
- #73 Prompt & Skill Registry pilot
- #81 Jev browser fast-path pilot
- #84 DeepSeek Harness runtime pilot
- #88 Harness sandbox baseline
- #103 Utopia maturity challenge
- #104 ArcBox portability/security challenge
- #105 Orca remote/mobile challenge
- #107 ExecutionSandboxProvider benchmark
- #109 Project N.O.M.A.D. controlled pilot
- #110 N.O.M.A.D. auth/Docker/update trust-boundary challenge
- #111 Offline backup/restore/resync challenge
- #109 Project NOMAD Offline Company Continuity Node pilot
- #110 NOMAD auth / Docker-socket / update trust challenge
- #111 offline backup / restore / resync challenge
