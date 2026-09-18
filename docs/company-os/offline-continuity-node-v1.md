# Offline Company Continuity Node — Architecture & Pilot Contract v1

Date: 2026-09-18
Parent: #109
Challenges: #110 #111

## Purpose

Provide a degraded-but-useful Company OS mode when external internet/cloud services are unavailable.

The continuity node is an **availability layer**, not a second control plane and not the authoritative Company Knowledge store.

## Owner boundary

Company OS owns:
- continuity policy
- activation/deactivation
- data classification
- approved content/export set
- reconciliation rules
- verification
- recovery decision

CompanyKnowledgeProvider / Utopia owns:
- authoritative temporal knowledge
- ontology/provenance
- decision/history semantics

OfflineContinuityNodeProvider owns:
- serving an approved local snapshot/bundle
- local reference content
- local model access
- local RAG over approved replicated content
- local scratch artifacts during outage
- continuity health/status

AgentRuntimeProvider / DeepSeek Harness owns:
- agent execution/session behavior when available locally

ExecutionSandboxProvider owns:
- untrusted code/tool isolation

## Data model

Separate data into three classes.

### A. Rehydratable static assets

Examples:
- Kiwix ZIM files
- model weights
- public maps
- public manuals
- package/container images

Prefer manifest + checksum + re-download/reseed over full backup when practical.

### B. Authoritative replicated company snapshot

Examples:
- approved exported Company Knowledge subset
- runbooks
- decision summaries
- architecture docs
- selected Prompt/Skill Registry content

This copy is read-mostly and carries:
- source version
- export timestamp
- provenance
- expiry/freshness indicator

It MUST NOT overwrite the authoritative online store automatically.

### C. Offline-created scratch state

Examples:
- outage notes
- draft incident reports
- local task results
- new documents created during disconnect

On reconnection this becomes a reconciliation queue.

No automatic last-write-wins merge into Company Knowledge.

## Reconnect protocol

1. detect network restoration,
2. freeze automatic writeback,
3. refresh provider health,
4. compare authoritative source version,
5. classify local changes,
6. reconcile conflicts explicitly,
7. verify imported results,
8. refresh static content,
9. record continuity event and outcome.

## Security minimum

For Project NOMAD pilot:
- isolated host/test LAN
- no sensitive company data
- no production secrets
- no internet-exposed Command Center
- no co-location with sensitive Docker workloads
- firewall allowlist
- exact version pinning
- audit Docker socket consumers
- audit host filesystem mounts
- disable unnecessary services
- stage and verify updates

## Pilot baseline

Candidate:
Crosstalk-Solutions/project-nomad v1.34.1.

Comparison:
- Project NOMAD full/subset deployment
- Open WebUI + Ollama
- AnythingLLM
- Kiwix + standalone local model/RAG components

Project NOMAD wins only if the integrated continuity-appliance value exceeds the additional host-control/security/maintenance burden.

## Pilot scenario

ONLINE
→ preload approved bundle
→ verify checksums/snapshot metadata
→ hard network loss
→ local search/RAG/LLM/reference access
→ create offline scratch artifacts
→ restart node while offline
→ restore from backup on clean host
→ restore network
→ controlled reconciliation
→ verify no stale overwrite/data loss
→ record RPO/RTO and operational burden

## Metrics

- offline availability
- retrieval quality
- model latency/tokens per second
- RAM/VRAM
- storage growth
- idle/load power
- LAN/mobile usability
- cold recovery time
- clean restore time
- RPO/RTO
- conflict rate
- maintenance effort
- security exposure
- update recovery
- integration cost

## Exit

ADOPT:
only if appliance-wide value is proven and security/recovery gaps are solved.

INTEGRATE:
use NOMAD behind the provider contract with hardened deployment.

REUSE-PARTS:
reuse Kiwix/content-management/hardware ideas or selected components while using other RAG/local-AI layers.

WATCH:
promising but auth/security/recovery/maintenance is not ready.

REJECT:
continuity value does not justify risk/complexity or mature component stack is superior.
