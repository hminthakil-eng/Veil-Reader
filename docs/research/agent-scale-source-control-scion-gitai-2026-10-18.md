# Eyad Studio Research — Agent-scale source control: Scion and gitai

**Date:** 2026-10-18  
**Source reel:** https://www.instagram.com/reel/DeoXYcovvum/ (Prince Gupta, `@princegupta.ai`)  
**Evidence level:** Anthropic's primary engineering blog + project source and documentation; the Instagram video could not be played directly, so claims were checked against its public caption.  
**Status:** Saved as an R&D candidate only. No installation, migration, production deployment, or performance test has been completed.

## Verified facts

1. Anthropic published [Redesigning source control at Anthropic](https://www.anthropic.com/engineering/redesigning-source-control) on **2026-10-13**, updated **2026-10-15**. Anthropic reports a rise from roughly **2,500 merged changes/day (May 2026)** to **over 30,000/day (September 2026)** and describes moving its unusually large internal monorepo to **Scion** in five months. This is Anthropic's reported workload, **not a universal benchmark showing Git is obsolete**.
2. **Scion is proprietary/internal**. It assumes always-online agents, centralizes server-side operations, uses sparse object fetching and cached source-history APIs, and offers incremental changesets. Anthropic itself says conventional Git remains appropriate for most repositories.
3. The independently developed project [feder-cr/git-ai](https://github.com/feder-cr/git-ai) is **not Anthropic's Scion source code** nor an official Scion release. It implements some published Scion concepts while retaining ordinary Git as the underlying storage/plumbing (built on open-source Gitaly).
4. The project README identifies version **0.1.0** as its **first public release**, with interfaces still subject to change. README describes server-assisted sparse reads, changeset logs, concurrent landing, opt-in resolvers, policies, `graft-verify`, caching, tests, benchmarks and review APIs. These are **upstream feature claims, not features verified here by executing a deployment**.
5. Upstream requirements currently list **Linux/macOS, Go 1.26, C compiler, Meson and Ninja**. Windows is not listed as a supported native build host. A separate Linux VM/container/lab would be needed for a Windows-based workflow.
6. Upstream's [LIMITATIONS.md](https://github.com/feder-cr/git-ai/blob/main/doc/graft/LIMITATIONS.md) emphasizes: it is **not a GitHub/GitLab hosting, issues, review-UI or CI replacement**; its policy configuration is not an independent security boundary; agent policies/tokens require care; **pack garbage collection** and other operations have known limitations. Auto-resolve is off by default.
7. [AGENT-SECURITY.md](https://github.com/feder-cr/git-ai/blob/main/doc/graft/AGENT-SECURITY.md) recommends a distinct token per agent, protected default/release branches, separately protected operator credentials and reviewable changesets.
8. **Licensing:** The top-level repository identifies an MIT license and Gitaly-derived attribution; retain LICENSE/NOTICE when reusing. Audit subdependencies and redistribution obligations separately before redistribution.

## Direct relevance to Eyad Studio and Veil Reader

| ID | Pattern to investigate | Application | Priority | Acceptance evidence |
| --- | --- | --- | --- | --- |
| AS-01 | Agent identity and per-branch permissions | Grand Forge agents work in isolated branches; no unrestricted production-main writes | P0 (practice) | Negative authorization test; audit trail links all changes to correct agent |
| AS-02 | Fast selective/sparse file access | Short-lived Codex/Gemini/QA workspaces read only required project paths | P1 (pilot) | Compare checkout elapsed time, bytes and full correctness vs Git sparse/partial clone |
| AS-03 | Conflict-aware concurrent landing | Independently proposed UI, Reader and QA changes without silent overwrites | P1 (pilot) | Deterministic test with 10/25/50 agents, same-file conflicts, every landed commit accounted for |
| AS-04 | Automated post-landing verification | Dev QA catches regressions and reverts a culprit when proven | P1 (pilot) | Green/broken fixture tests, controlled rollback, no production dependency |
| AS-05 | Changeset stream and observer agents | SID/QA receives precise code-change events for reuse and risk registry | P1 | Accurate actor/path/branch metadata and no notification floods |
| AS-06 | Server-side file history/search API | Ask who changed page-turn engine and why, without full clones | P2 | Relevant queries correct across renames and changed files |
| AS-07 | Model-driven merge conflict resolver | Highly experimental: propose repairs for conflicted code | HOLD | Human review, path restrictions, tests, no model-merged reader engine without signoff |
| AS-08 | Replace GitHub as canonical source | Not justified at current Veil Reader scale | REJECT NOW | Continue GitHub branches, PRs, checks, release gates as canonical |

## Evaluation and risks

- **Technical concept value:** HIGH for future parallel agent workloads. **Readiness for production migration:** LOW at the current project scale and first public release.
- **Primary risks:** data loss/corruption when concurrent landing is wrong; inaccurate conflict resolution; unknown operational burden; incomplete GitHub integration; incompatible host environment; permission misconfiguration; custom service becoming a single point of failure; storage GC gaps.
- **Do not turn off existing gates:** GitHub PR reviews, required checks, manual QA and release protection remain authoritative. Never let an AI resolver merge into the live Reader engine or main without explicit review and CI.
- **Potential easier alternatives to benchmark first:** existing GitHub workflows, protected branches, Git worktrees, sparse-checkout/partial clone, CI test matrix, isolated agent credentials, and read-only review agents.

## Reversible pilot proposal (NOT EXECUTED)

1. Mirror only **synthetic test content** into a disposable private lab; do not upload private books, tokens, source secrets, signing keys or users' data. Run on isolated Linux host.
2. Record baseline using Git + worktrees and a GitHub-branch/PR simulation before assessing `gitai`.
3. Run 10/25/50 concurrent simulated agents; vary 0%, 5%, 20%, 50% same-file conflicts. Log throughput, p50/p95/p99 land latency, exact diff parity, conflict rate, rollback and auditor attribution.
4. Test process kill during commit, restart, corrupt object, slow network, repeated retries, forbidden token access and unauthorized main/release write. Verify no lost or silently mangled changes.
5. Accept only if correctness and permission tests pass **and** there is repeatable, meaningful performance benefit on our scale. Otherwise keep GitHub and archive the pilot; never migrate the canonical repository based on vanity benchmarks.

## References

- Instagram: https://www.instagram.com/reel/DeoXYcovvum/
- Anthropic original article: https://www.anthropic.com/engineering/redesigning-source-control
- Open-source implementation: https://github.com/feder-cr/git-ai
- README: https://github.com/feder-cr/git-ai/blob/main/README.md
- Quickstart: https://github.com/feder-cr/git-ai/blob/main/doc/graft/QUICKSTART.md
- Limitations: https://github.com/feder-cr/git-ai/blob/main/doc/graft/LIMITATIONS.md
- Security: https://github.com/feder-cr/git-ai/blob/main/doc/graft/AGENT-SECURITY.md
- Bench methodology: https://github.com/feder-cr/git-ai/blob/main/doc/graft/BENCH.md

**Intake decision:** `RESEARCH SAVED / LAB-CANDIDATE / NO INSTALL OR REPLACE`. Owner team: SID + DevOps + QA. Suggested follow-up when parallel-agent contention is actually measured.