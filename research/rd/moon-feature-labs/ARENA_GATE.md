# Arena Gate for R&D Reader Labs

The repository Arena skill expects a self-contained task, attacks, defenses and a scoring rubric.
Each lab must package the following before a real tournament:

## Task
- Product problem in one sentence.
- Existing canonical behavior that must not regress.
- Exact non-goals.
- Allowed dependencies.
- Data/privacy/offline requirements.
- Accessibility + RTL requirements.
- Performance budget when runtime-sensitive.
- Migration/backward-compatibility constraints.

## Rubric
Score 0–10:
- Correctness — does it satisfy the contract without lying about renderer capability?
- Completeness — are lifecycle, persistence, accessibility and failure states covered?
- Specificity — can the implementation be executed without architectural guessing?
- Robustness — process death, malformed data, duplicate events, large inputs, RTL, large text.
- Clarity — ownership boundaries and integration path are obvious.

## Fatal conditions
- copies proprietary Moon+ implementation/assets;
- introduces a parallel EPUB engine without a proven Readium blocker;
- loses reader position/annotation data;
- hides unsupported PDF/format capability behind fake UI;
- requires network/account for offline core reading;
- bypasses existing Reader input ownership;
- worsens a release performance gate without an approved budget change.

## Output
Arena champion produces a proposal/diff only. R&D never auto-merges into canonical.
