# Ruflo source-grounded PR 509 audit — evidence-gate results (2026-10-21)

## Scope
- All test files and workflows exist on `experiment/ruflo-keyless-cloud-20261021`.
- Source under examination: PR #509 exact head `e37869bb68e0497b5cbb9ca9f9b56c4e53286020`.
- No changes to `main`, production Android code, release gates or PR #509.
- Experiments use cloud-hosted ephemeral GitHub runners, local Ollama, Ruflo 3.59.8 and no provider credentials.

## Deterministic quality gate v2
- Script: `tools/experiments/ruflo_evidence_gate_v2.py`.
- Regression suite: `tools/experiments/test_ruflo_evidence_gate_v2.py`.
- All **9 adversarial unit tests PASS** locally and on the GitHub runner.
- Gate checks wrong GitHub commit-comparison endpoint, exact dependency-graph endpoint for both roles, snapshot warning header, head/base SHAs, ref hypothesis, independent critique, and explicit `Decision: BLOCK`.
- `merge_authorized` is always false. Contract PASS, if any, does not prove diagnosis or license automated merges.

## Cloud run: verbose source evidence
- https://github.com/hminthakil-eng/Veil-Reader/actions/runs/39638198262
- Deterministic tests: PASS.
- Researcher: real Ruflo → local Qwen2.5-Coder 3B invocation, 473 output tokens, 150648 ms, success.
- Reviewer: real invocation, **5 output tokens**, 115161 ms, reported success; rejected as unusable; overall model job FAIL.
- No verified engineering review resulted.

## Cloud run: bounded source excerpts, preserved failure artifacts
- https://github.com/hminthakil-eng/Veil-Reader/actions/runs/39639481598
- Deterministic tests: PASS.
- Researcher: real invocation, input 944/output 312 tokens, 55459 ms, success.
- Reviewer: real invocation, input 1043/output **4** tokens, 14836 ms, success with `stop`; answer was only `Decision: BLOCK`.
- Generated report saved as `ruflo-grounded-source-review` artifact. Quality gate: **FAIL** (six issues).
- The investigator correctly mentioned the dependency-graph compare API and warning header, but did not demonstrate exact SHA/ref distinction well, and offered a vague/duplicative branch experiment.
- The reviewer failed to independently check exact endpoint, warnings semantics, alternate causes, and rejection conditions.
- The reduction in prompt length improved runtime but did **not** establish reliable independent technical review.

## Root cause under investigation: dependency graph gate
- Actual failed review: https://github.com/hminthakil-eng/Veil-Reader/actions/runs/38422084217
- Exact head snapshot was missing to dependency compare API after 600 seconds.
- Successful submission job: https://github.com/hminthakil-eng/Veil-Reader/actions/runs/38422083914
- Submission logged exact head `SHA=e37869bb68e0497b5cbb9ca9f9b56c4e53286020`, but `REF=refs/pull/509/merge`.
- Submission acceptance is not equivalent to visibility in the dependency comparison endpoint.
- Candidate, not verified fix: on a separate PR, test consistent `GITHUB_DEPENDENCY_GRAPH_SHA` (exact PR head), `GITHUB_DEPENDENCY_GRAPH_REF=refs/heads/reader/native-paper-rebuild-20261011`, and preserved job correlator; confirm the exact `/dependency-graph/compare/{base_sha}...{head_sha}?per_page=5` with a **present, valid, empty** `x-github-dependency-graph-snapshot-warnings` header and policy evaluation.
- Do not disable the dependency policy, fake graph readiness, or mark a device/product gate green.

## Disposition
**Ruflo Cloud Harness = FUNCTIONAL PILOT.**
**Autonomous engineering and review by Qwen2.5-Coder 3B = NOT ACCEPTED.**

Further QA work should prioritize measured model competence and expert comparison against deterministic source contracts, not repeatedly retrying an undersized reviewer model. Before deployment, require a stronger authenticated inference provider, independent correctness evidence, secret isolation, resource/cost controls, and human release approval.
