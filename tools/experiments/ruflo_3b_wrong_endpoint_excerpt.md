## CI investigator
Confirmed Observation:
The PR 509 review failed after a 600 second wait for an exact HEAD dependency snapshot.
Proposed Experiment:
- SHA: e37869bb68e0497b5cbb9ca9f9b56c4e53286020
- Ref: refs/pull/509/merge
GET /repos/{owner}/{repo}/compare/{base}...{head}
## Skeptical reviewer
Alternative causes: API latency, API configuration.
GET /repos/{owner}/{repo}/compare/{base}...{head}
FAIL Criteria: No snapshot for head.
