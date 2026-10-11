# AGENTS — Veil Reader Grand Forge

This repository is owned by the Grand Forge product engineering process. For any agent (Codex, Work, CLI, design/QA automation), the canonical branch for accepted **integrated development** is main; do not assume latest experimental/draft branch or old integration branches replace main. Public release requires a separate quality decision.

**Mandatory before ANY material task, resumed task or stage:**
1. Read current GitHub main HEAD directly; do not use a remembered SHA.
2. Read docs/roadmap/GRAND_FORGE_MASTER_ROADMAP.md, docs/roadmap/LIVE_STATUS.md, docs/roadmap/BRANCH_AND_ARCHIVE_POLICY.md, docs/roadmap/GRAND_FORGE_EXECUTION_PROTOCOL.md, and issue #495.
3. Check matching issues/PRs/CI/device evidence, previous rejected designs, Work Reuse Gate and dependencies. Preserve source ownership and working features.
4. Prepare user problem, scoped stage ID, affected files, reuse decision, test acceptance, real-device gates, risk, rollback and next step.
5. **BEFORE writing implementation or starting a new coding stage, record PRE-WORK on its open issue.** Use Actions → Grand Forge Stage Receipts on main, or a trusted owner manual issue comment following the exact format if workflow blocked. Do not backdate. Link the issue and stage in any PR.
6. Work only on a reversible branch. Avoid unreviewed ref promotions, branch deletion, ad hoc direct main pushes, bypass of release flags or copying proprietary third-party code.

**Immediately after each material stage:**
1. Verify source/CI and hardware/product acceptance separately; record exact SHA, workflows and failures. Never invent green evidence.
2. Record POST-STAGE on the same issue (DONE_SOURCE, TEST_FAILED, BLOCKED, NEEDS_REWORK, REJECTED or SUPERSEDED). Source done != device done.
3. Update the status/next plan in #495 and reconciled LIVE_STATUS.md; append dated decisions, preserve rejection history and record current main SHA.
4. If phase failed, create new stage ID and fresh preflight before retry. If merged, verify main source actually contains the code; release remains separate.

**Before PR readiness:** a new PR targets main, includes exact Grand Forge work issue/stage lines and valid preflight + DONE_SOURCE stage-end receipts. A CI receipt gate only establishes documentation provenance. Physical Page Curl, audio TTS, RTL, accessibility, security and signing retain independent gates. Historical PRs predating rollout require manual review.

**No silent shortcuts.** If access to GitHub/action/tests/device is blocked, state BLOCKED with known facts, missing evidence and next action. Do not claim continuous autonomous development unless an actual scheduled workflow/agent executes it.

Full policy and examples: docs/roadmap/GRAND_FORGE_EXECUTION_PROTOCOL.md. Canonical tracking issue: https://github.com/hminthakil-eng/Veil-Reader/issues/495.
