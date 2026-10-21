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
