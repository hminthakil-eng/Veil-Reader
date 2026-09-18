# Eyad Studio Company OS — Work Reuse Gate v1

Status: ACTIVE COMPANY POLICY  
Parent: #94  
Scope: All Eyad Studio units and projects

## Objective

Prevent duplicate research, duplicate design, duplicate implementation, duplicate pilots, and parallel work on the same problem.

This policy extends—not replaces—SID, the Prompt/Skill Registry, project-specific registries, and existing Decision Logs.

## Mandatory rule

**NO NEW WORK WITHOUT A PREFLIGHT RECEIPT.**

No unit may begin material research, design, implementation, pilot execution, or tooling work until it has completed the Work Reuse Gate.

## Company sequence

SEARCH INTERNAL
→ MATCH
→ CLAIM
→ REUSE / EXTEND
→ SID EXTERNAL SEARCH
→ INTEGRATE / AUTOMATE
→ BUILD
→ VERIFY
→ PUBLISH RESULT

## 1. Internal Search

Before creating a new execution thread, search:

- open issues
- closed issues
- pull requests
- merged PRs
- project docs
- Company OS docs
- Decision Log
- Prompt/Skill Registry
- Solution Registry / SID findings
- project-specific registries
- previous benchmark/pilot reports
- reusable assets, fixtures, prompts, scripts, adapters and reference implementations

Search must include:
- exact terms
- synonyms
- project/domain aliases
- likely older names
- related components

## 2. Problem Fingerprint

Every Work Item receives a compact fingerprint:

- problem
- desired outcome
- project
- affected subsystem
- work type
- inputs
- expected artifacts
- constraints
- external dependencies

The fingerprint is used to detect exact and semantic overlap.

## 3. Canonical Work ID

Each problem has one canonical Work ID.

Format:

`EYAD-WORK-<project-or-company>-<number>`

GitHub issue number may be used as the canonical implementation locator where appropriate.

Every duplicate/child/extension must point to the canonical Work ID.

## 4. Claim Lock

A Work Item may be:

- UNCLAIMED
- ACTIVE
- BLOCKED
- HOLD
- VERIFYING
- DONE
- SUPERSEDED

An ACTIVE item has one accountable owner/unit.

If another unit discovers the same work while it is ACTIVE, it must:
1. attach to the canonical item,
2. contribute evidence/output there,
3. avoid starting independent execution.

Parallel execution requires explicit justification such as:
- deliberate A/B comparison,
- independent benchmark,
- security red-team,
- competing implementation proof.

## 5. Match Classes

### EXACT_DUPLICATE
Same problem and intended outcome.

Action:
- do not execute separately,
- link to canonical item,
- close duplicate task as duplicate.

### SEMANTIC_DUPLICATE
Different wording, materially same objective.

Action:
- merge into canonical work,
- preserve useful unique requirements.

### PARTIAL_OVERLAP
Shares a substantial subset of scope.

Action:
- create child/extension,
- reuse parent artifacts,
- execute only the delta.

### CONFLICT
Two work items imply incompatible architecture, ownership, or decisions.

Action:
- HOLD both conflicting deltas,
- escalate to SID/Board/owner,
- do not silently execute both.

### DISTINCT
No material reusable overlap.

Action:
- proceed after SID external reuse check when relevant.

## 6. Freshness Rule

Existing research is not repeated merely because it is old.

First classify each evidence item:
- STILL_VALID
- NEEDS_TARGETED_REFRESH
- STALE
- SUPERSEDED

Refresh only the stale portion.

Freshness depends on domain:
- fast-moving APIs/models/pricing/security advisories: short validity
- architecture/design principles: longer validity
- legal/compliance/current-provider capabilities: re-verify when material
- quarterly standards review remains the default scheduled refresh cycle

A full research redo requires evidence that prior conclusions are materially invalid.

## 7. Reuse Decision

Preflight must record one of:

- REUSE_AS_IS
- REUSE_WITH_UPDATE
- EXTEND_EXISTING
- INTEGRATE_EXISTING
- AUTOMATE_EXISTING
- BUILD_NEW
- HOLD
- REJECT

`BUILD_NEW` is valid only when the reusable path is insufficient and the gap is recorded.

## 8. Definition of Ready

A Work Item is not Ready unless:

- internal search completed
- matching items recorded
- canonical Work ID selected
- owner identified
- active-claim collision checked
- reuse decision recorded
- freshness reviewed
- external SID check completed when relevant
- dependencies/blockers linked
- expected output and verifier defined

## 9. Definition of Done

Work is not Done until:

- verifier passes
- canonical issue/record is updated
- reusable artifacts are linked
- important decisions are written to Decision Log
- obsolete/duplicate work items are closed or superseded
- lessons/regression fixtures are preserved where applicable
- downstream projects can discover the output

## 10. Research-specific anti-rework rule

A research request must start by answering:

1. Has Eyad Studio already researched this?
2. Has SID already evaluated a tool/solution?
3. Has the upstream changed since the previous evaluation?
4. Which exact findings are stale?
5. What is the minimum delta research required?

Do not repeat unchanged evidence collection.

## 11. Build-specific anti-rework rule

Before implementation:

- search existing internal modules/adapters/utilities
- search approved external solutions through SID
- identify reusable interfaces
- verify license/provenance
- prefer extension/integration over parallel replacement

## 12. Metrics

Company OS tracks:

- Duplicate Work Rate
- Reuse Rate
- Parallel Collision Count
- Duplicate Issues Closed
- Targeted Refresh Ratio
- Work Items With Valid Preflight %
- Time Saved by Reuse
- Build-New After Reuse-Check %
- Cross-project Artifact Reuse

## 13. Escalation

A unit may bypass the normal gate only for:
- urgent security containment
- production outage
- imminent destructive failure

Even then, retrospective preflight/reconciliation is mandatory after containment.

## 14. Authority

Company OS owns this policy.
SID owns solution/reuse intelligence.
Project owners own execution.
Board resolves cross-project conflicts.

The policy applies immediately to all new material work.
