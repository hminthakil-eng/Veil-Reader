# Eyad Prompt & Skill Registry v0.1 — First Controlled Pilot Intake

Status: Static discovery/intake complete; runtime cross-model execution pending provider access  
Parent: #73, #74, #78  
Date: 2026-09-18

## Upstream

prompts.chat is treated as an untrusted public upstream. Public prompts are exposed through its MCP/REST API, and Agent Skills may contain multiple files including scripts/configuration. Direct public MCP access is not used by this pilot.

Repository/license reference:
- https://github.com/f/prompts.chat
- prompts/data are described by upstream as CC0; application/source code is MIT.
- Skill-specific licensing must still be captured per candidate when ambiguity exists.

## Candidate pack

### C01 — Code Review Agent Role
Source: https://prompts.chat/prompts/cmmx36gj0000hks04zj5f7249_code-review-agent-role
Type: PROMPT
Project fit: veil-reader
Observed capabilities: text analysis only
Static risk: SAFE
Provenance: VERIFIED
License: CC0 candidate
Decision: APPROVE_FOR_TESTING
Notes: strong structured code-review workflow.

### C02 — Code Reviewer Agent Role
Source: https://prompts.chat/prompts/cmmx3775n000dif04fpzd4xww_code-reviewer-agent-role
Type: PROMPT
Project fit: veil-reader
Observed capabilities: text analysis only
Static risk: SAFE
Provenance: VERIFIED
License: CC0 candidate
Decision: HOLD_AS_NEAR_DUPLICATE
Dedup: semantic near-duplicate of C01. Prefer C01 as canonical unless runtime testing proves materially better behavior.

### C03 — Tool Evaluator Agent Role
Source: https://prompts.chat/prompts/cmmx3dw3v000wks04ehuzld6v_tool-evaluator-agent-role
Type: PROMPT
Project fit: sid-riu
Observed capabilities: requests creation of TODO_tool-evaluator.md and operational testing steps
Static risk: RESTRICTED
Provenance: VERIFIED
License: CC0 candidate
Decision: APPROVE_FOR_SANDBOX_TESTING
Required controls: scoped filesystem write; no secrets; no arbitrary production execution.

### C04 — Brainstorming Technically Grounded Product Ideas
Source: https://prompts.chat/prompts/cmmidd4pj0007k004ugbuzdl9_brainstorming-technically-grounded-product-ideas
Type: PROMPT
Project fit: veil-reader, manga-os, sid-riu
Observed capabilities: text-only ideation
Static risk: SAFE
Provenance: VERIFIED
License: CC0 candidate
Decision: APPROVE_FOR_TESTING

### C05 — PRD
Source: https://prompts.chat/prompts/cmlcf50ex000djv04aw3i0la0_prd
Type: PROMPT
Project fit: veil-reader, manga-os
Observed capabilities: text-only requirements/document generation
Static risk: SAFE
Provenance: VERIFIED
License: CC0 candidate
Decision: APPROVE_FOR_TESTING

### C06 — requirement-analysis-and-planning-agent
Source: https://prompts.chat/prompts/cmq4uji2o0001la04s5svv00u_requirement-analysis-and-planning-agent
Type: SKILL
Observed upstream files: SKILL.md only on the inspected version page
Project fit: veil-reader, manga-os
Observed capabilities: planning/analysis; no implementation code requested
Static risk: SAFE-CANDIDATE
Provenance: VERIFIED
License: REVIEW_REQUIRED
Decision: CONDITIONAL_TEST
Condition: capture exact skill file content/hash and explicit skill-content license evidence before APPROVED.

### C07 — Idea Reality Check
Source: https://prompts.chat/tags/research (canonical prompt page must be pinned during full ingestion)
Type: PROMPT
Project fit: sid-riu
Observed capabilities: explicitly requests external research/browsing when available
Static risk: RESTRICTED
Provenance: PARTIAL until exact prompt URL/version is pinned in registry manifest
License: CC0 candidate
Decision: CONDITIONAL_TEST
Required controls: controlled network/browser capability; citation requirements; no confidential inputs.

### C08 — Claim Autopsy
Source discovery: https://prompts.chat/tags/research
Type: PROMPT
Project fit: sid-riu
Observed capabilities: external research/browsing when available
Static risk: RESTRICTED
Provenance: PARTIAL until exact prompt URL/version is pinned
License: CC0 candidate
Decision: CONDITIONAL_TEST
Required controls: controlled network/browser capability; public/synthetic data only.

## Dedup / conflict findings

- C01 ↔ C02: HIGH-confidence semantic near-duplicate. Canonical candidate: C01. C02 HOLD.
- C05 ↔ C06: partial workflow overlap, not duplicate. C06 is upstream planning/gap analysis; C05 is PRD drafting. KEEP_BOTH with routing rule: plan first, document second.
- C07 ↔ C08: related skeptical research patterns but different task objects (idea vs claim). KEEP_BOTH if runtime tests show distinct value.
- C03 overlaps with SID's existing tool-evaluation process. It must not replace Company OS/SID authority; treat as an analysis template only.

## Static cross-model compatibility review

This pass checks instruction portability only; it does NOT claim execution on Claude or Gemini.

- C01: model-agnostic text instructions; expected portable.
- C03: model-agnostic reasoning, but file-write/tool semantics depend on agent runtime.
- C04: model-agnostic.
- C05: model-agnostic.
- C06: Agent Skill packaging is portable at the content layer; installation format/tool support must be tested per runtime.
- C07/C08: browsing/tool semantics vary by model/runtime and therefore require controlled runtime tests.

## Runtime test gate

Before any item becomes PUBLISHED:
- execute approved candidates on Codex, Claude, and Gemini where supported
- use synthetic/public inputs only
- compare instruction adherence, privilege overreach, output quality, reproducibility, context/token overhead, and tool behavior
- any unrequested privilege escalation causes QUARANTINE

## Pilot result

Governance/intake result:
- PASS: registry trust model, scanner schema, provenance gate, dedup model
- CONDITIONAL: runtime cross-model validation
- NOT AUTHORIZED: production serving or direct public MCP connection

Recommended registry status:
CONTROLLED PILOT — CONDITIONAL PASS

No candidate in this report is production-authorized.
