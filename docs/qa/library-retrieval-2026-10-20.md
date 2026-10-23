# Library retrieval correctness

Owner-approved bounded repair; base `be4df10c28a67f93abd25ca5d42c8c711d157158` (foundation draft #556). Applies `build-premium-readers`: fixture, action, oracle and bounded evidence.

| User outcome | Source observation / reuse | Acceptance |
|---|---|---|
| Truthful sorting | Existing search ranked by relevance while saying Recent; reuse normalization and scores | Contextual search says Relevance; explicit Recent stays chronological; equal keys use stable book IDs |
| Recover stale series | Collection cleanup existed; series cleanup absent | Metadata rename/removal cannot hide remaining books |
| Group identity matches filtering | Grouping trims and lowercases; old matching only ignored case | Whitespace/case group variants still find the intended book; no fuzzy metadata merging |
| Preserve valid selections | Wings display only eight groups; search may legitimately have no results | Validate against all canonical books, never query results or the preview |

No reader engine/session, Room schema, dependencies, signing or application identity changes.

## Evidence, before sandbox reset

- Existing search/composition baseline: 17 tests, zero failures/errors/skips.
- Real production Compose RED: six tests, four expected assertion failures (English/Persian sorting labels, stale series, whitespace series matching); two valid-filter protections passed. No errors/skips.
- Scoped GREEN: 33 tests, zero failures/errors/skips: the six UI cases, ten pure selection tests and 17 existing tests. Includes Persian 320dp/200% semantic reachability.
- 39 reading-policy checks and six Paper guard tests passed on the product baseline.
- Full verification stopped at a Kotlin incremental snapshot mismatch before suite execution; no full-suite pass claimed.
- After the interruption, all seven source/resource/test blobs were restored byte-identical against recorded Git blob hashes. These are historical scoped results; subsequent changes require new verification.

## Review and remaining gates

Independent review identified that an initially empty books flow does not prove metadata absence. A restored series must survive loading. A new production Compose state-restoration regression reproduced it: 1 test, 1 assertion failure, missing the saved Cycle chip after restore with empty input. The correction defers destructive collection/series reconciliation while the canonical books snapshot is empty. Nonempty snapshots still clear stale labels. A truly empty library can retain its chip until reset or the next nonempty snapshot; it cannot hide existing books. No storage readiness subsystem is introduced by this repair. Fresh post-correction scoped verification: 35 tests (7 real Compose interactions, 11 selection tests, 17 existing search/composition tests), zero failures/errors/skips; Gradle exit 0. The restoration RED/GREEN is observed. Reading policy: 39 checks passed; Paper source contract passed; six Paper guard tests passed. Independent final source review found no remaining Critical, Important or Minor findings. Full-suite/build checks are running.

Full suite, lint/build, final source review and physical-device accessibility/return comparison remain pending or UNVERIFIED. No release or main merge is authorized by this change.
