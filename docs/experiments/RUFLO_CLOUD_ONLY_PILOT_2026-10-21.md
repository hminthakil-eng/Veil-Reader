# Ruflo cloud-only pilot — 2026-10-21

## Scope and constraints
- Repository: hminthakil-eng/Veil-Reader.
- Branch: `experiment/ruflo-keyless-cloud-20261021` (branched from `main`).
- No local/Windows computer used.
- No project source checkout, credentials, LLM API key, or write-enabled GITHUB_TOKEN in the two isolated smoke workflows.
- `main` and PR #509 remained unchanged.

## Run 1: cloud CLI and coordination smoke
- URL: https://github.com/hminthakil-eng/Veil-Reader/actions/runs/39630093523
- Conclusion: PASS (all nine CLI invocations exit code 0).
- Ruflo version: 3.59.8.
- Hierarchical swarm initialized, maximum 3 agents.
- Researcher and reviewer were registered; both remained idle. **No autonomous model calls were made.**
- Memory record could be retrieved/searched within run, **but not trusted as durable**: fallback sql.js reported invalid `wal_checkpoint(passive)` and warned about unpersisted writes.

## Run 2: native SQLite durability probe
- URL: https://github.com/hminthakil-eng/Veil-Reader/actions/runs/39631320225
- Conclusion: PASS (strict `set -euo pipefail`, assertions on retrieved value and semantic search).
- Installed pinned `ruflo@3.59.8` and `better-sqlite3@12.9.0` with install scripts enabled on **ephemeral cloud runner**; native DB write sanity check passed.
- `memory init`, `memory store`, separate-process `memory retrieve`, `memory search`, `memory peek` succeeded.
- No fallback durability warning during store/retrieve/search.
- Important remaining warning: CLI and MCP/AgentDB paths are distinct stores; memory search reported a partial result when another store had one item. Need explicit unified data path/namespace and a **cross-run artifact or external storage test** before claiming globally persistent shared memory.
- GitHub-hosted runner filesystem is ephemeral: passing this run does **not** prove memories persist into a future, new GitHub Actions run.

## Real Veil Reader task candidate: PR #509 dependency gate
- PR: https://github.com/hminthakil-eng/Veil-Reader/pull/509
- Failed policy job: https://github.com/hminthakil-eng/Veil-Reader/actions/runs/38422084217
- Failure: exact-head dependency snapshot not visible to review API after 600s, even though dependency submission job succeeded.
- Evidence from dependency submission run https://github.com/hminthakil-eng/Veil-Reader/actions/runs/38422083914:
  - `GITHUB_DEPENDENCY_GRAPH_SHA=e37869bb68e0497b5cbb9ca9f9b56c4e53286020` (correct PR head);
  - `GITHUB_DEPENDENCY_GRAPH_REF=refs/pull/509/merge` (PR merge reference, not a stable source branch ref);
  - Submission accepted but warned: `The snapshot was accepted, but it is not for the default branch. It will not update dependency results for the repository.`
- **Hypothesis, not confirmed fix:** submit the head snapshot using explicit `GITHUB_DEPENDENCY_GRAPH_REF=refs/heads/${{ github.head_ref }}` and checkout exact `github.event.pull_request.head.sha`. Maintain existing exact SHA verification and fail-closed Dependency Review. Validate separately in an isolated PR before touching PR #509.
- Avoid simply weakening/skipping dependency policy.

## Follow-on gates
1. Verify shared CLI/MCP memory paths and storage durability between new cloud runs.
2. Verify a real task reaches an AI provider and returns review output (not just spawn/idle). This requires a secure inference provider. GitHub Models inference was retired July 30, 2026.
3. Keep model credentials scoped to a separate trusted execution flow, with no untrusted PR checkout or arbitrary code execution in the same job.
4. Verify the proposed dependency snapshot ref correction on an isolated branch and compare the exact head/base GitHub API warnings.
5. Only after functional model inference, security and quality gates can Ruflo be promoted beyond pilot.

## Run 3: independent cloud-runner handoff (cross-job)
- URL: https://github.com/hminthakil-eng/Veil-Reader/actions/runs/39632021184
- Conclusion: **PASS**. `write-memory` and `read-memory` were successful jobs on separate GitHub-hosted runners.
- Synthetic `.swarm` and `.claude` state was archived and uploaded using `actions/upload-artifact@v7`; the second job downloaded and restored it.
- The second job verified both the exact stored value and one semantic search match (score 0.91). `memory peek` showed one record in both the CLI and MCP memory files.
- Open limitation: the search reported `Partial result` due to split CLI vs MCP/AgentDB stores; explicit path selection or unified memory policy is needed before multi-agent sharing.
- This establishes an explicit **artifact handoff between independent jobs in one run**. It does not mean Ruflo automatically retains state on GitHub runners between unrelated workflow runs.

## Run 4: actual inference via isolated local model
- URL: https://github.com/hminthakil-eng/Veil-Reader/actions/runs/39633068312
- **PASS, real inference confirmed**, using Ruflo 3.59.8 `agent_execute`, `RUFLO_PROVIDER=ollama`, a local Ollama daemon, and `qwen2.5:0.5b`.
- Researcher output: 123 tokens / 4.116s; reviewer: 127 tokens / 3.357s.
- No personal PC, no API keys, no checkout or repository mutations. Model prompts stayed on the temporary runner's local HTTP inference server.
- Output artifact: `ruflo-real-agent-report` in the run; contains a preliminary diagnosis of PR #509.
- **QUALITY FAIL**: model's investigator proposed a weakly specified verification; reviewer mostly repeated input facts instead of challenging the hypothesis. This proves technical routing, not trustworthy expert engineering quality.
- Next comparison: `Ruflo Engineering Review Pilot` using a larger `qwen2.5-coder:3b` model with explicit evidence/hypothesis separation and adversarial reviewer prompt. This is still an experiment.

## Run 5: stronger 3B coding-model evaluation — engine PASS, reasoning QUALITY FAIL
- URL: https://github.com/hminthakil-eng/Veil-Reader/actions/runs/39633644560
- Environment: ephemeral GitHub-hosted Ubuntu, locked `ruflo@3.59.8`, isolated local Ollama `qwen2.5-coder:3b`, no project checkout, no secrets or repo write access.
- Investigator: real `agent_execute` completion (427 output tokens, 49.013s); reviewer: real completion (409 output tokens, 59.395s).
- The investigator separated some observations and hypotheses, **but selected the wrong GitHub API endpoint**: `GET /repos/{owner}/{repo}/compare/{base}...{head}` is the ordinary commit comparison, not the dependency-graph snapshot readiness endpoint.
- The actual production gate uses `GET /repos/{owner}/{repo}/dependency-graph/compare/{base_sha}...{head_sha}?per_page=5` with exact source SHAs and requires the `x-github-dependency-graph-snapshot-warnings` response header to be present, valid and empty.
- The reviewer repeated the bad endpoint and the unchanged `refs/pull/509/merge` instead of critically examining the alternate head branch ref hypothesis. This **fails independent-review quality** despite both model calls succeeding.
- Artifact: `ruflo-engineering-review` in this workflow run. Treat its contents as experimental untrusted suggestions, **not an approved fix**.
- Decision: keep `Ruflo` as experimental orchestration infrastructure; use a materially stronger, properly authenticated model plus deterministic source-grounded tests before permitting unattended engineering writes. Do not weaken dependency policy or promote this pilot.

## Run 6: adversarial static quality screen
- URL: https://github.com/hminthakil-eng/Veil-Reader/actions/runs/39639953254
- **PASS (9 tests)** against deterministic no-false-green tests.
- Screen rejects the real 3B model's erroneous ordinary code-compare API path, missing exact source SHAs, duplicate/missing reviewer, missing snapshot-warnings header, and unreviewed merge-ref-only logic.
- Even a syntactically compliant report gets `approved_for_merge: false`. Static screening is not evidence of technical correctness.
- Production branch and PR #509 have not been changed by this test.

## Source-grounded LLM evidence trials: correct fail-closed outcome
- Runs: https://github.com/hminthakil-eng/Veil-Reader/actions/runs/39638198262 and https://github.com/hminthakil-eng/Veil-Reader/actions/runs/39639481598
- The source files used by the PR were read, without execution, and the local Qwen2.5-Coder 3B attempted the analysis.
- First run: reviewer produced only 5 output tokens and the run stopped at inference output validation.
- Second run: both agent calls returned, but reviewer output was only 15 characters; independent evidence gate rejected the report. **Correct safety behavior is FAIL**, not a successful engineering review.
- Avoid allowing small CPU models to approve security/release changes. They remain experiments.
- Subsequent exact dependency-graph compare diagnostic: https://github.com/hminthakil-eng/Veil-Reader/actions/runs/39640375339 (read-only; record outcome separately).

## Read-only exact snapshot diagnostic and successful policy rerun (2026-10-21)
- Diagnostic: https://github.com/hminthakil-eng/Veil-Reader/actions/runs/39640375339 — PASS; exact PR 509 base/head dependency-graph compare API returned HTTP 200, valid JSON list and an empty snapshot-warnings header.
- Previous failed policy workflow: https://github.com/hminthakil-eng/Veil-Reader/actions/runs/38422084217. Re-ran **only failed dependency-review job** using GitHub workflow rerun; the latest attempt completed **SUCCESS** with all review/policy steps passing.
- Latest step evidence: exact source snapshots ready; dependency-review action reported zero vulnerable packages of severity high or greater, zero incompatible/denied licenses, 11 dependencies with unknown license metadata (warnings, not denied by current policy).
- This confirms the dependency-review gate passes on the original PR source *now*; it does **not** establish that the suspected refs/pull merge metadata was the root cause or that future ingestion will be reliable. Avoid speculative production edits.
- PR #509 remains OPEN and DRAFT and UNMERGED. Native Android physical-device acceptance in issue #408 is independent and not satisfied by this policy result.
- All Ruflo experiments and report screening live only on experiment/ruflo-keyless-cloud-20261021.
