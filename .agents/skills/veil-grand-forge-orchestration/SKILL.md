---
name: veil-grand-forge-orchestration
description: Coordinate multi-specialist, evidence-first Veil Reader work across Reader, Paper, TTS, search, PDF, UI, QA, security, performance, and release. Use for cross-cutting implementation, competitive parity, integration sequencing, or deciding whether to promote a PR.
---

# Veil Reader Grand Forge — orchestration

## Mandatory repository discovery
1. Inspect live branches, latest PRs, commit heads, CI workflows, and issue blockers. Do not assume main is canonical.
2. Trace candidate work to the current canonical Reader integration branch; respect verification-only PR labels and explicit DO NOT MERGE notes.
3. Read existing .agents/skills/veil-world-class-app-development/SKILL.md, WORLD_CLASS_RELEASE_GATES.md, and relevant implementation skills. Do not duplicate installed Superpowers, Impeccable, Arena, or project skills.
4. Complete the existing company work-reuse gate before creating overlapping tickets, agents, dependencies, or implementations.

## Specialist dispatch
- Reader interaction/Page engine: veil-paper-engine-physics-qa.
- State/annotations/backup: veil-reading-state-durability.
- Visual UI/RTL/a11y: veil-visual-regression-accessibility.
- TTS/search/PDF: veil-reading-capabilities-verification.
- Security and third-party tools: veil-secure-supply-chain.
- Competitor parity/research: veil-competitor-cleanroom-intel.
- Performance, CI and release: veil-performance-release-lab.
Activate existing repository Android Engineer, Product Designer, Release Guardian, Superpowers, and Arena where applicable, with no redundant fan-out.

## Execution contract
Observe -> Predict -> Reuse -> Integrate -> Automate -> Build -> Verify -> Learn.
Every work item declares: user outcome, exact baseline SHA, owner, risk, affected state boundaries, rollback, measurable acceptance matrix and evidence paths.
Never claim a specialist team ran or a tool was installed solely because a skill exists. A skill describes behavior; its prerequisites need available execution tooling.
Use isolated, reversible branches and draft PRs. No giant combined refactors and no user-facing half features.
Physical page-curl, TalkBack, battery, latency, and process-kill claims require relevant device evidence; CI green is not equivalent to end-to-end acceptance.
Never merge or ship on unverified, blocked, or simulation-only evidence.

## Exit record
Report exact branch/SHA/PR, executed tests, failures, observed metrics and devices, blockers, unverified requirements, next smallest step.
