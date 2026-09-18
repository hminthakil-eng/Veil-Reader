# Eyad Prompt & Skill Registry — Deduplication & Conflict Detection v0.1

Status: Draft pilot specification  
Parent: #73, #74, #76

## Purpose

Prevent duplicate, near-duplicate, contradictory, obsolete, or authority-conflicting items from entering the Eyad Approved Registry.

## Processing order

1. Exact hash duplicate
2. Normalized-text duplicate
3. Semantic near-duplicate
4. Same-name / different-behavior collision
5. Capability conflict
6. Workflow ownership conflict
7. Version supersession / deprecation
8. Company OS policy conflict

The system is fail-closed: unresolved high-impact conflicts cannot publish.

## Canonicalization

For text-like items, derive a normalized representation by:
- Unicode normalization
- line ending normalization
- trim repeated whitespace
- preserve semantic punctuation and code
- exclude registry metadata from content comparison

Never normalize away executable instructions, URLs, capability declarations, or security-relevant content.

## Match classes

### EXACT_DUPLICATE
Same content hash.

Action: keep one canonical item; preserve all provenance aliases.

### NORMALIZED_DUPLICATE
Different raw hash but equivalent normalized text.

Action: MERGE metadata when licenses/provenance are compatible.

### SEMANTIC_NEAR_DUPLICATE
Substantially overlapping goal, workflow, and output with wording differences.

Action: choose canonical based on quality, provenance, test results, and maintenance value; otherwise KEEP_BOTH with explicit routing rules.

### NAME_COLLISION
Same/similar name but materially different behavior.

Action: KEEP_BOTH only after renaming/disambiguation; otherwise HOLD.

### CAPABILITY_CONFLICT
Similar task but different privilege requirements.

Action: never collapse a lower-risk item into a higher-risk item. Prefer the least-privilege canonical option where capability is not essential.

### WORKFLOW_CONFLICT
Two items try to own the same Company OS stage or issue contradictory operational instructions.

Action: internal Company OS policy wins unless explicitly superseded by Board/SID decision.

### VERSION_SUPERSESSION
Newer upstream version replaces an older compatible version.

Action: retain old immutable version for rollback; publish new version only after full gates.

### POLICY_CONFLICT
Item instructs bypass of Company OS, SID approval, security boundaries, user approval, or other higher-authority policy.

Action: REJECT or QUARANTINE depending on intent and remediability.

## Decision output

Each candidate receives:
- duplicate_group_id
- conflict_type
- canonical_candidate_id
- confidence: LOW | MEDIUM | HIGH
- rationale
- recommended_action:
  - MERGE
  - KEEP_BOTH
  - REPLACE
  - HOLD
  - REJECT

## Canonical selection priority

1. Company-owned approved item
2. Higher trust / clearer provenance
3. Compatible clearer license
4. Lower privilege requirement
5. Better cross-model test results
6. Better task precision and output contract
7. Lower maintenance/context overhead
8. More recent compatible version

Popularity/upvotes are never sufficient by themselves.

## Cross-collection rules

The same approved item may appear in multiple project collections by reference. Do not copy content into each collection.

Collections route to immutable registry versions:
- veil-reader
- manga-os
- sid-riu

## Exit criteria

- exact duplicates collapse deterministically
- near-duplicates are grouped with rationale
- capability escalation cannot be hidden by deduplication
- internal policy outranks public upstream content
- rollback versions remain addressable
- every conflict decision is auditable
