# Offline Company Continuity Node — Controlled Pilot Matrix v1

Parent: #109
Challenges: #110 #111
Date: 2026-09-18

## Candidate A — Project N.O.M.A.D.

Pinned pilot baseline:
- upstream: Crosstalk-Solutions/project-nomad
- stable release: v1.34.1
- license: Apache-2.0

Verified useful capabilities:
- browser-first offline management UI
- Ollama and OpenAI-compatible model backends
- document RAG using Qdrant
- Kiwix offline knowledge
- Kolibri education content
- ProtoMaps offline maps
- CyberChef and local utilities
- local notes
- Docker-managed service bundle
- built-in hardware/AI benchmark capability

Security/operations concerns to test:
- no built-in authentication by default
- Docker socket access by management/updater services
- host root read-only mount used by disk collector
- rolling/latest images and pull behavior in current management compose
- root/sudo installer path
- recovery across all service state
- RAG/vector consistency
- deterministic post-outage resync

## Candidate B — Modular baseline

Required comparison components:

### Model runtime
- Ollama
- or LocalAI / another approved OpenAI-compatible local runtime

### AI / RAG UI
- Open WebUI or equivalent mature self-hosted UI

### Vector storage
- Qdrant with explicit snapshots/restore

### Offline static knowledge
- Kiwix

### Access
- Company-controlled firewall + VPN/private network/authenticated gateway

### Backup and resync
- Company-owned scripts/contracts with explicit manifests, snapshots and conflict rules

Purpose:
measure whether N.O.M.A.D.'s integration convenience is worth its additional privilege/supply-chain/maintenance surface.

## Test phases

### P0 — Static audit

- license
- installer
- compose images
- image tags/digests
- Docker socket mounts
- host mounts
- network ports
- update path
- dependency inventory

### P1 — Clean install

Measure:
- install time
- disk
- RAM
- services
- network calls
- privilege requirements

### P2 — Local AI/RAG

Known-answer test set:
- ingestion success
- citation correctness
- retrieval precision/recall proxy
- re-index correctness
- deleted/replaced document cleanup
- model TTFT
- tokens/sec

### P3 — Full outage

Block WAN completely.

Verify:
- UI
- local model
- RAG
- Kiwix
- approved tools
- mobile LAN/VPN access
- restart without internet

### P4 — Failure/recovery

Inject:
- admin restart
- Qdrant failure
- database failure
- disk remount/restart
- corrupted test index
- node reboot

Verify recovery and data integrity.

### P5 — Full-node restore

Destroy the disposable node.
Restore from documented backup/manifest.
Measure RTO/RPO.

### P6 — Internet return / resync

Use approved asymmetric sync:
- compare manifests
- import upstream changes
- export locally-created test changes to review queue
- detect conflicts
- no silent overwrite

### P7 — Maintainability

Simulate:
- version update
- image change
- rollback
- dependency replacement

## Decision outputs

Per capability:
- ADOPT
- INTEGRATE
- REUSE-PARTS
- WATCH
- REJECT

Final recommendation must distinguish:
1. need for ContinuityNodeProvider,
2. value of N.O.M.A.D. bundle,
3. components worth reusing,
4. gaps requiring Company-owned adapters,
5. any true gap that justifies custom build.
