# Eyad Studio Company OS — Offline Continuity Node Architecture v1

Status: Controlled Pilot Architecture
Parent: #109
Canonical Challenges: #110 #111
Date: 2026-09-18

## Purpose

Provide a local/offline business-continuity capability that can preserve access to approved models, documents, reference knowledge and selected tools during loss of internet connectivity.

This capability is not a replacement for Company OS, Utopia, primary cloud systems, or normal production infrastructure.

## Owner rule

**Company OS owns the ContinuityNodeProvider contract.**

Project N.O.M.A.D. is one candidate implementation.
A modular stack is the required comparison baseline.

## Layer boundaries

Company OS — Control Plane
→ ContinuityNodeProvider
→ Offline appliance implementation
→ Local Model Provider
→ Local RAG / Vector Store
→ Offline reference services
→ Local browser-accessible tools
→ Backup / Restore / Resync adapters
→ Verification

Separate owner layers remain:

- Utopia / CompanyKnowledgeProvider: historical truth, provenance, ontology, decision memory
- DeepSeek Harness / AgentRuntimeProvider: agents, sessions, subagents
- ExecutionSandboxProvider: isolated untrusted execution
- Browser Execution Router: browser backend selection
- Prompt & Skill Registry: approved skills/prompts
- Verification Layer: success authority

## Data classes

### Tier A — Safe to preload during pilot

- synthetic/public documents
- public standards/docs
- public Kiwix/ZIM content
- test prompts/skills
- disposable models
- benchmark fixtures

### Tier B — Future production candidate only after approval

- approved company runbooks
- approved project documentation
- approved read-only decision snapshots
- approved registry subsets

### Tier C — prohibited during pilot

- real credentials
- production secrets
- customer/confidential data
- private source code unless separately approved later
- production signing keys
- unrestricted backups of live systems

## Synchronization model

Pilot v1 uses **asymmetric synchronization by default**.

Primary systems → Continuity Node:
- versioned export
- snapshot
- checksum/provenance
- read-only import when possible

Continuity Node → Primary systems:
- disabled by default
- explicit review/import after connectivity restoration

No automatic bidirectional merge is authorized until:
- conflict semantics are defined,
- source-of-truth ownership is explicit,
- replay/idempotency is tested,
- data-loss tests pass.

## Mobile access

Phone access is allowed only through:
- trusted local network, or
- Company-approved VPN/private overlay, or
- authenticated reverse proxy inside approved network boundary.

Direct public internet exposure is prohibited.

## Supply-chain policy

For controlled pilot:
- stable release pin
- image tag/digest inventory
- no rolling auto-update during benchmark
- record Docker image digests
- record installer commit/tag
- review root/sudo actions
- review Docker socket consumers
- review host mounts
- backup before update
- rollback path required

## Backup contract

A valid continuity node backup must account for:

- management database
- configuration
- vector store snapshots
- local documents
- Kiwix/ZIM content indexes
- notes
- downloaded model inventory/configuration
- maps/reference packs
- service metadata
- exact software/image versions

Large immutable content/models may be reproducible from a manifest instead of duplicated into every backup, but restoration must be tested while offline.

## Recovery targets

Pilot records:

- RPO: amount of test data lost after simulated failure
- RTO: time from failure to verified service restoration
- RAG recovery correctness
- model availability
- mobile-access restoration
- checksum/provenance validity

## Build-new gate

Do not build an Eyad-specific offline appliance until #109 proves that neither N.O.M.A.D. nor a modular reusable stack meets the contract with acceptable integration effort.
