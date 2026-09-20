# Eyad Studio Company OS — Project File Hygiene v1

Status: ACTIVE COMPANY POLICY
Parent: #139
Scope: all active projects, Work spaces, Project Files surfaces, repositories, research packs, build artifacts and shared project documents.

## Core rule

**Active Project Surface = Current Canonical Files Only.**

The active project area must stay small, current, unambiguous and useful to humans and agents.

## File states

- `KEEP_ACTIVE` — current canonical file used by the project.
- `REPLACE` — older active file superseded by a newer canonical version.
- `ARCHIVE` — not active, but retained for provenance, audit, rollback or legal/history value.
- `DELETE` — duplicate, obsolete, temporary, generated, safely reproducible, or no longer useful.

## Canonical version rule

Every important artifact must have one active canonical version.

When a new approved version replaces an old one:

1. the new version becomes canonical,
2. old active copies are marked superseded,
3. the superseded copy leaves the active Project Files surface,
4. rollback/history is retained through Git/GitHub or an explicit archive when required.

Do not keep multiple similarly named active versions such as:

- architecture-final.md
- architecture-final-v2.md
- architecture-final-new.md
- architecture-final-latest.md

Prefer a stable canonical path with version history in Git.

## What should stay active

Keep only material currently needed for execution, for example:

- current architecture
- current product requirements
- current design system / UI standards
- current QA matrix
- current implementation plan
- current decision/policy documents
- active canonical research summary
- active schemas/contracts
- current test fixtures needed by execution

## What should leave the active surface

Regularly remove or archive:

- superseded architecture documents
- old duplicated research reports
- stale implementation plans
- obsolete prompts/skills
- outdated exported copies
- temporary screenshots after evidence is recorded elsewhere
- old generated APK/build artifacts not needed for current QA
- intermediate conversion files
- abandoned experiments
- duplicate attachments
- files reproducible from source/build
- obsolete benchmarks replaced by newer canonical results

## Safety rule

Never delete the only remaining copy of:

- provenance evidence
- legal/license evidence
- accepted decisions
- security findings
- release evidence
- migration/rollback data
- irreplaceable source assets

If history matters, archive it outside the active execution surface.

## Review cadence

### Immediate cleanup

Run after:

- architecture migration,
- major requirements update,
- replacement of a canonical document,
- release/milestone completion,
- large research refresh,
- major tool/provider migration.

### Milestone hygiene pass

At each major merge/milestone:

- identify newly superseded files,
- remove duplicate active copies,
- remove temporary generated artifacts,
- confirm canonical links.

### Monthly full audit

For every active project:

- list active project files,
- classify KEEP_ACTIVE / REPLACE / ARCHIVE / DELETE,
- detect duplicates and stale versions,
- verify the canonical owner/version/source,
- remove obsolete files from the active Project Files area,
- record any retained archive rationale.

## Work Mode requirement

Before substantial Work tasks:

1. inspect the Project Files surface and canonical repository documents,
2. identify stale/duplicate/superseded inputs,
3. use only the canonical active files,
4. never base new work on a superseded file,
5. report cleanup candidates when stale context is found.

If direct Project Files deletion is unavailable through the current API, Work must produce the exact file list and classification so the user can remove/replace those files in the Project UI.

## Git/GitHub rule

Repository-controlled documents should normally be updated at the same canonical path instead of accumulating renamed copies.

Git history is the preferred rollback/history mechanism.

Generated build artifacts should use retention policies rather than permanent active storage.

## Metrics

Track:

- active file count
- canonical file coverage
- duplicate count
- superseded-file count
- ownerless file count
- safely reproducible artifact count
- archive size
- active-context reduction after cleanup

## Definition of healthy

A project is file-hygiene GREEN only when:

- every important active file has a clear canonical role,
- no known superseded file is presented as active,
- duplicate active copies are eliminated,
- rollback/provenance remains available where needed,
- agents can reliably identify the current source of truth.
