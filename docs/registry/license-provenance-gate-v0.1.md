# Eyad Prompt & Skill Registry — License & Provenance Gate v0.1

Status: Draft pilot specification  
Parent: #73, #74, #77

## Purpose

No prompt, skill, workflow, or tool descriptor may enter TESTING or APPROVED state until provenance and licensing are classified.

## Provenance classes

### VERIFIED

All required source identity evidence is present and consistent:

- canonical upstream project identified
- source URL resolves to expected owner/project
- version/tag/commit recorded
- author/project identity sufficiently established
- content hash captured
- no unexplained mirrors/forks in the import chain

### PARTIAL

Some source evidence exists, but one or more non-critical gaps remain.

PARTIAL requires explicit SID review before TESTING.

### UNKNOWN

Source identity, version lineage, or ownership cannot be established with reasonable confidence.

UNKNOWN MUST be quarantined.

## License checks

The gate MUST record:

- code license, if applicable
- prompt/content/data license, if separate
- SPDX identifier when possible
- license evidence source
- restrictions relevant to redistribution, modification, commercial use, attribution, network use, or copyleft
- embedded third-party material and its license
- dependency license notes when execution is in scope

## Policy

- VERIFIED + compatible license → may proceed if other gates pass
- PARTIAL → manual SID review
- UNKNOWN → QUARANTINED
- incompatible/prohibited license → REJECTED
- missing license evidence → QUARANTINED
- conflicting license claims → QUARANTINED pending resolution

## Mirrors and forks

A mirror or fork is not trusted solely because it is public.

The gate must preserve:
- original upstream if discoverable
- fork owner
- divergence point/version
- reason for preferring the fork
- any code/content modifications relevant to trust or license

## Immutability

For every approved item, the registry stores immutable provenance metadata:

- upstream canonical URL
- exact version/tag/commit
- content hash
- license evidence
- review outcome
- reviewer and timestamp

An upstream update NEVER silently replaces an approved item. It creates a new registry version and repeats the gate.

## Output

The gate emits:

- provenance_status: VERIFIED | PARTIAL | UNKNOWN
- license_status: COMPATIBLE | REVIEW_REQUIRED | INCOMPATIBLE | UNKNOWN
- evidence[]
- unresolved_questions[]
- recommended_action

Actions:
- PROCEED_TO_DEDUP
- MANUAL_REVIEW
- QUARANTINE
- REJECT

## Exit criteria

The gate is accepted when:
- approved items preserve immutable source/version/license evidence
- UNKNOWN cannot pass automatically
- conflicting license claims fail closed
- fork/mirror lineage is recorded
- upstream updates force re-review
